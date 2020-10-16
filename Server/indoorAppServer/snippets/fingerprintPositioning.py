import numpy as np
from sklearn.preprocessing import MinMaxScaler

from .algorithms import *
from ..snippets import common, radiomap


'''
HANDLER FOR SCANNING PHASE
'''


def apply_rf_regressor_scanning(estimator_options, radio_map, access_points, beacons):
    x_train = radiomap.get_x_train(radio_map)
    # Init testing dataset by checking which access points to fill
    sample_list = list()
    for column in x_train:
        if column in access_points:
            sample_list.append(access_points[column])
        elif column in beacons:
            sample_list.append(beacons[column])
        else:
            sample_list.append(0)
    sample_2dlist = list()
    sample_2dlist.append(sample_list)
    X_test_list = np.array(sample_2dlist)
    X_test = pd.DataFrame(data=X_test_list, columns=x_train.columns)
    first_beacon_index_t2 = -1
    for ap in X_test.iloc[:, 0:]:
        if not ap.islower():
            first_beacon_index_t2 = list(X_test).index(ap)
            break
    X_test = common.replace_features_nan(X_test, 0)
    common.compute_data_cleaning_with_global_minimum(X_test, first_beacon_index_t2, -1)
    access_points_tst = X_test.iloc[:, 0:first_beacon_index_t2]
    beacons_tst = X_test.iloc[:, first_beacon_index_t2:]
    if access_points_tst.isnull().values.any():
        common.replace_features_minimum(X_test, 0)
    if beacons_tst.isnull().values.any():
        common.replace_features_minimum(X_test, 0)
    # Compute Algorithm
    result = compute_rf_regression(main_estimator=estimator_options['RFR'], testX_data=X_test)
    return result


def apply_rf_classification_scanning(estimator_options, radio_map, access_points, beacons):
    x_train = radiomap.get_x_train(radio_map)
    # Init testing dataset by checking which access points to fill
    sample_list = list()
    for column in x_train:
        if column in access_points:
            sample_list.append(access_points[column])
        elif column in beacons:
            sample_list.append(beacons[column])
        else:
            sample_list.append(0)
    sample_2dlist = list()
    sample_2dlist.append(sample_list)
    X_test_list = np.array(sample_2dlist)
    X_test = pd.DataFrame(data=X_test_list, columns=x_train.columns)
    first_beacon_index_t2 = -1
    for ap in X_test.iloc[:, 0:]:
        if not ap.islower():
            first_beacon_index_t2 = list(X_test).index(ap)
            break
    X_test = common.replace_features_nan(X_test, 0)
    common.compute_data_cleaning_with_global_minimum(X_test, first_beacon_index_t2, zone_index=-1)
    access_points_tst = X_test.iloc[:, 0:first_beacon_index_t2]
    beacons_tst = X_test.iloc[:, first_beacon_index_t2:]
    if access_points_tst.isnull().values.any():
        common.replace_features_minimum(X_test, 0)
    if beacons_tst.isnull().values.any():
        common.replace_features_minimum(X_test, 0)
    # Compute Algorithm
    result = compute_rf_classification(main_estimator=estimator_options['RFC'], testX_data=X_test)
    encoder = radiomap.get_labels(radio_map)
    return encoder.inverse_transform(result)


'''
EXPERIMENTAL PHASE HANDLERS
'''


def checkScaler(preprocessingString):
    if preprocessingString == 'MaxAbsScaler':
        return MaxAbsScaler()
    elif preprocessingString == 'StandardScaler':
        return StandardScaler()
    elif preprocessingString == 'MinMaxScaler':
        return MinMaxScaler()
    else:
        return None


