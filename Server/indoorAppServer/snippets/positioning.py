import numpy as np
from sklearn.neighbors import KNeighborsRegressor, KNeighborsClassifier
from sklearn.model_selection import train_test_split,KFold,StratifiedKFold
import pandas as pd
from sklearn.pipeline import make_pipeline
from sklearn.preprocessing import StandardScaler, MinMaxScaler, MaxAbsScaler
from sklearn.model_selection import LeaveOneGroupOut
from sklearn.model_selection import cross_val_score, cross_val_predict
from .algorithms import compute_KNN_with_Classifier
from IPython.display import display

def apply_knnr(types,sample):
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
    for ap in dataset.iloc[:,4:]:
        if ap.islower() == False:
            first_beacon_index = list(dataset).index(ap)
            break
    return first_beacon_index


def apply_knn_classifier(types,access_points,beacons,deviceData):
    dataset = pd.read_csv('Notebooks/radiomapBluetoothWiFiclassifier.csv')
    parameters = pd.read_csv('Notebooks/parameters_results.csv')
    beacon_index = find_beacon_index(dataset)
    if('Wi-fi' in types and 'Bluetooth' in types):
        row = parameters.loc[parameters['Experimentation'] == "KNN Classifier Wifi + Bluetooth"]
        X_train = dataset.iloc[:, 4:]
        Y_train = dataset.iloc[:, 3:4]
        nan_filler = X_train.min().min() * 1.010
        X_train = X_train.replace(0, np.nan)
        X_train = X_train.fillna(nan_filler)
    elif('Wi-fi' in types and 'Bluetooth' not in types):
        row = parameters.query("Experimentation == KNN Classifier Wifi")
        X_train = dataset.iloc[:, 4:beacon_index]
        Y_train = dataset.iloc[:, 3:4]
        nan_filler = X_train.min().min() * 1.010
        X_train = X_train.replace(0, np.nan)
        X_train = X_train.fillna(nan_filler)
    elif('Wi-fi' not in types and 'Bluetooth' in types):
        row = parameters.query("Experimentation == KNN Classifier Bluetooth")
        X_train = dataset.iloc[:, beacon_index:]
        Y_train = dataset.iloc[:, 3:4]
        nan_filler = X_train.min().min() * 1.010
        X_train = X_train.replace(0, np.nan)
        X_train = X_train.fillna(nan_filler)
    number_neighbors = int(row['K Parameter'].tolist()[0])
    weights = row['Weight'].tolist()[0]
    preprocessing = checkScaler(row['Preprocessing'].tolist()[0])
    algorithm = row['Algorithm'].tolist()[0]
    distance = row['Distance'].tolist()[0]
    sample_list = list()
    for column in X_train:
        if column in access_points:
            sample_list.append(access_points[column])
        else:
            sample_list.append(0)
        if column in beacons:
            sample_list.append(beacons[column])
    sample_2dlist = list()
    sample_2dlist.append(sample_list)
    X_test_list = np.array(sample_2dlist)
    X_test = pd.DataFrame(data=X_test_list,columns = X_train.columns)
    X_test.replace(0, np.nan)
    X_test = X_test.fillna(nan_filler)
    result = compute_KNN_with_Classifier(trainX_data=X_train,trainY_data=Y_train,testX_data=X_test,scaler=preprocessing,
                                           n_neighbors=number_neighbors,weights=weights,algorithm=algorithm,metric=distance)
    return result

