
import pandas as pd
import autogluon.core as ag
from autogluon import TabularPrediction as task
from sklearn.metrics import accuracy_score,confusion_matrix,precision_score,recall_score,f1_score

#autogluon
label_column = 'test'
dir = 'agModels-predictClass_jiagnwei'
train_data = task.Dataset(file_path="/dataset/jiangweitrai.csv")
test_data = task.Dataset(file_path="/dataset/jiangweitrai.csv")
# TODO
predictor = task.fit(train_data=train_data, label='test', output_directory=dir,auto_stack=True,time_limits = 1800)
results = predictor.fit_summary()
print(predictor.feature_importance(dataset=test_data,subsample_size=None))

# predictor = task.load(dir)
# print(predictor.info())
# print(predictor.feature_importance(dataset=train_data))
