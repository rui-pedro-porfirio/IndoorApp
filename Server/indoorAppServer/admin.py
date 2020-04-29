from django.contrib import admin
from .models import Fingerprint,DeviceSensor,WiFiSensor,BluetoothSensor

admin.site.register(Fingerprint)
admin.site.register(DeviceSensor)
admin.site.register(WiFiSensor)
admin.site.register(BluetoothSensor)

