import numpy as np
import pandas as pd
from sklearn.ensemble import RandomForestRegressor, RandomForestClassifier
from sklearn.pipeline import make_pipeline
from sklearn.preprocessing import StandardScaler, LabelEncoder


def replace_features_nan(dataset, position):
    dataset.iloc[:, position:] = dataset.iloc[:, position:].replace(0, np.nan)
    return dataset


def find_beacon_index(dataset):
    first_beacon_index = -1
    for ap in dataset.iloc[:, 4:]:
        if not ap.islower():
            first_beacon_index = list(dataset).index(ap)
            break
    return first_beacon_index


def compute_data_cleaning_with_global_minimum(dataset, first_beacon_index, zone_index):
    if first_beacon_index != -1:
        numpy_arr_wifi = dataset.iloc[:, zone_index + 1:first_beacon_index].to_numpy()
        numpy_arr_ble = dataset.iloc[:, first_beacon_index:].to_numpy()
        if numpy_arr_wifi.size != 0:
            nan_filler_wifi = np.nanmin(numpy_arr_wifi) * 1.010
            dataset.iloc[:, zone_index + 1:first_beacon_index] = dataset.iloc[:,
                                                                 zone_index + 1:first_beacon_index].fillna(
                nan_filler_wifi)
        if numpy_arr_ble.size != 0:
            nan_filler_ble = np.nanmin(numpy_arr_ble) * 1.010
            dataset.iloc[:, first_beacon_index:] = dataset.iloc[:, first_beacon_index:].fillna(nan_filler_ble)
    else:
        numpy_arr_wifi = dataset.iloc[:, zone_index + 1:].to_numpy()
        if numpy_arr_wifi.size != 0:
            nan_filler_wifi = np.nanmin(numpy_arr_wifi) * 1.010
            dataset.iloc[:, zone_index + 1:] = dataset.iloc[:, zone_index + 1:].fillna(nan_filler_wifi)


def compute_feature_selection(trainX_data,train_Y):
    THRESHOLD_IMPORTANCE = 0.005
    random_forest_estimator = RandomForestRegressor()
    main_estimator = make_pipeline(StandardScaler(), random_forest_estimator)
    main_estimator.fit(trainX_data, train_Y)
    feature_imp = pd.Series(random_forest_estimator.feature_importances_,
                            index=trainX_data.columns).sort_values(ascending=False)
    feature_dict = feature_imp.to_dict()
    cleaned_dict = {}
    for k,v in feature_dict.items():
        if v >= THRESHOLD_IMPORTANCE:
            cleaned_dict[k] = v
    return cleaned_dict

def compute_encoder(categorical_data,flag):
    label_encoder = LabelEncoder()
    if flag == 0:
        labels = label_encoder.fit_transform(categorical_data)
    else:
        labels = label_encoder.transform(categorical_data)
    return labels