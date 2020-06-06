import numpy as np
import pandas as pd
from sklearn.neighbors import KNeighborsRegressor, KNeighborsClassifier
from sklearn.pipeline import make_pipeline
from sklearn.preprocessing import StandardScaler, MaxAbsScaler
from sklearn.neural_network import MLPClassifier, MLPRegressor
from sklearn.svm import SVC
from sklearn.cluster import KMeans
from IPython.core.display import display
from sklearn.metrics import classification_report, confusion_matrix, accuracy_score


def compute_KNN_with_Classification(trainX_data=None, trainY_data=None, testX_data=None, scaler=None, n_neighbors=4,
                                    weights='uniform', algorithm='auto', leaf_size=30, p=2, metric='minkowski',
                                    metric_params=None, n_jobs=-1):
    # Init the KNN Regressor Estimator
    knn_classifier_estimator = KNeighborsClassifier(n_neighbors, weights, algorithm, leaf_size, p, metric,
                                                    metric_params, n_jobs)
    if scaler is not None:
        # Make pipeline using scaler transformation
        main_estimator = make_pipeline(scaler, knn_classifier_estimator)
    else:
        main_estimator = knn_classifier_estimator
    # Fit the training data
    main_estimator.fit(trainX_data, trainY_data.values.ravel())
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


def compute_KNN_with_Regression(trainX_data=None, trainY_data=None, testX_data=None, scaler=None, n_neighbors=5,
                                weights='uniform', algorithm='auto', leaf_size=30, p=2, metric='minkowski',
                                metric_params=None, n_jobs=-1):
    # Init the KNN Regressor Estimator
    knn_regression_estimator = KNeighborsRegressor(n_neighbors, weights, algorithm, leaf_size, p, metric, metric_params,
                                                   n_jobs)
    if scaler is not None:
        # Make pipeline using scaler transformation
        main_estimator = make_pipeline(scaler, knn_regression_estimator)
    else:
        main_estimator = knn_regression_estimator
    # Fit the training data
    main_estimator.fit(trainX_data, trainY_data)
    # Predict the results of the testing data features
    predict_test = main_estimator.predict(testX_data)
    print("The MSE is:", format(np.power(trainY_data - predict_test, 2).mean()))
    return predict_test


def compute_MLP_with_Classification(number_features, trainX_data=None, trainY_data=None, testX_data=None,
                                    scaler=StandardScaler(),
                                    activation_function='relu', solver_function='adam',
                                    learning_rate_value='constant', momentum_value=0.9, alpha_value=0.0001,
                                    max_iterations=1000):
    # Init the NN Classifier
    mlp_classifier_estimator = MLPClassifier(hidden_layer_sizes=(number_features, number_features, number_features),
                                             activation=activation_function, solver=solver_function,
                                             max_iter=max_iterations,
                                             learning_rate=learning_rate_value, momentum=momentum_value,
                                             alpha=alpha_value, random_state=6
                                             )
    if scaler is not None:
        # Make pipeline using scaler transformation
        main_estimator = make_pipeline(scaler, mlp_classifier_estimator)
    else:
        main_estimator = mlp_classifier_estimator
        # Fit the training data
    main_estimator.fit(trainX_data, trainY_data.values.ravel())
    # Predict the results of the testing data features
    predict_test = main_estimator.predict(testX_data)
    return predict_test


def compute_MLP_with_Regression(number_features, trainX_data=None, trainY_data=None, testX_data=None,
                                scaler=StandardScaler(),
                                activation_function='relu', solver_function='adam',
                                learning_rate_value='constant', momentum_value=0.9, alpha_value=0.0001,
                                max_iterations=1000):
    # Init the NN Classifier
    mlp_regression_estimator = MLPRegressor(hidden_layer_sizes=(number_features, number_features, number_features),
                                            activation=activation_function, solver=solver_function,
                                            max_iter=max_iterations,
                                            learning_rate=learning_rate_value, momentum=momentum_value,
                                            alpha=alpha_value, random_state=6
                                            )
    if scaler is not None:
        # Make pipeline using scaler transformation
        main_estimator = make_pipeline(scaler, mlp_regression_estimator)
    else:
        main_estimator = mlp_regression_estimator
    # Fit the training data
    main_estimator.fit(trainX_data, trainY_data)
    # Predict the results of the testing data features
    predict_test = main_estimator.predict(testX_data)
    print("The MSE is:", format(np.power(trainY_data - predict_test, 2).mean()))
    return predict_test


statistical_cols_knn = ['mae', 'mse', 'rmse', 'precision', 'accuracy', 'f1-score']


def compute_KMeans(trainX_data=None, trainY_data=None, testX_data=None, labels=None, n_clusters=4,
                   init_parameter='k-means++', n_init=10, algorithms='auto', scaler=None, precompute_distances='auto',
                   n_jobs=-1):
    k_means_estimator = KMeans(n_clusters, init=init_parameter, n_init=n_init, random_state=5,algorithm=algorithms,
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
    result = compute_KNN_with_Classification(trainX_data=frame, testX_data=testX_data, trainY_data=trainY_data,
                                             scaler=MaxAbsScaler(),
                                             metric='canberra', weights='distance', algorithm='brute')
    return result
