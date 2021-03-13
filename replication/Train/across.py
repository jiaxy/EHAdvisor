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


if os.path.exists('agModels-predictClass' + mapping[t_id]):
    exit(0)



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
    
    
    zxqcommentPath ="../dataset/data.csv"
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

        # tmp.sort()
        zxqindex = zxqindex + 1
        doc.append(tmp)


    doc_size = 128
    res = doc2vec_abstract(size=doc_size)
    abstract = np.zeros(shape=(data.shape[0], doc_size))
    j = 0
    for i in project_id_:
        abstract[j] = res[mapping[i]]
        j += 1

    d = get_refer('../dataset/depend_all.csv', '../dataset/depend')
    refers = np.zeros(shape=(data.shape[0], d[mapping[0]].shape[1]))
    j = 0
    for i in project_id_:
        refers[j] = d[mapping[i]]
        j += 1
    # exceptions = np.array(data.iloc[:, 1], dtype=np.str)

    data.index = range(len(data))



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


# build and train
def build_conv_model(sentences_train, sentences_test, features_train, features_test,
                     labels_train, labels_test, refers_train, refers_test,
                     abstract_train, abstract_test, standardized=True) -> Model:
    # if standardized:
    #     ss = StandardScaler()
    #     features_train = ss.fit_transform(features_train)
    #     sentences_train = ss.fit_transform(sentences_train)
    #     abstract_train = ss.fit_transform(abstract_train)
    #
    #     features_test = ss.fit_transform(features_test)
    #     sentences_test = ss.fit_transform(sentences_test)
    #     abstract_test = ss.fit_transform(abstract_test)

    # features_train = scale(features_train)
    # sentences_train = scale(sentences_train)
    # abstract_train = scale(abstract_train)
    # features_test = scale(features_test)
    # sentences_test = scale(sentences_test)
    # abstract_test = scale(abstract_test)


    sentences_train = np.expand_dims(sentences_train, axis=2)
    features_train = np.expand_dims(features_train, axis=2)
    refers_train = np.expand_dims(refers_train, axis=2)

    sentences_test = np.expand_dims(sentences_test, axis=2)
    features_test = np.expand_dims(features_test, axis=2)
    refers_test = np.expand_dims(refers_test, axis=2)

    # input1
    X_train_1, X_test_1 = sentences_train, sentences_test
    # input2
    X_train_2, X_test_2 = features_train, features_test
    # input3
    X_train_3, X_test_3 = refers_train, refers_test
    # input4
    X_train_4, X_test_4 = abstract_train, abstract_test

    y_train = to_categorical(labels_train, num_classes=2)
    y_test = to_categorical(labels_test, num_classes=2)
    y_test_origin = labels_test


    kernel_size = 5

    inputs1 = Input(shape=(sentences_train.shape[1], 1))
    inputs1_conv = Conv1D(filters=64, kernel_size=kernel_size, activation='relu')(inputs1)
    # inputs1_pool = GlobalMaxPooling1D()(inputs1_conv)

    inputs1_p = MaxPool1D(pool_size=5)(inputs1_conv)
    inputs1_pool = Flatten()(inputs1_p)

    inputs1_d1 = Dense(64, activation='relu')(inputs1_pool)
    inputs1_d1 = Dense(32, activation='relu')(inputs1_d1)
    inputs1_d2 = Dense(32, activation='relu')(inputs1_d1)
    inputs1_d3 = Dense(16, activation='relu')(inputs1_d2)

    inputs2 = Input(shape=(features_train.shape[1], 1))
    inputs2_conv = Conv1D(filters=16, kernel_size=kernel_size, activation='relu')(inputs2)
    # inputs2_pool = GlobalMaxPooling1D()(inputs2_conv)
    inputs2_p = MaxPool1D(pool_size=3)(inputs2_conv)
    inputs2_pool = Flatten()(inputs2_p)
    inputs2_d1 = Dense(64, activation='relu')(inputs2_pool)
    inputs2_d2 = Dense(32, activation='relu')(inputs2_d1)
    inputs2_d2 = Dense(32, activation='relu')(inputs2_d2)
    inputs2_d3 = Dense(16, activation='relu')(inputs2_d2)

    # reference
    inputs3 = Input(shape=(refers_train.shape[1], 1))
    inputs3_conv = Conv1D(filters=16, kernel_size=kernel_size, activation='relu')(inputs3)
    inputs3_p = MaxPool1D(pool_size=5)(inputs3_conv)
    inputs3_pool = Flatten()(inputs3_p)
    inputs3_d = Dense(64, activation='relu')(inputs3_pool)
    inputs3_d1 = Dense(32, activation='relu')(inputs3_d)
    inputs3_d2 = Dense(32, activation='relu')(inputs3_d1)
    inputs3_d3 = Dense(16, activation='relu')(inputs3_d2)

    # abstract
    inputs4 = Input(shape=(abstract_train.shape[1], ))
    inputs4_d = Dense(64, activation='relu')(inputs4)
    inputs4_d1 = Dense(32, activation='relu')(inputs4_d)
    inputs4_d2 = Dense(16, activation='relu')(inputs4_d1)

    # conca = Concatenate(axis=1)([inputs1_d3, inputs2_d3])
    # conca = Concatenate(axis=1)([inputs1_d3, inputs2_d3, inputs3_d3])
    conca = Concatenate(axis=1)([inputs1_d3, inputs2_d3, inputs3_d3, inputs4_d2])
    output = Dense(2, activation='softmax')(conca)

    # model = Model(inputs=[inputs1, inputs2], outputs=[output])
    # model = Model(inputs=[inputs1, inputs2, inputs3], outputs=[output])
    model = Model(inputs=[inputs1, inputs2, inputs3, inputs4], outputs=[output])
    model.compile(optimizer='rmsprop',
                  loss='binary_crossentropy',
                  metrics=['acc'])
    # model.fit([X_train_1, X_train_2], y_train, epochs=20,
    #           validation_data=([X_test_1, X_test_2], y_test))
    # model.fit([X_train_1, X_train_2, X_train_3], y_train, epochs=20,
    #           validation_data=([X_test_1, X_test_2, X_test_3], y_test))
    model.fit([X_train_1, X_train_2, X_train_3, X_train_4], y_train, epochs=10, batch_size=512,
              validation_data=([X_test_1, X_test_2, X_test_3, X_test_4], y_test))
    # y_pred = [0 if a[0] > a[1] else 1 for a in model.predict([X_test_1, X_test_2])]
    # y_pred = [0 if a[0] > a[1] else 1 for a in model.predict([X_test_1, X_test_2, X_test_3])]
    y_pred = [0 if a[0] > a[1] else 1 for a in model.predict([X_test_1, X_test_2, X_test_3, X_test_4])]
    print('accuracy: ', accuracy_score(y_test_origin, y_pred))
    print('precision: ', precision_score(y_test_origin, y_pred))
    print('recall: ', recall_score(y_test_origin, y_pred))
    print('f1: ', f1_score(y_test_origin, y_pred))
    print(confusion_matrix(y_test_origin, y_pred))
    return model


