import numpy as np
from sklearn.neighbors import KNeighborsRegressor, KNeighborsClassifier
from sklearn.pipeline import make_pipeline
from sklearn.preprocessing import StandardScaler
from sklearn.neural_network import MLPClassifier, MLPRegressor
from sklearn.svm import SVC

def compute_KNN_with_Classifier(trainX_data = None,trainY_data = None,testX_data = None,scaler = None,n_neighbors=4,
                                weights='uniform', algorithm='auto', leaf_size=30, p=2, metric='minkowski', metric_params=None, n_jobs=-1):
    # Init the KNN Regressor Estimator
    knn_classifier_estimator = KNeighborsClassifier(n_neighbors,weights,algorithm,leaf_size,p,metric,metric_params,n_jobs)
    if scaler is not None:
        # Make pipeline using scaler transformation
        main_estimator = make_pipeline(scaler,knn_classifier_estimator)
    else:
        main_estimator = knn_classifier_estimator
    # Fit the training data
    main_estimator.fit(trainX_data,trainY_data.values.ravel())
    # Predict the results of the testing data features
    predict_test = main_estimator.predict(testX_data)
    return predict_test

def compute_SVM_with_Classifier(trainX_data = None,trainY_data = None,testX_data = None,scaler = StandardScaler()
                               ,C_parameter = 1.0, kernel_parameter='rbf', gamma_parameter="scale",
                                class_weigth_parameter=None, decision_function_shape_parameter='ovr'):
    # Init the SVM
    svm_classifier_estimator = SVC(C=C_parameter,kernel=kernel_parameter,gamma=gamma_parameter,class_weight=class_weigth_parameter,
                                  decision_function_shape=decision_function_shape_parameter,random_state = 6)
    if scaler is not None:
        # Make pipeline using scaler transformation
        main_estimator = make_pipeline(scaler,svm_classifier_estimator)
    else:
        main_estimator = svm_classifier_estimator
    # Fit the training data
    main_estimator.fit(trainX_data,trainY_data.values.ravel())
    # Predict the results of the testing data features
    predict_test = main_estimator.predict(testX_data)
    print("The MSE is:", format(np.power(trainY_data - predict_test, 2).mean()))
    return  predict_test

def compute_KNN_with_Regression(trainX_data = None,trainY_data = None,testX_data = None,scaler = None,n_neighbors=5,
                                weights='uniform', algorithm='auto', leaf_size=30, p=2, metric='minkowski', metric_params=None, n_jobs=-1):
    # Init the KNN Regressor Estimator
    knn_regression_estimator = KNeighborsRegressor(n_neighbors,weights,algorithm,leaf_size,p,metric,metric_params,n_jobs)
    if scaler is not None:
        # Make pipeline using scaler transformation
        main_estimator = make_pipeline(scaler,knn_regression_estimator)
    else:
        main_estimator = knn_regression_estimator
    # Fit the training data
    main_estimator.fit(trainX_data,trainY_data)
    # Predict the results of the testing data features
    predict_test = main_estimator.predict(testX_data)
    print("The MSE is:", format(np.power(trainY_data - predict_test, 2).mean()))
    return predict_test

def compute_MLP_with_Classification(number_features,trainX_data = None,trainY_data = None,testX_data = None,scaler = StandardScaler(),
                                    activation_function='relu',solver_function='adam',
                                    learning_rate_value='constant',momentum_value = 0.9,alpha_value = 0.0001 ,max_iterations = 1000):
    # Init the NN Classifier
    mlp_classifier_estimator = MLPClassifier(hidden_layer_sizes=(number_features,number_features,number_features),
                                            activation=activation_function,solver=solver_function,max_iter=max_iterations,
                                            learning_rate=learning_rate_value,momentum=momentum_value,
                                             alpha=alpha_value,random_state=6
                                            )
    if scaler is not None:
        # Make pipeline using scaler transformation
        main_estimator = make_pipeline(scaler,mlp_classifier_estimator)
    else:
        main_estimator = mlp_classifier_estimator
        # Fit the training data
    main_estimator.fit(trainX_data,trainY_data.values.ravel())
    # Predict the results of the testing data features
    predict_test = main_estimator.predict(testX_data)
    print("The MSE is:", format(np.power(trainY_data - predict_test, 2).mean()))
    return predict_test

def compute_MLP_with_Regression(number_features,trainX_data = None,trainY_data = None,testX_data = None,scaler = StandardScaler(),
                                    activation_function='relu',solver_function='adam',
                                    learning_rate_value='constant',momentum_value = 0.9, alpha_value = 0.0001,max_iterations = 1000):
    # Init the NN Classifier
    mlp_regression_estimator = MLPRegressor(hidden_layer_sizes=(number_features,number_features,number_features),
                                            activation=activation_function,solver=solver_function,max_iter=max_iterations,
                                            learning_rate=learning_rate_value,momentum=momentum_value,
                                             alpha=alpha_value,random_state=6
                                            )
    if scaler is not None:
        # Make pipeline using scaler transformation
        main_estimator = make_pipeline(scaler,mlp_regression_estimator)
    else:
        main_estimator = mlp_regression_estimator
    # Fit the training data
    main_estimator.fit(trainX_data,trainY_data)
    # Predict the results of the testing data features
    predict_test = main_estimator.predict(testX_data)
    print("The MSE is:", format(np.power(trainY_data - predict_test, 2).mean()))
    return predict_test