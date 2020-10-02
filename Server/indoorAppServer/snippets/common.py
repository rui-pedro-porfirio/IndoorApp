import json
import math
import glob

import numpy as np
import pandas as pd
from sklearn.ensemble import RandomForestRegressor
from sklearn.pipeline import make_pipeline
from sklearn.preprocessing import StandardScaler, LabelEncoder

label_encoder = LabelEncoder()


def replace_features_nan(dataset, position):
    dataset.iloc[:, position:] = dataset.iloc[:, position:].replace(0, np.nan)
    return dataset


def replace_features_nan_proximity(dataset):
    dataset['rssi_Value'] = dataset['rssi_Value'].replace(0, np.nan)
    dataset['rolling_mean_rssi'] = dataset['rolling_mean_rssi'].replace(0, np.nan)
    return dataset


def replace_features_minimum(dataset, position):
    dataset.iloc[:, position:] = dataset.iloc[:, position:].replace(np.nan, -100)
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


def compute_feature_selection(trainX_data, train_Y):
    THRESHOLD_IMPORTANCE = 0.005
    random_forest_estimator = RandomForestRegressor()
    main_estimator = make_pipeline(StandardScaler(), random_forest_estimator)
    main_estimator.fit(trainX_data, train_Y)
    feature_imp = pd.Series(random_forest_estimator.feature_importances_,
                            index=trainX_data.columns).sort_values(ascending=False)
    feature_dict = feature_imp.to_dict()
    cleaned_dict = {}
    for k, v in feature_dict.items():
        if v >= THRESHOLD_IMPORTANCE:
            cleaned_dict[k] = v
    return cleaned_dict


def compute_encoder(categorical_data, flag):
    return_dict = dict()
    label_encoder_priv = LabelEncoder()
    if flag == 0:
        labels = label_encoder_priv.fit_transform(categorical_data)
    else:
        labels = label_encoder_priv.transform(categorical_data)
    return_dict['labels'] = labels
    return_dict['encoder'] = label_encoder_priv
    return return_dict


def compute_data_cleaning(dataset, feature):
    nan_filler = dataset[feature].min() * 1.010
    dataset[feature] = dataset[feature].fillna(nan_filler)  # Fill missing values


def decode(prediction):
    global label_encoder
    label = label_encoder.inverse_transform(prediction)
    return label


def load_access_points_locations():
    locations_local = glob.glob(
        '*.json')
    locations_heroku = glob.glob('/app/*.json')
    location_dict = {}
    for location in locations_heroku:
        with open(location) as json_file:
            data = json.load(json_file)
            beacons = {}
            for k, v in data.items():
                beacons[k] = v
            location_dict[location] = beacons
    return location_dict


def check_zone(y):
    if y <= 1.0:
        return 'Personal'
    elif y > 1.0 and y <= 3.5:
        return 'Social'
    else:
        return 'Public'


def mse(x, rfv, distances, beacons):
    squared_errors = 0.0
    empty_list = {}
    x = (x[0], x[1])
    for k, v in rfv:
        distance_known = distances[k]
        distance_computed = compute_distance_coordinate_system(x[0], x[1], beacons[k]['x'], beacons[k]['y'])
        squared_errors += compute_squared_errors(distance_known, distance_computed)
    mse = squared_errors / len(rfv)
    return mse


def compute_squared_errors(d1, d2):
    squared_errors = math.pow(d1 - d2, 2.0)
    return squared_errors


def compute_distance_coordinate_system(x1, y1, x2, y2):
    dist = math.hypot(x2 - x1, y2 - y1)
    return dist
