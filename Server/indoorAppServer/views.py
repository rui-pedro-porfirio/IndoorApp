import json
import json
import math
from enum import Enum
from os import path

import numpy as np
import pandas as pd
import scipy.optimize as opt
from IPython.core.display import display
from rest_framework import status
from rest_framework import viewsets
from rest_framework.response import Response
from rest_framework.views import APIView

from .models import Fingerprint, DeviceSensor, BluetoothSensor, WiFiSensor, UserTable
from .serializers import FingerprintSerializer, DeviceDataSerializer, WifiDataSerializer, BluetoothSerializer, \
    UserSerializer
from .snippets import radiomap, filters, convertJson, fingerprintPositioning, proximityPositioning, decision_system, \
    websockets, initializationModule

'''
 INITIALIZATION OF THE FUZZY SET SYSTEM AND TEST THE WS COMMUNICATION
'''

fuzzy_dict = initializationModule.create_and_assert_fuzzy_system()
fuzzy_system = fuzzy_dict['System']
fuzzy_technique = fuzzy_dict['Technique MF']
initializationModule.test_ws_communication()

'''
CLASSES FOR MODELS SAVED IN THE DABATASE
'''


class UserView(viewsets.ModelViewSet):
    queryset = UserTable.objects.all()
    serializer_class = UserSerializer


class FingerprintView(viewsets.ModelViewSet):
    queryset = Fingerprint.objects.all()
    serializer_class = FingerprintSerializer


class DeviceView(viewsets.ModelViewSet):
    queryset = DeviceSensor.objects.all()
    serializer_class = DeviceDataSerializer


class WifiView(viewsets.ModelViewSet):
    queryset = WiFiSensor.objects.all()
    serializer_class = WifiDataSerializer


class BluetoothView(viewsets.ModelViewSet):
    queryset = BluetoothSensor.objects.all()
    serializer_class = BluetoothSerializer


class FilterEnum(Enum):
    MEDIAN_FILTER = 1
    MEAN_FILTER = 2


class TypeEnum(Enum):
    WIFI = 1
    BLUETOOTH = 2


'''
CLASS TO HANDLE SCANNING SAMPLES SENT FROM THE MOBILE DEVICE
'''


