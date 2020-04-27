from django.db import models
from django.utils import timezone
from django.contrib.auth.models import User


class Paradigm(models.Model):
    name = models.CharField(max_length=50)

    def __str__(self):
        return self.name


class language(models.Model):
    name = models.CharField(max_length=50)
    paradigm = models.ForeignKey(Paradigm, on_delete=models.CASCADE)

    def __str__(self):
        return self.name


class Programmer(models.Model):
    name = models.CharField(max_length=50)
    languages = models.ManyToManyField(language)

    def __str__(self):
        return self.name


class Fingerprint(models.Model):
    coordinate_X = models.FloatField()
    coordinate_Y = models.FloatField()

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