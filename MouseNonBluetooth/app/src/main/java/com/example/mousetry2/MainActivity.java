package com.example.mousetry2;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.hardware.usb.UsbConstants;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbEndpoint;
import android.hardware.usb.UsbInterface;
import android.hardware.usb.UsbManager;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.widget.Button;

import java.io.IOException;
import java.io.OutputStream;

public class MainActivity extends AppCompatActivity implements SensorEventListener {

    private static final int USB_INTERFACE_CLASS = (UsbConstants.USB_CLASS_VENDOR_SPEC);
    private static final int USB_INTERFACE_SUBCLASS = 0x42;
    private static final int USB_INTERFACE_PROTOCOL = 0x01;

    private UsbManager usbManager;
    private UsbDevice usbDevice;
    private UsbDeviceConnection usbConnection;
    private UsbInterface usbInterface;
    private UsbEndpoint outEndpoint;

    private SensorManager sensorManager;
    private Sensor accelerometerSensor;
    private float last_x, last_y;

    private long lastUpdate = 0;
    private OutputStream outputStream = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        accelerometerSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        sensorManager.registerListener(this, accelerometerSensor, SensorManager.SENSOR_DELAY_NORMAL);

        // Setup UI and button listeners for mouse control
        setupMouseControlButtons();

        usbManager = (UsbManager) getSystemService(USB_SERVICE);
        setupUsbConnection();
    }
    private void setupUsbConnection() {
        // Find the USB device with the specified interface class, subclass, and protocol
        for (UsbDevice device : usbManager.getDeviceList().values()) {
            if (device.getInterface(USB_INTERFACE_CLASS) != null) {
                usbDevice = device;
                break;
            }
        }

        if (usbDevice != null) {
            UsbInterface usbInterface = usbDevice.getInterface(USB_INTERFACE_CLASS);
            UsbEndpoint outEndpoint = usbInterface.getEndpoint(UsbConstants.USB_DIR_OUT);
            usbConnection = usbManager.openDevice(usbDevice);
            usbConnection.claimInterface(usbInterface, true);
        }
    }

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
        Sensor mySensor = event.sensor;
        if (mySensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            float x = event.values[0];
            float y = event.values[1];

            long curTime = System.currentTimeMillis();

            if((curTime - lastUpdate) > 100){
                long diffTime = curTime - lastUpdate;
                lastUpdate = curTime;
            }

            last_x = x;
            last_y = y;
            System.out.println(x + " " + y);
            sendAccelerometerData(last_x,last_y);
        }

    }
    private void sendAccelerometerData(float x, float y) {
        if (usbConnection != null && outEndpoint != null) {
            byte[] data = String.format("X: %.2f, Y: %.2f, Z: %.2f", x, y).getBytes();
            int bytesWritten = usbConnection.bulkTransfer(outEndpoint, data, data.length, 0);
            if (bytesWritten != data.length) {
                System.out.println("Failed when trying to send the accelerometer data");
            }
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