def apply_knn_classifier(types, access_points, beacons, deviceData):
    dataset = pd.read_csv('Notebooks/radiomapBluetoothWiFiclassifier.csv')
    parameters = pd.read_csv('Notebooks/parameters_results.csv')
    beacon_index = common.find_beacon_index(dataset)
    if ('Wi-fi' in types and 'Bluetooth' in types):
        row = parameters.loc[parameters['Experimentation'] == "KNN Classifier Wifi + Bluetooth"]
        X_train = dataset.iloc[:, 4:]
        Y_train = dataset.iloc[:, 3:4]
        nan_filler = X_train.min().min() * 1.010
        X_train = X_train.replace(0, np.nan)
        X_train = X_train.fillna(nan_filler)
    elif ('Wi-fi' in types and 'Bluetooth' not in types):
        row = parameters.query("Experimentation == KNN Classifier Wifi")
        X_train = dataset.iloc[:, 4:beacon_index]
        Y_train = dataset.iloc[:, 3:4]
        nan_filler = X_train.min().min() * 1.010
        X_train = X_train.replace(0, np.nan)
        X_train = X_train.fillna(nan_filler)
    elif ('Wi-fi' not in types and 'Bluetooth' in types):
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
    X_test = pd.DataFrame(data=X_test_list, columns=X_train.columns)
    X_test.replace(0, np.nan)
    X_test = X_test.fillna(nan_filler)
    result = compute_knn_classification(trainX_data=X_train, trainY_data=Y_train, testX_data=X_test,
                                        scaler=preprocessing,
                                        n_neighbors=number_neighbors, weights=weights, algorithm=algorithm,
                                        metric=distance)
    return result


def apply_knn_regressor(types, access_points, beacons, deviceData):
    dataset = pd.read_csv('Notebooks/radiomapBluetoothWiFi.csv')
    parameters = pd.read_csv('Notebooks/parameters_results.csv')
    beacon_index = common.find_beacon_index(dataset)
    if ('Wi-fi' in types and 'Bluetooth' in types):
        row = parameters.loc[parameters['Experimentation'] == "KNN Regressor Wifi + Bluetooth"]
        X_train = dataset.iloc[:, 4:]
        Y_train = dataset.iloc[:, 3:4]
        nan_filler = X_train.min().min() * 1.010
        X_train = X_train.replace(0, np.nan)
        X_train = X_train.fillna(nan_filler)
    elif ('Wi-fi' in types and 'Bluetooth' not in types):
        row = parameters.query("Experimentation == KNN Regressor Wifi")
        X_train = dataset.iloc[:, 4:beacon_index]
        Y_train = dataset.iloc[:, 3:4]
        nan_filler = X_train.min().min() * 1.010
        X_train = X_train.replace(0, np.nan)
        X_train = X_train.fillna(nan_filler)
    elif ('Wi-fi' not in types and 'Bluetooth' in types):
        row = parameters.query("Experimentation == KNN Regressor Bluetooth")
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
    X_test = pd.DataFrame(data=X_test_list, columns=X_train.columns)
    X_test.replace(0, np.nan)
    X_test = X_test.fillna(nan_filler)
    result = compute_knn_regression(trainX_data=X_train, trainY_data=Y_train, testX_data=X_test,
                                    scaler=preprocessing,
                                    n_neighbors=number_neighbors, weights=weights, algorithm=algorithm,
                                    metric=distance)
    return result


def apply_svm_classifier(types, access_points, beacons, deviceData):
    dataset = pd.read_csv('Notebooks/radiomapBluetoothWiFiclassifier.csv')
    parameters = pd.read_csv('Notebooks/parameters_svm_results.csv')
    beacon_index = common.find_beacon_index(dataset)
    if ('Wi-fi' in types and 'Bluetooth' in types):
        row = parameters.loc[parameters['Experimentation'] == "SVM Classifier Wifi + Bluetooth"]
        X_train = dataset.iloc[:, 4:]
        Y_train = dataset.iloc[:, 3:4]
        nan_filler = X_train.min().min() * 1.010
        X_train = X_train.replace(0, np.nan)
        X_train = X_train.fillna(nan_filler)
    elif ('Wi-fi' in types and 'Bluetooth' not in types):
        row = parameters.query("Experimentation == SVM Classifier Wifi")
        X_train = dataset.iloc[:, 4:beacon_index]
        Y_train = dataset.iloc[:, 3:4]
        nan_filler = X_train.min().min() * 1.010
        X_train = X_train.replace(0, np.nan)
        X_train = X_train.fillna(nan_filler)
    elif ('Wi-fi' not in types and 'Bluetooth' in types):
        row = parameters.query("Experimentation == SVM Classifier Bluetooth")
        X_train = dataset.iloc[:, beacon_index:]
        Y_train = dataset.iloc[:, 3:4]
        nan_filler = X_train.min().min() * 1.010
        X_train = X_train.replace(0, np.nan)
        X_train = X_train.fillna(nan_filler)
    c_parameter = float(row['C Parameter'].tolist()[0])
    preprocessing = checkScaler(row['Preprocessing'].tolist()[0])
    kernel = row['Kernel'].tolist()[0]
    gamma = row['Gamma'].tolist()[0]
    weights = row['Class Weights'].tolist()[0]
    decision_function = row['Decision Function'].tolist()[0]
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
    X_test = pd.DataFrame(data=X_test_list, columns=X_train.columns)
    X_test.replace(0, np.nan)
    X_test = X_test.fillna(nan_filler)
    result = compute_SVM_with_Classification(trainX_data=X_train, trainY_data=Y_train, testX_data=X_test,
                                             scaler=preprocessing, C_parameter=c_parameter, kernel_parameter=kernel,
                                             gamma_parameter=gamma,
                                             class_weigth_parameter=weights,
                                             decision_function_shape_parameter=decision_function)
    return result