class ScanningView(APIView):
    username = None
    deviceUuid = None
    accessPointsDetected = None
    beaconsDetected = None
    deviceSensorDetected = None
    position_regression = None
    position_classification = None
    access_points_ml = {}
    beacons_ml = {}
    access_points_structured = list()
    beacons_structured = list()
    beacons_ml_fingerprinting = {}

    def configure_data_structures(self, sample_dict):
        self.username = sample_dict['username']
        self.deviceUuid = sample_dict['uuid']
        self.accessPointsDetected = sample_dict['mAccessPoints']
        self.beaconsDetected = sample_dict['mBeaconsList']
        self.deviceSensorDetected = sample_dict['mSensorInformationList']

        self.structure_access_points_data()
        self.structure_beacons_data()
        self.structure_beacons_data()

    def structure_access_points_data(self):
        for access_point in self.accessPointsDetected:
            self.access_points_structured.append(access_point['name'])
            self.access_points_ml[access_point['name']] = access_point['singleValue']

    def structure_beacons_data(self):
        for beacon in self.beaconsDetected:
            self.beacons_structured.append(beacon['name'])
            single_list = beacon['singleValue']
            rolling_list = beacon['values']
            extending_tp = (single_list, rolling_list)
            self.beacons_ml[beacon['name']] = extending_tp
            self.beacons_ml_fingerprinting[beacon['name']] = single_list

    def match_radio_map_similarity(self):
        return radiomap.compute_matching_data(self.access_points_structured, self.beacons_structured)

    def structure_radio_map_for_fuzzy_system(self, radio_map):
        result_dict = {'isClassifier': radio_map['isClassifier'], 'input_aps': radio_map['length_wifi'],
                       'input_beacons': radio_map['length_ble']}
        beacons_known_positions = load_access_points_locations()
        result_dict['beacons_known_positions'] = beacons_known_positions
        result_dict['beacons_locations_length'] = len(beacons_known_positions)
        return result_dict

    def apply_ml_algorithm(self, position_technique, radio_map_is_classifier,
                           matching_radio_map, beacons_known_locations):
        if position_technique == 'Fingerprinting':
            print('Fingerprinting chosen. Applying Random Forest Algorithm')
            self.apply_fingerprinting(matching_radio_map=matching_radio_map,
                                      radio_map_is_classifier=radio_map_is_classifier)
            print('ML Algorithm done running.')
        elif position_technique == 'Trilateration':
            print('Trilateration chosen.')
            self.apply_trilateration(matching_radio_map=matching_radio_map,
                                     radio_map_is_classifier=radio_map_is_classifier)
            print('ML Algorithm done running.')
        elif position_technique == 'Proximity':
            print('Proximity chosen.')
            self.apply_proximity(matching_radio_map=matching_radio_map,
                                 radio_map_is_classifier=radio_map_is_classifier)
            print('ML Algorithm done running')

    def apply_fingerprinting(self, matching_radio_map, radio_map_is_classifier):
        # Apply RF to Regression
        self.position_regression = fingerprintPositioning.apply_rf_regressor_scanning(matching_radio_map['dataset'],
                                                                                      self.access_points_ml,
                                                                                      self.beacons_ml_fingerprinting)
        # Apply RF to Classification
        if radio_map_is_classifier:
            self.position_classification = fingerprintPositioning.apply_rf_classification_scanning(
                matching_radio_map['dataset'], self.access_points_ml, self.beacons_ml_fingerprinting)

    def apply_proximity(self, matching_radio_map, radio_map_is_classifier):
        # FOR TEST ONLY
        sample = {}

        # Sort beacons by number of samples recorded
        sorted_dict = sorted(self.beacons_ml, key=lambda k: len(self.beacons_ml[k][1]), reverse=True)

        highest_beacon = self.beacons_ml[sorted_dict[0]]
        sample['singleValue'] = highest_beacon[0]
        sample['values'] = highest_beacon[1]

        # Structure sample into a csv
        test_df = compute_csv_sample(sample)

        # Apply KNN to Regression
        self.position_regression = proximityPositioning.apply_knn_regression_scanning(matching_radio_map['dataset'],
                                                                                      test_df)
        # Apply KNN to Classification
        if radio_map_is_classifier:
            self.position_classification = proximityPositioning.apply_knn_classification_scanning(
                matching_radio_map['dataset'], test_df)

    def apply_trilateration(self, matching_radio_map, radio_map_is_classifier, beacons_known_locations):
        # Trilateration with LSE variables
        min_distance = float('inf')
        closest_location = None

        # Sort beacons by number of samples recorded
        sorted_dict = sorted(self.beacons_ml, key=lambda k: len(self.beacons_ml[k][1]), reverse=True)

        # Structure beacons into csv file
        test_df = compute_csv_sample_trilateration(sorted_dict, self.beacons_ml)
        display(test_df)

        # Compute distance predictions using proximity KNN algorithm from the user and the beacons locations
        distance_predictions = {}
        rfv = test_df.groupby(['BLE Beacon'])
        for k, v in rfv:
            print("K: " + str(k))
            print("V: " + str(v))
            distance_prediction = proximityPositioning.apply_knn_regression_scanning(
                matching_radio_map['dataset'],
                v)
            distance_predictions[k] = distance_prediction[0, 0]
        print('DISTANCE PREDICTION: ' + str(distance_predictions))

        # Find the nearest beacon
        for k, v in distance_predictions.items():
            if v < min_distance:
                min_distance = v
                closest_location = beacons_known_locations[k]

        # Compute SciPy minimize function to obtain position prediction
        initial_location = closest_location
        initial_location_tuple = (initial_location['x'], initial_location['y'])
        result = opt.minimize(
            mse,  # The error function
            initial_location_tuple,  # The initial guess
            args=(rfv,
                  distance_predictions,
                  beacons_known_locations),
            method='L-BFGS-B',  # The optimisation algorithm
            options={
                'ftol': 1e-5,  # Tolerance
                'maxiter': 1e+7  # Maximum iterations
            })

        prediction = result.x
        print("GUESSED: " + str(prediction))

        self.position_regression = (prediction[0], prediction[1])
        if radio_map_is_classifier:
            self.position_classification = check_zone(prediction[1])  # Use an implicit classification method

    def structure_position_results(self):
        position_dict = {}
        if self.position_regression is not None:
            position_dict['Regression'] = (self.position_regression[0][0],self.position_regression[0][1])
        if self.position_classification is not None:
            position_dict['Classification'] = self.position_classification
        return position_dict

    def update_position_results(self, position_dict):
        websockets.publish(self.username, self.deviceUuid, position_dict)

    def post(self, request):
        serializer_context = {
            'request': request,
        }
        sample_dict = request.data
        print(sample_dict)

        self.configure_data_structures(sample_dict)

        number_beacons_detected = len(self.beacons_structured)

        matching_radio_map = self.match_radio_map_similarity()

        radio_map_is_empty = not bool(matching_radio_map)
        if matching_radio_map is None or radio_map_is_empty:
            raise Exception('Impossible to get data. No data returned from BLE and Wifi')
        else:
            radio_map_data = self.structure_radio_map_for_fuzzy_system(matching_radio_map)
            radio_map_is_classifier = radio_map_data['isClassifier']
            similar_access_points = radio_map_data['input_aps']
            similar_beacons = radio_map_data['input_beacons']
            beacons_known_locations = radio_map_data['beacons_known_positions']
            size_of_beacons_known_locations = radio_map_data['beacons_locations_length']
            print('Number of Matching access_points: ' + str(similar_access_points))  # TODO: BUG VALUE ABNORMALLY HIGH
            print('Number of Matching beacons: ' + str(similar_beacons))  # TODO: BUG VALUE ABNORMALLY HIGH

            # Compute decision function to choose best technique
            position_technique = decision_system.compute_fuzzy_decision(fuzzy_system, fuzzy_technique
                                                                        , number_beacons_detected,
                                                                        similar_access_points,
                                                                        similar_beacons,
                                                                        size_of_beacons_known_locations)
            print('DECISION MADE. TECHNIQUE IS ' + position_technique)

            # Apply ML algorithm
            self.apply_ml_algorithm(position_technique=position_technique,
                                    radio_map_is_classifier=radio_map_is_classifier,
                                    matching_radio_map=matching_radio_map,
                                    beacons_known_locations=beacons_known_locations)

            position_dictionary = self.structure_position_results()

            self.update_position_results(position_dictionary)

            return Response(status=status.HTTP_200_OK)


