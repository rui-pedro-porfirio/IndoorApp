import re
import sys
import random

from yanux.cruncher.model.location import IndoorLocation


class WifiIndoorLocation(IndoorLocation):
    def __init__(self, place, floor, x, y):
        super().__init__(place, floor, x, y)
        self.wifi_samples = []

    def shuffle_samples(self):
        random.shuffle(self.wifi_samples)

class WifiConnectionInfo(object):
    def __init__(self, connection_info):
        self.bssid = connection_info["bssid"]
        self.detailed_state = connection_info["detailedState"]
        self.dhcp_dns1 = connection_info["dhcpDns1"]
        self.dhcp_dns2 = connection_info["dhcpDns2"]
        self.dhcp_gateway = connection_info["dhcpGateway"]
        self.dhcp_ip_address = connection_info["dhcpIpAddress"]
        self.dhcp_lease_duration = connection_info["dhcpLeaseDuration"]
        self.dhcp_netmask = connection_info["dhcpNetmask"]
        self.ip_address = connection_info["ipAddress"]
        self.link_speed = connection_info["linkSpeed"]
        self.mac_adsress = connection_info["macAddress"]
        self.network_id = connection_info["networkId"]
        self.rssi = connection_info["rssi"]
        self.ssid = connection_info["ssid"]
        self.supplicant_state = connection_info["supplicantState"]
        self.ssid_hidden = connection_info["ssidHidden"]


class SensorEntry(object):
    def __init__(self, sensor_entry):
        self.accuracy = sensor_entry["accuracy"]
        self.sensor_name = sensor_entry["sensorName"]
        self.sensor_type = sensor_entry["sensorType"]
        self.sensor_type_name = sensor_entry["sensorTypeName"]
        self.timestamp = sensor_entry["timestamp"]
        self.values = sensor_entry["values"]


class WifiResult(object):
    def __init__(self, wifi_result):
        self.timestamp = wifi_result["timestamp"]
        self.frequency = wifi_result["frequency"]
        self.mac_address = wifi_result["macAddress"]
        self.signal_strength = wifi_result["signalStrength"]
        self.ssid = wifi_result["ssid"]


class WifiSample(object):
    def __init__(self, filename, sample_id, timestamp, wifi_reading):
        self.filename = filename
        self.sample_id = sample_id
        self.timestamp = timestamp
        self.current_connection_info = WifiConnectionInfo(wifi_reading["connectionInfo"])
        self.sensor_entries = {}
        for sensor_entry in wifi_reading["sensorEntries"]:
            self.sensor_entries[sensor_entry["sensorTypeName"]] = SensorEntry(sensor_entry)
        self.wifi_results = []
        for wifi_result in wifi_reading["wifiResults"]:
            self.wifi_results.append(WifiResult(wifi_result))


class WifiLogs(object):
    _DEFAULT_PLACE = "ed2.fct.unl.pt"
    _DEFAULT_FLOOR = 2

    def __init__(self, json_data):
        self._json_data = json_data
        self.locations = {}
        self._load()

    def _load(self):
        for name, log_file in self._json_data.items():
            # Find a better way to get the coordinates so that I don't have to follow a specific filename format.
            # Besides that I really shouldn't hardcode the default floor into the code
            parsed_coord = re.findall(r"[-+]?\d*\.\d+|\d+", log_file["name"])
            coordinates = (WifiLogs._DEFAULT_PLACE,
                           WifiLogs._DEFAULT_FLOOR,
                           float(parsed_coord[0]),
                           float(parsed_coord[1]))
            if coordinates not in self.locations:
                self.locations[coordinates] = WifiIndoorLocation(coordinates[0],
                                                                 coordinates[1],
                                                                 coordinates[2],
                                                                 coordinates[3])
            location = self.locations[coordinates]
            for session in log_file["sessions"]:
                timestamp = session["timestamp"]
                for entry in session["entries"]:
                    location.wifi_samples.append(WifiSample(log_file["name"], entry["id"], timestamp, entry["reading"]))

    def shuffle_samples(self):
        for key, location in self.locations.items():
            location.shuffle_samples()

    def wifi_samples(self, start_slice_index=0, end_slice_index=sys.maxsize):
        wifi_samples = []
        for coordinates, location in self.locations.items():
            for wifi_sample in location.wifi_samples[start_slice_index:end_slice_index]:
                sample = {
                    "place": location.place,
                    "floor": location.floor,
                    "x": location.x,
                    "y": location.y,
                    "orientation": wifi_sample.sensor_entries["Orientation"].values[0],
                    "filename": wifi_sample.filename,
                    "sample_id": wifi_sample.sample_id,
                    "timestamp": wifi_sample.timestamp
                }
                for wifi_result in wifi_sample.wifi_results:
                    sample[wifi_result.mac_address] = wifi_result.signal_strength
                wifi_samples.append(sample)
        return wifi_samples

    def wifi_results(self, start_slice_index=0, end_slice_index=sys.maxsize):
        wifi_results = []
        for coordinates, location in self.locations.items():
            for wifi_sample in location.wifi_samples[start_slice_index:end_slice_index]:
                for wifi_result in wifi_sample.wifi_results:
                    wifi_results.append({
                        "place": location.place,
                        "floor": location.floor,
                        "x": location.x,
                        "y": location.y,
                        "orientation": wifi_sample.sensor_entries["Orientation"].values[0],
                        "filename": wifi_sample.filename,
                        "sample_id": wifi_sample.sample_id,
                        "mac_address": wifi_result.mac_address,
                        "timestamp": wifi_sample.timestamp,
                        "signal_strength": wifi_result.signal_strength
                    })
        return wifi_results
