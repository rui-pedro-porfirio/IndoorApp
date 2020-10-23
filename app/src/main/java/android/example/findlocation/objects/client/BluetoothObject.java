package android.example.findlocation.objects.client;

import java.util.ArrayList;
import java.util.List;

public class BluetoothObject {

    private final String name;
    private final List<Integer> values;
    private String mac;
    private int singleValue;

    public BluetoothObject(String name, List<Integer> values) {
        this.name = name;
        this.values = values;
    }

    public BluetoothObject(String name, int value) {
        this.name = name;
        this.singleValue = value;
        this.values = new ArrayList<>();
    }

    public BluetoothObject(String name, String mac, List<Integer> values) {
        this.name = name;
        this.values = values;
        this.mac = mac;
    }

    public BluetoothObject(String name, String mac, int value) {
        this.name = name;
        this.singleValue = value;
        this.mac = mac;
        this.values = new ArrayList<>();
    }

    public int getSingleValue() {
        return singleValue;
    }

    public void setSingleValue(int singleValue) {
        this.singleValue = singleValue;
    }

    public void addValue(int rssi) {
        values.add(rssi);
    }

    public List<Integer> getValues() {
        return values;
    }

    public String getName() {
        return name;
    }
}
