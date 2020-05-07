package android.example.findlocation.activities;

import android.example.findlocation.R;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import android.example.findlocation.objects.server.ServerBluetoothData;
import android.example.findlocation.objects.server.ServerDeviceData;
import android.example.findlocation.objects.server.ServerFingerprint;
import android.example.findlocation.objects.server.ServerWifiData;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.widget.Toast;


import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.net.ConnectException;
import java.net.SocketTimeoutException;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class OnlineActivity extends AppCompatActivity {


    public static final MediaType JSON = MediaType.get("application/json; charset=utf-8");
    private static final String ADDRESS = "http://192.168.1.7:8000/";

    private OkHttpClient client;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_online);
        client = new OkHttpClient();
        new SendHTTPRequest().execute();
    }

    private class SendHTTPRequest extends AsyncTask<Void, Void, String> {



        @RequiresApi(api = Build.VERSION_CODES.O)
        @Override
        protected String doInBackground(Void... voids) {
            Gson gson = new Gson();
            String fingerprintInJson = null;

            try {
                post(ADDRESS + "filter/","");

            } catch (IOException e) {
                e.printStackTrace();
            }
            return "";
        }

        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);
        }

        @RequiresApi(api = Build.VERSION_CODES.KITKAT)
        protected String post(String url, String parameter) throws IOException {
            RequestBody body = RequestBody.create(null, new byte[0]);
            Handler mainHandler = new Handler(getMainLooper());
            String responseString = "";
            Request request = new Request.Builder()
                    .url(url)
                    .post(body)
                    .build();
            try (Response response = client.newCall(request).execute()) {
                if (!response.isSuccessful()) throw new IOException("Unexpected code " + response);
                if (parameter.length() > 1)
                    responseString = parse(response.body().string(), parameter);
                else
                    responseString = response.body().string();

            } catch (ConnectException e) {

                mainHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        // Do your stuff here related to UI, e.g. show toast
                        Toast.makeText(getApplicationContext(), "Failed to connect to the server", Toast.LENGTH_SHORT).show();
                    }
                });
            } catch (SocketTimeoutException e) {
                mainHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        // Do your stuff here related to UI, e.g. show toast
                        Toast.makeText(getApplicationContext(), "Failed to connect to the server", Toast.LENGTH_SHORT).show();
                    }
                });
            }
            return responseString;
        }

        public String parse(String jsonLine, String parameter) {
            JsonElement jelement = new JsonParser().parse(jsonLine);
            JsonObject jobject = jelement.getAsJsonObject();
            String result = jobject.get(parameter).getAsString();
            return result;
        }
    }
}
