
import numpy as np
import pandas as pd
import jpype
import re, os, nltk, string
import  xml.dom.minidom
import re
from sklearn.metrics import accuracy_score,confusion_matrix,precision_score,recall_score,f1_score
from autogluon import TabularPrediction as task
from gensim.models import word2vec, doc2vec
from gensim.models import Doc2Vec
from nltk.corpus import stopwords

readMePath=""
def doLine(line,list):
    line = line.replace('\"', '\'')
    try:
        z=re.search(r'\'.*\'', line).group()
        z=z.replace('\'','')
        z=z.split(":")
        if z[0]!="":
            list.append([z[0], z[1]])
    finally:
        return list
def doGradle(path,list):
    sym_dependcies=1
    try:
        for line in open(path,'r', encoding='UTF-8'):
            line= line.replace(' ','')
            line = line.replace('\n', '')
            if line=='}':
                sym_dependcies=1
            if sym_dependcies==2:
                if line!="":
                    list=doLine(line,list)
            if line=='dependencies{':
                sym_dependcies=2
    finally:
        return list;
def openFile(path1,list):
    path = path1
    files2 = os.listdir(path)
    for file2 in files2:
        if os.path.isdir(path + "\\" + file2):
            openFile(path + "\\" + file2,list)
        elif file2=='pom.xml':
            print(path + "\\" + file2)
            xml_filepath = os.path.abspath(path + "\\" + file2)
            dom = xml.dom.minidom.parse(xml_filepath)
            root = dom.documentElement
            if root.getElementsByTagName('dependency'):
                dependecies=root.getElementsByTagName('dependency')
                for i in range(0,len(dependecies)):
                    groupId = dependecies[i].getElementsByTagName('groupId')
                    artifactId = dependecies[i].getElementsByTagName('artifactId')
                    try:
                        list.append([groupId[0].firstChild.data,artifactId[0].firstChild.data])
                    except:
                        list = list
        elif file2=='build.gradle':
            print(path + "\\" + file2)
            list=doGradle(path + "\\" + file2,list)
    return list

def getFeatures(javaSourcePath,jvmPath):
    jarPackPath = "jar//getFeatures.jar"
    jarpath = os.path.join(os.path.abspath("."), jarPackPath)
    jvmPath = jpype.getDefaultJVMPath()

    jpype.startJVM(jvmPath, "-ea", "-Djava.class.path=%s" % (jarpath))

    javaClass = jpype.JClass("com.tcl.App")

    javaClass.getFeatures(javaSourcePath)
    javaClass.getComments(javaSourcePath)

    jpype.shutdownJVM()
    sum_all('features.csv')
def ParseCallGraph(javaSourcePath,jvmPath):
    #getCommons(javaSourcePath)
    getFeatures(javaSourcePath,jvmPath)
def getDepend(javaSourcePath):
    gs = []
    listD = list(gs)
    if os.path.isdir(javaSourcePath):
        listD = openFile(javaSourcePath , listD)
    name = ['groupId', 'artifactId']
    test = pd.DataFrame(columns=name, data=listD)
    test.drop(test.columns[0:1], axis=1, inplace=True)
    test.to_csv('depend.csv', index=False)
    listD.clear()

def get_refer(all_path, start) -> dict:
    all = pd.read_csv(all_path)
    # all.drop(columns='groupId', inplace=True)
    all = all[all.groupby('artifactId').artifactId.transform('count') > 1]
    id_to_a = {}

    a_to_id = {}
    for i in all['artifactId']:
        if a_to_id.get(i) is None:
            a_to_id[i] = len(a_to_id)
    hotarray = np.zeros(len(a_to_id))
    # print(a_to_id)
    depend = pd.read_csv('depend.csv')
    for artifact in depend['artifactId']:
        ind = a_to_id.get(artifact, -1)
        if(ind!=-1):
            hotarray[ind]=1


    dim = len(a_to_id)
    pro_to_one_hot = {}
    pro_to_one_hot["project"]=hotarray
    return pro_to_one_hot


# for doc2vec
def doc2vec_generate(name_sp):
    result = []
    for i, n in enumerate(name_sp):
        doc = doc2vec.TaggedDocument(words=n, tags=[i])
        result.append(doc)
    return result


def doc2vec_abstract(readMePath,size=128):

    name_to_txt = {}
    with open(readMePath, 'r') as file:
        name_to_txt["project"] = file.readline()

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

