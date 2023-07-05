package com.example.myapplication;

import android.Manifest;
import android.content.ContentValues;
import android.content.Context;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private static final int PERMISSION_REQUEST_CODE = 1;
    private TextView humidityTextView;
    private ListView humidityListView;
    private Button saveButton;
    private Button deleteButton;
    private List<Integer> humidityList;
    private ArrayAdapter<Integer> humidityAdapter;
    private SQLiteDatabase database;
    private SensorManager sensorManager;
    private Sensor humiditySensor;
    private SensorEventListener sensorEventListener;
    private float currentHumidity;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        humidityTextView = findViewById(R.id.humidityTextView);
        humidityListView = findViewById(R.id.humidityListView);
        saveButton = findViewById(R.id.saveButton);
        deleteButton = findViewById(R.id.deleteButton);

        humidityList = new ArrayList<>();
        humidityAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, humidityList);
        humidityListView.setAdapter(humidityAdapter);

        database = openOrCreateDatabase("HumidityDB", MODE_PRIVATE, null);
        database.execSQL("CREATE TABLE IF NOT EXISTS humidity (id INTEGER PRIMARY KEY AUTOINCREMENT, value INTEGER)");

        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        humiditySensor = sensorManager.getDefaultSensor(Sensor.TYPE_RELATIVE_HUMIDITY);


        sensorEventListener = new SensorEventListener() {
            @Override
            public void onSensorChanged(SensorEvent event) {
                if (event.sensor.getType() == Sensor.TYPE_RELATIVE_HUMIDITY) {
                    currentHumidity = event.values[0];
                    humidityTextView.setText("Kelembaban Ruangan: " + currentHumidity + "%");
                }
            }

            @Override
            public void onAccuracyChanged(Sensor sensor, int accuracy) {
                // Do nothing
            }
        };

        saveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int currentHumidity = (int) getHumidity();
                ContentValues values = new ContentValues();
                values.put("value", currentHumidity);
                database.insert("humidity", null, values);
                humidityList.add(currentHumidity);
                humidityAdapter.notifyDataSetChanged();
                Toast.makeText(MainActivity.this, "Kelembaban tersimpan", Toast.LENGTH_SHORT).show();
            }
        });

        deleteButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                database.delete("humidity", null, null);
                humidityList.clear();
                humidityAdapter.notifyDataSetChanged();
                Toast.makeText(MainActivity.this, "Data dihapus", Toast.LENGTH_SHORT).show();
            }
        });

        humidityListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                int selectedHumidity = humidityList.get(position);
                Toast.makeText(MainActivity.this, "Kelembaban: " + selectedHumidity, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private float getHumidity() {
        if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            // If location access permission is granted
            return currentHumidity;
        } else {
            // If location access permission is not granted
            return -1;
        }
    }

    // Metode untuk menangani hasil permintaan izin
    @Override
    public void onRequestPermissionsResult(int requestCode,  String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Jika pengguna memberikan izin
                // Cek kembali kelembaban
                int currentHumidity = (int) getHumidity();
                // Lanjutkan proses selanjutnya
            } else {
                // Jika pengguna menolak izin
                Toast.makeText(this, "Izin akses lokasi ditolak", Toast.LENGTH_SHORT).show();
                // Tindakan apa yang ingin Anda lakukan jika izin ditolak
            }
        }
    }




    private void displayHumidityData() {
        Cursor cursor = database.rawQuery("SELECT value FROM humidity", null);
        if (cursor.moveToFirst()) {
            do {
                int humidityValue = cursor.getInt(0);
                humidityList.add(humidityValue);
            } while (cursor.moveToNext());
        }
        cursor.close();
        humidityAdapter.notifyDataSetChanged();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (humiditySensor != null) {
            sensorManager.registerListener(sensorEventListener, humiditySensor, SensorManager.SENSOR_DELAY_NORMAL);
        } else {
            Toast.makeText(this, "Sensor kelembaban tidak tersedia", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        sensorManager.unregisterListener(sensorEventListener);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        database.close();
    }
}
