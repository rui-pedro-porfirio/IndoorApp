package android.example.findlocation;

public class SensorObject {

    private String name;
    private float x_value;
    private float y_value;
    private float z_value;

    public SensorObject(String name, float[] values){
        this.name = name;
        this.x_value = values[0];
        this.y_value = values[1];
        this.z_value = values[2];
    }

    public float getX_value(){
        return x_value;
    }

    public float getY_value() {
        return y_value;
    }

    public float getZ_value() {
        return z_value;
    }

    public String getName() {
        return name;
    }

    public void setValue(float[] values){
        if(values.length >= 3){
            this.x_value = values[0];
            this.y_value = values[1];
            this.z_value = values[2];
        }
        else if(values.length == 2){
            this.x_value = values[0];
            this.y_value = values[1];
        }
        else{
            this.x_value = values[0];
        }
    }

    public float[] getValues(){
        float[] values = {this.x_value,this.y_value,this.z_value};
        return values;
    }

}