'''
HELPER FUNCTIONS FOR MAIN FLOW
'''


def load_access_points_locations():
    file_heroku = '/app/access_points_location.json'
    file_local = 'access_points_location.json'
    with open(file_heroku) as json_file:
        data = json.load(json_file)
        access_points = {}
        for k, v in data.items():
            print('KEY: ' + k)
            access_points[k] = v
            print('X: ', v['x'])
            print('Y: ', v['y'])
            print('')
        return access_points


def check_zone(y):
    if y >= 3.0:
        return 'Personal'
    elif y >= 0.0 and y < 3.0:
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


def compute_csv_sample_trilateration(sample_dict, beacons_ml):
    csv_columns = ['BLE Beacon', 'coordinate_X', 'coordinate_Y', 'rssi_Value', 'rolling_mean_rssi', 'zone']
    sample = {}
    results_list_2d = list()
    for k in sample_dict:
        beacon = beacons_ml[k]
        sample['singleValue'] = beacon[0]
        sample['values'] = beacon[1]
        if 'zone' in sample:
            zone = sample['zone']
        else:
            zone = ''
        single_value_scanned = sample['singleValue']
        valuesScanned = sample['values']
        rolling_mean = np.mean(valuesScanned)
        print(rolling_mean)
        x_coordinate = 0.0
        y_coordinate = 0.0
        results_list = list()
        results_list.append(k)
        results_list.append(x_coordinate)
        results_list.append(y_coordinate)
        results_list.append(single_value_scanned)
        results_list.append(rolling_mean)
        results_list.append(zone)
        results_list_2d.append(results_list)
    display(results_list_2d)
    df = pd.DataFrame(data=results_list_2d, columns=csv_columns)
    display(df)
    return df


def compute_csv_sample(sample):
    csv_columns = ['coordinate_X', 'coordinate_Y', 'rssi_Value', 'rolling_mean_rssi', 'zone']
    if 'zone' in sample:
        zone = sample['zone']
    else:
        zone = ''
    single_value_scanned = sample['singleValue']
    valuesScanned = sample['values']
    aux_list = list()
    '''rolling_mean_list = list()
    for value in valuesScanned:
        aux_list.append(value)
        rolling_mean_list.append(np.mean(aux_list))'''
    rolling_mean = np.mean(valuesScanned)
    print(rolling_mean)
    x_coordinate = 0.0
    y_coordinate = 0.0
    results_list_2d = list()
    results_list = list()
    results_list.append(x_coordinate)
    results_list.append(y_coordinate)
    results_list.append(single_value_scanned)
    results_list.append(rolling_mean)
    results_list.append(zone)
    results_list_2d.append(results_list)
    display(results_list_2d)
    df = pd.DataFrame(data=results_list_2d, columns=csv_columns)
    display(df)
    return df


