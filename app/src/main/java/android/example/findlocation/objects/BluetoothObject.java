package android.example.findlocation.objects;

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
    }

    public int getSingleValue() {
        return singleValue;
    }

    public void setSingleValue(int singleValue) {
        this.singleValue = singleValue;
    }

    public List<Integer> getValues() {
        return values;
    }

    public String getName() {
        return name;
    }
}
