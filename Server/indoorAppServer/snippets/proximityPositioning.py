import numpy as np
import pandas as pd
from IPython.core.display import display
from sklearn.preprocessing import StandardScaler, MinMaxScaler, MaxAbsScaler
from sklearn.preprocessing import OneHotEncoder,LabelEncoder
from .algorithms import *
from . import auxiliarFunctions as aux

dataset = pd.read_csv('/app/Notebooks/PROXIMITY/dataset_train_university.csv')
label_encoder = LabelEncoder()
train_X_rssi = None
train_X_rolling_mean = None
combination_features_X = None
train_Y = None
test_X_rssi = None
test_X_rolling_mean = None
test_combination_features_X = None

def compute_encoder(categorical_data,flag):
    if flag == 0:
        labels = label_encoder.fit_transform(categorical_data)
    else:
        labels = label_encoder.transform(categorical_data)
    return labels

def initialize_testing_data(test_dataframe):
    global test_X_rssi
    global test_X_rolling_mean
    global test_combination_features_X
    test_X_rssi = pd.DataFrame(test_dataframe['rssi_Value']).values.reshape(-1, 1)
    display(test_X_rssi.shape)
    test_X_rolling_mean = pd.DataFrame(test_dataframe['rolling_mean_rssi']).values.reshape(-1, 1)
    display(test_X_rolling_mean.shape)
    test_combination_features_X = test_dataframe[['rssi_Value', 'rolling_mean_rssi']]
    display(test_combination_features_X.shape)

def initialize_training_data(training_dataframe):
    global train_X_rssi
    global train_X_rolling_mean
    global combination_features_X
    train_X_rssi = pd.DataFrame(training_dataframe['rssi_Value']).values.reshape(-1, 1)
    display(train_X_rssi.shape)
    train_X_rolling_mean = pd.DataFrame(training_dataframe['rolling_mean_rssi']).values.reshape(-1, 1)
    display(train_X_rolling_mean.shape)
    combination_features_X = training_dataframe[['rssi_Value', 'rolling_mean_rssi']]
    display(combination_features_X.shape)

def data_cleaning(dataset,flag):
    # DATA CLEANING
    aux.compute_data_cleaning(dataset, 'rssi_Value')
    aux.compute_data_cleaning(dataset, 'rolling_mean_rssi')
    categorical_zone = dataset[['zone']]
    print("Previous Categorical Data")
    display(categorical_zone)
    zone_changed = aux.compute_encoder(categorical_zone,flag)
    print("After One Hot Encoder")
    dataset['labels'] = zone_changed

def prepare_dataset(dataset,test_datadf):
    global train_Y
    global test_Y
    positions = dataset['coordinate_Y']
    dataset['distance'] = positions
    aux.replace_features_nan(dataset)
    aux.replace_features_nan(test_datadf)
    display(dataset)
    # DATA CLEANING
    data_cleaning(dataset,0)
    data_cleaning(test_datadf,0)
    train_Y = dataset['labels'].values.reshape(-1, 1)
    display(train_Y)
    initialize_training_data(dataset)
    initialize_testing_data(test_datadf)

def prepare_dataset_trilateration(test_datadf):
    global dataset
    global train_Y
    global test_Y
    positions = dataset['coordinate_Y']
    dataset['distance'] = positions
    aux.replace_features_nan(dataset)
    aux.replace_features_nan(test_datadf)
    display(dataset)
    # DATA CLEANING
    data_cleaning(dataset,0)
    data_cleaning(test_datadf,0)
    train_Y = dataset['distance'].values.reshape(-1, 1)
    display(train_Y)
    initialize_training_data(dataset)
    initialize_testing_data(test_datadf)