def compute_csv(request):
    csv_columns = ['coordinate_X', 'coordinate_Y', 'rssi_Value', 'rolling_mean_rssi', 'zone']
    sample = request.data
    if 'zone' in sample:
        zone = sample['zone']
    else:
        zone = ''
    single_value_scanned = sample['singleValue']
    valuesScanned = sample['values']
    aux_list = list()
    rolling_mean_list = list()
    for value in valuesScanned:
        aux_list.append(value)
        rolling_mean_list.append(np.mean(aux_list))
    print(rolling_mean_list)
    x_coordinate = sample['x_coordinate']
    y_coordinate = sample['y_coordinate']
    results_list_2d = list()
    for i in range(len(valuesScanned)):
        results_list = list()
        results_list.append(x_coordinate)
        results_list.append(y_coordinate)
        results_list.append(valuesScanned[i])
        results_list.append(rolling_mean_list[i])
        results_list.append(zone)
        results_list_2d.append(results_list)
    display(results_list_2d)
    df = pd.DataFrame(data=results_list_2d, columns=csv_columns)
    display(df)
    return df


def compute_csv_trilateration(sample, beacon_address):
    csv_columns = ['BLE Beacon', 'coordinate_X', 'coordinate_Y', 'rssi_Value', 'rolling_mean_rssi', 'zone']
    if 'zone' in sample:
        zone = sample['zone']
    else:
        zone = ''
    mac = beacon_address
    single_value_scanned = sample['singleValue']
    valuesScanned = sample['values']
    aux_list = list()
    rolling_mean_list = list()
    for value in valuesScanned:
        aux_list.append(value)
        rolling_mean_list.append(np.mean(aux_list))
    print(rolling_mean_list)
    x_coordinate = sample['x_coordinate']
    y_coordinate = sample['y_coordinate']
    results_list_2d = list()
    for i in range(len(valuesScanned)):
        results_list = list()
        results_list.append(mac)
        results_list.append(x_coordinate)
        results_list.append(y_coordinate)
        results_list.append(valuesScanned[i])
        results_list.append(rolling_mean_list[i])
        results_list.append(zone)
        results_list_2d.append(results_list)
    display(results_list_2d)
    df = pd.DataFrame(data=results_list_2d, columns=csv_columns)
    display(df)
    return df


'''
EXPERIMENT CLASSES FOR TRILATERATION/PROXIMITY/FINGERPRINTING
'''


class ProximityDistanceView(APIView):

    def post(self, request, formate=None):
        serializer_context = {
            'request': request,
        }
        df = compute_csv(request)
        if path.exists(".\dataset_test_university.csv"):
            df.to_csv(r'.\dataset_test_university.csv', mode='a', index=False, header=False)
        else:
            df.to_csv(r'.\dataset_test_university.csv', index=False, header=True)
        return Response(status=status.HTTP_200_OK)


class FilterView(APIView):

    def post(self, request, format=None):
        convertJson.jsonToFile('BluetoothWiFi')
        return Response(status=status.HTTP_200_OK)

    def apply_filter(self, filter_identifier, window_size):
        reference_points = Fingerprint.objects.raw(
            'SELECT indoorAppServer_fingerprint.id, indoorAppServer_fingerprint.coordinate_X,indoorAppServer_fingerprint.coordinate_Y, count(indoorAppServer_fingerprint.coordinate_X) FROM indoorAppServer_fingerprint GROUP BY indoorAppServer_fingerprint.coordinate_X, indoorAppServer_fingerprint.coordinate_Y')
        for rp in reference_points:
            fingerprints_per_reference_point = Fingerprint.objects.all().filter(coordinate_X=rp.coordinate_X,
                                                                                coordinate_Y=rp.coordinate_Y)
            number_fingerprints_per_reference_point = len(fingerprints_per_reference_point)
            number_partitions = int(number_fingerprints_per_reference_point / window_size)
            if number_partitions != number_fingerprints_per_reference_point:
                if filter_identifier == FilterEnum.MEDIAN_FILTER:
                    existing_fingerprint = fingerprints_per_reference_point.all()[:1]
                    filters.apply_median_filter(fingerprints_per_reference_point, TypeEnum.WIFI, existing_fingerprint)
                    filters.apply_median_filter(fingerprints_per_reference_point, TypeEnum.BLUETOOTH,
                                                existing_fingerprint)
                elif filter_identifier == FilterEnum.MEAN_FILTER:
                    existing_fingerprint = fingerprints_per_reference_point.all()[:1]
                    filters.apply_mean_filter(fingerprints_per_reference_point, TypeEnum.WIFI, existing_fingerprint)
                    filters.apply_mean_filter(fingerprints_per_reference_point, TypeEnum.BLUETOOTH,
                                              existing_fingerprint)


