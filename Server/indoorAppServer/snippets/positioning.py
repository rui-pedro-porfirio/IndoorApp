import numpy as np
from sklearn.neighbors import KNeighborsRegressor
import pandas as pd
from sklearn.model_selection import train_test_split


def apply_knn(types):
    if 'Wifi' in types:
        dataset = pd.read_csv('radiomap.csv')
        for column in dataset.iloc[:,3:]:
            dataset[column] = dataset[column].replace(0,np.NaN)
        array = dataset.values
        print(dataset.head())
        print(array)
        x = dataset.iloc[:,3:].values
        print(x)
        y = dataset.iloc[:,1:3].values
        print(y)

    if 'Bluetooth' in types:
        '''TODO: To Implement'''