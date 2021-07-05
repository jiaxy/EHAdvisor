import os
from collections import OrderedDict
from typing import Optional, Dict, List, Iterable

import jpype
import numpy as np
import pandas as pd
from autogluon.tabular import TabularPrediction, TabularPredictor
from gensim.models.doc2vec import Doc2Vec

from . import config_parser, nlp, pre_generate, deserializer
from .data_structure import ProjectFeature, Method, ChainEntry, MethodFeature, PositionFeature


def abstract_to_tokens(project_folder: str, readme_path: Optional[str]) -> List[str]:
    if readme_path is None:
        # use project folder name as abstract
        folder_name = os.path.basename(project_folder)
        tokens = nlp.text_to_tokens(folder_name)
    else:
        with open(readme_path, encoding='utf-8') as readme_file:
            tokens = nlp.lines_to_tokens(readme_file.read().splitlines())
    return tokens


def train_abstract_model(project_folder: str,
                         readme_path: Optional[str],
                         vector_size: int,
                         min_count: int) -> Doc2Vec:
    tokens = abstract_to_tokens(project_folder, readme_path)
    return nlp.train_model_by_tokens([tokens], vector_size=vector_size, min_count=min_count)


def make_abstract_vec(project_folder: str, readme_path: Optional[str],
                      model: Doc2Vec) -> np.ndarray:
    tokens = abstract_to_tokens(project_folder, readme_path)
    return model.infer_vector(tokens)


def make_dependencies_vec(project_folder: str):
    """build dependencies bit vector(multi-hot encoding)"""
    artifacts = {dep.artifact_id for dep in config_parser.dependencies_in_path(project_folder)}
    vec_size = len(pre_generate.COMPLETE_DEPENDENCIES)
    dep_bit_vec = np.zeros(shape=vec_size)
    for i, artifact in enumerate(pre_generate.COMPLETE_DEPENDENCIES):
        if artifact in artifacts:
            dep_bit_vec[i] = 1
    return dep_bit_vec


def train_method_docs_model(features: Iterable[MethodFeature],
                            vector_size: int,
                            min_count: int) -> Doc2Vec:
    return nlp.train_model_by_tokens(
        map(lambda feat: feat.docs, features),
        vector_size=vector_size, min_count=min_count
    )


def read_comments(comments_csv_path: str) -> Dict[str, List[str]]:
    comments_df: pd.DataFrame = pd.read_csv(
        comments_csv_path, header=None, error_bad_lines=False, encoding='gbk')
    comments_df.columns = ['method_name', 'useless1', 'comment', 'useless2']
    method_comments: Dict[str, List[str]] = {}
    for method, comment in zip(comments_df['method_name'], comments_df['comment']):
        if method not in method_comments:
            method_comments[method] = []
        if pd.notna(comment):
            method_comments[method].extend(comment.split())
    return method_comments


def method_features_from_df(feature_df: pd.DataFrame) -> Dict[Method, MethodFeature]:
    columns = ['Method', 'Exception', 'ProjectId', 'ProjectName', 'MethodTop', 'MethodBottom',
               'ClassTop', 'ClassBottom', 'PacTop', 'PacBottom', 'Throw', 'Catch', 'Calling']
    # 去除重复，即对同一个异常同时catch和throw
    feature_df.drop_duplicates(subset=columns[:-3], keep=False, inplace=True)
    # 只保留 JDK 异常
    feature_df = feature_df.loc[feature_df['Exception'].str.contains('java')]
    # FIXME exception id 的获取是错的
    exception_id_dict = {e: i for i, e in enumerate(set(feature_df['Exception']))}
    feat_dict: Dict[Method, MethodFeature] = {}
    for index, row in feature_df.iterrows():
        method = deserializer.method_from_txt(row['Method'])
        feat = MethodFeature(method)
        feat.exception_id = exception_id_dict[row['Exception']]
        feat.throw = False if int(row['Throw']) == 0 else True
        feat_dict[method] = feat
    return feat_dict


def complete_method_features(method_features: Dict[Method, MethodFeature],
                             graph: Dict[Method, List[Method]],
                             comment_dict: Dict[str, List[str]]):
    for method in graph:
        if method not in method_features:
            method_features[method] = MethodFeature(method)
    for method in method_features:
        if method.method_name in comment_dict:
            method_features[method].comments = comment_dict[method.method_name]


def load_predictor(model_directory: str) -> TabularPredictor:
    predictor = TabularPrediction.load(model_directory)
    predictor.save_space()
    predictor.delete_models(models_to_keep='best', dry_run=False)
    return predictor


def method_row_vec(method_feat: MethodFeature,
                   source_feat: MethodFeature,
                   pos_feat: PositionFeature,
                   proj_feat: ProjectFeature,
                   model: Doc2Vec):
    return np.concatenate([
        model.infer_vector(method_feat.docs),
        [source_feat.exception_id,
         method_feat.package_depth,
         method_feat.param_num],
        [pos_feat.method_top,
         pos_feat.method_bottom,
         pos_feat.class_top,
         pos_feat.class_bottom,
         pos_feat.package_top,
         pos_feat.package_bottom],
        proj_feat.dependencies_vec,
        proj_feat.abstract_vec,
        np.zeros(shape=(1,))  # FIXME 不知道为什么要加个 0 在这里
    ])