def sum_all(path: str) -> dict:

    result = {}

    all = pd.read_csv(path)
    all.insert(loc=all.shape[1], column='Doc', value=None)
    for i in range(all.shape[0]):

        class_name = all['Method'][i].split('$')[0].split('.')[-1]
        method_name = all['Method'][i].split('$')[1].split('@')[0]
        callings = []
        if type(all['Calling'][i]) is str:
            for s in all['Calling'][i].split('|'):
                if len(s.split('$')) < 2:
                    continue
                callings.append(s.split('$')[0].split('.')[-1])
                callings.append(s.split('$')[1].split('@')[0])
        tmp = []
        tmp.extend(name_split([class_name, method_name]))
        tmp.extend(name_split(callings))
        doc = ''
        for s in tmp:
            doc = doc + '|'.join(s) + '|'
        all.loc[i, 'Doc'] = doc
    all.drop(columns='Calling', inplace=True)
    pd.DataFrame(all).to_csv(path, index=False)
    # all.to_csv(path, index=False)
    return result


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
                if tmpr + st != '':
                    r.append(tmpr + st)
                tmpr = ''
                i += 1
        if tmpr != '':
            r.append(tmpr)
        result.append(r)
    return result


def preprocess(path,readMePath, test_id, train_id, with_args_num=True, with_pac_depth=True):


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

    zxqcommentPath = "comments.csv"
    zxqcomments = pd.read_csv(zxqcommentPath, header=None, error_bad_lines=False, encoding="gbk")
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

        zxqcom = zxqdict.get(zxqmethod)
        zxqcom = str(zxqcom)
        if (zxqcom == 'nan'):
            zxqcom = ""
        tmp.extend([p for p in zxqcom.split()])

        zxqindex = zxqindex + 1
        doc.append(tmp)


    doc_size = 128
    res = doc2vec_abstract(readMePath,size=doc_size)
    abstract = np.zeros(shape=(data.shape[0], doc_size))
    j = 0
    for i in project_id_:
        abstract[j] = res["project"]
        j += 1


    d = get_refer('depend_all.csv', 'depend')
    refers = np.zeros(shape=(data.shape[0], d["project"].shape[0]))
    j = 0
    for i in project_id_:
        refers[j] = d["project"]
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
    # from gensim.test.utils import get_tmpfile
    # fname = get_tmpfile("doc2vec_sentences_model")
    # d2v.save(fname)
    # d2v = Doc2Vec.load(fname)
    sentences = np.array([list(d2v.infer_vector(n)) for n in doc], dtype=np.float)
    # sentences = np.zeros(shape=(data.shape[0], vector_size))
    # j = 0
    # for i in project_id_:
    #     sentences[j] = sen[j]
    #     j += 1

    # 划分train和test

    method = data['Method']

    exception = data['Exception']

    sentences_train, sentences_test = sentences[train_id], sentences[test_id]
    features_train, features_test = features[train_id], features[test_id]
    refers_train, refers_test = refers[train_id], refers[test_id]
    abstract_train, abstract_test = abstract[train_id], abstract[test_id]
    labels_train, labels_test = labels[train_id], labels[test_id]

    # return doc, features, labels, refers, abstract

    return method[test_id], exception[
        test_id], sentences_train, sentences_test, features_train, features_test, refers_train, refers_test, \
           abstract_train, abstract_test, labels_train, labels_test

def ReadChainFile(filePath):
    listChain = []
    file=open(filePath)
    for line in file:
        listChain.append(line)
        # print(file.name)
        # global delThrow
        # delThrow= True
        # printTop(list1)
        # delThrow = False
    return listChain
def SplitListOne(listChain):
    splitList=[]
    targetList=[]
    for line in listChain:
        splitLine = line.split('::')

        targetList.append([splitLine[0],splitLine[2]])

        sList = []

        for i in range(len(splitLine)-3):
            sList.append(splitLine[i+3])
        splitList.append(sList)
    return targetList,splitList

def SplitListTwo(listChain):
    splitList=[]
    for line in listChain:
        lineList = []
        splitLine = line[0].split("->")
        splitLine.remove('\n')
        for i in range(len(splitLine)):
            splitline = splitLine[i].split("|")
            lineList.append(splitline)
        splitList.append(lineList)
    return splitList

def Rec(listChain):
    recList=[]
    for line in listChain:
        recList.append(GetRec(line))
    return recList
def GetRec(line):
    recList=[]
    for i in range(len(line)):
        recList.append(line[i])
    recList=Sort(recList)
    recList=InsertThrow(recList)
    return recList
def Sort(listChain):
    n=len(listChain)

    for i in range(n):
        listChain[i][1] = str(float(listChain[i][1]))
    for i in range(n):
        # Last i elements are already in place
        for j in range(0, n - i - 1):
            if float(listChain[j][1]) < float(listChain[j + 1][1]):
                listChain[j], listChain[j + 1] = listChain[j + 1], listChain[j]
    return listChain

def InsertThrow(listChain):
    for i in range(len(listChain)):
        if 0.5>float(listChain[i][1]):
            listChain.insert(i,["throw",0])
            return listChain
    listChain.append(["throw",0])
    return listChain
