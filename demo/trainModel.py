
import pandas as pd
import autogluon as ag
from autogluon import TabularPrediction as task

# autogluon
label_column = 'test'
dir = 'agModels-predictClass_KXM'
train_data = task.Dataset(file_path='train7_14.csv')

# # TODO
predictor = task.fit(train_data=train_data, label='test', output_directory=dir, auto_stack=True, time_limits=1800)
predictor.save_space()
predictor.delete_models(models_to_keep='best', dry_run=False)