def make_pos_features(chain: List[Method]) -> Dict[Method, PositionFeature]:
    # use ordered dicts as ordered sets
    # multi-dict data structures
    cls_methods: Dict[str, OrderedDict[Method, None]] = {}
    pkg_classes: Dict[str, OrderedDict[str, None]] = {}
    package_set: OrderedDict[str, None] = OrderedDict()
    for method in chain:
        class_name = method.class_name
        if class_name not in cls_methods:  # ensure in dict
            cls_methods[class_name] = OrderedDict()
        if method not in cls_methods[class_name]:  # insert into set
            cls_methods[class_name][method] = None
        package = method.package
        if package not in pkg_classes:  # ensure in dict
            pkg_classes[package] = OrderedDict()
        if class_name not in pkg_classes[package]:  # insert into set
            pkg_classes[package][class_name] = None
        if package not in package_set:  # insert into set
            package_set[package] = None
    method_pos: Dict[Method, PositionFeature] = {method: PositionFeature() for method in chain}
    # method-top, method-bottom
    for class_name, method_set in cls_methods.items():
        for i, method in enumerate(method_set):
            method_pos[method].method_top = i
            method_pos[method].method_bottom = len(method_set) - 1 - i
    # class-top, class-bottom
    cls_top_dict: Dict[str, int] = {}
    cls_bottom_dict: Dict[str, int] = {}
    for package, class_set in pkg_classes.items():
        for i, class_name in enumerate(class_set):
            cls_top_dict[class_name] = i
            cls_bottom_dict[class_name] = len(class_set) - 1 - i
    for method in method_pos:
        method_pos[method].class_top = cls_top_dict[method.class_name]
        method_pos[method].class_bottom = cls_bottom_dict[method.class_name]
    # package-top, package-bottom
    pkg_top_dict: Dict[str, int] = {}
    pkg_bottom_dict: Dict[str, int] = {}
    for i, package in enumerate(package_set):
        pkg_top_dict[package] = i
        pkg_bottom_dict[package] = len(package_set) - 1 - i
    for method in method_pos:
        method_pos[method].package_top = pkg_top_dict[method.package]
        method_pos[method].package_bottom = pkg_bottom_dict[method.package]
    return method_pos


def chain_to_df(chain: List[Method],
                source: Method,
                method_features: Dict[Method, MethodFeature],
                project_feature: ProjectFeature,
                d2v_model: Doc2Vec) -> pd.DataFrame:
    assert len(chain) > 0
    method_pos = make_pos_features(chain)
    rows = []
    for method in chain:
        row_vec = method_row_vec(
            method_feat=method_features[method],
            source_feat=method_features[source],
            pos_feat=method_pos[method],
            proj_feat=project_feature,
            model=d2v_model)
        rows.append(row_vec)
    df = pd.DataFrame(rows)
    df_col_num = df.shape[1]
    headers = ['train']
    headers.extend([f'train.{i + 1}' for i in range(df_col_num - 2)])
    headers.append('test')
    df.columns = headers
    return df


def predict_chain(chain: List[Method],
                  source: Method,
                  method_features: Dict[Method, MethodFeature],
                  project_feature: ProjectFeature,
                  d2v_model: Doc2Vec,
                  predictor: TabularPredictor) -> List[ChainEntry]:
    if len(chain) == 0:
        return []
    df = chain_to_df(
        chain=chain,
        source=source,
        method_features=method_features,
        project_feature=project_feature,
        d2v_model=d2v_model
    )
    probabilities: np.ndarray = predictor.predict_proba(df)
    result = [ChainEntry(method, probabilities[i]) for i, method in enumerate(chain)]
    result.append(ChainEntry(None, 0.5))
    return result


def predict_chains(chains: Iterable[List[Method]],
                   sources: Iterable[Method],
                   method_feats: Dict[Method, MethodFeature],
                   proj_feat: ProjectFeature,
                   d2v_model: Doc2Vec,
                   predictor: TabularPredictor) -> List[List[ChainEntry]]:
    df_list: List[pd.DataFrame] = []
    for chain, source in zip(chains, sources):
        if len(chain) == 0:
            continue
        df = chain_to_df(
            chain=chain,
            source=source,
            method_features=method_feats,
            project_feature=proj_feat,
            d2v_model=d2v_model
        )
        df_list.append(df)
    large_df = pd.concat(df_list)
    prob: np.ndarray = predictor.predict_proba(large_df)
    results: List[List[ChainEntry]] = []
    cur = 0  # row cursor of large df
    for chain in chains:
        chain_prob: List[ChainEntry] = []
        for method in chain:
            chain_prob.append(ChainEntry(method, prob[cur]))
            cur += 1
        results.append(chain_prob)
    assert cur == len(large_df)
    return results


def call_graph_from_link(content: Iterable[str]) -> Dict[Method, List[Method]]:
    from .graph import build_call_graph
    from .deserializer import method_chain_from_txt
    return build_call_graph(map(method_chain_from_txt, content))


def run_jar(project_folder: str, get_features_jar: str, jvm_path: Optional[str] = None):
    """调用 getFeatures.jar 生成 features.csv, comments.csv, link.csv"""
    if jvm_path is None:
        jvm_path = jpype.getDefaultJVMPath()
    abs_jar_path = os.path.abspath(get_features_jar)
    jpype.startJVM(jvm_path, '-ea', f'-Djava.class.path={abs_jar_path}')
    jpype.addClassPath(abs_jar_path)  # 手动添加 classpath
    java_class = jpype.JClass('com.tcl.App')
    abs_proj_path = os.path.abspath(project_folder)
    java_class.getFeatures(abs_proj_path)
    java_class.getComments(abs_proj_path)
    jpype.shutdownJVM()
