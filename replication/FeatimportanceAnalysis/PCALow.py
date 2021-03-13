import numpy as np
import matplotlib.pyplot as plt
import pandas as pd

from sklearn.datasets import make_classification
from sklearn.ensemble import ExtraTreesClassifier
from sklearn.decomposition import PCA
from sklearn.preprocessing import MinMaxScaler
#
# list=[]
# list.append(1)
# list.append(2)
# print(np.shape(list))

data = pd.read_csv("D:\\all.csv", error_bad_lines=False, encoding="gbk")
data = np.array(data)

Functional=data[:,0:127+1]
Method=data[:,131:133]
Class=data[:,133:135]
Package = data[:,135:137]
Exception= data[:,128]
Architecture= data[:,129:136+1]
Project= data[:,137:893+1]
pca1 = PCA(n_components=1)
pca2 = PCA(n_components=1)
pca3 = PCA(n_components=1)
pca4 = PCA(n_components=1)
Functional=pca1.fit_transform(Functional)
#Functional=MinMaxScaler().fit_transform(Functional)
Exception=np.expand_dims(Exception,axis=1)
#Exception=MinMaxScaler().fit_transform(Exception)
Architecture=pca3.fit_transform(Architecture)
#Architecture=MinMaxScaler().fit_transform(Architecture)
Project=pca4.fit_transform(Project)
#mm = MinMaxScaler()
#Project=mm.fit_transform(Project)
y = np.expand_dims(data[:,894],axis=1)
for i in range(y.shape[0]):
    y[i,0]=int(y[i,0])
X = np.concatenate([Functional,Exception,Architecture,Project,y], axis=-1)
data1 = pd.DataFrame(X)
train_samples=[]
test_samples=[]
for i in range(data1.shape[0]):
    if (i%2==0):
        train_samples.append(data1.iloc[i])
    else:
        test_samples.append(data1.iloc[i])

train_samples = pd.DataFrame(train_samples)
test_samples = pd.DataFrame(test_samples)
train_samples.columns = train_samples.columns.map(lambda x: 'test' if x==(train_samples.shape[1]-1) else 'train')
trainData='jiangwei.csv'
test_samples.columns = test_samples.columns.map(lambda x: 'test' if x==(test_samples.shape[1]-1) else 'train')
train_samples.to_csv('jiangweitrai.csv',index=False)
test_samples.to_csv('jiangweites.csv',index=False)