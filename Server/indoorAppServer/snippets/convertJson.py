from ..models import Fingerprint, DeviceSensor, BluetoothSensor, WiFiSensor
from django.core.serializers.json import DjangoJSONEncoder
import json
import csv
from itertools import chain

def jsonToFile(type):
    fingerprints = Fingerprint.objects.all().values('id', 'coordinate_X', 'coordinate_Y','zone')
    list_fingerprint = list(fingerprints)
    list_json_fingerprints = list()
    csv_columns = ['Fingerprint ID', 'coordinate_X', 'coordinate_Y','zone']
    if type == 'Wi-Fi':
        devices = WiFiSensor.objects.all().values('name').distinct()
        devices_list = list(devices)
    elif type == 'Bluetooth':
        devices = BluetoothSensor.objects.all().values('name').distinct()
        devices_list = list(devices)
    else:
        wifi_list = WiFiSensor.objects.all().values('name').distinct()
        ble_list = BluetoothSensor.objects.all().values('name').distinct()
        devices = list(wifi_list) + list(ble_list)
        devices_list = devices
    for receiver in devices_list:
        csv_columns.append(receiver['name'])
    for fingerprint in list_fingerprint:
        dictionary_fingerprint = {'Fingerprint ID': fingerprint['id'], 'coordinate_X': fingerprint['coordinate_X'],
                                  'coordinate_Y': fingerprint['coordinate_Y'],'zone':fingerprint['zone']}
        for r in devices_list:
            if type == 'Wi-Fi':
                match_fingerprint = WiFiSensor.objects.all().filter(name=r['name'],
                                                                fingerprint_id=fingerprint['id']).values('name', 'rssi')
            elif type == 'Bluetooth':
                match_fingerprint = BluetoothSensor.objects.all().filter(name=r['name'],
                                                                    fingerprint_id=fingerprint['id']).values('name',
                                                                                                             'rssi')
            else:
                match_fingerprint_wifi = WiFiSensor.objects.all().filter(name=r['name'],
                                                                fingerprint_id=fingerprint['id']).values('name', 'rssi')
                match_fingerprint_ble = BluetoothSensor.objects.all().filter(name=r['name'],
                                                                         fingerprint_id=fingerprint['id']).values('name','rssi')
                match_fingerprint = list(chain(match_fingerprint_wifi, match_fingerprint_ble))

            if len(match_fingerprint) == 0:
                dictionary_fingerprint[r['name']] = 0
            else:
                for match in match_fingerprint:
                    dictionary_fingerprint[match['name']] = match['rssi']
        list_json_fingerprints.append(dictionary_fingerprint)
    dict_fingerprints = {'fingerprints': list_json_fingerprints}
    try:
        with open('radiomap' +type +'classifier_university_dataset.csv', 'w') as csvfile:
            writer = csv.DictWriter(csvfile, fieldnames=csv_columns)
            writer.writeheader()
            for data in list_json_fingerprints:
                writer.writerow(data)
    except IOError:
        print("I/O error")
    with open('radiomap' +type +'classifier_university_dataset.json', 'w') as outfile:
        json.dump(dict_fingerprints, outfile, cls=DjangoJSONEncoder, indent=3)
