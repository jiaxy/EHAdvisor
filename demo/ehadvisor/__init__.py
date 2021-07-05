from typing import List, Optional

import pandas as pd

from . import core, graph, nlp
from .data_structure import Method, ChainEntry, ProjectFeature

__all__ = ['EHAdvisor']


class EHAdvisor:
    def __init__(self,
                 project_folder: str,
                 abstract_path: Optional[str],
                 predictor_path: str,
                 get_features_jar_path: str,
                 features_csv_path: str,
                 comments_csv_path: str,
                 link_txt_path: str,
                 abstract_doc2vec_path: str,
                 method_docs_doc2vec_path: str,
                 abstract_vec_size=128,
                 method_doc_vec_size=128):
        self.__proj_folder = project_folder
        self.__abs_path = abstract_path
        self.__jar_path = get_features_jar_path
        self.__feat_csv = features_csv_path
        self.__cmt_csv = comments_csv_path
        self.__link_txt = link_txt_path
        self.___abs_vec_size = abstract_vec_size
        self.__mdoc_vec_size = method_doc_vec_size
        print('__init__')
        # load autogluon model
        self.__predictor = core.load_predictor(predictor_path)
        print('autogluon model loaded')
        # construct graph and method feat
        self.__graph = None
        self.__method_feats = None
        self.update_methods()
        print('method feat ok')
        # load doc2vec models
        from gensim.models.doc2vec import Doc2Vec
        self.__abs_model = Doc2Vec.load(abstract_doc2vec_path)
        self.__mdoc_model = Doc2Vec.load(method_docs_doc2vec_path)
        print('doc2vec model loaded')
        # construct project feature
        self.__proj_feat = ProjectFeature()
        self.update_abstract()
        self.update_dependencies()
        print('project feat ok')

    def update_methods(self):
        """update call graph and method feat dict"""
        core.run_jar(self.__proj_folder, self.__jar_path)  # run jar
        # construct call graph
        with open(self.__link_txt, encoding='utf-8') as link_txt:
            self.__graph = core.call_graph_from_link(
                link_txt.read().splitlines())
        # construct method feature dict
        self.__method_feats = core.method_features_from_df(
            pd.read_csv(self.__feat_csv))
        core.complete_method_features(
            method_features=self.__method_feats,
            graph=self.__graph,
            comment_dict=core.read_comments(self.__cmt_csv)
        )

    def update_abstract(self, new_abs_path: Optional[str] = None):
        if new_abs_path is not None:
            self.__abs_path = new_abs_path
        self.__proj_feat.abstract_vec = core.make_abstract_vec(
            project_folder=self.__proj_folder,
            readme_path=self.__abs_path,
            model=self.__abs_model
        )

    def update_dependencies(self):
        self.__proj_feat.dependencies_vec = core.make_dependencies_vec(
            self.__proj_folder)

    def query(self, exception_source: Method) -> List[List[ChainEntry]]:
        # TODO check the method is ex-src or not
        if exception_source not in self.__method_feats:
            return []
        chains: List[List[Method]] = graph.chains_from_source(
            self.__graph, exception_source)
        results: List[List[ChainEntry]] = []
        for chain in chains:
            prediction = core.predict_chain(
                chain=chain,
                source=exception_source,
                method_features=self.__method_feats,
                project_feature=self.__proj_feat,
                d2v_model=self.__mdoc_model,
                predictor=self.__predictor
            )
            results.append(prediction)
        return results

    def query_all(self) -> List[List[ChainEntry]]:
        chains: List[List[Method]] = []
        sources: List[Method] = []
        for source in self.__method_feats:
            for chain in graph.chains_from_source(self.__graph, source):
                chains.append(chain)
                sources.append(source)
        results = core.predict_chains(
            chains=chains,
            sources=sources,
            method_feats=self.__method_feats,
            proj_feat=self.__proj_feat,
            d2v_model=self.__mdoc_model,
            predictor=self.__predictor
        )
        return results
