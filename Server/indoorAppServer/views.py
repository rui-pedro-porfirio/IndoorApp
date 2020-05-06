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


class TypeEnum(Enum):
    DEVICE_DATA = 0
    WIFI_DATA = 1
    BLUETOOTH_DATA = 2


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
        device_data = self.apply_filter(FilterEnum.MEDIAN_FILTER,device_data,TypeEnum.DEVICE_DATA)
        wifi_data = WiFiSensor.objects.all()
        wifi_data = self.apply_filter(FilterEnum.MEDIAN_FILTER,wifi_data,TypeEnum.WIFI_DATA)
        bluetooth_data = BluetoothSensor.objects.all()
        bluetooth_data = self.apply_filter(FilterEnum.MEDIAN_FILTER,bluetooth_data,TypeEnum.BLUETOOTH_DATA)
        serializer = DeviceDataSerializer(device_data, context=serializer_context, many=True)
        return Response(serializer.data,status=status.HTTP_200_OK)

     def apply_filter(self,filter_identifier,data,type):
        if filter_identifier == FilterEnum.MEDIAN_FILTER:
            data = self.apply_median_filter(data,3,type)
        else:
            '''TODO: DO OTHER STUFF'''
        return data

     def apply_median_filter(self,data,window_size,type):
        number_neighbours = window_size -1
        if type == TypeEnum.DEVICE_DATA:
            x_values = list()
            y_values = list()
            z_values = list()
            for item in data:
                x_values.append(item.x_value)
                y_values.append(item.y_value)
                z_values.append(item.z_value)
            print('Previous X Values: '+str(x_values))
            print('Previous Y Values: '+str(y_values))
            print('Previous Z Values: '+str(z_values))
             #APPLY MEAN FILTER TO EACH ARRAY
            x_values = signal.medfilt(x_values).tolist()
            y_values = signal.medfilt(y_values).tolist()
            z_values = signal.medfilt(z_values).tolist()
            print('Updated X Values: ' + str(x_values))
            print('Updated Y Values: ' + str(y_values))
            print('Updated Z Values: ' + str(z_values))
            #UPDATE ON DEVICE SENSOR THE NEW VALUES FOR EACH COORDINATE
            length_data = len(data)
            for i in range(length_data):
                data[i].x_value = x_values[i]
                data[i].y_value = y_values[i]
                data[i].z_value = z_values[i]
                data[i].save()
        elif type == TypeEnum.WIFI_DATA:
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
        elif type == TypeEnum.BLUETOOTH_DATA:
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
        else:
            raise exceptions.SensorTypeError("Type Error")
        return data