def apply_knn_classifier(test_data_df):
    global combination_features_X
    global test_combination_features_X
    prepare_dataset(test_data_df)  # initialized dataset including training set and testing set
    trainX_data = combination_features_X
    testX_data = test_combination_features_X
    result = compute_KNN_with_Classification(trainX_data=trainX_data, trainY_data=train_Y.ravel(), testX_data=testX_data,
                                             scaler=StandardScaler(),
                                             n_neighbors=12, weights='uniform', algorithm='auto',
                                             metric='manhattan')
    if len(result) > 1:
        counts = np.bincount(result)
        result[0] = np.argmax(counts)
        prediction = aux.decode(result)
        print("PREDICTION: ", prediction[0])
    return prediction


def apply_knn_regressor(test_data_df):
    global train_X_rolling_mean
    global test_X_rolling_mean
    prepare_dataset_trilateration(test_data_df)
    display(train_Y.shape)# initialized dataset including training set and testing set
    trainX_data = combination_features_X
    testX_data = test_combination_features_X
    result = compute_KNN_with_Regression(trainX_data=trainX_data, trainY_data=train_Y, testX_data=testX_data,
                                         scaler=StandardScaler(),
                                         n_neighbors=12, weights='uniform', algorithm='auto',
                                         metric='manhattan')
    return result

'''
def apply_mlp_classifier(test_data_df):
    prepare_dataset(test_data_df)  # initialized dataset including training set and testing set
    trainX_data = combination_features_X
    testX_data = test_combination_features_X
    result = compute_MLP_with_Classification(num_neurons_basic=len(trainX_data), trainX_data=trainX_data,
                                             trainY_data=train_Y, testX_data=testX_data,
                                             scaler=StandardScaler(), verbose=1)
    if len(result) > 1:
        counts = np.bincount(result)
        result[0] = np.argmax(counts)
        prediction = aux.decode(result)
        print("PREDICTION: ", prediction[0])
    return prediction


def apply_mlp_regressor(test_data_df):
    prepare_dataset(test_data_df)  # initialized dataset including training set and testing set
    trainX_data = combination_features_X
    testX_data = test_combination_features_X
    result = compute_MLP_with_Regression(num_neurons_basic=len(trainX_data), trainX_data=trainX_data,
                                         trainY_data=train_Y, testX_data=testX_data,
                                         scaler=StandardScaler(), verbose=1)
    return result'''


def apply_svm_classifier(test_data_df):
    prepare_dataset(test_data_df)  # initialized dataset including training set and testing set
    trainX_data = combination_features_X
    testX_data = test_combination_features_X
    result = compute_SVM_with_Classification(trainX_data=trainX_data, trainY_data=train_Y, testX_data=testX_data,
                                             scaler=StandardScaler(), C_parameter=1.0, kernel_parameter='rbf',
                                             gamma_parameter='scale',
                                             class_weigth_parameter=None)
    if len(result) > 1:
        counts = np.bincount(result)
        result[0] = np.argmax(counts)
        prediction = aux.decode(result)
        print("PREDICTION: ", prediction[0])
    return prediction


def apply_svm_regressor(test_data_df):
    prepare_dataset(test_data_df)  # initialized dataset including training set and testing set
    trainX_data = combination_features_X
    testX_data = test_combination_features_X
    result = compute_SVM_with_Regression(trainX_data=trainX_data, trainY_data=train_Y, testX_data=testX_data,
                                         scaler=StandardScaler(), C_parameter=1.0, kernel_parameter='rbf',
                                         gamma_parameter='scale', epsilon_value=0.1)
    return result


def apply_linear_regression(test_data_df):
    prepare_dataset(test_data_df)  # initialized dataset including training set and testing set
    trainX_data = combination_features_X
    testX_data = test_combination_features_X
    result = compute_LinearRegression(trainX_data=trainX_data, trainY_data=train_Y, testX_data=testX_data,
                                         scaler=StandardScaler())
    return result