def apply_kmeans_knn_classifier(types, access_points, beacons, deviceData):
    dataset = pd.read_csv('Notebooks/radiomapBluetoothWiFiclassifier.csv')
    parameters = pd.read_csv('Notebooks/parameters_results_clustering.csv')
    beacon_index = common.find_beacon_index(dataset)
    if ('Wi-fi' in types and 'Bluetooth' in types):
        row = parameters.loc[parameters['Experimentation'] == "K-Means Clustering Wifi + Bluetooth"]
        X_train = dataset.iloc[:, 4:]
        Y_train = dataset.iloc[:, 3:4]
        nan_filler = X_train.min().min() * 1.010
        X_train = X_train.replace(0, np.nan)
        X_train = X_train.fillna(nan_filler)
    elif ('Wi-fi' in types and 'Bluetooth' not in types):
        row = parameters.query("Experimentation == K-Means Clustering Wifi")
        X_train = dataset.iloc[:, 4:beacon_index]
        Y_train = dataset.iloc[:, 3:4]
        nan_filler = X_train.min().min() * 1.010
        X_train = X_train.replace(0, np.nan)
        X_train = X_train.fillna(nan_filler)
    elif ('Wi-fi' not in types and 'Bluetooth' in types):
        row = parameters.query("Experimentation == K-Means Clustering Bluetooth")
        X_train = dataset.iloc[:, beacon_index:]
        Y_train = dataset.iloc[:, 3:4]
        nan_filler = X_train.min().min() * 1.010
        X_train = X_train.replace(0, np.nan)
        X_train = X_train.fillna(nan_filler)
    k_parameter = int(row['K Parameter'].tolist()[0])
    preprocessing = checkScaler(row['Preprocessing'].tolist()[0])
    init_param = row['Init Parameter'].tolist()[0]
    algorithm = row['Algorithnm'].tolist()[0]
    distance = row['Preprocessing Distance'].tolist()[0]
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
    X_test = pd.DataFrame(data=X_test_list, columns=X_train.columns)
    X_test.replace(0, np.nan)
    X_test = X_test.fillna(nan_filler)
    labels = dataset.drop(columns=['coordinate_X', 'coordinate_Y']).iloc[:, 1:2]
    reference_points = dataset.groupby(['zone'])
    dict_zones = {}
    counter = 0
    for rp, rp_data in reference_points:
        dict_zones[rp] = counter
        counter = counter + 1
    display(dict_zones)
    labels['label'] = labels['zone'].map(lambda p: dict_zones[p])
    result = compute_KMeans(trainX_data=X_train, trainY_data=Y_train, testX_data=X_test, labels=labels['label'],
                            scaler=preprocessing, n_clusters=k_parameter, init_parameter=init_param,
                            algorithms=algorithm, precompute_distances=distance)
    return result


