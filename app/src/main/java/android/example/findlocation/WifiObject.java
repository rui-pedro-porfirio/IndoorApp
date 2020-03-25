package android.example.findlocation;

import java.util.List;

public class WifiObject {

    private String name;
    private List<Integer> values;

    public WifiObject(String name, List<Integer> values){
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
