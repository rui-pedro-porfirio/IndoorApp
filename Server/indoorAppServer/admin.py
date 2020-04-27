from django.contrib import admin
from .models import Fingerprint,DeviceSensor,WiFiSensor,BluetoothSensor,language

admin.site.register(Fingerprint)
admin.site.register(DeviceSensor)
admin.site.register(WiFiSensor)
admin.site.register(BluetoothSensor)
admin.site.register(language)

