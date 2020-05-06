from django.shortcuts import render
from .models import Fingerprint, DeviceSensor, BluetoothSensor, WiFiSensor
from rest_framework import viewsets
from .serializers import FingerprintSerializer, DeviceDataSerializer, WifiDataSerializer, BluetoothSerializer
from rest_framework import status
from rest_framework.response import Response
from rest_framework.views import APIView
from enum import Enum


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

class FilterView(APIView):

     def get(self, request, format=None):
        '''
        fingerprints = Fingerprint.objects.all()
        serializer_context = {
            'request': request,
        }
        serializer = FingerprintSerializer(fingerprints, context=serializer_context,many=True)
        '''
        serializer_context = {
            'request': request,
        }
        device_data = DeviceSensor.objects.all()
        device_data = self.apply_filter(FilterEnum.MEDIAN_FILTER,device_data)
        serializer = DeviceDataSerializer(device_data, context=serializer_context, many=True)
        return Response(serializer.data,status=status.HTTP_200_OK)

     def apply_filter(self,filter_identifier,data):
        if filter_identifier == FilterEnum.MEDIAN_FILTER:
            data = self.apply_median_filter(data)
        else:
            '''TODO: DO OTHER STUFF'''
        return data

     def apply_median_filter(self,data):
         return data
