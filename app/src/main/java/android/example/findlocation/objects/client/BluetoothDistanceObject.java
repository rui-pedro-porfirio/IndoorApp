package android.example.findlocation.objects.client;

import java.util.ArrayList;
import java.util.List;

public class BluetoothDistanceObject {

    private String name;
    private float x_coordinate;
    private float y_coordinate;
    private List<Integer> values;
    private int singleValue;

    public BluetoothDistanceObject(String name, List<Integer> values){
        this.name = name;
        this.values = values;
        this.singleValue = Integer.MIN_VALUE;
        this.x_coordinate = 0.0f;
        this.y_coordinate = 0.0f;
    }

    public BluetoothDistanceObject(String name, int value){
        this.name = name;
        this.values = new ArrayList<>();
        this.singleValue = value;
        this.x_coordinate = 0.0f;
        this.y_coordinate = 0.0f;
    }

    public void setX_coordinate(float x_coordinate) {
        this.x_coordinate = x_coordinate;
    }

    public void setY_coordinate(float y_coordinate) {
        this.y_coordinate = y_coordinate;
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

    public void addRSSIValue(int rssi){
        values.add(rssi);
    }

    public String getName() {
        return name;
    }

    public float getX_coordinate() {
        return x_coordinate;
    }

    public float getY_coordinate() {
        return y_coordinate;
    }
}
