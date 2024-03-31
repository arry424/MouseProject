package com.example.accel;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.LocationManager;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class MainActivity extends AppCompatActivity implements SensorEventListener {


    BluetoothManager bluetoothManager;
    BluetoothAdapter bluetoothAdapter;
    private List<BluetoothDevice> discoveredDevices = new ArrayList<>();
    private OutputStream outputStream = null;
    private SensorManager sensorManager;
    private Sensor accelerometerSensor;
    private float last_x, last_y;

    // BroadcastReceiver to handle Bluetooth device discovery
    private final BroadcastReceiver discoveryReceiver = new BroadcastReceiver() {

        //@SuppressLint("MissingPermission")
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                if (device != null) {
                    discoveredDevices.add(device);
                }
            } else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
                showDeviceSelectionDialog(discoveredDevices);
                unregisterReceiver(this); // Unregister receiver after discovery is finished
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main); // Replace "activity_main" with your layout file name
        bluetoothManager = getSystemService(BluetoothManager.class);
     //   bluetoothAdapter = bluetoothManager.getAdapter();
        bluetoothAdapter = bluetoothAdapter.getDefaultAdapter();
        LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        boolean isGpsEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
        if (!isGpsEnabled) {
            startActivityForResult(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS), 1);
        }


        if (bluetoothAdapter == null) {
            System.out.println("the bluetooth adapter is null");
            return; // Device doesn't support Bluetooth
        }
        System.out.println("the bluetooth adapter is " + bluetoothAdapter);

        // Request to enable Bluetooth if it's not already enabled
        if (!bluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);

            if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                System.out.println("Breaks at the check self permission thing on line 75");
                return;
            }
            startActivityForResult(enableBtIntent, 1);
        }

        // Start Bluetooth discovery process
        startBluetoothDiscovery();

        // Initialize sensor manager and accelerometer sensor
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        accelerometerSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        sensorManager.registerListener(this, accelerometerSensor, SensorManager.SENSOR_DELAY_NORMAL);

        // Setup UI and button listeners for mouse control
        setupMouseControlButtons();
    }

    // Starts the Bluetooth discovery process and registers the discovery receiver
    private void startBluetoothDiscovery() {
        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        filter.addAction(BluetoothDevice.ACTION_NAME_CHANGED);
        registerReceiver(discoveryReceiver, filter);


        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        boolean check = bluetoothAdapter.startDiscovery();
        System.out.println("The 'start discovery' is returning " + check);
    }

    // Shows a dialog for the user to select a Bluetooth device from the discovered devices
    private void showDeviceSelectionDialog(final List<BluetoothDevice> devices) {
        List<String> deviceNames = new ArrayList<>();
        for (BluetoothDevice device : devices) {
            if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {

                return;
            }
            deviceNames.add(device.getName());
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Choose a Bluetooth device")
                .setItems(deviceNames.toArray(new CharSequence[0]), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // Connect to the selected device
                        connectToDevice(devices.get(which));
                    }
                })
                .create()
                .show();
    }

    // Connects to the selected Bluetooth device
    private void connectToDevice(BluetoothDevice device) {
        try {
            UUID uuid = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB"); // Standard SerialPortService ID
            if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                return;
            }
            outputStream = device.createRfcommSocketToServiceRecord(uuid).getOutputStream();
        } catch (IOException e) {
            System.out.println("fail on 'connect to device'" + " with exception " + e);
            e.printStackTrace();
        }
    }

    // Sets up the UI buttons for mouse control and their touch listeners
    private void setupMouseControlButtons() {
        Button leftButton = findViewById(R.id.left); // Assuming you have a button with id left in your layout
        if (leftButton != null) {
            leftButton.setOnTouchListener((v, event) -> {
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    System.out.println("LEFT");
                    sendMouseClick(1); // Left mouse button down
                } else if (event.getAction() == MotionEvent.ACTION_UP) {
                    sendMouseClick(0); // Left mouse button up
                }
                return true;
            });
        } else {
            Log.e("MainActivity", "Left button not found in the layout.");
        }

        Button rightButton = findViewById(R.id.right); // Assuming you have a button with id right in your layout
        if (rightButton != null) {
            rightButton.setOnTouchListener((v, event) -> {
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    System.out.println("RIGHT");
                    sendMouseClick(2); // Right mouse button downY
                } else if (event.getAction() == MotionEvent.ACTION_UP) {
                    sendMouseClick(0); // Right mouse button up
                }
                return true;
            });
        } else {
            Log.e("MainActivity", "Right button not found in the layout.");
        }
    }

    // Sends mouse click commands to the connected device
    private void sendMouseClick(int button) {
        if (outputStream != null) {
            String clickCommand = "C" + button + "\n";
            try {
                outputStream.write(clickCommand.getBytes());
            } catch (IOException e) {
                System.out.println("Failed at 'send mouse click'" + " with exception " + e);
                e.printStackTrace();
            }
        }
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER && outputStream != null) {
            float x = event.values[0];
            float y = event.values[1];
            sendMouseMove(x, y);
            last_x = x;
            last_y = y;
        }
    }

    // Sends mouse move commands based on accelerometer data
    private void sendMouseMove(float x, float y) {
        float deltaX = x - last_x;
        float deltaY = y - last_y;
        int scaledX = (int) (deltaX * 10);
        int scaledY = (int) (deltaY * 10);
        String moveCommand = "M" + scaledX + "," + scaledY + "\n";
        try {
            outputStream.write(moveCommand.getBytes());
        } catch (IOException e) {
            System.out.println("Failed at mouse move" + " with exception " + e);
            e.printStackTrace();
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // Not used in this context
    }

    @Override
    protected void onPause() {
        super.onPause();
        sensorManager.unregisterListener(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        sensorManager.registerListener(this, accelerometerSensor
                , SensorManager.SENSOR_DELAY_NORMAL);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Unregister the discovery receiver when the activity is destroyed
        unregisterReceiver(discoveryReceiver);
        // Unregister the sensor listener to prevent battery drain
        sensorManager.unregisterListener(this);
        // Close the outputStream if it's not null to prevent memory leaks
        if (outputStream != null) {
            try {
                outputStream.close();
            } catch (IOException e) {
                System.out.println("Failed at on destroy" + " with exception " + e);
                e.printStackTrace();
            }
        }
    }
}