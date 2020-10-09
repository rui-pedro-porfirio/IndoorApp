import math
from enum import Enum
from os import path

import numpy as np
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
    websockets, initializationModule, csvHandler, common

'''
 INITIALIZATION OF THE FUZZY SET SYSTEM AND TEST THE WS COMMUNICATION
'''
# aps = ['c4:e9:84:42:ac:ff', '00:06:91:d4:77:00', '00:06:91:d4:77:02', '8c:5b:f0:78:a1:d6', '1c:ab:c0:df:99:c8', '1c:ab:c0:df:99:c9', '00:26:5b:d1:93:38', '00:26:5b:d1:93:39', '00:fc:8d:cf:98:08', '00:fc:8d:cf:98:09']
# beacons=['FF:20:88:3C:97:E7','CA:E0:7D:11:26:B3']

print('Initializing Django Server...')
fuzzy_dict = initializationModule.create_and_assert_fuzzy_system()
fuzzy_system = fuzzy_dict['System']
fuzzy_technique = fuzzy_dict['Technique MF']
# initializationModule.test_ws_communication()
trained_radio_maps = initializationModule.train_existent_radio_maps()
proximityPositioning.structure_dataset()
print('Server initialization finished with code 0.')

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
    environment_name = None
    access_points_ml = {}
    beacons_ml = {}
    access_points_structured = list()
    beacons_structured = list()
    beacons_ml_fingerprinting = {}
    beacons_name = {}

    def clear_cache(self):
        self.username = None
        self.deviceUuid = None
        self.accessPointsDetected = None
        self.beaconsDetected = None
        self.deviceSensorDetected = None
        self.position_regression = None
        self.position_classification = None
        self.environment_name = None
        self.access_points_ml = {}
        self.beacons_ml = {}
        self.beacons_name = {}
        self.access_points_structured = list()
        self.beacons_structured = list()
        self.beacons_ml_fingerprinting = {}

    def configure_data_structures(self, sample_dict):
        self.clear_cache()
        self.username = sample_dict['username']
        self.deviceUuid = sample_dict['uuid']
        self.accessPointsDetected = sample_dict['mAccessPoints']
        self.beaconsDetected = sample_dict['mBeaconsList']
        self.deviceSensorDetected = sample_dict['mSensorInformationList']

        self.structure_access_points_data()
        self.structure_beacons_data()

    def structure_access_points_data(self):
        for access_point in self.accessPointsDetected:
            self.access_points_structured.append(access_point['name'])
            self.access_points_ml[access_point['name']] = access_point['singleValue']

    def structure_beacons_data(self):
        for beacon in self.beaconsDetected:
            self.beacons_structured.append(beacon['mac'])
            single_list = beacon['singleValue']
            rolling_list = beacon['values']
            extending_tp = (single_list, rolling_list)
            self.beacons_ml[beacon['mac']] = extending_tp
            self.beacons_ml_fingerprinting[beacon['mac']] = single_list
            self.beacons_name[beacon['mac']] = beacon['name']

    def match_radio_map_similarity(self):
        return radiomap.compute_matching_data(self.access_points_structured, self.beacons_structured)

    def find_beacons_location(self):
        result_dict = dict()
        beacons_known_positions = common.load_access_points_locations()
        available_beacons = dict()
        for location_name, locations in beacons_known_positions.items():
            for beacon, position in locations.items():
                if beacon in self.beacons_structured:
                    available_beacons[beacon] = position
                    if self.environment_name is None:
                        self.environment_name = location_name
                elif beacon in self.beacons_name.values():
                    beacon_address = common.get_key(self.beacons_name,beacon)
                    available_beacons[beacon_address] = position
                    if self.environment_name is None:
                        self.environment_name = location_name
        result_dict['beacons_known_positions'] = available_beacons
        result_dict['beacons_locations_length'] = len(available_beacons)
        return result_dict

    def structure_radio_map_for_fuzzy_system(self, radio_map):
        result_dict = {'isClassifier': radio_map['isClassifier'], 'input_aps': radio_map['length_wifi'],
                       'input_beacons': radio_map['length_ble']}
        beacons_location = self.find_beacons_location()
        result_dict['beacons_known_positions'] = beacons_location['beacons_known_positions']
        result_dict['beacons_locations_length'] = beacons_location['beacons_locations_length']
        return result_dict

    def apply_ml_algorithm(self, position_technique, radio_map_is_classifier,
                           matching_radio_map, beacons_known_locations):
        if position_technique == 'Fingerprinting':
            print('Fingerprinting chosen. Applying Random Forest Algorithm')
            self.apply_fingerprinting(matching_radio_map=matching_radio_map,
                                      radio_map_is_classifier=radio_map_is_classifier)
            print('ML Algorithm done running.')
            position_dictionary = self.structure_position_results(position_technique)

            self.update_position_results(position_dictionary, position_technique)
        elif position_technique == 'Trilateration':
            print('Trilateration chosen.')
            self.apply_trilateration(beacons_known_locations)
            print('ML Algorithm done running.')
            position_dictionary = self.structure_position_results(position_technique)

            self.update_position_results(position_dictionary, position_technique)
        elif position_technique == 'Proximity':
            print('Proximity chosen.')

            for beacon, rssi_values in self.beacons_ml.items():
                beacon_name = self.beacons_name[beacon]
                self.apply_proximity(self.beacons_ml[beacon])
                position_dictionary = self.structure_position_results(position_technique)

                self.update_position_results(position_dictionary, position_technique, beacon_name)
            print('ML Algorithm done running')

    def apply_fingerprinting(self, matching_radio_map, radio_map_is_classifier):
        # Apply RF to Regression
        self.environment_name = matching_radio_map['dataset']
        self.position_regression = fingerprintPositioning.apply_rf_regressor_scanning(
            estimator_options=trained_radio_maps[matching_radio_map['dataset']],
            radio_map=matching_radio_map['dataset'],
            access_points=self.access_points_ml,
            beacons=self.beacons_ml_fingerprinting)
        # Apply RF to Classification
        if radio_map_is_classifier:
            self.position_classification = fingerprintPositioning.apply_rf_classification_scanning(
                estimator_options=trained_radio_maps[matching_radio_map['dataset']],
                radio_map=matching_radio_map['dataset'],
                access_points=self.access_points_ml,
                beacons=self.beacons_ml_fingerprinting)

    def apply_proximity(self, target_beacon):
        sample = {}

        sample['singleValue'] = target_beacon[0]
        sample['values'] = target_beacon[1]

        # Structure sample into a csv
        test_df = csvHandler.compute_csv_in_scanning_phase(sample)

        # Apply KNN to Regression
        self.position_regression = proximityPositioning.apply_knn_regression_scanning(test_df)
        # Apply KNN to Classification
        self.position_classification = proximityPositioning.apply_knn_classification_scanning(test_df)

    def apply_trilateration(self, beacons_known_locations):

        # Trilateration with LSE variables
        min_distance = float('inf')
        closest_location = None

        # Sort beacons by number of samples recorded
        sorted_dict = sorted(self.beacons_ml, key=lambda k: len(self.beacons_ml[k][1]), reverse=True)

        # Structure beacons into csv file
        test_df = csvHandler.compute_csv_in_scanning_phase_trilateration(sorted_dict, self.beacons_ml)

        # Compute distance predictions using proximity KNN algorithm from the user and the beacons locations
        distance_predictions = {}
        rfv = test_df.groupby(['BLE Beacon'])
        for k, v in rfv:
            distance_prediction = proximityPositioning.apply_knn_regression_scanning(v)
            distance_predictions[k] = distance_prediction[0, 0]
        print('Distance Prediction to each beacon: ' + str(distance_predictions))

        # Find the nearest beacon
        for k, v in distance_predictions.items():
            if v < min_distance:
                min_distance = v
                closest_location = beacons_known_locations[k]

        # Compute SciPy minimize function to obtain position prediction
        initial_location = closest_location
        initial_location_tuple = (initial_location['x'], initial_location['y'])
        result = opt.minimize(
            common.mse,  # The error function
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

        self.position_regression = (prediction[0], prediction[1])
        self.position_classification = common.check_zone(prediction[1])  # Use an implicit classification method

    def structure_position_results(self, position_technique):
        position_dict = {}
        if position_technique is 'Trilateration':
            position_dict['Regression'] = self.position_regression
            position_dict['Classification'] = self.position_classification
        else:
            if self.position_regression is not None:
                if len(self.position_regression) == 2:
                    position_dict['Regression'] = (self.position_regression[0][0], self.position_regression[0][1])
                else:
                    position_dict['Regression'] = self.position_regression[0][0]
            if self.position_classification is not None:
                position_dict['Classification'] = self.position_classification[0]
        return position_dict

    def update_position_results(self, position_dict, position_technique, beacon_name=None):
        print('Position computed. Sending update to subscribers.')
        radio_map_identifier = None
        beacon = None
        if position_technique is 'Trilateration' or position_technique is 'Fingerprinting':
            radio_map_identifier = self.environment_name
        if position_technique is 'Proximity':
            beacon = beacon_name
        websockets.publish(self.username, self.deviceUuid, position_dict, radio_map_identifier, beacon)

    def post(self, request):
        serializer_context = {
            'request': request,
        }
        print('Received POST Request with Scanning Data. Computing Position...')
        sample_dict = request.data

        self.configure_data_structures(sample_dict)

        number_beacons_detected = len(self.beacons_structured)

        matching_radio_map = self.match_radio_map_similarity()

        radio_map_is_empty = not bool(matching_radio_map)
        if matching_radio_map is None or radio_map_is_empty:
            radio_map_is_classifier = False
            similar_access_points = 0
            similar_beacons = 0
            beacons_location = self.find_beacons_location()
            beacons_known_locations = beacons_location['beacons_known_positions']
            size_of_beacons_known_locations = beacons_location['beacons_locations_length']
            print('% of Matching access_points: ' + str(similar_access_points))
            print('Number of Matching beacons: ' + str(similar_beacons))
        else:
            radio_map_data = self.structure_radio_map_for_fuzzy_system(matching_radio_map)
            radio_map_is_classifier = radio_map_data['isClassifier']
            similar_access_points = radio_map_data['input_aps']
            similar_beacons = radio_map_data['input_beacons']
            beacons_known_locations = radio_map_data['beacons_known_positions']
            size_of_beacons_known_locations = radio_map_data['beacons_locations_length']
            print('% of Matching access_points: ' + str(similar_access_points))
            print('Number of Matching beacons: ' + str(similar_beacons))

        # Compute decision function to choose best technique
        position_technique = decision_system.compute_fuzzy_decision(fuzzy_system, fuzzy_technique
                                                                    , number_beacons_detected,
                                                                    similar_access_points,
                                                                    similar_beacons,
                                                                    size_of_beacons_known_locations)
        print('DECISION MADE. TECHNIQUE IS ' + position_technique)

        if position_technique == 'None':
            return Response(status=status.HTTP_404_NOT_FOUND)
        else:
            # Apply ML algorithm
            self.apply_ml_algorithm(position_technique=position_technique,
                                    radio_map_is_classifier=radio_map_is_classifier,
                                    matching_radio_map=matching_radio_map,
                                    beacons_known_locations=beacons_known_locations)

        return Response(status=status.HTTP_200_OK)


'''
EXPERIMENT CLASSES FOR TRILATERATION/PROXIMITY/FINGERPRINTING
'''


class ProximityDistanceView(APIView):

    def post(self, request, formate=None):
        serializer_context = {
            'request': request,
        }
        df = csvHandler.compute_csv(request)
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
        access_points = common.load_access_points_locations()
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
                df = csvHandler.compute_csv_trilateration(v, k)
                prediction_list = proximityPositioning.apply_knn_regressor(df)
                distances[k] = np.mean(prediction_list)
                print("PREDICTION")
                display(distances[k])
        elif algorithm == 'MLP Regression':
            for k, v in sample_dict.items():
                df = csvHandler.compute_csv_trilateration(v, k)
                prediction_list = proximityPositioning.apply_mlp_regressor(df)
                distances[k] = np.mean(prediction_list)
                print("PREDICTION")
                display(distances[k])
        elif algorithm == 'SVM Regressor':
            for k, v in sample_dict.items():
                df = csvHandler.compute_csv_trilateration(v, k)
                prediction_list = proximityPositioning.apply_svm_regressor(df)
                distances[k] = np.mean(prediction_list)
                print("PREDICTION")
                display(distances[k])
        elif algorithm == 'Linear Regression':
            for k, v in sample_dict.items():
                df = csvHandler.compute_csv_trilateration(v, k)
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
        df = csvHandler.compute_csv(request)
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
