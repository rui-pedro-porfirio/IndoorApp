package android.example.findlocation.objects.client;

import java.util.List;

public class WifiObject {

    private String name;
    private List<Integer> values;
    private int singleValue;
    private boolean isChecked;

    public WifiObject(String name, List<Integer> values){
        this.name = name;
        this.values = values;
    }
    public WifiObject(String name, int value){
        this.name = name;
        this.singleValue = value;
    }
    public List<Integer> getValues() {
        return values;
    }

    public String getName() {
        return name;
    }

    public boolean isChecked() {
        return isChecked;
    }

    public void setChecked(boolean checked) {
        isChecked = checked;
    }

    public int getSingleValue() {
        return singleValue;
    }

    public void setSingleValue(int singleValue) {
        this.singleValue = singleValue;
    }

}
