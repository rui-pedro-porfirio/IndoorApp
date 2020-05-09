import numpy as np
from sklearn.neighbors import KNeighborsRegressor
from sklearn.metrics import classification_report, confusion_matrix, accuracy_score
import pandas as pd
from sklearn.model_selection import train_test_split
from sklearn.preprocessing import StandardScaler


def apply_knn(types,sample):
    if 'Wifi' in types:
        dataset = pd.read_csv('radiomap.csv')
        array = dataset.values
        print(dataset.head())
        print(array)
        columns = dataset.iloc[:,3:]
        sample_list = list()
        for column in columns:
            if column in sample:
                sample_list.append(sample[column])
            else:
                sample_list.append(0)
        print(sample_list)
        sample_2dlist = list()
        sample_2dlist.append(sample_list)
        X_test = np.array(sample_2dlist)
        X_test.reshape(-1,1)
        X_train = dataset.iloc[:,3:].values
        print(X_train)
        Y_train = dataset.iloc[:,1:3].values
        print(Y_train)
        scaler = StandardScaler()
        scaler.fit(X_train)
        X_train = scaler.transform(X_train)
        X_test = scaler.transform(X_test)
        knnr = KNeighborsRegressor(n_neighbors=2)
        knnr.fit(X_train,Y_train)
        Y_pred = knnr.predict(X_test)
        print("Prediction X: " + str(Y_pred[0][0]))
        print("Prediction Y: " + str(Y_pred[0][1]))
        print("The MSE is:", format(np.power(Y_train - Y_pred, 2).mean()))
        return Y_pred;
    if 'Bluetooth' in types:
        '''TODO: To Implement'''