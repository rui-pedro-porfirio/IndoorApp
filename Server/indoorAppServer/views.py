from django.shortcuts import render
from .models import Fingerprint, DeviceSensor, BluetoothSensor, WiFiSensor
from rest_framework import viewsets
from .serializers import FingerprintSerializer, DeviceDataSerializer, WifiDataSerializer, BluetoothSerializer


# Create your views here.
def fingerprintInfo(request):
    # fingerprint_1 = Fingerprint.objects.create(coordinate_X=10.442, coordinate_Y=-42.234)
    # device_sensor_1 = DeviceSensor.objects.create(sensor_type="Orientation", x_value=10, y_value=10, z_value=10,
    #                                               fingerprint=fingerprint_1)
    # wifi_sensor_1 = WiFiSensor.objects.create(mac_address="ba:24:ds:12", rssi=-60, fingerprint=fingerprint_1)
    # bluetooth_sensor_1 = BluetoothSensor.objects.create(b eacon_address="b:24:ds:12:cs", rssi=-40,
    #                                                     fingerprint=fingerprint_1)
    context = {
        'fingerprints': Fingerprint.objects.all()
    }
    return render(request, 'indoorAppServer/home.html', context)

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
