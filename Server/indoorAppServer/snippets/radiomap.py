import glob
import pandas as pd
from IPython.core.display import display
import math

radio_maps = glob.glob('D:/College/5th Year College/TESE\Desenvolvimento/Code\Application/findLocationApp/findLocation/Server/Notebooks/FINGERPRINT/radiomap*.csv')
#aps=['c4:e9:84:42:ac:ff', '00:06:91:d4:77:00', '00:06:91:d4:77:02', '8c:5b:f0:78:a1:d6', '1c:ab:c0:df:99:c8', '1c:ab:c0:df:99:c9', '00:26:5b:d1:93:38', '00:26:5b:d1:93:39', '00:fc:8d:cf:98:08', '00:fc:8d:cf:98:09']


def get_matching_access_points(scannedAps):
    matching_radio_maps = {}
    size_dataset = {}
    columns_dataset = {}
    for radio_map in radio_maps:
        print(radio_map)
        dataset = pd.read_csv(radio_map)
        result = {}
        columns = list(dataset.columns)
        length = len(dataset.index)
        size_dataset[radio_map] = length
        first_beacon_index = -1
        existent_aps = None
        if 'zone' in columns:
            zone_index = dataset.columns.get_loc('zone')
            print('zone: ' + str(zone_index))
            for ap in dataset.iloc[:, zone_index + 1:]:
                if ap.islower() == False:
                    first_beacon_index = list(dataset).index(ap)
                    break
            existent_aps = dataset.iloc[:, zone_index + 1:first_beacon_index]
        else:
            for ap in dataset.iloc[:, 3:]:
                if ap.islower() == False:
                    first_beacon_index = list(dataset).index(ap)
                    break
            existent_aps = dataset.iloc[:, 3:first_beacon_index]
        existent_aps_columns = list(existent_aps.columns)
        columns_dataset[radio_map] = len(existent_aps_columns)
        matching_list = list()
        for ap in scannedAps:
            if ap in existent_aps_columns:
                matching_list.append(ap)
        if len(matching_list) > (len(existent_aps_columns) / 2):
            matching_radio_maps[radio_map] = matching_list
    for k, v in sorted(size_dataset.items(), key=lambda item: item[1], reverse=True):
        if k in matching_radio_maps:
            result[k] = matching_radio_maps[k]
            n = columns_dataset[k]
            result['length'] = math.floor((len(matching_radio_maps[k]) / n) * 100)
            break
    print('RESULT')
    display(result)
    return result


def get_matching_beacons(scannedBeacons):
    matching_radio_maps = {}
    size_dataset = {}
    for radio_map in radio_maps:
        print(radio_map)
        result = {}
        dataset = pd.read_csv(radio_map)
        columns = list(dataset.columns)
        length = len(dataset.index)
        size_dataset[radio_map] = length
        first_beacon_index = -1
        existent_beacons = None
        if 'zone' in columns:
            zone_index = dataset.columns.get_loc('zone')
            print('zone: ' + str(zone_index))
            for ap in dataset.iloc[:, zone_index + 1:]:
                if ap.islower() == False:
                    first_beacon_index = list(dataset).index(ap)
                    break
            existent_beacons = dataset.iloc[:, first_beacon_index:]
        else:
            for ap in dataset.iloc[:, 3:]:
                if ap.islower() == False:
                    first_beacon_index = list(dataset).index(ap)
                    break
            existent_beacons = dataset.iloc[:, first_beacon_index:]
        existent_beacons_columns = list(existent_beacons.columns)
        matching_list = list()
        for beacon in scannedBeacons:
            if beacon in existent_beacons_columns:
                matching_list.append(beacon)
        if len(matching_list) >= 3:
            matching_radio_maps[radio_map] = matching_list
    for k, v in sorted(size_dataset.items(), key=lambda item: item[1], reverse=True):
        if k in matching_radio_maps:
            result[k] = matching_radio_maps[k]
            result['length'] = v
            break
    print('RESULT')
    display(result)
    return result