prefix = '../dataset/data/'
path = ["big-data", "commons", "database-connection", "json", "logging", "network", "orm", "web"]
suffix = "/txt"



sum_path = '../dataset/sum_all_v2.csv'
test_id = [t_id] 
train_id = [a for a in range(29)]
train_id.remove(t_id)
method, exception, sentences_train, sentences_test, features_train, features_test, refers_train, refers_test, \
abstract_train, abstract_test, labels_train, labels_test = preprocess(sum_path,test_id,train_id)
labels_train = np.expand_dims(labels_train, axis=1)
labels_test = np.expand_dims(labels_test, axis=1)


#if features_train.shape[0] < 100:
#    exit(0)


# train_samples = np.concatenate([ sentences_train,features_train,refers_train,abstract_train], axis=-1)
#train_samples = np.concatenate([[i * 10 for i in features_train]], axis=-1)
train_samples = np.concatenate([sentences_train,features_train,refers_train,abstract_train], axis=-1)
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



trainData='train7_14.csv'
testData ='test7_14.csv'
data1 = pd.DataFrame(train_samples)
data1.columns = data1.columns.map(lambda x: 'test' if x==(data1.shape[1]-1) else 'train')
data1.to_csv(trainData,index=False)

data1 = pd.DataFrame(test_samples)
data1.columns = data1.columns.map(lambda x: 'test' if x==(data1.shape[1]-1) else 'train')
data1.to_csv(testData,index=False)

import pandas as pd
import autogluon as ag
from autogluon import TabularPrediction as task
from sklearn.metrics import accuracy_score,confusion_matrix,precision_score,recall_score,f1_score

#autogluon
label_column = 'test'
dir = 'agModels-predictClass_KXM'
train_data = task.Dataset(file_path=trainData)
test_data = task.Dataset(file_path= testData)

