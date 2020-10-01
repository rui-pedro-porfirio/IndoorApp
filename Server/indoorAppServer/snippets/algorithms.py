import pandas as pd
from IPython.core.display import display
from sklearn.cluster import KMeans
from sklearn.ensemble import RandomForestClassifier, RandomForestRegressor
from sklearn.linear_model import LinearRegression
from sklearn.metrics import classification_report, confusion_matrix
from sklearn.neighbors import KNeighborsRegressor, KNeighborsClassifier
from sklearn.pipeline import make_pipeline
from sklearn.preprocessing import StandardScaler, MaxAbsScaler
from sklearn.svm import SVC, SVR

'''
FINGERPRINTING MAIN ALGORITHM
'''


def initialize_rf_regressor(trainX_data, trainY_data, n_estimators_parameter=2000,
                            criterion_parameter='mse',
                            max_depth_parameter=None, min_samples_split_parameter=2,
                            min_samples_leaf_parameter=1, max_features_parameter='auto',
                            bootstrap_parameter=True, random_state_parameter=42):
    scaler = StandardScaler()
    random_forest_estimator = RandomForestRegressor(n_estimators=n_estimators_parameter, criterion=criterion_parameter,
                                                    max_features=max_features_parameter, max_depth=max_depth_parameter,
                                                    min_samples_leaf=min_samples_leaf_parameter,
                                                    min_samples_split=min_samples_split_parameter,
                                                    bootstrap=bootstrap_parameter, random_state=random_state_parameter)
    if scaler is not None:
        # Make pipeline using scaler transformation
        main_estimator = make_pipeline(scaler, random_forest_estimator)
    else:
        main_estimator = random_forest_estimator
    # Fit the training data
    main_estimator.fit(trainX_data, trainY_data)
    return main_estimator


def compute_rf_regression(main_estimator, testX_data):
    # Predict the results of the testing data features
    predict_test = main_estimator.predict(testX_data)

    return predict_test


def initialize_rf_classifier(trainX_data, trainY_data,
                             scaler=StandardScaler(), n_estimators_parameter=100,
                             criterion_parameter='gini',
                             max_depth_parameter=110.0, min_samples_split_parameter=2,
                             min_samples_leaf_parameter=1, max_features_parameter='auto',
                             bootstrap_parameter=True, random_state_parameter=42):
    random_forest_estimator = RandomForestClassifier(n_estimators=n_estimators_parameter, criterion=criterion_parameter,
                                                     max_features=max_features_parameter, max_depth=max_depth_parameter,
                                                     min_samples_leaf=min_samples_leaf_parameter,
                                                     min_samples_split=min_samples_split_parameter,
                                                     bootstrap=bootstrap_parameter, random_state=random_state_parameter)
    if scaler is not None:
        # Make pipeline using scaler transformation
        main_estimator = make_pipeline(scaler, random_forest_estimator)
    else:
        main_estimator = random_forest_estimator
    # Fit the training data
    main_estimator.fit(trainX_data, trainY_data)

    return main_estimator


def compute_rf_classification(main_estimator, testX_data):
    # Predict the results of the testing data features
    predict_test = main_estimator.predict(testX_data)

    return predict_test


'''
PROXIMITY MAIN ALGORITHM
'''


def initialize_knn_classifier(trainX_data, trainY_data, scaler=StandardScaler(),
                              n_neighbors=12,
                              weights='uniform', algorithm='auto', metric='canberra', n_jobs=-1):
    knn_classifier_estimator = KNeighborsClassifier(n_neighbors=n_neighbors, weights=weights, algorithm=algorithm,
                                                    metric=metric, n_jobs=n_jobs)
    if scaler is not None:
        # Make pipeline using scaler transformation
        main_estimator = make_pipeline(scaler, knn_classifier_estimator)
    else:
        main_estimator = knn_classifier_estimator
    # Fit the training data
    main_estimator.fit(trainX_data, trainY_data)
    return main_estimator


def compute_knn_classification(main_estimator, testX_data):
    # Predict the results of the testing data features
    predict_test = main_estimator.predict(testX_data)

    return predict_test