class TrilaterationHandlerView(APIView):

    def post(self, request, format=None):
        serializer_context = {
            'request': request,
        }
        room_limit_x_min = -2.0
        room_limit_x_max = 3.0
        room_limit_y_min = -1.0
        room_limit_y_max = 4.0
        access_points = load_access_points_locations()
        display(access_points)
        sample_dict = request.data
        print(sample_dict)
        distances = {}
        first_beacon = list(sample_dict.keys())[0]
        beacon_information = sample_dict[first_beacon]
        print(beacon_information)
        algorithm = beacon_information['algorithm']
        print(algorithm)
        if algorithm == 'KNN Regression':
            for k, v in sample_dict.items():
                df = compute_csv_trilateration(v, k)
                prediction_list = proximityPositioning.apply_knn_regressor(df)
                distances[k] = np.mean(prediction_list)
                print("PREDICTION")
                display(distances[k])
        elif algorithm == 'MLP Regression':
            for k, v in sample_dict.items():
                df = compute_csv_trilateration(v, k)
                prediction_list = proximityPositioning.apply_mlp_regressor(df)
                distances[k] = np.mean(prediction_list)
                print("PREDICTION")
                display(distances[k])
        elif algorithm == 'SVM Regressor':
            for k, v in sample_dict.items():
                df = compute_csv_trilateration(v, k)
                prediction_list = proximityPositioning.apply_svm_regressor(df)
                distances[k] = np.mean(prediction_list)
                print("PREDICTION")
                display(distances[k])
        elif algorithm == 'Linear Regression':
            for k, v in sample_dict.items():
                df = compute_csv_trilateration(v, k)
                prediction_list = proximityPositioning.apply_linear_regression(df)
                distances[k] = np.mean(prediction_list)
                print("PREDICTION")
                display(distances[k])
        print("DISTANCES ESTIMATIONS")
        print(distances)
        results_mse = {}
        for i in np.arange(room_limit_x_min, room_limit_x_max, 1.0):
            for j in np.arange(room_limit_y_min, room_limit_y_max, 1.0):
                mse = self.compute_trilateration(x=i, y=j, access_points=access_points, distances=distances)
                results_mse[(i, j)] = mse
                display(results_mse)
        print("DONE COMPUTING MSE")
        prediction = min(results_mse, key=results_mse.get)
        print(prediction)
        return compute_Response_Trilateration(prediction, False, serializer_context)

    def compute_distance_coordinate_system(self, x1, y1, x2, y2):
        dist = math.hypot(x2 - x1, y2 - y1)
        return dist

    def compute_trilateration(self, x, y, access_points, distances):
        squared_errors = 0.0
        locations = []
        distances_list = []
        for k, v in access_points.items():
            locations.append((v['x'], v['y']))
        for k, v in distances.items():
            distances_list.append(v)
        for aps, d in zip(locations, distances_list):
            print(aps)
            print(d)
            distance_calculated = self.compute_distance_coordinate_system(x, y, aps[0], aps[1])
            squared_errors += math.pow(distance_calculated - d, 2.0)
            mse = squared_errors / len(locations)
        print('MSE FOR POINT: x: ' + str(x) + ", y: " + str(y) + " is " + str(mse))
        return mse


