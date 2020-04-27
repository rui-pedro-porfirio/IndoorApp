from rest_framework import serializers
from .models import language, Paradigm, Programmer, Fingerprint,DeviceSensor,BluetoothSensor,WiFiSensor


class languageSerializer(serializers.HyperlinkedModelSerializer):
    class Meta:
        model = language
        fields = ('id', 'url', 'name', 'paradigm')


class paradigmSerializer(serializers.HyperlinkedModelSerializer):
    class Meta:
        model = Paradigm
        fields = ('id', 'url', 'name')


class programmerSerializer(serializers.HyperlinkedModelSerializer):
    class Meta:
        model = Programmer
        fields = ('id', 'url', 'name', 'languages')


class FingerprintSerializer(serializers.HyperlinkedModelSerializer):
    class Meta:
        model = Fingerprint
        fields = ('id','url','coordinate_X','coordinate_Y')


class DeviceDataSerializer(serializers.HyperlinkedModelSerializer):
    class Meta:
        model = DeviceSensor
        fields = ('id','url','sensor_type','x_value','y_value','z_value','fingerprint')


class WifiDataSerializer(serializers.HyperlinkedModelSerializer):
    class Meta:
        model = WiFiSensor
        fields = ('id','url','name','rssi','fingerprint')


class BluetoothSerializer(serializers.HyperlinkedModelSerializer):
    class Meta:
        model = BluetoothSensor
        fields = ('id','url','name','rssi','fingerprint')