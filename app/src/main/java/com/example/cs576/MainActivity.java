package com.example.cs576;

import android.Manifest;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorListener;
import android.hardware.SensorManager;
import android.icu.text.SimpleDateFormat;
import android.icu.util.Calendar;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.BatteryManager;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.widget.TextViewCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;

import java.util.Date;

public class MainActivity extends AppCompatActivity implements SensorEventListener, LocationListener {

    private LocationManager locationManager;
    private SensorManager sensorManager;

    private TextView latitude;
    private TextView longitude;
    private TextView timestamp;
    private TextView interv;

    final private int MY_PERMISSION_ACCESS_COARSE_LOCATION = 101;
    final private int MY_PERMISSION_ACCESS_FINE_LOCATION = 102;


    private int X, Y, W, U, Z, interval;

    private int bat;

    private long lastShake;
    private long lastSense;

    private float last_x;
    private float last_y;
    private float last_z;

    BroadcastReceiver batteryReceiver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        latitude = (TextView) findViewById(R.id.latitude);
        longitude = (TextView) findViewById(R.id.longitude);
        timestamp = (TextView) findViewById(R.id.time);
        interv = (TextView) findViewById(R.id.interval);

        latitude.setText("Latitude");
        longitude.setText("Longitude");

        this.X = 12;
        this.Y = 3;
        this.W = 50;
        this.U = 6;
        this.Z = 12;
        this.interval = this.X;

        this.bat = 0;

        interv.setText(Integer.toString(interval) + " Seconds");

        lastSense = System.currentTimeMillis()+1000;
        lastShake = System.currentTimeMillis()+1000;
        last_x = 0;
        last_y = 0;
        last_z = 0;



        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);


        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.ACCESS_COARSE_LOCATION},
                        MY_PERMISSION_ACCESS_COARSE_LOCATION);
            }
            // The ACCESS_FINE_LOCATION is denied, then I request it and manage the result in
            // onRequestPermissionsResult() using the constant MY_PERMISSION_ACCESS_FINE_LOCATION
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                        MY_PERMISSION_ACCESS_FINE_LOCATION);
            }
        }

        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, interval * 1000, Criteria.ACCURACY_FINE, this);
        //System.out.println(interval);
        //Location loc = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
        //listener.onLocationChanged(loc);
        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        sensorManager.registerListener(this, sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER), SensorManager.SENSOR_DELAY_NORMAL);

        batteryReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                int level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, 0);
                int scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1);

                float batteryPct = level / (float)scale;

                if(batteryPct*100 < W && bat == 0){
                    interval = interval + Z;
                    X = X + Z;
                    U = U + Z;
                    bat = 1;
                    updateTime();
                }
                else if(batteryPct*100 > W && bat == 1) {
                    interval = interval - Z;
                    X = X - Z;
                    U = U - Z;
                    bat = 0;
                    updateTime();
                }

            }
        };

        this.registerReceiver(batteryReceiver, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
    }

        @Override
        public void onLocationChanged(Location location) {
            latitude.setText(Double.toString(location.getLatitude()));
            longitude.setText(Double.toString(location.getLongitude()));
            long time = location.getTime();
            SimpleDateFormat format = new SimpleDateFormat("MM-dd hh:mm:ss.SSS");
            String t = format.format(time);
            timestamp.setText(t);
        }

        // Called when a provider gets turned off by the user in the settings
        @Override
        public void onProviderDisabled(String provider) {
        }

        // Called when a provider is turned on by the user in the settings
        @Override
        public void onProviderEnabled(String provider) {
        }

        // Signals a state change in the GPS (e.g. you head through a tunnel and
        // it loses its fix on your position)
        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {
        }


        @Override
        public void onSensorChanged (SensorEvent event){
            if (event.sensor == sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)) {
                long curTime = System.currentTimeMillis();

                if (curTime - lastSense > 100) {
                    long timeDiff = curTime - lastSense;
                    lastSense = curTime;

                    float x = event.values[0];
                    float y = event.values[1];
                    float z = event.values[2];


                    float speed = Math.abs(x + y + z - last_x - last_y - last_z) / timeDiff * 10000;

                    System.out.println(speed);
                    if (speed > 50 && curTime - lastShake > 2000)  {
                        lastShake = curTime;
                        if (interval == X || interval == U) {
                            Y = Y * -1;
                        }
                        interval = interval + Y;
                        interv.setText(Integer.toString(this.interval) + " Seconds");

                        updateTime();
                    }
                    last_x = x;
                    last_y = y;
                    last_z = z;
                }
            }
        }
        @Override
        public void onAccuracyChanged (Sensor sensor,int accuracy){

        }



        @Override
        public void onRequestPermissionsResult ( int requestCode, String permissions[],
        int[] grantResults){
            switch (requestCode) {
                case MY_PERMISSION_ACCESS_COARSE_LOCATION: {
                    if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                        // permission was granted
                    } else {
                        // permission denied
                    }
                    break;
                }
                case MY_PERMISSION_ACCESS_FINE_LOCATION: {
                    if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                        // permission was granted
                    } else {
                        // permission denied
                    }
                    break;
                }
            }
        }

        @Override
        protected void onResume () {
            super.onResume();
            if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

                if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(this,
                            new String[]{Manifest.permission.ACCESS_COARSE_LOCATION},
                            MY_PERMISSION_ACCESS_COARSE_LOCATION);
                }
                // The ACCESS_FINE_LOCATION is denied, then I request it and manage the result in
                // onRequestPermissionsResult() using the constant MY_PERMISSION_ACCESS_FINE_LOCATION
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(this,
                            new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                            MY_PERMISSION_ACCESS_FINE_LOCATION);
                }
            }
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, this.interval * 1000, Criteria.ACCURACY_FINE, this);


        }

        @Override
        protected void onPause () {
            super.onPause();
            unregisterReceiver(batteryReceiver);
            locationManager.removeUpdates(this);
        }

        private void updateTime(){
            if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

                if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(this,
                            new String[]{Manifest.permission.ACCESS_COARSE_LOCATION},
                            MY_PERMISSION_ACCESS_COARSE_LOCATION);
                }
                // The ACCESS_FINE_LOCATION is denied, then I request it and manage the result in
                // onRequestPermissionsResult() using the constant MY_PERMISSION_ACCESS_FINE_LOCATION
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(this,
                            new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                            MY_PERMISSION_ACCESS_FINE_LOCATION);
                }
            }
            System.out.println(this.interval);
            interv.setText(Integer.toString(this.interval) + " Seconds");
            locationManager.removeUpdates(this);
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, this.interval * 1000, Criteria.ACCURACY_FINE, this);
        }
}