def apply_randomForest_classifier(test_data_df):
    prepare_dataset(test_data_df)  # initialized dataset including training set and testing set
    trainX_data = combination_features_X
    testX_data = test_combination_features_X
    result = compute_RF_Classification(trainX_data=trainX_data, trainY_data=train_Y, testX_data=testX_data,
                                      scaler=StandardScaler(),n_estimators_parameter=200,max_depth_parameter=10.0,min_samples_split_parameter=5)
    if len(result) > 1:
        counts = np.bincount(result)
        result[0] = np.argmax(counts)
        prediction = aux.decode(result)
        print("PREDICTION: ", prediction[0])
    return prediction


def replace_features_nan(dataset):
    dataset['rssi_Value'] = dataset['rssi_Value'].replace(0, np.nan)
    dataset['rolling_mean_rssi'] = dataset['rolling_mean_rssi'].replace(0, np.nan)
    return dataset


def compute_data_cleaning(dataset,feature):
    nan_filler = dataset[feature].min()*1.010
    dataset[feature] = dataset[feature].fillna(nan_filler) # Fill missing values


def apply_knn_classification_scanning(train_dataset,test_dataset):
    # Initialize Dataset
    dataset = pd.read_csv(train_dataset)
    positions = dataset['coordinate_Y']
    dataset['distance'] = positions
    zone_index = dataset.columns.get_loc('zone')
    # Replace 0 values with nan
    dataset = replace_features_nan(dataset)
    display(dataset)
    display(dataset.shape)
    # Init variables
    combination_features_X = None
    train_Y = None
    # Clean missing values
    compute_data_cleaning(dataset, 'rssi_Value')
    compute_data_cleaning(dataset, 'rolling_mean_rssi')
    #Assign training values
    combination_features_X = dataset[['rssi_Value', 'rolling_mean_rssi']]
    display(combination_features_X.shape)
    categorical_zone = dataset[['zone']]
    zone_changed = compute_encoder(categorical_zone, 0)
    dataset['labels'] = zone_changed
    train_Y = dataset['labels'].values.reshape(-1, 1)
    #Init testing dataset
    test_combination_features_X = test_dataset[['rssi_Value', 'rolling_mean_rssi']]
    display(test_combination_features_X.shape)
    display('DATAFRAMES----------------------------------------------------CLASSIFICATION')
    display(train_Y)
    display(combination_features_X)
    display('DATAFRAMES-TEST---------------------------------------------------CLASSIFICATION')
    display(test_combination_features_X)
    # Compute Algorithm
    result = compute_KNN_with_Classification(trainX_data=combination_features_X, trainY_data=train_Y.ravel(), testX_data=test_combination_features_X)
    return label_encoder.inverse_transform(result)


def apply_knn_regression_scanning(train_dataset,test_dataset):
    # Initialize Dataset
    dataset = pd.read_csv(train_dataset)
    positions = dataset['coordinate_Y']
    dataset['distance'] = positions
    # Replace 0 values with nan
    dataset = replace_features_nan(dataset)
    display(dataset)
    display(dataset.shape)
    # Init variables
    combination_features_X = None
    train_Y = None
    # Clean missing values
    compute_data_cleaning(dataset, 'rssi_Value')
    compute_data_cleaning(dataset, 'rolling_mean_rssi')
    #Assign training values
    combination_features_X = dataset[['rssi_Value', 'rolling_mean_rssi']]
    display(combination_features_X.shape)
    train_Y = pd.DataFrame(dataset['distance'])
    #Init testing dataset
    test_combination_features_X = test_dataset[['rssi_Value', 'rolling_mean_rssi']]
    display(test_combination_features_X.shape)
    display('DATAFRAMES----------------------------------------------------CLASSIFICATION')
    display(train_Y)
    display(combination_features_X)
    display('DATAFRAMES-TEST---------------------------------------------------CLASSIFICATION')
    display(test_combination_features_X)
    # Compute Algorithm
    result = compute_KNN_with_Regression(trainX_data=combination_features_X, trainY_data=train_Y, testX_data=test_combination_features_X)
    return result