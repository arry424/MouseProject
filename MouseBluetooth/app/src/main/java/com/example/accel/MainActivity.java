package com.example.accel;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothSocket;
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
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.Switch;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@RequiresApi(api = Build.VERSION_CODES.S)
public class MainActivity extends AppCompatActivity implements SensorEventListener, ActivityCompat.OnRequestPermissionsResultCallback {

    private MousePackage mouse;
    BluetoothManager bluetoothManager;
    BluetoothAdapter bluetoothAdapter;
    private List<BluetoothDevice> discoveredDevices = new ArrayList<>();
    private OutputStream outputStream = null;
    private SensorManager sensorManager;
    private Sensor accelerometerSensor;
    private Sensor distanceSensor;
    private float last_x, last_y;

    int REQUEST_ENABLE_BLUETOOTH = 1;


    int REQUEST_ALL_PERMISSIONS_STORAGE = 2;
    int REQUEST_ALL_PERMISSIONS_LOCATION = 3;

    private final static String[] PERMISSIONS_STORAGE = {
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_LOCATION_EXTRA_COMMANDS,
            Manifest.permission.BLUETOOTH_SCAN,
            Manifest.permission.BLUETOOTH_CONNECT,
            Manifest.permission.BLUETOOTH_PRIVILEGED
    };
    private final static String[] PERMISSIONS_LOCATION = {
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_LOCATION_EXTRA_COMMANDS,
            Manifest.permission.BLUETOOTH_SCAN,
            Manifest.permission.BLUETOOTH_CONNECT,
            Manifest.permission.BLUETOOTH_PRIVILEGED
    };


