package android.example.findlocation.objects.client;

import java.util.ArrayList;
import java.util.List;

public class BluetoothObject {

    private String name;
    private List<Integer> values;
    private int singleValue;

    public BluetoothObject(String name, List<Integer> values){
        this.name = name;
        this.values = values;
    }

    public BluetoothObject(String name, int value){
        this.name = name;
        this.singleValue = value;
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
