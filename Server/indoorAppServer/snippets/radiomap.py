import glob

import pandas as pd
from operator import itemgetter

from ..snippets import algorithms, common

radio_maps_local = glob.glob(
    'D:/College/5th Year College/TESE\Desenvolvimento/Code\Application/findLocationApp/findLocation/Server/Notebooks/FINGERPRINT/radiomap*.csv')
radio_maps_heroku = glob.glob('/app/Notebooks/FINGERPRINT/radiomap*.csv')
feature_importance = {}
trained_data = {}
label_encoders = {}


def get_x_train(radio_map):
    return trained_data[radio_map]


def get_labels(radio_map):
    return label_encoders[radio_map]


def train_algorithms(x_train, dataset, radio_map, trained_radio_maps, columns):
    algorithms_to_train = ['RFR', 'RFC']
    trained_radio_maps[radio_map] = dict()
    for algorithm in algorithms_to_train:
        if algorithm == 'RFR':
            y_train = dataset.iloc[:, 1:3]
            estimator = algorithms.initialize_rf_regressor(trainX_data=x_train,
                                                           trainY_data=y_train)
            trained_radio_maps[radio_map]['RFR'] = estimator
        elif algorithm == 'RFC':
            if 'zone' in columns:
                categorical_zone = dataset[['zone']]
                encoder = common.compute_encoder(categorical_zone, 0)
                zone_changed = encoder['labels']
                label_encoders[radio_map] = encoder['encoder']
                dataset['labels'] = zone_changed
                y_train = dataset['labels'].values.reshape(-1, 1).ravel()
                estimator = algorithms.initialize_rf_classifier(trainX_data=x_train,
                                                                trainY_data=y_train)

                trained_radio_maps[radio_map]['RFC'] = estimator


def train_each_radio_map():
    global feature_importance
    trained_radio_maps = dict()
    for radio_map in radio_maps_heroku:
        dataset = pd.read_csv(radio_map)

        columns = list(dataset.columns)
        if 'zone' in columns:
            zone_index = dataset.columns.get_loc('zone')
        else:
            zone_index = 2

        # Replace 0 values with nan
        dataset = common.replace_features_nan(dataset, zone_index + 1)

        # Init variables
        first_beacon_index = -1
        X_train = None
        train_Y = None

        # Find beacon position
        for ap in dataset.iloc[:, zone_index + 1:]:
            if not ap.islower():
                first_beacon_index = list(dataset).index(ap)
                break

        # Clean missing values
        common.compute_data_cleaning_with_global_minimum(dataset, first_beacon_index, zone_index)

        x_train = dataset.iloc[:, zone_index + 1:]
        trained_data[radio_map] = x_train
        y_train = dataset.iloc[:, 1:3]
        feature_importance[radio_map] = common.compute_feature_selection(x_train, y_train)
        train_algorithms(x_train=x_train, columns=columns, radio_map=radio_map,
                         trained_radio_maps=trained_radio_maps,dataset=dataset)
    return trained_radio_maps


def get_access_points_from_feature_importance_dict(radio_map):
    feature_importance_ap_list = {}
    for k, v in feature_importance[radio_map].items():
        if k.islower():
            feature_importance_ap_list[k] = v
    return feature_importance_ap_list

def compute_highest_tuple(maps):
    highest_ap = max(maps.values(), key=itemgetter(0))[0]
    highest_beacon = max(maps.values(), key=itemgetter(1))[1]

    tuples_with_high_ap = [(x, y) for (x, y) in maps.values() if x == highest_ap]
    tuples_with_high_beacon = [(x, y) for (x, y) in maps.values() if y == highest_beacon]
    final_list = tuples_with_high_ap + tuples_with_high_beacon
    max_value = 0.0
    max_tuple = final_list[0]
    for t in final_list:
        for j in final_list:
            x = t[0] - j[0]
            y = t[1] - j[1]
            sum = x + y
            if sum > max_value:
                max_value = sum
                max_tuple = t
    return max_tuple


def compute_matching_data(access_points_scanned, beacons_scanned):
    # Initialization of variables
    similar_radio_maps = list()
    matching_radio_map = {}
    size_dataset = {}
    classification_assert_dict = {}

    for radio_map in radio_maps_heroku:
        # Init dataset related with radio map
        dataset = pd.read_csv(radio_map)
        result = {}
        columns = list(dataset.columns)
        length = len(dataset.index)
        size_dataset[radio_map] = length
        first_beacon_index = -1

        # Structure the access points and beacons present in the dataset into two different Dataframes
        if 'zone' in columns:
            classification_assert_dict[radio_map] = True
            zone_index = dataset.columns.get_loc('zone')
            for ap in dataset.iloc[:, zone_index + 1:]:
                if not ap.islower():
                    first_beacon_index = list(dataset).index(ap)
                    break
            access_points_df = dataset.iloc[:, zone_index + 1:first_beacon_index]
        else:
            classification_assert_dict[radio_map] = False
            for ap in dataset.iloc[:, 3:]:
                if not ap.islower():
                    first_beacon_index = list(dataset).index(ap)
                    break
            access_points_df = dataset.iloc[:, 3:first_beacon_index]

        beacons_df = dataset.iloc[:, first_beacon_index:]

        # Structure of the available access points and beacons in the radio map
        access_points_df_columns = list(access_points_df.columns)
        beacons_df_columns = list(beacons_df.columns)

        # Check which scanned access points/beacons are featured in the radio map and whose importance is greater
        # than the threshold
        matching_list_aps = list()
        matching_list_beacons = list()
        for beacon in beacons_scanned:
            if beacon in beacons_df_columns and beacon in feature_importance[radio_map]:
                matching_list_beacons.append(beacon)
        for access_point in access_points_scanned:
            if access_point in access_points_df_columns and access_point in feature_importance[radio_map]:
                matching_list_aps.append(access_point)

        aps_feature_imp = get_access_points_from_feature_importance_dict(radio_map)
        if len(aps_feature_imp) != 0 and len(matching_list_aps) != 0:
            percentage_of_similar_aps = len(matching_list_aps) / len(aps_feature_imp)
        else:
            percentage_of_similar_aps = 0
        matching_radio_map[radio_map] = (percentage_of_similar_aps, len(matching_list_beacons))
        # Now we have for each radio map the number of matching access points and beacons

    ordered_radio_maps = compute_highest_tuple(matching_radio_map)
    print('The most similar radio map is: ' + str(ordered_radio_maps))
    for dataset_name, matching_values in matching_radio_map.items():
        if matching_values == (ordered_radio_maps[0], ordered_radio_maps[1]):
            similar_radio_maps.append(dataset_name)
    # We give preference to radio maps with classification to give more information
    chosen_radio_map = {}
    if len(similar_radio_maps) == 0:
        return None
    for similar_rm in similar_radio_maps:
        if 'classifier' in similar_rm:
            chosen_radio_map['isClassifier'] = classification_assert_dict[similar_rm]
            chosen_radio_map['length_wifi'] = matching_radio_map[similar_rm][0]
            chosen_radio_map['length_ble'] = matching_radio_map[similar_rm][1]
            chosen_radio_map['dataset'] = similar_rm
            return chosen_radio_map
    chosen_radio_map['isClassifier'] = classification_assert_dict[similar_radio_maps[0]]
    chosen_radio_map['length_wifi'] = matching_radio_map[similar_radio_maps[0]][0]
    chosen_radio_map['length_ble'] = matching_radio_map[similar_radio_maps[0]][1]
    chosen_radio_map['dataset'] = similar_radio_maps[0]
    return chosen_radio_map
