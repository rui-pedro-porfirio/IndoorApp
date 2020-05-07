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
from django.core.serializers.json import DjangoJSONEncoder
from django.core import serializers
from scipy import signal
from .exceptions import exceptions
from django.db.models import Sum, Count
import json

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


def apply_median_filter(fingerprints_reference_point, type, existing_fingerprint):  # AP1,AP2,AP3,AP1,AP4
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


def apply_mean_filter(fingerprints_reference_point, type, existing_fingerprint):  # AP1,AP2,AP3,AP1,AP4
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
            rssi_value_median = statistics.mean(rssi_values)
            similar_beacons.delete()
            BluetoothSensor.objects.create(name=mac_address, rssi=rssi_value_median,
                                      fingerprint_id=existing_fingerprint[0].id)

def jsonToFile():
    fingerprints = Fingerprint.objects.all().values('id','coordinate_X','coordinate_Y')
    list_fingerprint = list(fingerprints)
    list_json_fingerprints = list()
    for fingerprint in list_fingerprint:
        dictionary_fingerprint = {'id':fingerprint['id'],'coordinate_X':fingerprint['coordinate_X'],'coordinate_Y':fingerprint['coordinate_Y']}
        device_data = DeviceSensor.objects.all().filter(fingerprint_id=fingerprint['id']).values('id','sensor_type','x_value','y_value','z_value','fingerprint_id')
        wifi_data = WiFiSensor.objects.all().filter(fingerprint_id=fingerprint['id']).values('id','name','rssi','fingerprint_id')
        bluetooth_data = BluetoothSensor.objects.all().filter(fingerprint_id=fingerprint['id']).values('id','name','rssi','fingerprint_id')
        dictionary_fingerprint['device'] = list(device_data)
        dictionary_fingerprint['wifi'] = list(wifi_data)
        dictionary_fingerprint['bluetooth'] = list(device_data)
        list_json_fingerprints.append(dictionary_fingerprint)
    dict_fingerprints = {'fingerprints': list_json_fingerprints}
    with open('radiomap.json', 'w') as outfile:
        json.dump(dict_fingerprints, outfile,cls=DjangoJSONEncoder, indent=3)


class FilterView(APIView):

    def get(self, request, format=None):
        self.apply_filter(FilterEnum.MEAN_FILTER, 1)
        jsonToFile()
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
                    apply_median_filter(fingerprints_per_reference_point, TypeEnum.WIFI,existing_fingerprint)
                    apply_median_filter(fingerprints_per_reference_point, TypeEnum.BLUETOOTH,existing_fingerprint)
                elif filter_identifier == FilterEnum.MEAN_FILTER:
                    existing_fingerprint = fingerprints_per_reference_point.all()[:1]
                    apply_mean_filter(fingerprints_per_reference_point, TypeEnum.WIFI,existing_fingerprint)
                    apply_mean_filter(fingerprints_per_reference_point, TypeEnum.BLUETOOTH,existing_fingerprint)