    // BroadcastReceiver to handle Bluetooth device discovery
    private final BroadcastReceiver discoveryReceiver = new BroadcastReceiver() {

        //@SuppressLint("MissingPermission")
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                if (device != null && !(device.getName() == null))  {
                    discoveredDevices.add(device);
                    Log.d("INFO", "onReceive: Has Added a new Device!");
                    Log.d("INFO", "onReceive: Device name:" + device.getName());
                }
            }
            if (BluetoothDevice.ACTION_ACL_CONNECTED.equals(action)) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                Log.d("DEBUG", "onReceive: Just Connected to device: " + device.getName() + "!");
            }
            if (BluetoothDevice.ACTION_ACL_DISCONNECTED.equals(action)) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                Log.d("DEBUG", "onReceive: Just Disconnected from device: " + device.getName() + "!");
            }
            if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
                Log.d("DEBUG", "onReceive: Discovery Process Has Finished.");
                showDeviceSelectionDialog(discoveredDevices);
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d("DEBUG", "Starting Mouse Application!");
        setContentView(R.layout.activity_main); // Replace "activity_main" with your layout file name

        // Check Permissions
        checkPermissions();

        boolean bluetoothAvailable = getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH);
        BluetoothManager bluetoothManager = getSystemService(BluetoothManager.class);
        bluetoothAdapter = bluetoothManager.getAdapter();

        if (bluetoothAvailable) {
            Log.d("INFO", "Bluetooth is Available");
        } else {
            Log.e("ERROR", "Bluetooth is Not Available");
            return; // Device doesn't support Bluetooth
        }

        if (bluetoothAdapter == null) {
            Log.e("ERROR", "The Bluetooth Adapter is Null");
            return; // Device doesn't support Bluetooth
        }
        Log.d("DEBUG", "The Bluetooth Adapter is " + bluetoothAdapter);

        // Request to enable Bluetooth if it's not already enabled
        if (!bluetoothAdapter.isEnabled()) {
            Log.d("DEBUG", "the bluetooth adapter is not enabled.");
            Log.d("DEBUG", "Requesting Bluetooth Permissions.");
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BLUETOOTH);
        } else {
            // Start Bluetooth discovery process
            BluetoothButton();
        }

        // TODO: Work on Sensor Manager, DO NOT DELETE
        // Initialize sensor manager and accelerometer sensor
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        if (sensorManager != null) {
           accelerometerSensor = sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION);
           sensorManager.registerListener(this, accelerometerSensor, SensorManager.SENSOR_DELAY_NORMAL);

            // Setup UI and button listeners for mouse control
           setupMouseControlButtons();
        }
        mouse = new MousePackage(800);
    }

    public void BluetoothButton(){
        Switch bluetoothSwitch = findViewById(R.id.switch1); // Assuming you have a button with id left in your layout
        bluetoothSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    startBluetoothDiscovery();
                    Log.d("BLUETOOTH", "Bluetooth is Already Turned On");
                } else {
                    onDestroy();
                    Log.d("BLUETOOTH", "Bluetooth is turned off");
                }
            }
        });
    }

    @Override public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_ENABLE_BLUETOOTH) {
            if (resultCode == Activity.RESULT_OK) {
                // Do Bluetooth Tasks
                Log.d("DEBUG", "User Accepted Request: The bluetooth adapter is enabled!");
                startBluetoothDiscovery();
                return;
            }
            if (resultCode == Activity.RESULT_CANCELED) {
                Log.d("DEBUG", "User Declined Request: The bluetooth adapter remains disabled.");
                return;
            }
        }
        if (requestCode == REQUEST_ALL_PERMISSIONS_LOCATION) {
            if (resultCode == Activity.RESULT_OK) {
                // Do Bluetooth Tasks
                Log.d("DEBUG", "User Accepted Request: REQUEST_ALL_PERMISSIONS_LOCATION");
                startBluetoothDiscovery();
                return;
            }
            if (resultCode == Activity.RESULT_CANCELED) {
                Log.d("DEBUG", "User Declined Request: REQUEST_ALL_PERMISSIONS_LOCATION");
                return;
            }
        }
        if (requestCode == REQUEST_ALL_PERMISSIONS_LOCATION) {
            if (resultCode == Activity.RESULT_OK) {
                // Do Bluetooth Tasks
                Log.d("DEBUG", "User Accepted Request: REQUEST_ALL_PERMISSIONS_LOCATION");
                startBluetoothDiscovery();
                return;
            }
            if (resultCode == Activity.RESULT_CANCELED) {
                Log.d("DEBUG", "User Declined Request: REQUEST_ALL_PERMISSIONS_LOCATION");
                return;
            }
        }
        // Add More Request Handlers Here
    }

    // Starts the Bluetooth discovery process and registers the discovery receiver
    private void startBluetoothDiscovery() {
        Log.d("DEBUG", "startBluetoothDiscovery: Attempting to Start Discovery");
        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        // Actions we want to register for
        filter.addAction(BluetoothDevice.ACTION_NAME_CHANGED);
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        filter.addAction(BluetoothDevice.ACTION_ACL_CONNECTED);
        filter.addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED);
        registerReceiver(discoveryReceiver, filter);
        boolean hasStartedDiscovery = bluetoothAdapter.startDiscovery();
        String message = hasStartedDiscovery ? "successfully started discovery" : "failed to start discovery";
        Log.d("DEBUG", "startBluetoothDiscovery: Started Discovery Status: " + message);
    }

    // Shows a dialog for the user to select a Bluetooth device from the discovered devices
    private void showDeviceSelectionDialog(final List<BluetoothDevice> devices) {
        List<String> deviceNames = new ArrayList<>();
        for (BluetoothDevice device : devices) {
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
        Log.d("DEBUG", "connectToDevice: Attempting to Connect to Device:" + device.getName());
        try {
            UUID uuid = UUID.fromString("00001101-0000-1000-8000-00805f9b34fb"); // Standard SerialPortService ID
            BluetoothSocket btSocket = device.createRfcommSocketToServiceRecord(uuid);
            btSocket.connect();
            outputStream = btSocket.getOutputStream();
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
                    sendMouseClick(3); // Right mouse button downY
                } else if (event.getAction() == MotionEvent.ACTION_UP) {
                    sendMouseClick(2); // Right mouse button up
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
            String clickCommand = "C," + button + "\n";
            try {
                outputStream.write(clickCommand.getBytes());
            } catch (IOException e) {
                System.out.println("Failed at 'send mouse click'" + " with exception " + e);
                e.printStackTrace();
            }
        }
    }

    public class MousePackage{
        private final float VELOCITY_THRESHOLD = .0001F; //correct
        private final float THRESHOLD = .001F;
        private final float INCH_PER_METER = 39.3701F;
        private int DPI;
        private long nanoStart,nanoEnd;
        private float[] previousAcceleration;

        private double[] velocity;
        private double[] dV_previous;

        private int[] ignoreEvents;
        private final int NUM_IGNORE = 2;

        private float accelCount = 0;
        private float[] velocityError;

        //TODO: Add mouse settings
        public MousePackage(int dpi) {
            previousAcceleration = new float[]{0, 0};
            velocity = new double[]{0,0};
            dV_previous = new double[]{0, 0};
            nanoEnd = System.nanoTime();
            DPI = dpi;
            ignoreEvents = new int[]{0,0};
            velocityError = new float[]{0,0};
        }
        //TODO: Integrate with DPI
        //TODO: look at this again for accuracy
        //TODO: Calculate answer in one pass
        public double[] getMouseDistance(float xAcceleration, float yAcceleration, long currTime) {

            nanoStart = nanoEnd;
            nanoEnd = currTime;
            long dt = nanoEnd-nanoStart;
            accelCount += xAcceleration;

            Log.d("count of accel", ""+(accelCount));
            return new double[] {getAxisDistance(xAcceleration, 0, dt), getAxisDistance(yAcceleration,1,dt)};
//            return new double[] {getAxisDistance2(xAcceleration, 0, dt), getAxisDistance2(yAcceleration,1,dt)};

        }
        private double getAxisDistance(float genAccel, int MASK, long dt){
            velocityError[MASK] += genAccel;
            if(ignoreEvents[MASK] > 0){
                ignoreEvents[MASK]--;
                //previousAcceleration[MASK] = genAccel;
                return 0;
            }
            if(!(previousAcceleration[MASK] < 0 && genAccel < 0) ^ !(previousAcceleration[MASK] > 0 && genAccel>0)){
                ignoreEvents[MASK] = NUM_IGNORE-1;
                velocity[MASK] = 0;
                velocityError[MASK] = 0;
                previousAcceleration[MASK] = 0;

                return 0;
            }


            Log.d("dA", "x " + genAccel);

            double dV_currentGen = (dt * ((previousAcceleration[MASK]+genAccel)/2))/1000000000.0;
//            dV_currentGen = (VELOCITY_THRESHOLD > Math.abs(dV_currentGen))? 0: dV_currentGen;
            velocity[MASK] += dV_previous[MASK];
            if (.01 > Math.abs(velocity[MASK])) {
                velocity[MASK] = 0;
            }

            Log.d("dV", "x " + dV_currentGen);
            double changeInPositionGen = velocity[MASK] * dt/1000000000.0;

            dV_previous[MASK] = (dV_currentGen*1000)/1000.0;
//            velocity[MASK] += dV_previous[MASK];
            previousAcceleration[MASK] = genAccel;


            return (THRESHOLD > Math.abs(changeInPositionGen))? 0: changeInPositionGen;

        }

        private double getAxisDistance2(float acceleration, int MASK, long dt) {
            if(ignoreEvents[MASK] > 0){
                ignoreEvents[MASK]--;
                return 0;
            }

            if (THRESHOLD > Math.abs(acceleration)) {
                previousAcceleration[MASK] = 0;
                return 0;
            }

            if(!(previousAcceleration[MASK] < 0 && acceleration < 0) ^ !(previousAcceleration[MASK] > 0 && acceleration>0)) {
                previousAcceleration[MASK] = 0;
                ignoreEvents[MASK]--;
                return 0;
            }

            double avgAcceleration = (previousAcceleration[MASK] + acceleration)/2.0;
            previousAcceleration[MASK] = acceleration;

            return 1.0/6.0 * avgAcceleration*avgAcceleration*avgAcceleration * dt/1000000000.0;
        }

    }
    @Override
    public void onSensorChanged(SensorEvent event) {

        if (event.sensor.getType() == Sensor.TYPE_LINEAR_ACCELERATION && outputStream != null) {
            float x = event.values[0];
            float y = event.values[1];
            double[] sendData = mouse.getMouseDistance(x, y, System.nanoTime());
            sendMouseMove(sendData[0], sendData[1]);
            last_x = x;
            last_y = y;
        }

    }

    // Sends mouse move commands based on accelerometer data
    private void sendMouseMove(double x, double y) {
//        float deltaX = x - last_x;
//        float deltaY = y - last_y;
//        int scaledX = (int) (deltaX * 10);
//        int scaledY = (int) (deltaY * 10);
        x = -1*x;
        y = -1*y;
        String moveCommand = "M," + x + "," + y + "\n";
        try {
            if(!(x == 0 && y==0))
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
        if (sensorManager != null) {
            sensorManager.unregisterListener(this);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (sensorManager != null) {
            sensorManager.registerListener(this, accelerometerSensor, SensorManager.SENSOR_DELAY_NORMAL);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Unregister the discovery receiver when the activity is destroyed
        if (discoveryReceiver != null) {
            unregisterReceiver(discoveryReceiver);
        }
        // Unregister the sensor listener to prevent battery drain
        if (sensorManager != null) {
            sensorManager.unregisterListener(this);
        }
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

    /**
     * Once you call this method, you can be safe and assume all permissions & checks are met
     * Feel free to ignore compiler red lines about these permissions
     */
    private void checkPermissions(){
        int permission1 = ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE);
        int permission2 = ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN);
        if (permission1 != PackageManager.PERMISSION_GRANTED) {
            // We don't have permission so prompt the user
            ActivityCompat.requestPermissions(
                    this,
                    PERMISSIONS_STORAGE,
                    REQUEST_ALL_PERMISSIONS_STORAGE
            );
        } else if (permission2 != PackageManager.PERMISSION_GRANTED){
            ActivityCompat.requestPermissions(
                    this,
                    PERMISSIONS_LOCATION,
                    REQUEST_ALL_PERMISSIONS_LOCATION
            );
        }
    }
}