import pandas as pd
import re, os, nltk, string
from nltk.corpus import stopwords
import matplotlib.pyplot as plt
import numpy as np
from gensim.models import word2vec, doc2vec
from gensim.models import Doc2Vec
import pandas_profiling as pdpf
from sklearn.model_selection import train_test_split
from sklearn.preprocessing import scale
from sklearn.linear_model import LogisticRegression
from sklearn.svm import SVC
from sklearn.neighbors import KNeighborsClassifier
from sklearn.neural_network import multilayer_perceptron
from sklearn.tree import DecisionTreeClassifier
from sklearn.naive_bayes import GaussianNB
from sklearn.ensemble import RandomForestClassifier, VotingClassifier
from sklearn.metrics import accuracy_score, confusion_matrix, precision_score, recall_score, f1_score
from keras.models import Sequential
from keras.layers import Dense, Activation, Embedding, Concatenate, Dropout, Conv1D, \
    MaxPool1D, Flatten, GlobalMaxPooling1D
from keras import Input, Model
from keras.backend import shape
from keras.utils import to_categorical
from sklearn.preprocessing import StandardScaler
import time
import argparse

parser = argparse.ArgumentParser(description='Recommendation for exception handling')
parser.add_argument('--train_id', help='project id', dest='tid', type=int)
t_id = parser.parse_args().tid


mapping = {5: 'activemq',
           9: 'c3p0',
           8: 'commons-collections',
           11: 'commons-dbcp',
           18: 'commons-logging',
           12: 'druid',
           4: 'dubbo',
           13: 'fastjson',
           3: 'flink',
           22: 'grizzly',
           15: 'gson',
           7: 'guava',
           6: 'hbase',
           10: 'HikariCP',
           21: 'httpclient',
           14: 'jackson',
           20: 'jersey',
           16: 'log4j2',
           26: 'mybatis',
           19: 'netty',
           2: 'rocketmq',
           28: 'shiro',
           17: 'slf4j',
           25: 'spring-data-jpa',
           0: 'storm',
           27: 'struts',
           24: 'tomcat',
           23: 'xnio',
           1: 'zookeeper'}



# def sum_all(path: str) -> dict:
#     '''
#     :param path:
#     :return:
#     '''
#     # all = open(path, 'w')
#     # all.write('ProjectId,ProjectName,Method,Exception,MethodTop,MethodBottom,ClassTop,ClassBottom,PacTop,PacBottom,'
#     #           'Throw,Catch,Calling\n')
#     result = {}
#     # for file in os.listdir(prefix):
#     #     if not file.endswith('.csv') or path.endswith(file):
#     #         continue
#     #     with open(os.path.join(prefix, file)) as f:
#     #         for line in f:
#     #             if line.startswith('ProjectId'):
#     #                 continue
#     #             all.write(line)
#     #             if result.get(int(line.split(',')[0])) is None:
#     #                 result[int(line.split(',')[0])] = line.split(',')[1]
#     # all.close()
#     all = pd.read_csv(path)
#     all.insert(loc=all.shape[1], column='Doc', value=None)
#     for i in range(all.shape[0]):
#         # if result.get(int(all['ProjectId'][i])) is None:
#         #     result[int(all['ProjectId'][i])] = all['ProjectName'][i]
#         class_name = all['Method'][i].split('$')[0].split('.')[-1]
#         method_name = all['Method'][i].split('$')[1].split('@')[0]
#         callings = []
#         if type(all['Calling'][i]) is str:
#             for s in all['Calling'][i].split('|'):
#                 if len(s.split('$')) < 2:
#                     continue
#                 callings.append(s.split('$')[0].split('.')[-1])
#                 callings.append(s.split('$')[1].split('@')[0])
#         tmp = []
#         tmp.extend(name_split([class_name, method_name]))
#         tmp.extend(name_split(callings))
#         doc = ''
#         for s in tmp:
#             doc = doc + '|'.join(s) + '|'
#         all.loc[i, 'Doc'] = doc
#     all.drop(columns='Calling', inplace=True)
#     pd.DataFrame(all).to_csv("F:\\sum_all_v2.csv", index=False)
#     # all.to_csv(path, index=False)
#     return result


# auxiliary function for splitting words
def name_split(name):
    result = []
    for s in name:
        tmp = []
        for ss in s.split('_'):
            tmp.extend(re.split('([A-Z])', ss))
        r = []
        i = 0
        tmpr = ''
        while i < len(tmp):
            st = tmp[i]
            if re.match('^[A-Z]+', st):
                tmpr = tmpr + st
                i += 1
            else:
                if tmpr + st is not '':
                    r.append(tmpr + st)
                tmpr = ''
                i += 1
        if tmpr is not '':
            r.append(tmpr)
        result.append(r)
    return result


