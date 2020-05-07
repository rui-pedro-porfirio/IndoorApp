from ..models import Fingerprint, DeviceSensor, BluetoothSensor, WiFiSensor
from enum import Enum
import statistics
from django.db.models import Count

class FilterEnum(Enum):
    MEDIAN_FILTER = 1
    MEAN_FILTER = 2


class TypeEnum(Enum):
    WIFI = 1
    BLUETOOTH = 2


def apply_median_filter(fingerprints_reference_point, type, existing_fingerprint):  # AP1,AP2,AP3,AP1,AP4
    if type == TypeEnum.WIFI:
        access_points = WiFiSensor.objects.all().filter(fingerprint_id__in=fingerprints_reference_point.values('id')).values('name').annotate(
            Count('name'))  # F1: AP1, AP2, AP3
        for ap in access_points:  # AP1
            similar_access_points = WiFiSensor.objects.filter(name=ap['name']).order_by('rssi')
            rssi_values = list()
            mac_address = ''
            for sap in similar_access_points:
                rssi_values.append(sap.rssi)
                mac_address = sap.name
            rssi_value_median = statistics.median(rssi_values)
            similar_access_points.delete()
            WiFiSensor.objects.create(name=mac_address,rssi=rssi_value_median,fingerprint_id=existing_fingerprint[0].id)
    elif type == TypeEnum.BLUETOOTH:
        beacons = BluetoothSensor.objects.all().filter(
            fingerprint_id__in=fingerprints_reference_point.values('id')).values('name').annotate(
            Count('name'))  # F1: AP1, AP2, AP3
        for beacon in beacons:  # AP1
            similar_beacons = BluetoothSensor.objects.filter(name=beacon['name']).order_by('rssi')
            rssi_values = list()
            mac_address = ''
            for similar_beacon in similar_beacons:
                rssi_values.append(similar_beacon.rssi)
                mac_address = similar_beacon.name
            rssi_value_median = statistics.median(rssi_values)
            similar_beacons.delete()
            BluetoothSensor.objects.create(name=mac_address, rssi=rssi_value_median,
                                      fingerprint_id=existing_fingerprint[0].id)


def apply_mean_filter(fingerprints_reference_point, type, existing_fingerprint):  # AP1,AP2,AP3,AP1,AP4
    if type == TypeEnum.WIFI:
        access_points = WiFiSensor.objects.all().filter(fingerprint_id__in=fingerprints_reference_point.values('id')).values('name').annotate(
            Count('name'))  # F1: AP1, AP2, AP3
        for ap in access_points:  # AP1
            similar_access_points = WiFiSensor.objects.filter(name=ap['name']).order_by('rssi')
            rssi_values = list()
            mac_address = ''
            for sap in similar_access_points:
                rssi_values.append(sap.rssi)
                mac_address = sap.name
            rssi_value_median = statistics.median(rssi_values)
            similar_access_points.delete()
            WiFiSensor.objects.create(name=mac_address,rssi=rssi_value_median,fingerprint_id=existing_fingerprint[0].id)
    elif type == TypeEnum.BLUETOOTH:
        beacons = BluetoothSensor.objects.all().filter(
            fingerprint_id__in=fingerprints_reference_point.values('id')).values('name').annotate(
            Count('name'))  # F1: AP1, AP2, AP3
        for beacon in beacons:  # AP1
            similar_beacons = BluetoothSensor.objects.filter(name=beacon['name']).order_by('rssi')
            rssi_values = list()
            mac_address = ''
            for similar_beacon in similar_beacons:
                rssi_values.append(similar_beacon.rssi)
                mac_address = similar_beacon.name
            rssi_value_median = statistics.mean(rssi_values)
            similar_beacons.delete()
            BluetoothSensor.objects.create(name=mac_address, rssi=rssi_value_median,
                                      fingerprint_id=existing_fingerprint[0].id)