package android.example.findlocation.objects;

import java.util.List;

public class BluetoothObject {

    private String name;
    private List<Integer> values;

    public BluetoothObject(String name, List<Integer> values){
        this.name = name;
        this.values = values;
    }

    public List<Integer> getValues() {
        return values;
    }

    public String getName() {
        return name;
    }
}