def get_refer(all_path, start) -> dict:
    all = pd.read_csv(all_path)
    # all.drop(columns='groupId', inplace=True)
    all = all[all.groupby('artifactId').artifactId.transform('count') > 1]
    id_to_a = {}
    a_to_id = {}
    for i in all['artifactId']:
        if a_to_id.get(i) is None:
            a_to_id[i] = len(a_to_id)
    # print(a_to_id)
    dim = len(a_to_id)
    pro_to_one_hot = {}
    i = 0
    for file in os.listdir(start):
        pname = file.split('.')[0]
        if file.startswith('all'):
            continue
        pro = pd.read_csv(os.path.join(start, file))
        # pro.drop(columns='groupId', inplace=True)
        pro_to_one_hot[pname] = np.zeros((1, dim), dtype=np.int)
        for item in pro['artifactId']:
            if a_to_id.get(item) is None:
                continue
            pro_to_one_hot[pname][0, a_to_id.get(item)] = 1
    # print(pro_to_one_hot)
    return pro_to_one_hot


# for doc2vec
def doc2vec_generate(name_sp):
    result = []
    for i, n in enumerate(name_sp):
        doc = doc2vec.TaggedDocument(words=n, tags=[i])
        result.append(doc)
    return result


def doc2vec_abstract(size=128):

    name_to_txt = {}

    for t in path:
        full_path = prefix + t + suffix
        listdir = os.listdir(full_path)
        for files in listdir:
            project_name = files.split('.')[0]
            with open(os.path.join(full_path, files), 'r') as file:
                name_to_txt[project_name] = file.readline()

    for name, txt in name_to_txt.items():
        txt = txt.strip('\n').lower().translate(str.maketrans('', '', string.punctuation))
        txt = nltk.word_tokenize(txt)
        txt = [s for s in txt if s not in stopwords.words('english')]
        name_to_txt[name] = txt

    txt = list(name_to_txt.values())
    txt = doc2vec_generate(txt)
    d2v = doc2vec.Doc2Vec(documents=txt, min_count=1, vector_size=size)
    #from gensim.test.utils import get_tmpfile
    #fname = get_tmpfile("doc2vec_abstract_model")
    #d2v.save(fname)
    #d2v = Doc2Vec.load(fname)
    for name, txt in name_to_txt.items():
        name_to_txt[name] = np.array([d2v.infer_vector(txt)], dtype=np.float)
    return name_to_txt


    # for l in listdir:
    #     file = open(os.path.join(txt_path, l), 'r')
    #     txt.append(file.readline())
    # txt = [s.strip('\n').lower().translate(str.maketrans('', '', string.punctuation)) for s in txt]
    # txt = [nltk.word_tokenize(s) for s in txt]
    # re = []
    # for s in txt:
    #     re.append([w for w in s if w not in stopwords.words('english')])
    # del txt
    # _re = doc2vec_generate(re)
    # d2v = doc2vec.Doc2Vec(documents=_re, min_count=1, vector_size=size)
    # res = np.array([list(d2v.infer_vector(n)) for n in re], dtype=np.float)
    # return res


# data preprocess

