from django.db import models
from django.utils import timezone
from django.contrib.auth.models import User

class UserTable(models.Model):
    username = models.CharField(max_length=100)

    def __str__(self):
        return srt(self.id)

class Fingerprint(models.Model):
    coordinate_X = models.FloatField(null=True)
    coordinate_Y = models.FloatField(null=True)
    zone = models.CharField(max_length=100)

    def __str__(self):
        return str(self.id)


class DeviceSensor(models.Model):
    sensor_type = models.CharField(max_length=100)
    x_value = models.FloatField()
    y_value = models.FloatField()
    z_value = models.FloatField()
    fingerprint = models.ForeignKey(Fingerprint, related_name='device', on_delete=models.CASCADE)

    def __str__(self):
        return str(self.id) + ' - ' +self.sensor_type


class WiFiSensor(models.Model):
    name = models.CharField(max_length=100)
    rssi = models.IntegerField()
    fingerprint = models.ForeignKey(Fingerprint, related_name='wifi', on_delete=models.CASCADE)

    def __str__(self):
        return str(self.id) + ' - '+self.name


class BluetoothSensor(models.Model):
    name = models.CharField(max_length=100)
    rssi = models.IntegerField()
    fingerprint = models.ForeignKey(Fingerprint, related_name='ble',on_delete=models.CASCADE)

    def __str__(self):
        return str(self.id) + ' - '+self.name