def recommend(readMePath):
    method, exception, sentences_train, sentences_test, features_train, features_test, refers_train, refers_test, \
    abstract_train, abstract_test, labels_train, labels_test = preprocess("features.csv",readMePath, [0], [0])
    labels_test = np.expand_dims(labels_test, axis=1)
    test_samples = np.concatenate([sentences_test, features_test, refers_test, abstract_test, labels_test], axis=-1)
    testData="test.csv"
    data1 = pd.DataFrame(test_samples)
    data1.columns = data1.columns.map(lambda x: 'test' if x == (data1.shape[1] - 1) else 'train')
    data1.to_csv(testData, index=False)
    label_column = 'test'
    test_data = task.Dataset(file_path='test.csv')
    y_test = test_data[label_column]  # values to predict
    test_data_nolab = test_data.drop(labels=[label_column], axis=1)  # delete label column to prove we're not cheating
    dir = 'agModels-predictClass_KXM'
    predictor = task.load(dir)
    predictor.save_space()
    predictor.delete_models(models_to_keep='best', dry_run=False)
    # y_pred = predictor.predict(test_data_nolab)
    # print(confusion_matrix(y_test, y_pred))
    # print('accuracy: ', accuracy_score(y_test, y_pred))
    # print('precision: ', precision_score(y_test, y_pred))
    # print('recall: ', recall_score(y_test, y_pred))
    # print('f1: ', f1_score(y_test, y_pred))
    # print("=x=x=x=x=x=x=x=x=x=x=x=x=x=x=x=x=x=x" +
    #       "=x=x=x=x=x=x=x=x=x=x=x=x=x=x=x=x=x=x=x=x=x=x=x=x=x=x=x=x=x=x=x=x=x=x=x=x=x=x=x=x=x=x=x=x=x=x")


    # recommendation_path = '/home/whu/data/zxq/py_recom/tmp1'
    recommendation_path = 'results'

    method_to_link = {}

    links = open('link.txt', 'r')
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

    target = open('resultLink.txt', 'w')

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

                    conc = np.concatenate(
                        [sentences_test[i], new_fea, refers_test[i], abstract_test[i], np.zeros(shape=(1,))], axis=0).T
                    conc = np.expand_dims(conc, axis=1)
                    frame = pd.DataFrame(conc.T, columns=headers)
                    # frame.columns = frame.columns.map(lambda x: 'test' if x == frame.shape[1] - 1 else 'train')
                    s_train[train_method.strip()] = frame
        else:

            con = np.concatenate(
                [sentences_test[i], features_test[i], refers_test[i], abstract_test[i], np.zeros(shape=(1,))], axis=0)
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
    tmp_result = predictor.predict_proba(tmp_result)

    j = 0

    for i, key in enumerate(all_key):
        content = all_str[i]
        for iii, k in enumerate(key):
            content = content + str(k).strip('\n') + '|' + str(tmp_result[j]).strip('\n') + '->'
            j += 1
        target.write(content + '\n')

    target.close()

def getMethod(rec):
    if rec != 'throw':
        rec = rec.split("$")[1]

    return rec
def getResult(methodName):
    listChain = ReadChainFile('resultLink.txt')
    targetList, splitList = SplitListOne(listChain)
    splitList = SplitListTwo(splitList)
    # 获得推荐list
    recList = Rec(splitList)
    methodList=[]
    for i in range(len(recList)):
        for j in range(len(recList[i])):
            if(methodName in recList[i][j][0]):
                methodList.append(recList[i])
                break;
    recList = methodList
    for i in range(len(recList)):
        print("call chain:",end=' ')
        print(i, end=' ')
        for j in range(len(recList[i])):
            if getMethod(recList[i][j][0])=='throw' :
                continue
            if(j!=0):
                print('->',end=' ')

            print(getMethod(recList[i][j][0]),end=' ')
        print()
        for j in range(len(recList[i])):
            print(j+1,":",end='')
            if recList[i][j][0]=='throw':
                print('no-handler')
            else:
                print(recList[i][j][0],recList[i][j][1])

if __name__ == '__main__':
    javaSourcePath = input("Please input the path of the source folder:\n")
    #javaSourcePath ="shiro"
    print("Parsing call graph……")
    #jvmPath = input("Please input the path of the jvm \n ")
    #jvmPath = "C://Users//40136//.jdks//corretto-11.0.10//bin//server//jvm.dll"
    jvmPath=''
    ParseCallGraph(javaSourcePath,jvmPath)
    print("Call graph is constructed successfully.")
    readMePath = input("Please input the path of the project document file: ")
    #readMePath = 'shiro\\readme.txt'
    getDepend(javaSourcePath)
    print("Project features are encoded successfully.")
    recommend(readMePath)
    getResult()