def initialize_knn_regressor(trainX_data, trainY_data, scaler=StandardScaler(),
                             n_neighbors=30,
                             weights='uniform', algorithm='auto', metric='braycurtis', n_jobs=-1):
    # Init the KNN Regressor Estimator
    knn_regression_estimator = KNeighborsRegressor(n_neighbors=n_neighbors, weights=weights, algorithm=algorithm,
                                                   metric=metric, n_jobs=n_jobs)
    if scaler is not None:
        # Make pipeline using scaler transformation
        main_estimator = make_pipeline(scaler, knn_regression_estimator)
    else:
        main_estimator = knn_regression_estimator
    # Fit the training data
    main_estimator.fit(trainX_data, trainY_data)
    return main_estimator


def compute_knn_regression(main_estimator, testX_data):
    # Predict the results of the testing data features
    predict_test = main_estimator.predict(testX_data)

    return predict_test


'''
OTHER ALGORITHMS FOR USAGE IN EXPERIMENTAL PHASE
'''


def compute_KNN_with_Classification(trainX_data=None, trainY_data=None, testX_data=None, scaler=StandardScaler(),
                                    n_neighbors=12,
                                    weights='uniform', algorithm='auto', metric='canberra', n_jobs=-1):
    # Init the KNN Regressor Estimator
    knn_classifier_estimator = KNeighborsClassifier(n_neighbors=n_neighbors, weights=weights, algorithm=algorithm,
                                                    metric=metric, n_jobs=n_jobs)
    if scaler is not None:
        # Make pipeline using scaler transformation
        main_estimator = make_pipeline(scaler, knn_classifier_estimator)
    else:
        main_estimator = knn_classifier_estimator
    # Fit the training data
    main_estimator.fit(trainX_data, trainY_data)
    # Predict the results of the testing data features
    predict_test = main_estimator.predict(testX_data)
    return predict_test


def compute_KNN_with_Regression(trainX_data=None, trainY_data=None, testX_data=None, scaler=StandardScaler(),
                                n_neighbors=30,
                                weights='uniform', algorithm='auto', metric='braycurtis', n_jobs=-1):
    # Init the KNN Regressor Estimator
    knn_regression_estimator = KNeighborsRegressor(n_neighbors=n_neighbors, weights=weights, algorithm=algorithm,
                                                   metric=metric, n_jobs=n_jobs)
    if scaler is not None:
        # Make pipeline using scaler transformation
        main_estimator = make_pipeline(scaler, knn_regression_estimator)
    else:
        main_estimator = knn_regression_estimator
    # Fit the training data
    main_estimator.fit(trainX_data, trainY_data)
    # Predict the results of the testing data features
    predict_test = main_estimator.predict(testX_data)
    return predict_test


def compute_SVM_with_Classification(trainX_data=None, trainY_data=None, testX_data=None, scaler=StandardScaler()
                                    , C_parameter=1.0, kernel_parameter='rbf', gamma_parameter="scale",
                                    class_weigth_parameter=None, decision_function_shape_parameter='ovr'):
    # Init the SVM
    svm_classifier_estimator = SVC(C=C_parameter, kernel=kernel_parameter, gamma=gamma_parameter,
                                   class_weight=class_weigth_parameter,
                                   decision_function_shape=decision_function_shape_parameter, random_state=6)
    if scaler is not None:
        # Make pipeline using scaler transformation
        main_estimator = make_pipeline(scaler, svm_classifier_estimator)
    else:
        main_estimator = svm_classifier_estimator
    # Fit the training data
    main_estimator.fit(trainX_data, trainY_data.values.ravel())
    # Predict the results of the testing data features
    predict_test = main_estimator.predict(testX_data)
    return predict_test


def compute_SVM_with_Regression(trainX_data=None, trainY_data=None, testX_data=None, scaler=StandardScaler()
                                , C_parameter=1.0, kernel_parameter='rbf', gamma_parameter="scale", epsilon_value=0.1):
    # Init the SVM
    svr_estimator = SVR(C=C_parameter, kernel=kernel_parameter, gamma=gamma_parameter, epsilon=epsilon_value)
    if scaler is not None:
        # Make pipeline using scaler transformation
        main_estimator = make_pipeline(scaler, svr_estimator)
    else:
        main_estimator = svr_estimator
    # Fit the training data
    main_estimator.fit(trainX_data, trainY_data.values.ravel())
    # Predict the results of the testing data features
    predict_test = main_estimator.predict(testX_data)
    return predict_test


