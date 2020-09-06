import glob
import pandas as pd
from IPython.core.display import display

radio_maps = glob.glob('D:/College/5th Year College/TESE\Desenvolvimento/Code\Application/findLocationApp/findLocation/Server/Notebooks/FINGERPRINT/radiomap*.csv')


def get_matching_access_points(scannedAps):
    matching_radio_maps = {}
    for radio_map in radio_maps:
        print(radio_map)
        dataset = pd.read_csv(radio_map)
        columns = list(dataset.columns)
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
        matching_list = list()
        for ap in scannedAps:
            if ap in existent_aps_columns:
                matching_list.append(ap)
        if len(matching_list) > (len(existent_aps_columns) / 2):
            print('MATCHING FOUND')
            print('LEN: ' + str(len(matching_list)))
            matching_radio_maps[radio_map] = matching_list
    print('MATCHING RADIO MAPS')
    display(matching_radio_maps)
    return matching_radio_maps


def get_matching_beacons(scannedBeacons):
    matching_radio_maps = {}
    for radio_map in radio_maps:
        print(radio_map)
        dataset = pd.read_csv(radio_map)
        columns = list(dataset.columns)
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
            print('MATCHING FOUND')
            print('LEN: ' + str(len(matching_list)))
            matching_radio_maps[radio_map] = matching_list
    print('MATCHING RADIO MAPS')
    display(matching_radio_maps)
    return matching_radio_maps