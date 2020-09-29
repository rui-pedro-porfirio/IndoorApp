import glob
import pandas as pd
from IPython.core.display import display
import math

radio_maps_local = glob.glob('D:/College/5th Year College/TESE\Desenvolvimento/Code\Application/findLocationApp/findLocation/Server/Notebooks/FINGERPRINT/radiomap*.csv')
radio_maps_heroku = glob.glob('/app/Notebooks/FINGERPRINT/radiomap*.csv')
#aps=['c4:e9:84:42:ac:ff', '00:06:91:d4:77:00', '00:06:91:d4:77:02', '8c:5b:f0:78:a1:d6', '1c:ab:c0:df:99:c8', '1c:ab:c0:df:99:c9', '00:26:5b:d1:93:38', '00:26:5b:d1:93:39', '00:fc:8d:cf:98:08', '00:fc:8d:cf:98:09']


def compute_matching_data(scannedAps,scannedBeacons):
    matching_radio_maps_wifi = {}
    matching_radio_maps_ble = {}
    similar_radio_maps = list()
    size_dataset = {}
    classification_holder = {}
    ap_columns_dataset = {}
    for radio_map in radio_maps_heroku:
        print(radio_map)
        dataset = pd.read_csv(radio_map)
        result = {}
        columns = list(dataset.columns)
        length = len(dataset.index)
        size_dataset[radio_map] = length
        first_beacon_index = -1
        existent_aps = None
        existent_beacons = None
        if 'zone' in columns:
            classification_holder[radio_map] = True
            zone_index = dataset.columns.get_loc('zone')
            print('zone: ' + str(zone_index))
            for ap in dataset.iloc[:, zone_index + 1:]:
                if ap.islower() == False:
                    first_beacon_index = list(dataset).index(ap)
                    break
            existent_aps = dataset.iloc[:, zone_index + 1:first_beacon_index]
            existent_beacons = dataset.iloc[:, first_beacon_index:]
        else:
            classification_holder[radio_map] = False
            for ap in dataset.iloc[:, 3:]:
                if ap.islower() == False:
                    first_beacon_index = list(dataset).index(ap)
                    break
            existent_aps = dataset.iloc[:, 3:first_beacon_index]
            existent_beacons = dataset.iloc[:, first_beacon_index:]
        existent_aps_columns = list(existent_aps.columns)
        existent_beacons_columns = list(existent_beacons.columns)
        ap_columns_dataset[radio_map] = len(existent_aps_columns)
        matching_list_aps = list()
        matching_list_beacons = list()
        for beacon in scannedBeacons:
            if beacon in existent_beacons_columns:
                matching_list_beacons.append(beacon)
        for ap in scannedAps:
            if ap in existent_aps_columns:
                matching_list_aps.append(ap)
        if len(matching_list_aps) > (len(existent_aps_columns) / 2):
            matching_radio_maps_wifi[radio_map] = matching_list_aps
        if len(matching_list_beacons) >= 3:
            matching_radio_maps_ble[radio_map] = matching_list_beacons
        if radio_map in matching_radio_maps_wifi and radio_map in matching_radio_maps_ble:
            similar_radio_maps.append(radio_map)
    if len(similar_radio_maps) != 0:
        for k, v in sorted(size_dataset.items(), key=lambda item: item[1], reverse=True):
            if k in similar_radio_maps:
                result['isClassifier'] = classification_holder[k]
                result['dataset'] = k
                result[k] = matching_radio_maps_wifi[k].extend(matching_radio_maps_ble)
                n = ap_columns_dataset[k]
                result['length_wifi'] = math.floor((len(matching_radio_maps_wifi[k]) / n) * 100)
                result['length_ble'] = v
                break
    else:
        if len(matching_radio_maps_wifi) != 0:
            for k, v in sorted(size_dataset.items(), key=lambda item: item[1], reverse=True):
                if k in matching_radio_maps_wifi:
                    result['isClassifier'] = classification_holder[k]
                    result['dataset'] = k
                    result[k] = matching_radio_maps_wifi[k]
                    n = ap_columns_dataset[k]
                    result['length_wifi'] = math.floor((len(matching_radio_maps_wifi[k]) / n) * 100)
                    result['length_ble'] = 0
                    break
        elif len(matching_radio_maps_ble) != 0:
            for k, v in sorted(size_dataset.items(), key=lambda item: item[1], reverse=True):
                if k in matching_radio_maps_ble:
                    result['isClassifier'] = classification_holder[k]
                    result['dataset'] = k
                    result[k] = matching_radio_maps_ble[k]
                    result['length_wifi'] =0
                    result['length_ble'] = 3
                    break
        else:
            result = None
    print('RESULT')
    return result