def compute_KMeans(trainX_data=None, trainY_data=None, testX_data=None, labels=None, n_clusters=4,
                   init_parameter='k-means++', n_init=10, algorithms='auto', scaler=None, precompute_distances='auto',
                   n_jobs=-1):
    k_means_estimator = KMeans(n_clusters, init=init_parameter, n_init=n_init, random_state=5, algorithm=algorithms,
                               precompute_distances=precompute_distances, n_jobs=n_jobs)
    if scaler is not None:
        # Make pipeline using scaler transformation
        main_estimator = make_pipeline(scaler, k_means_estimator)
    else:
        main_estimator = k_means_estimator
    predicted = main_estimator.fit_predict(trainX_data)
    frame = trainX_data.copy()
    frame['cluster'] = predicted
    predicted_X_test = main_estimator.predict(testX_data)
    testX_data['cluster'] = predicted_X_test
    display(frame)
    display(frame['cluster'].value_counts())
    display(confusion_matrix(frame['cluster'], labels))
    report = classification_report(frame['cluster'], labels, output_dict=True)
    statistics = pd.DataFrame(report).transpose()
    display(statistics)
    result = compute_knn_classification(trainX_data=frame, testX_data=testX_data, trainY_data=trainY_data,
                                        scaler=MaxAbsScaler(),
                                        metric='canberra', weights='distance', algorithm='brute')
    return result


def compute_LinearRegression(trainX_data=None, trainY_data=None, testX_data=None,
                             scaler=StandardScaler()):
    linear_regression_estimator = LinearRegression()
    if scaler is not None:
        # Make pipeline using scaler transformation
        main_estimator = make_pipeline(scaler, linear_regression_estimator)
    else:
        main_estimator = linear_regression_estimator
    # Fit the training data
    main_estimator.fit(trainX_data, trainY_data)
    # Predict the results of the testing data features
    predict_test = main_estimator.predict(testX_data)
    return predict_test




'''
KERAS MLP

# Function to create model, required for KerasClassifier
def create_model_classification(dim=2, num_neurons=180, activation='relu', optimizer='adam'):
    model = Sequential()
    model.add(Dense(num_neurons, input_dim=dim, activation=activation))
    model.add(Dense(num_neurons, activation=activation))
    model.add(Dense(4, activation='softmax'))
    model.compile(loss='sparse_categorical_crossentropy', optimizer=optimizer, metrics=['accuracy'])
    return model


# Function to create model, required for KerasClassifier
def create_model_regression(dim=2, num_neurons=180, activation='relu', optimizer='adam'):
    model = Sequential()
    model.add(Dense(num_neurons, input_dim=dim, activation=activation))
    model.add(Dense(num_neurons, activation=activation))
    model.add(Dense(1))
    model.compile(loss='mean_squared_error', optimizer=optimizer, metrics=['accuracy', 'mean_absolute_error'])
    return model


def compute_MLP_with_Classification(num_neurons_basic, dim=2,
                                    trainX_data=None, trainY_data=None,
                                    testX_data=None,
                                    scaler=None, batch_size=10, epochs=50, verbose=0):
    # actual_classes = np.array(classes)[testY_data]
    keras_classification_model = KerasClassifier(build_fn=create_model_classification, num_neurons=num_neurons_basic,
                                                 dim=dim, epochs=epochs, batch_size=batch_size, verbose=verbose)
    seed = 7
    np.random.seed(seed)
    if scaler is not None:
        # Make pipeline using scaler transformation
        main_estimator = make_pipeline(scaler, keras_classification_model)
    else:
        main_estimator = keras_classification_model
    # Fit the training data
    main_estimator.fit(trainX_data, trainY_data)
    # Predict the results of the testing data features
    predict_test = main_estimator.predict(testX_data)
    return predict_test


def compute_MLP_with_Regression(num_neurons_basic, dim=2,
                                trainX_data=None, trainY_data=None,
                                testX_data=None,
                                scaler=None, batch_size=10, epochs=50, verbose=0):
    # Init the NN Classifier
    keras_regressor = KerasRegressor(build_fn=create_model_regression, dim=dim, epochs=epochs, batch_size=batch_size,
                                     verbose=verbose)
    seed = 7
    if scaler is not None:
        # Make pipeline using scaler transformation
        main_estimator = make_pipeline(scaler, keras_regressor)
    else:
        main_estimator = keras_regressor
    # Fit the training data
    main_estimator.fit(trainX_data, trainY_data)
    # Predict the results of the testing data features
    predict_test = main_estimator.predict(testX_data)
    print("The MSE is:", format(np.power(trainY_data - predict_test, 2).mean()))
    return predict_test'''