# TODO
predictor = task.fit(train_data=train_data, label='test', output_directory=dir,auto_stack=True,time_limits = 1800)
results = predictor.fit_summary()


y_test = test_data[label_column]  # values to predict
test_data_nolab = test_data.drop(labels=[label_column],axis=1) # delete label column to prove we're not cheating
#print(test_data_nolab.head())

predictor = task.load(dir)
y_pred = predictor.predict(test_data_nolab)
print("acorss：",mapping[t_id])
print("Predictions:  ", y_pred)
print(confusion_matrix(y_test,y_pred))
print('accuracy: ', accuracy_score(y_test,y_pred))
print('precision: ', precision_score(y_test,y_pred))
print('recall: ', recall_score(y_test, y_pred))
print('f1: ', f1_score(y_test,y_pred))

print("=x=x=x=x=x=x=x=x=x=x=x=x=x=x=x=x=x=x" + 
"=x=x=x=x=x=x=x=x=x=x=x=x=x=x=x=x=x=x=x=x=x=x=x=x=x=x=x=x=x=x=x=x=x=x=x=x=x=x=x=x=x=x=x=x=x=x")

call_link_path = '../dataset/calling-link-1'


recommendation_path = '../dataset/resultsKXM'

method_to_link = {}

links = open(os.path.join(call_link_path, mapping[t_id] + '.txt'), 'r')
for line in links:
    line = line.strip()
    if line:
        methods = line.split('->')
        for m in methods:
            if m:
                method_to_link[m] = line
                    
links.close()


method = method.tolist()
exception = exception.tolist()


length = features_train.shape[1] + sentences_train.shape[1] + refers_train.shape[1] + abstract_train.shape[1]
headers = ['train']
headers.extend(['train.' + str(i) for i in range(1, length)])
headers.append('test')



target = open(os.path.join(recommendation_path, mapping[t_id] + '.txt'), 'w')


index = 0

all_key = []
all_value = []
all_str = []
for i in range(features_test.shape[0]):
    s_train = {}
    # new_features(method[i], s_train)

    # @profile
    if method_to_link.get(method[i]) is not None:
        linking = method_to_link[method[i]].strip()
        split = linking.split('->')
        for idx, train_method in enumerate(split):
            if train_method:
                new_fea = np.array(features_test[i], copy=True)
                new_fea[1] = train_method.count('.')
                new_fea[2] = train_method.count('@')
                new_fea[3] = idx
                new_fea[4] = len(split) - idx


                conc = np.concatenate([sentences_test[i], new_fea, refers_test[i], abstract_test[i], np.zeros(shape=(1,))], axis=0).T
                conc = np.expand_dims(conc, axis=1)
                frame = pd.DataFrame(conc.T, columns=headers)
                # frame.columns = frame.columns.map(lambda x: 'test' if x == frame.shape[1] - 1 else 'train')
                s_train[train_method.strip()] = frame
    else:

        con = np.concatenate([sentences_test[i], features_test[i], refers_test[i], abstract_test[i], np.zeros(shape=(1,))],axis=0)
        con = np.expand_dims(con, axis=1)
        data_frame = pd.DataFrame(
            con.T, columns=headers)
        # data_frame.columns = data_frame.columns.map(lambda x: 'test' if x == data_frame.shape[1] - 1 else 'train')
        s_train[method[i]] = data_frame


    
    all_value.extend(list(s_train.values()))
    all_key.append(list(s_train.keys()))

    

    content = method[i] + '::' + exception[i].strip('\n') + '::' + str(labels_test[i][0]) + '::'
    all_str.append(content)


tmp_result = pd.concat(all_value)
# tmp_result = tmp_result.iloc[:,:9]
# from sklearn.externals import joblib
# clf = joblib.load("forestFeather.model")
start = time.time()
# tmp_result = clf.predict_proba(tmp_result.iloc[:,:9])
# tmp_result = clf.predict_proba(tmp_result.iloc[:,128:9+128])
tmp_result = predictor.predict_proba(tmp_result)
end = time.time()
print("\033[32m ++ " + mapping[t_id] + ' Running time: ' + str(end - start) + ' s\033[0m')

j = 0

for i, key in enumerate(all_key):
    content = all_str[i]
    for iii, k in enumerate(key):
        content = content + str(k).strip('\n') + '|' + str(tmp_result[j]).strip('\n') + '->'
        j += 1
    target.write(content + '\n')


target.close()


