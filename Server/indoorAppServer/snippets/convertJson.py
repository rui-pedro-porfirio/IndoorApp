from ..models import Fingerprint, DeviceSensor, BluetoothSensor, WiFiSensor
from django.core.serializers.json import DjangoJSONEncoder
import json

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