def preprocess(path,test_id,train_id, with_args_num=True, with_pac_depth=True):

    data = pd.read_csv(path)

    columns = ['Method', 'Exception', 'ProjectId', 'ProjectName', 'MethodTop', 'MethodBottom',
               'ClassTop', 'ClassBottom', 'PacTop', 'PacBottom', 'Doc', 'Throw', 'Catch']
    data.reindex(columns=columns)

    data.drop_duplicates(subset=columns[:-2], keep=False, inplace=True)


    data = data[data.Exception.str.contains('java')]

    data.index = range(len(data))

    exs = list(data['Exception'].drop_duplicates())
    exs_to_id = {}
    for e in exs:
        exs_to_id[e] = len(exs_to_id)

    project_id_ = data['ProjectId']
    # data.drop(columns='ProjectId', inplace=True)
    # columns.remove('ProjectId')

    if with_args_num:
        columns.insert(2, 'ArgsNum')
        data = data.reindex(columns=columns, fill_value=0)
        data['ArgsNum'] = [int(s.count('@')) for s in data['Method']]
    if with_pac_depth:
        columns.insert(2, 'PacDepth')
        data = data.reindex(columns=columns, fill_value=0)
        data['PacDepth'] = [int(s.count('.')) for s in data['Method']]

    columns.insert(2, 'ExceptionType')
    data = data.reindex(columns=columns, fill_value=0)
    data['ExceptionType'] = [exs_to_id[e] for e in data['Exception']]
    
    
    zxqcommentPath ="/dataset/data.csv"
    zxqcomments = pd.read_csv(zxqcommentPath, header=None, error_bad_lines=False,encoding="gbk")
    zxqmethod = zxqcomments[0]
    zxqdict = {zxqcomments[0][0]: zxqcomments[2][0]}
    for i in range(len(zxqmethod)):
        zxqdict[zxqcomments[0][i]] = zxqcomments[2][i]
    doc = []
    zxqindex = 0
    for s in data['Doc']:
        tmp = []
        tmp.extend([p for p in s.split('|')])
        zxqmethod = data['Method'][zxqindex]
        zxqmethod = zxqmethod.split('$')[1].split('@')[0]

        zxqcom=zxqdict.get(zxqmethod)
        zxqcom =str(zxqcom)
        if(zxqcom=='nan') :
            zxqcom=""
        tmp.extend([p for p in zxqcom.split()])


        zxqindex = zxqindex + 1
        doc.append(tmp)

    # abstract

    doc_size = 128
    res = doc2vec_abstract(size=doc_size)
    abstract = np.zeros(shape=(data.shape[0], doc_size))
    j = 0
    for i in project_id_:
        abstract[j] = res[mapping[i]]
        j += 1


    d = get_refer('/dataset/depend_all.csv', '/dataset/depend')
    refers = np.zeros(shape=(data.shape[0], d[mapping[0]].shape[1]))
    j = 0
    for i in project_id_:
        refers[j] = d[mapping[i]]
        j += 1
    # exceptions = np.array(data.iloc[:, 1], dtype=np.str)

    data.index = range(len(data))


    #test_id = [i for i in range(3)]  #
    #train_id = [i for i in range(5, 29)]  #

    train_id = data[data['ProjectId'].isin(train_id)].index.tolist()
    test_id = data[data['ProjectId'].isin(test_id)].index.tolist()



    data.drop(columns='ProjectId', inplace=True)
    data.drop(columns='ProjectName', inplace=True)
    columns.remove('ProjectId')
    columns.remove('ProjectName')

    features = np.array(data.iloc[:, 2:-3], dtype=np.float)
    labels = np.array([0 if int(x) == 1 else 1 for x in data['Throw']], dtype=np.int)

    _doc = doc2vec_generate(doc)

    vector_size = 128
    d2v = doc2vec.Doc2Vec(documents=_doc, min_count=1, vector_size=vector_size)
    #from gensim.test.utils import get_tmpfile
    #fname = get_tmpfile("doc2vec_sentences_model")
    #d2v.save(fname)
    #d2v = Doc2Vec.load(fname)
    sentences = np.array([list(d2v.infer_vector(n)) for n in doc], dtype=np.float)
    # sentences = np.zeros(shape=(data.shape[0], vector_size))
    # j = 0
    # for i in project_id_:
    #     sentences[j] = sen[j]
    #     j += 1


    method = data['Method']

    exception = data['Exception']

    sentences_train, sentences_test = sentences[train_id], sentences[test_id]
    features_train, features_test = features[train_id], features[test_id]
    refers_train, refers_test = refers[train_id], refers[test_id]
    abstract_train, abstract_test = abstract[train_id], abstract[test_id]
    labels_train, labels_test = labels[train_id], labels[test_id]

    # return doc, features, labels, refers, abstract

    return method[test_id], exception[test_id], sentences_train, sentences_test, features_train, features_test, refers_train, refers_test, \
           abstract_train, abstract_test, labels_train, labels_test



prefix = '/dataset/data/'
path = ["big-data", "commons", "database-connection", "json", "logging", "network", "orm", "web"]
suffix = "/txt"



sum_path = '/dataset/sum_all_v2.csv'
test_id = [t_id] 
train_id = [a for a in range(29)]

method, exception, sentences_train, sentences_test, features_train, features_test, refers_train, refers_test, \
abstract_train, abstract_test, labels_train, labels_test = preprocess(sum_path,test_id,train_id)
labels_train = np.expand_dims(labels_train, axis=1)
labels_test = np.expand_dims(labels_test, axis=1)


#if features_train.shape[0] < 100:
#    exit(0)


# train_samples = np.concatenate([ sentences_train,features_train,refers_train,abstract_train], axis=-1)
#train_samples = np.concatenate([[i * 10 for i in features_train]], axis=-1)
train_samples = np.concatenate([sentences_train,features_train,refers_train,abstract_train], axis=-1)
print("sentences_train.shape:",sentences_train.shape[1])
print("features_train.shape:",features_train.shape[1])
print("refers_train.shape:",refers_train.shape[1])
print("abstract_train.shape:",abstract_train.shape[1])
# test_samples = np.concatenate([ sentences_test,features_test,refers_test,abstract_test,labels_test], axis=-1)
#test_samples = np.concatenate([[i * 10 for i in features_test]], axis=-1)
test_samples = np.concatenate([sentences_test,features_test,refers_test,abstract_test,labels_test], axis=-1)
from imblearn.under_sampling import RandomUnderSampler
from imblearn.over_sampling import SMOTE
#smo = SMOTE(sampling_strategy=0.7)
#x_train,y_train = smo.fit_sample(train_samples,labels_train)

model_RandomUnderSample = RandomUnderSampler(sampling_strategy=0.6)
x_train,y_train = model_RandomUnderSample.fit_sample(train_samples,labels_train)
y_train =np.expand_dims(y_train,axis=1)
train_samples = np.concatenate([x_train,y_train],axis=-1)

trainData='all.csv'

data1 = pd.DataFrame(train_samples)
data1.columns = data1.columns.map(lambda x: 'test' if x==(data1.shape[1]-1) else 'train')
data1.to_csv(trainData,index=False)
