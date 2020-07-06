import numpy as np
import pandas as pd
from IPython.core.display import display
from sklearn.preprocessing import OneHotEncoder,LabelEncoder
from sklearn.preprocessing import StandardScaler, MinMaxScaler, MaxAbsScaler
from .algorithms import *

label_encoder = LabelEncoder()

def checkScaler(preprocessingString):
    if preprocessingString == 'MaxAbsScaler':
        return MaxAbsScaler()
    elif preprocessingString == 'StandardScaler':
        return StandardScaler()
    elif preprocessingString == 'MinMaxScaler':
        return MinMaxScaler()
    else:
        return None


def find_beacon_index(dataset):
    first_beacon_index = -1
    for ap in dataset.iloc[:, 4:]:
        if ap.islower() == False:
            first_beacon_index = list(dataset).index(ap)
            break
    return first_beacon_index


def replace_features_nan(dataset):
    dataset['rssi_Value'] = dataset['rssi_Value'].replace(0,np.nan)
    dataset['rolling_mean_rssi	'] = dataset['rolling_mean_rssi'].replace(0,np.nan)


def compute_data_cleaning(dataset,feature):
    nan_filler = dataset[feature].min()*1.010
    dataset[feature] = dataset[feature].fillna(nan_filler) # Fill missing values


def compute_encoder(categorical_data,flag):
    global label_encoder
    if flag == 0:
        labels = label_encoder.fit_transform(categorical_data)
        print("Labels: ", labels)
    else:
        labels = label_encoder.transform(categorical_data)
    return labels

def decode(prediction):
    global label_encoder
    label = label_encoder.inverse_transform(prediction)
    return label