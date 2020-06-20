from django.shortcuts import render
from .models import Fingerprint, DeviceSensor, BluetoothSensor, WiFiSensor
from rest_framework import viewsets
from .serializers import FingerprintSerializer, DeviceDataSerializer, WifiDataSerializer, BluetoothSerializer
from rest_framework import status
from rest_framework.response import Response
from rest_framework.views import APIView
import os.path
from os import path
import pandas as pd
import numpy as np
from IPython.core.display import display
from enum import Enum
from .snippets import filters, convertJson,positioning


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

class ProximityDistanceView(APIView):

    def post(self,request,formate=None):
        serializer_context = {
            'request': request,
        }
        csv_columns = ['coordinate_X', 'coordinate_Y', 'rssi_Value','rolling_mean_rssi','zone']
        sample = request.data
        zone = sample['zone']
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
        df = pd.DataFrame(data=results_list_2d,columns=csv_columns)
        display(df)
        if path.exists(".\distance_proximity_experiments.csv"):
            df.to_csv(r'.\distance_proximity_experiments.csv', mode='a',index=False, header=False)
        else:
            df.to_csv(r'.\distance_proximity_experiments.csv',index=False, header=True)
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
            prediction = positioning.apply_knn_regressor(dataTypes,sample['aps'],sample['beacons'],sample['deviceData'])
        elif algorithm == 'KNN Classifier':
            prediction = positioning.apply_knn_classifier(dataTypes,sample['aps'],sample['beacons'],sample['deviceData'])
            isClassifier = True
        elif algorithm == 'MLP Regression':
            prediction = positioning.apply_mlp_regressor(dataTypes,sample['aps'],sample['beacons'],sample['deviceData'])
        elif algorithm == 'MLP Classifier':
            prediction = positioning.apply_mlp_classifier(dataTypes,sample['aps'],sample['beacons'],sample['deviceData'])
            isClassifier = True
        elif algorithm == 'K-Means Classifier':
            prediction = positioning.apply_kmeans_knn_classifier(dataTypes,sample['aps'],sample['beacons'],sample['deviceData'])
            isClassifier = True
        elif algorithm == 'SVM Classifier':
            prediction = positioning.apply_svm_classifier(dataTypes,sample['aps'],sample['beacons'],sample['deviceData'])
            isClassifier = True
        print('prediction',prediction)
        if len(prediction) != 0:
            if isClassifier == True:
                fingerprint = Fingerprint.objects.create(coordinate_X=0.0,coordinate_Y= 0.0,zone=prediction[0])
                print(fingerprint)
                serialized = FingerprintSerializer(fingerprint, context=serializer_context)
                return Response(serialized.data, status=status.HTTP_200_OK)
            else:
                fingerprint = Fingerprint.objects.create(coordinate_X=prediction[0][0],coordinate_Y=prediction[0][1])
                print(fingerprint)
                print(prediction)
                serialized = FingerprintSerializer(fingerprint,context=serializer_context)
                return Response(serialized.data,status=status.HTTP_200_OK)
        else:
            return Response(status=status.HTTP_500_INTERNAL_SERVER_ERROR)
