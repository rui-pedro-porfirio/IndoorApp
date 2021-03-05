# Android Indoor Positioning Client
When it comes to the __Indoor Positioning Solution__, this Android application is the mobile counterpart of the [__YanuX Desktop Client__]. It continuously scans for __Wi-Fi Access Points__ and __Bluetooth Low Energy Beacons__ (iBeacons) and submits that information to the [__Indoor Positioning Server__](https://github.com/YanuX-Framework/YanuX-IPSServer) so that the position of a device can be determined.

It also incorporates some additional tools that should be used in tandem with the [__Indoor Positioning Server__](https://github.com/YanuX-Framework/YanuX-IPSServer).


## Documentation
- Requires Android 6.0 (tested up to Android 10)
- Requires Wi-Fi and Bluetooth Low Energy support
	- The scanning of Bluetooth Low Energy Beacons (iBeacon) is achieved thanks to the [__Android Beacon Library__](https://github.com/AltBeacon/android-beacon-library)

## License
This work is licensed under [__GNU General Public License Version 3__](LICENSE)