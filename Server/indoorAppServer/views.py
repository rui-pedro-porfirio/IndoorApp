from django.shortcuts import render
from .models import Fingerprint, DeviceSensor, BluetoothSensor, WiFiSensor,UserTable
from rest_framework import viewsets
from .serializers import FingerprintSerializer, DeviceDataSerializer, WifiDataSerializer, BluetoothSerializer, UserSerializer
from rest_framework import status
from rest_framework.response import Response
from rest_framework.views import APIView
import json
import scipy.optimize as opt
import os.path
from os import path
import pandas as pd
import numpy as np
from IPython.core.display import display
from enum import Enum
from .snippets import radiomap,filters, convertJson,fingerprintPositioning, proximityPositioning,decision_system, websockets
import json
import math
from os import environ

# Initialize configuration functions and variables in Django
fuzzy_dict = decision_system.create_fuzzy_system()
fuzzy_system = fuzzy_dict['System']
fuzzy_technique = fuzzy_dict['Technique MF']
decision_system.test_phase(fuzzy_system,fuzzy_technique)
websockets.publish('RPP','Personal')

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



class ScanningView(APIView):

    def post(self,request,format=None):
        serializer_context = {
            'request': request,
        }
        sample_dict = request.data
        print(sample_dict)
        username = sample_dict['username']
        access_points = sample_dict['mAccessPoints']
        beacons = sample_dict['mBeaconsList']
        sensors = sample_dict['mSensorInformationList']
        positionRegression = None
        positionClassification = None
        isClassifier = False
        access_points_ml = {}
        beacons_ml = {}
        # Find number of Matching access_points
        access_points_scanned = list()
        for ap_object in access_points:
            access_points_scanned.append(ap_object['name'])
            access_points_ml[ap_object['name']] = ap_object['singleValue']
        beacons_scanned = list()
        for beacon_object in beacons:
            beacons_scanned.append(beacon_object['name'])
            single_list = beacon_object['singleValue']
            rolling_list = beacon_object['values']
            extending_tp = (single_list,rolling_list)
            beacons_ml[beacon_object['name']] = extending_tp
        number_beacons = len(beacons_scanned)
        matching_data = radiomap.compute_matching_data(access_points_scanned,beacons_scanned)
        empty_dict = not bool(matching_data)
        if matching_data == None or empty_dict == True:
            raise Exception('Impossible to get data. No data returned from BLE and Wifi')
        else:
            isClassifier = matching_data['isClassifier']
            input_aps = matching_data['length_wifi']
            print('Number of Matching access_points: ' + str(input_aps)) #TODO: BUG VALUE ABNORMALLY HIGH
            input_beacons = matching_data['length_ble']
            print('Number of Matching beacons: ' + str(input_beacons)) #TODO: BUG VALUE ABNORMALLY HIGH
            beacons_positions = load_access_points_locations()
            beacons_locations_length = len(beacons_positions)
            #Compute decision function to choose best technique
            position_technique = decision_system.compute_fuzzy_decision(fuzzy_system,fuzzy_technique
                                                    ,number_beacons,input_aps,input_beacons,beacons_locations_length)
            position_technique = 'Trilateration'
            print('DECISION MADE. TECHNIQUE IS ' + position_technique)
            #Apply ML algorithm
            if position_technique == 'Fingerprinting':
                print('Fingerprinting chosen. Applying Random Forest Algorithm')
                #Apply RF to Regression
                positionRegression = fingerprintPositioning.apply_rf_regressor_scanning(matching_data['dataset'],access_points_ml,beacons_ml)
                #Apply RF to Classification
                if isClassifier:
                    positionClassification = fingerprintPositioning.apply_rf_classification_scanning(matching_data['dataset'],access_points_ml,beacons_ml)
                print('ML Algorithm done running.')
            elif position_technique == 'Trilateration':
                # TODO: Apply ML to Trilateration
                print('Trilateration')
                isClassifier = True
                matching_data['dataset'] = 'D:/College/5th Year College/TESE/Desenvolvimento/Code/Application/findLocationApp/findLocation/Server/Notebooks/PROXIMITY/dataset_train_university.csv'
                prediction = None
                min_distance = float('inf')
                closest_location = None
                sorted_dict = sorted(beacons_ml, key=lambda k: len(beacons_ml[k][1]), reverse=True)
                test_df = compute_csv_sample_trilat(sorted_dict,beacons_ml)
                display(test_df)
                distance_predictions= {}
                rfv = test_df.groupby(['BLE Beacon'])
                for k, v in rfv:
                    print("K: " + str(k))
                    print("V: " + str(v))
                    distance_prediction = proximityPositioning.apply_knn_regression_scanning(matching_data['dataset'],v)
                    distance_predictions[k] = distance_prediction[0,0]
                print('DISTANCE PREDICTION: ' + str(distance_predictions))
                for k,v in distance_predictions.items():
                    if v < min_distance:
                        min_distance = v
                        closest_location = beacons_positions[k]
                initial_location = closest_location
                initial_location_tuple = (initial_location['x'], initial_location['y'])
                result = opt.minimize(
                    mse,  # The error function
                    initial_location_tuple,# The initial guess
                    args=(rfv,
                    distance_predictions,
                    beacons_positions),
                    method='L-BFGS-B',  # The optimisation algorithm
                    options={
                        'ftol': 1e-5,  # Tolerance
                        'maxiter': 1e+7  # Maximum iterations
                    })
                prediction = result.x
                print("GUESSED: " + str(prediction))
                positionRegression = (prediction[0],prediction[1])
                if isClassifier:
                    positionClassification = check_zone(prediction[1])
            elif position_technique == 'Proximity':
                isClassifier = True
                matching_data['dataset'] = 'D:/College/5th Year College/TESE/Desenvolvimento/Code/Application/findLocationApp/findLocation/Server/Notebooks/PROXIMITY/dataset_train_university.csv'
                sample = {}
                sorted_dict = sorted(beacons_ml, key=lambda k: len(beacons_ml[k][1]), reverse=True)
                highest_beacon = beacons_ml[sorted_dict[0]]
                sample['singleValue'] = highest_beacon[0]
                sample['values'] = highest_beacon[1]
                test_df = compute_csv_sample(sample)
                # Apply ML to Proximity
                print('Proximity chosen. Applying KNN Algorithm')
                # Apply ML to Regression
                positionRegression = proximityPositioning.apply_knn_regression_scanning(matching_data['dataset'],test_df)
                # Apply ML to Classification
                if isClassifier:
                    positionClassification = proximityPositioning.apply_knn_classification_scanning(matching_data['dataset'],test_df)
                print('ML Algorithm done running')
            #TODO: GET POSITION OF USER
            position = 0.5
            #TODO: SEND PUBLISH TO YANUX


