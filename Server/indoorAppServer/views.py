from django.shortcuts import render
from .models import Fingerprint, DeviceSensor, BluetoothSensor, WiFiSensor
from rest_framework import viewsets
from .serializers import FingerprintSerializer, DeviceDataSerializer, WifiDataSerializer, BluetoothSerializer
from rest_framework import status
from rest_framework.response import Response
from rest_framework.views import APIView
from enum import Enum
from scipy import signal
from .exceptions import exceptions
from django.db.models import Count,QuerySet


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


class FilterView(APIView):

    def get(self, request, format=None):

        self.apply_filter(FilterEnum.MEDIAN_FILTER,2)

        return Response(status=status.HTTP_200_OK)

    def apply_filter(self, filter_identifier, window_size):
        reference_points = Fingerprint.objects.raw('SELECT indoorAppServer_fingerprint.id, indoorAppServer_fingerprint.coordinate_X,indoorAppServer_fingerprint.coordinate_Y, count(indoorAppServer_fingerprint.coordinate_X) FROM indoorAppServer_fingerprint GROUP BY indoorAppServer_fingerprint.coordinate_X, indoorAppServer_fingerprint.coordinate_Y')
        for rp in reference_points:
            fingerprints_per_reference_point = Fingerprint.objects.all().filter(coordinate_X=rp.coordinate_X,coordinate_Y=rp.coordinate_Y)
            number_fingerprints_per_reference_point = len(fingerprints_per_reference_point)
            number_partitions = int(number_fingerprints_per_reference_point / window_size)
            if number_partitions != number_fingerprints_per_reference_point:
                wifi_raw_data_list = list()
                bluetooth_raw_data_list = list()
                for fingerprint in fingerprints_per_reference_point:
                    wifi_raw_data = WiFiSensor.objects.all().filter(fingerprint_id=fingerprint.id)
                    wifi_raw_data_list.extend(wifi_raw_data)
                    bluetooth_raw_data = BluetoothSensor.objects.all().filter(fingerprint_id=fingerprint.id)
                    bluetooth_raw_data_list.extend(bluetooth_raw_data)
                if filter_identifier == FilterEnum.MEDIAN_FILTER:
                    self.apply_median_filter(wifi_raw_data_list,window_size,number_partitions)
                    self.apply_median_filter(bluetooth_raw_data_list,window_size,number_partitions)
                elif filter_identifier == FilterEnum.MEAN_FILTER:
                    self.apply_mean_filter(wifi_raw_data_list)
                    self.apply_mean_filter(bluetooth_raw_data_list)


    def apply_median_filter(self, data,window_size,number_partitions):
        rssi_values = list()
        for item in data:
            rssi_values.append(item.rssi)
        for i in range(window_size):
            rssi_values_to_filter = rssi_values[i*number_partitions:number_partitions*i+number_partitions]
            print('PREVIOUS RSSI VALUES: ' + str(rssi_values_to_filter))
            rssi_values_to_filter = signal.medfilt(rssi_values_to_filter).tolist()
            print('UPDATED RSSI VALUES: ' + str(rssi_values_to_filter))
            rssi_values[i*number_partitions:number_partitions*i+number_partitions] = rssi_values_to_filter
            print('HERE: '+str(rssi_values))
        length_data = len(data)
        for i in range(data):
            data[i].rssi = rssi_values[i]
            data[i].save()

    def apply_mean_filter(self, data):
        rssi_values = list()
        for item in data:
            rssi_values.append(item.rssi)
        print('PREVIOUS RSSI VALUES: ' + str(rssi_values))
        rssi_values = signal.medfilt(rssi_values).tolist()
        print('UPDATED RSSI VALUES: ' + str(rssi_values))
        length_data = len(data)
        for i in range(length_data):
            data[i].rssi = rssi_values[i]
            data[i].save()
