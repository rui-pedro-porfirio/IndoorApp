from django.shortcuts import render
from .models import Fingerprint, DeviceSensor, BluetoothSensor, WiFiSensor
from rest_framework import viewsets
from .serializers import FingerprintSerializer, DeviceDataSerializer, WifiDataSerializer, BluetoothSerializer
from rest_framework import status
from rest_framework.response import Response
from rest_framework.views import APIView
from enum import Enum
import math
import statistics
from scipy import signal
from .exceptions import exceptions
from django.db.models import Sum, Count


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


class FilterView(APIView):

    def get(self, request, format=None):

        self.apply_filter(FilterEnum.MEDIAN_FILTER, 2)

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
                    self.apply_median_filter(fingerprints_per_reference_point, TypeEnum.WIFI,existing_fingerprint)
                    self.apply_median_filter(fingerprints_per_reference_point, TypeEnum.BLUETOOTH,existing_fingerprint)
                elif filter_identifier == FilterEnum.MEAN_FILTER:
                    self.apply_mean_filter(wifi_raw_data_list)
                    self.apply_mean_filter(bluetooth_raw_data_list)

    def apply_median_filter(self, fingerprints_reference_point, type,existing_fingerprint):  # AP1,AP2,AP3,AP1,AP4
        if type == TypeEnum.WIFI:
            access_points = WiFiSensor.objects.all().filter(fingerprint_id__in=fingerprints_reference_point.values('id')).values('name').annotate(
                Count('name'))  # F1: AP1, AP2, AP3
            for ap in access_points:  # AP1
                similar_access_points = WiFiSensor.objects.filter(name=ap['name']).order_by('rssi')
                rssi_values = list()
                mac_address = ''
                for sap in similar_access_points:
                    rssi_values.append(sap.rssi)
                    mac_address = sap.name
                rssi_value_median = statistics.median(rssi_values)
                similar_access_points.delete()
                WiFiSensor.objects.create(name=mac_address,rssi=rssi_value_median,fingerprint_id=existing_fingerprint[0].id)
        elif type == TypeEnum.BLUETOOTH:
            beacons = BluetoothSensor.objects.all().filter(
                fingerprint_id__in=fingerprints_reference_point.values('id')).values('name').annotate(
                Count('name'))  # F1: AP1, AP2, AP3
            for beacon in beacons:  # AP1
                similar_beacons = BluetoothSensor.objects.filter(name=beacon['name']).order_by('rssi')
                rssi_values = list()
                mac_address = ''
                for similar_beacon in similar_beacons:
                    rssi_values.append(similar_beacon.rssi)
                    mac_address = similar_beacon.name
                rssi_value_median = statistics.median(rssi_values)
                similar_beacons.delete()
                BluetoothSensor.objects.create(name=mac_address, rssi=rssi_value_median,
                                          fingerprint_id=existing_fingerprint[0].id)


    def apply_mean_filter(self, data):
        rssi_values = list()
        for item in data:
            rssi_values.append(item.rssi)
        print('PREVIOUS RSSI VALUES: ' + str(rssi_values))
        print('UPDATED RSSI VALUES: ' + str(rssi_values))
        length_data = len(data)
        for i in range(length_data):
            data[i].rssi = rssi_values[i]
            data[i].save()
