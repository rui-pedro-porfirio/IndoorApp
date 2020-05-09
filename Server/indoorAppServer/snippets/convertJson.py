from ..models import Fingerprint, DeviceSensor, BluetoothSensor, WiFiSensor
from django.core.serializers.json import DjangoJSONEncoder
import json
import csv


def jsonToFile():
    fingerprints = Fingerprint.objects.all().values('id', 'coordinate_X', 'coordinate_Y')
    list_fingerprint = list(fingerprints)
    list_json_fingerprints = list()
    csv_columns = ['Fingerprint ID', 'coordinate_X', 'coordinate_Y']
    access_points = WiFiSensor.objects.all().values('name').distinct()  # ap1,ap2,ap3
    wifi_list_data = list(access_points)
    for ap in wifi_list_data:
        csv_columns.append(ap['name'])
    for fingerprint in list_fingerprint:
        dictionary_fingerprint = {'Fingerprint ID': fingerprint['id'], 'coordinate_X': fingerprint['coordinate_X'],
                                  'coordinate_Y': fingerprint['coordinate_Y']}
        # device_data = DeviceSensor.objects.all().filter(fingerprint_id=fingerprint['id']).values('sensor_type','x_value','y_value','z_value')
        # bluetooth_data = BluetoothSensor.objects.all().filter(fingerprint_id=fingerprint['id']).values('name','rssi')
        # dictionary_fingerprint['device'] = list(device_data)
        for access_point in wifi_list_data:
            match_fingerprint = WiFiSensor.objects.all().filter(name=access_point['name'],
                                                                fingerprint_id=fingerprint['id']).values('name', 'rssi')
            if len(match_fingerprint) == 0:
                dictionary_fingerprint[access_point['name']] = 0
            else:
                for match in match_fingerprint:
                    dictionary_fingerprint[match['name']] = match['rssi']
        # dictionary_fingerprint['bluetooth'] = list(bluetooth_data)
        list_json_fingerprints.append(dictionary_fingerprint)
    dict_fingerprints = {'fingerprints': list_json_fingerprints}
    try:
        with open('radiomap.csv', 'w') as csvfile:
            writer = csv.DictWriter(csvfile, fieldnames=csv_columns)
            writer.writeheader()
            for data in list_json_fingerprints:
                writer.writerow(data)
    except IOError:
        print("I/O error")
    with open('radiomap.json', 'w') as outfile:
        json.dump(dict_fingerprints, outfile, cls=DjangoJSONEncoder, indent=3)
