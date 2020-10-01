import pandas as pd
import numpy as np


def compute_csv_in_scanning_phase_trilateration(sample_dict, beacons_ml):
    csv_columns = ['BLE Beacon', 'coordinate_X', 'coordinate_Y', 'rssi_Value', 'rolling_mean_rssi', 'zone']
    sample = {}
    results_list_2d = list()
    for k in sample_dict:
        beacon = beacons_ml[k]
        sample['singleValue'] = beacon[0]
        sample['values'] = beacon[1]
        if 'zone' in sample:
            zone = sample['zone']
        else:
            zone = ''
        single_value_scanned = sample['singleValue']
        valuesScanned = sample['values']
        rolling_mean = np.mean(valuesScanned)
        x_coordinate = 0.0
        y_coordinate = 0.0
        results_list = list()
        results_list.append(k)
        results_list.append(x_coordinate)
        results_list.append(y_coordinate)
        results_list.append(single_value_scanned)
        results_list.append(rolling_mean)
        results_list.append(zone)
        results_list_2d.append(results_list)
    df = pd.DataFrame(data=results_list_2d, columns=csv_columns)
    return df


def compute_csv_in_scanning_phase(sample):
    csv_columns = ['coordinate_X', 'coordinate_Y', 'rssi_Value', 'rolling_mean_rssi', 'zone']
    if 'zone' in sample:
        zone = sample['zone']
    else:
        zone = ''
    single_value_scanned = sample['singleValue']
    valuesScanned = sample['values']
    aux_list = list()
    '''rolling_mean_list = list()
    for value in valuesScanned:
        aux_list.append(value)
        rolling_mean_list.append(np.mean(aux_list))'''
    rolling_mean = np.mean(valuesScanned)
    x_coordinate = 0.0
    y_coordinate = 0.0
    results_list_2d = list()
    results_list = list()
    results_list.append(x_coordinate)
    results_list.append(y_coordinate)
    results_list.append(single_value_scanned)
    results_list.append(rolling_mean)
    results_list.append(zone)
    results_list_2d.append(results_list)
    df = pd.DataFrame(data=results_list_2d, columns=csv_columns)
    return df


def compute_csv(request):
    csv_columns = ['coordinate_X', 'coordinate_Y', 'rssi_Value', 'rolling_mean_rssi', 'zone']
    sample = request.data
    if 'zone' in sample:
        zone = sample['zone']
    else:
        zone = ''
    single_value_scanned = sample['singleValue']
    valuesScanned = sample['values']
    aux_list = list()
    rolling_mean_list = list()
    for value in valuesScanned:
        aux_list.append(value)
        rolling_mean_list.append(np.mean(aux_list))
    print(rolling_mean_list)
    x_coordinate = sample['x_coordinate']
    y_coordinate = sample['y_coordinate']
    results_list_2d = list()
    for i in range(len(valuesScanned)):
        results_list = list()
        results_list.append(x_coordinate)
        results_list.append(y_coordinate)
        results_list.append(valuesScanned[i])
        results_list.append(rolling_mean_list[i])
        results_list.append(zone)
        results_list_2d.append(results_list)
    df = pd.DataFrame(data=results_list_2d, columns=csv_columns)
    return df


def compute_csv_trilateration(sample, beacon_address):
    csv_columns = ['BLE Beacon', 'coordinate_X', 'coordinate_Y', 'rssi_Value', 'rolling_mean_rssi', 'zone']
    if 'zone' in sample:
        zone = sample['zone']
    else:
        zone = ''
    mac = beacon_address
    single_value_scanned = sample['singleValue']
    valuesScanned = sample['values']
    aux_list = list()
    rolling_mean_list = list()
    for value in valuesScanned:
        aux_list.append(value)
        rolling_mean_list.append(np.mean(aux_list))
    print(rolling_mean_list)
    x_coordinate = sample['x_coordinate']
    y_coordinate = sample['y_coordinate']
    results_list_2d = list()
    for i in range(len(valuesScanned)):
        results_list = list()
        results_list.append(mac)
        results_list.append(x_coordinate)
        results_list.append(y_coordinate)
        results_list.append(valuesScanned[i])
        results_list.append(rolling_mean_list[i])
        results_list.append(zone)
        results_list_2d.append(results_list)
    df = pd.DataFrame(data=results_list_2d, columns=csv_columns)
    return df