class ProximityAlgorithmsView(APIView):

    def post(self, request, format=None):
        isClassifier = False
        serializer_context = {
            'request': request,
        }
        sample = request.data
        df = compute_csv(request)
        prediction = []
        algorithm = sample['algorithm']
        if algorithm == 'KNN Regression':
            prediction = proximityPositioning.apply_knn_regressor(df)
        elif algorithm == 'KNN Classifier':
            prediction = proximityPositioning.apply_knn_classifier(df)
            isClassifier = True
        # elif algorithm == 'MLP Regression':
        # prediction = proximityPositioning.apply_mlp_regressor(df)
        elif algorithm == 'MLP Classifier':
            # prediction = proximityPositioning.apply_mlp_classifier(df)
            isClassifier = True
        elif algorithm == 'SVM Classifier':
            prediction = proximityPositioning.apply_svm_classifier(df)
            isClassifier = True
        elif algorithm == 'SVM Regressor':
            prediction = proximityPositioning.apply_svm_regressor(df)
            isClassifier = True
        elif algorithm == 'Linear Regression':
            prediction = proximityPositioning.apply_linear_regression(df)
        elif algorithm == 'Random Forest Classifier':
            prediction = proximityPositioning.apply_randomForest_classifier(df)
            isClassifier = True

        return compute_Response(prediction, isClassifier, serializer_context)


class PositioningAlgorithmsView(APIView):

    def post(self, request, format=None):
        isClassifier = False
        serializer_context = {
            'request': request,
        }
        sample = request.data
        prediction = []
        filter = sample['filter']
        algorithm = sample['algorithm']
        dataTypes = sample['dataTypes']
        if filter == 'Mean':
            FilterView.apply_filter(FilterEnum.MEAN_FILTER, len(Fingerprint.objects.all()))
        elif filter == 'Median':
            FilterView.apply_filter(FilterEnum.MEDIAN_FILTER, len(Fingerprint.objects.all()))
        if algorithm == 'KNN Regression':
            prediction = fingerprintPositioning.apply_knn_regressor(dataTypes, sample['aps'], sample['beacons'],
                                                                    sample['deviceData'])
        elif algorithm == 'KNN Classifier':
            prediction = fingerprintPositioning.apply_knn_classifier(dataTypes, sample['aps'], sample['beacons'],
                                                                     sample['deviceData'])
            isClassifier = True
        #       elif algorithm == 'MLP Regression':
        # prediction = fingerprintPositioning.apply_mlp_regressor(dataTypes, sample['aps'], sample['beacons'], sample['deviceData'])
        elif algorithm == 'MLP Classifier':
            # prediction = fingerprintPositioning.apply_mlp_classifier(dataTypes, sample['aps'], sample['beacons'], sample['deviceData'])
            isClassifier = True
        elif algorithm == 'K-Means Classifier':
            prediction = fingerprintPositioning.apply_kmeans_knn_classifier(dataTypes, sample['aps'], sample['beacons'],
                                                                            sample['deviceData'])
            isClassifier = True
        elif algorithm == 'SVM Classifier':
            prediction = fingerprintPositioning.apply_svm_classifier(dataTypes, sample['aps'], sample['beacons'],
                                                                     sample['deviceData'])
            isClassifier = True

        return compute_Response(prediction, isClassifier, serializer_context)


'''
HELPER FUNCTIONS FOR EXPERIMENT CLASSES
'''


def compute_Response_Trilateration(prediction, isClassifier, serializer_context):
    print('prediction', prediction)
    if len(prediction) != 0:
        fingerprint = Fingerprint.objects.create(coordinate_X=prediction[0], coordinate_Y=prediction[1])
        print(fingerprint)
        print(prediction)
        serialized = FingerprintSerializer(fingerprint, context=serializer_context)
        return Response(serialized.data, status=status.HTTP_200_OK)
    else:
        return Response(status=status.HTTP_500_INTERNAL_SERVER_ERROR)


def compute_Response(prediction, isClassifier, serializer_context):
    print('prediction', prediction)
    if len(prediction) != 0:
        if isClassifier == True:
            fingerprint = Fingerprint.objects.create(coordinate_X=0.0, coordinate_Y=0.0, zone=prediction[0])
            print(fingerprint)
            serialized = FingerprintSerializer(fingerprint, context=serializer_context)
            return Response(serialized.data, status=status.HTTP_200_OK)
        else:
            fingerprint = Fingerprint.objects.create(coordinate_X=prediction[0][0], coordinate_Y=prediction[0][1])
            print(fingerprint)
            print(prediction)
            serialized = FingerprintSerializer(fingerprint, context=serializer_context)
            return Response(serialized.data, status=status.HTTP_200_OK)
    else:
        return Response(status=status.HTTP_500_INTERNAL_SERVER_ERROR)