'''
def apply_mlp_classifier(types, access_points, beacons, deviceData):
    dataset = pd.read_csv('Notebooks/radiomapBluetoothWiFiclassifier.csv')
    parameters = pd.read_csv('Notebooks/parameters_neural_networks_results.csv')
    beacon_index = find_beacon_index(dataset)
    if ('Wi-fi' in types and 'Bluetooth' in types):
        row = parameters.loc[parameters['Experimentation'] == "MLP Classifier Wifi + Bluetooth"]
        X_train = dataset.iloc[:, 4:]
        Y_train = dataset.iloc[:, 3:4]
        nan_filler = X_train.min().min() * 1.010
        X_train = X_train.replace(0, np.nan)
        X_train = X_train.fillna(nan_filler)
    elif ('Wi-fi' in types and 'Bluetooth' not in types):
        row = parameters.query("Experimentation == MLP Classifier Wifi")
        X_train = dataset.iloc[:, 4:beacon_index]
        Y_train = dataset.iloc[:, 3:4]
        nan_filler = X_train.min().min() * 1.010
        X_train = X_train.replace(0, np.nan)
        X_train = X_train.fillna(nan_filler)
    elif ('Wi-fi' not in types and 'Bluetooth' in types):
        row = parameters.query("Experimentation == MLP Classifier Bluetooth")
        X_train = dataset.iloc[:, beacon_index:]
        Y_train = dataset.iloc[:, 3:4]
        nan_filler = X_train.min().min() * 1.010
        X_train = X_train.replace(0, np.nan)
        X_train = X_train.fillna(nan_filler)
    activation_function = row["Activation Function"].tolist()[0]
    solver = row['Solver'].tolist()[0]
    alpha = float(row['Alpha'].tolist()[0])
    learning_rate = row['Learning Rate'].tolist()[0]
    momentum = float(row['Momentum'].tolist()[0])
    iterations = int(row['Iterations'].tolist()[0])
    preprocessing = checkScaler(row['Preprocessing'].tolist()[0])
    n_features = X_train.shape[1]
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
    X_test = pd.DataFrame(data=X_test_list, columns=X_train.columns)
    X_test.replace(0, np.nan)
    X_test = X_test.fillna(nan_filler)
    result = compute_MLP_with_Classification(number_features=n_features, trainX_data=X_train, trainY_data=Y_train,
                                             testX_data=X_test, scaler=preprocessing,
                                             activation_function=activation_function, solver_function=solver,
                                             alpha_value=alpha, learning_rate_value=learning_rate,
                                             momentum_value=momentum, max_iterations=iterations)
    return result


def apply_mlp_regressor(types, access_points, beacons, deviceData):
    dataset = pd.read_csv('Notebooks/radiomapBluetoothWiFi.csv')
    parameters = pd.read_csv('Notebooks/parameters_neural_networks_results.csv')
    beacon_index = find_beacon_index(dataset)
    if ('Wi-fi' in types and 'Bluetooth' in types):
        row = parameters.loc[parameters['Experimentation'] == "MLP Regressor Wifi + Bluetooth"]
        X_train = dataset.iloc[:, 4:]
        Y_train = dataset.iloc[:, 3:4]
        nan_filler = X_train.min().min() * 1.010
        X_train = X_train.replace(0, np.nan)
        X_train = X_train.fillna(nan_filler)
    elif ('Wi-fi' in types and 'Bluetooth' not in types):
        row = parameters.query("Experimentation == MLP Regressor Wifi")
        X_train = dataset.iloc[:, 4:beacon_index]
        Y_train = dataset.iloc[:, 3:4]
        nan_filler = X_train.min().min() * 1.010
        X_train = X_train.replace(0, np.nan)
        X_train = X_train.fillna(nan_filler)
    elif ('Wi-fi' not in types and 'Bluetooth' in types):
        row = parameters.query("Experimentation == MLP Regressor Bluetooth")
        X_train = dataset.iloc[:, beacon_index:]
        Y_train = dataset.iloc[:, 3:4]
        nan_filler = X_train.min().min() * 1.010
        X_train = X_train.replace(0, np.nan)
        X_train = X_train.fillna(nan_filler)
    activation_function = row['Activation Function'].tolist()[0]
    solver = row['Solver'].tolist()[0]
    alpha = float(row['Alpha'].tolist()[0])
    learning_rate = row['Learning Rate'].tolist()[0]
    momentum = float(row['Momentum'].tolist()[0])
    iterations = int(row['Iterations'].tolist()[0])
    preprocessing = checkScaler(row['Preprocessing'].tolist()[0])
    n_features = X_train.shape[1]
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
    X_test = pd.DataFrame(data=X_test_list, columns=X_train.columns)
    X_test.replace(0, np.nan)
    X_test = X_test.fillna(nan_filler)
    result = compute_MLP_with_Regression(number_features=n_features, trainX_data=X_train, trainY_data=Y_train,
                                         testX_data=X_test, scaler=preprocessing,
                                         activation_function=activation_function, solver_function=solver,
                                         alpha_value=alpha, learning_rate_value=learning_rate,
                                         momentum_value=momentum, max_iterations=iterations)
    return result
'''