def load_access_points_locations():
    with open('access_points_location.json') as json_file:
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


def mse(x,rfv,distances,beacons):
    squared_errors = 0.0
    empty_list = {}
    x= (x[0],x[1])
    for k, v in rfv:
        distance_known = distances[k]
        distance_computed = compute_distance_coordinate_system(x[0],x[1],beacons[k]['x'],beacons[k]['y'])
        squared_errors += compute_squared_errors(distance_known,distance_computed)
    mse = squared_errors / len(rfv)
    return mse


def compute_squared_errors(d1,d2):
    squared_errors = math.pow(d1 - d2, 2.0)
    return squared_errors


def compute_distance_coordinate_system(x1,y1,x2,y2):
    dist = math.hypot(x2 - x1, y2 - y1)
    return dist


def compute_csv_sample_trilat(sample_dict,beacons_ml):
    csv_columns = ['BLE Beacon','coordinate_X', 'coordinate_Y', 'rssi_Value', 'rolling_mean_rssi', 'zone']
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

def compute_csv_trilateration(sample,beacon_address):
    csv_columns = ['BLE Beacon','coordinate_X', 'coordinate_Y', 'rssi_Value', 'rolling_mean_rssi', 'zone']
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

class ProximityDistanceView(APIView):

    def post(self,request,formate=None):
        serializer_context = {
            'request': request,
        }
        df = compute_csv(request)
        if path.exists(".\dataset_test_university.csv"):
            df.to_csv(r'.\dataset_test_university.csv', mode='a',index=False, header=False)
        else:
            df.to_csv(r'.\dataset_test_university.csv',index=False, header=True)
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


    def post(self,request,format=None):
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
        for i in np.arange(room_limit_x_min,room_limit_x_max,1.0):
            for j in np.arange(room_limit_y_min,room_limit_y_max,1.0):
                mse = self.compute_trilateration(x=i,y=j,access_points=access_points,distances=distances)
                results_mse[(i,j)]=mse
                display(results_mse)
        print("DONE COMPUTING MSE")
        prediction = min(results_mse,key=results_mse.get)
        print(prediction)
        return compute_Response_Trilateration(prediction,False,serializer_context)

    def compute_distance_coordinate_system(self,x1,y1,x2,y2):
        dist = math.hypot(x2 - x1, y2 - y1)
        return dist

    def compute_trilateration(self,x,y,access_points,distances):
        squared_errors = 0.0
        locations = []
        distances_list = []
        for k, v in access_points.items():
            locations.append((v['x'],v['y']))
        for k,v in distances.items():
            distances_list.append(v)
        for aps, d in zip(locations,distances_list):
            print(aps)
            print(d)
            distance_calculated = self.compute_distance_coordinate_system(x,y,aps[0],aps[1])
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
        elif algorithm == 'MLP Regression':
            prediction = proximityPositioning.apply_mlp_regressor(df)
        elif algorithm == 'MLP Classifier':
            prediction = proximityPositioning.apply_mlp_classifier(df)
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

        return compute_Response(prediction,isClassifier,serializer_context)


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
            FilterView.apply_filter(FilterEnum.MEAN_FILTER,len(Fingerprint.objects.all()))
        elif filter == 'Median':
            FilterView.apply_filter(FilterEnum.MEDIAN_FILTER,len(Fingerprint.objects.all()))
        if algorithm == 'KNN Regression':
            prediction = fingerprintPositioning.apply_knn_regressor(dataTypes, sample['aps'], sample['beacons'], sample['deviceData'])
        elif algorithm == 'KNN Classifier':
            prediction = fingerprintPositioning.apply_knn_classifier(dataTypes, sample['aps'], sample['beacons'], sample['deviceData'])
            isClassifier = True
        elif algorithm == 'MLP Regression':
            prediction = fingerprintPositioning.apply_mlp_regressor(dataTypes, sample['aps'], sample['beacons'], sample['deviceData'])
        elif algorithm == 'MLP Classifier':
            prediction = fingerprintPositioning.apply_mlp_classifier(dataTypes, sample['aps'], sample['beacons'], sample['deviceData'])
            isClassifier = True
        elif algorithm == 'K-Means Classifier':
            prediction = fingerprintPositioning.apply_kmeans_knn_classifier(dataTypes, sample['aps'], sample['beacons'], sample['deviceData'])
            isClassifier = True
        elif algorithm == 'SVM Classifier':
            prediction = fingerprintPositioning.apply_svm_classifier(dataTypes, sample['aps'], sample['beacons'], sample['deviceData'])
            isClassifier = True

        return compute_Response(prediction,isClassifier,serializer_context)


def compute_Response_Trilateration(prediction,isClassifier,serializer_context):
    print('prediction', prediction)
    if len(prediction) != 0:
            fingerprint = Fingerprint.objects.create(coordinate_X=prediction[0], coordinate_Y=prediction[1])
            print(fingerprint)
            print(prediction)
            serialized = FingerprintSerializer(fingerprint, context=serializer_context)
            return Response(serialized.data, status=status.HTTP_200_OK)
    else:
        return Response(status=status.HTTP_500_INTERNAL_SERVER_ERROR)


def compute_Response(prediction,isClassifier,serializer_context):
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