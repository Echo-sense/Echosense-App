package com.example.echosense;

import androidx.appcompat.app.AppCompatActivity;

import android.Manifest;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.MediaPlayer;
import android.media.RingtoneManager;
import android.net.Uri;
import android.nfc.Tag;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.google.android.gms.appindexing.AppIndex;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.firebase.appindexing.FirebaseAppIndex;
import com.google.firebase.appindexing.FirebaseUserActions;
import com.google.firebase.appindexing.Action;
import com.google.firebase.appindexing.Indexable;
import com.google.firebase.appindexing.builders.Actions;


import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {

    BluetoothManager btManager;
    BluetoothAdapter btAdapter;
    BluetoothLeScanner btScanner;
    Button startScanningButton;
    Button stopScanningButton;
    TextView peripheralTextView;
    private final static int REQUEST_ENABLE_BT = 1;
    private static final int PERMISSION_REQUEST_COARSE_LOCATION = 1;

    Boolean btScanning = false;
    int deviceIndex = 0;
    ArrayList<BluetoothDevice> devicesDiscovered = new ArrayList<BluetoothDevice>();
    EditText deviceIndexInput;
    Button connectToDevice;
    Button disconnectDevice;
    BluetoothGatt bluetoothGatt;


    public final static String ACTION_GATT_CONNECTED =
            "com.example.bluetooth.le.ACTION_GATT_CONNECTED";
    public final static String ACTION_GATT_DISCONNECTED =
            "com.example.bluetooth.le.ACTION_GATT_DISCONNECTED";
    public final static String ACTION_GATT_SERVICES_DISCOVERED =
            "com.example.bluetooth.le.ACTION_GATT_SERVICES_DISCOVERED";
    public final static String ACTION_DATA_AVAILABLE =
            "com.example.bluetooth.le.ACTION_DATA_AVAILABLE";
    public final static String EXTRA_DATA =
            "com.example.bluetooth.le.EXTRA_DATA";

    private final static UUID BT_service_ID1 = UUID.fromString("0000a000-0000-1000-8000-00805f9b34fb");
    private final static UUID char_service_ID1 = UUID.fromString("0000a001-0000-1000-8000-00805f9b34fb");

    private final static UUID BT_service_ID2 = UUID.fromString("00001800-0000-1000-8000-00805f9b34fb");
    private final static UUID char_service_ID2 = UUID.fromString("00002a00-0000-1000-8000-00805f9b34fb");
    private final static UUID char_service_ID3 = UUID.fromString("00002a01-0000-1000-8000-00805f9b34fb");
    private final static UUID char_service_ID4 = UUID.fromString("00002a04-0000-1000-8000-00805f9b34fb");

    private final static UUID BT_service_ID3 = UUID.fromString("00001801-0000-1000-8000-00805f9b34fb");
    private final static UUID char_service_ID5 = UUID.fromString("00002a05-0000-1000-8000-00805f9b34fb");

    private final static UUID desc_ID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");
    public Map<String, String> uuids = new HashMap<String, String>();

    // Stops scanning after 5 seconds.
    private Handler mHandler = new Handler();
    private static final long SCAN_PERIOD = 5000;


    /**
     * ATTENTION: This was auto-generated to implement the App Indexing API.
     * See https://g.co/AppIndexing/AndroidStudio for more information.
     */
    private GoogleApiClient client;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        peripheralTextView = (TextView) findViewById(R.id.PeripheralTextView);
        peripheralTextView.setMovementMethod(new ScrollingMovementMethod());
        deviceIndexInput = (EditText) findViewById(R.id.InputIndex);
        deviceIndexInput.setText("0");

        connectToDevice = (Button) findViewById(R.id.ConnectButton);
        connectToDevice.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                connectToDeviceSelected();
            }
        });

        disconnectDevice = (Button) findViewById(R.id.DisconnectButton);
        disconnectDevice.setVisibility(View.INVISIBLE);
        disconnectDevice.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                disconnectDeviceSelected();
            }
        });

        startScanningButton = (Button) findViewById(R.id.StartScanButton);
        startScanningButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                startScanning();
            }
        });

        stopScanningButton = (Button) findViewById(R.id.StopScanButton);
        stopScanningButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                stopScanning();
            }
        });
        stopScanningButton.setVisibility(View.INVISIBLE);

        btManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        btAdapter = btManager.getAdapter();
        btScanner = btAdapter.getBluetoothLeScanner();

        if (btAdapter != null && !btAdapter.isEnabled()) {
            Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
        }

        // Make sure we have access coarse location enabled, if not, prompt the user to enable it
        if (this.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            final AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("This app needs location access");
            builder.setMessage("Please grant location access so this app can detect peripherals.");
            builder.setPositiveButton(android.R.string.ok, null);
            builder.setOnDismissListener(new DialogInterface.OnDismissListener() {
                @Override
                public void onDismiss(DialogInterface dialog) {
                    requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, PERMISSION_REQUEST_COARSE_LOCATION);
                }
            });
            builder.show();
        }

        client = new GoogleApiClient.Builder(this).addApi(AppIndex.API).build();
    }

    // Device scan callback.
    private ScanCallback leScanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            if(result.getDevice().getName() != null){
                peripheralTextView.append("Index: " + deviceIndex + ", Device Name: " + result.getDevice().getName() + " rssi: " + result.getRssi() + "\n");
                devicesDiscovered.add(result.getDevice());
                deviceIndex++;
                // auto scroll for text view
                final int scrollAmount = peripheralTextView.getLayout().getLineTop(peripheralTextView.getLineCount()) - peripheralTextView.getHeight();
                // if there is no need to scroll, scrollAmount will be <=0
                if (scrollAmount > 0) {
                    peripheralTextView.scrollTo(0, scrollAmount);
                }
            }
        }
    };

    // Device connect call back
    private final BluetoothGattCallback btleGattCallback = new BluetoothGattCallback() {

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, final BluetoothGattCharacteristic characteristic) {
            // this will get called anytime you perform a read or write characteristic operation
            MainActivity.this.runOnUiThread(new Runnable() {
                public void run() {
                    peripheralTextView.append("device read or wrote to\n");
                }
            });
            super.onCharacteristicChanged(gatt, characteristic);
            final MediaPlayer mp = MediaPlayer.create(MainActivity.this, R.raw.noti);
            byte[] msg = characteristic.getValue();

            System.out.println("length is " + msg.length);
            for(int i=0; i< msg.length; ++i){
                int cInt = msg[i];
                System.out.println("msg[" + i + "] is: " + cInt);
            }
            int charInt = msg[0] & 0xFF;
            if(charInt == 1){
                mp.start();
            }
            else{
                mp.stop();
            }
            //float charFloat = characteristic.getFloatValue(BluetoothGattCharacteristic.FORMAT_FLOAT, 0);
            //System.out.println(charInt);
            //String charString = Float.toString(charFloat);
            /*
            String msgString = null;
            try
            {
                msgString = new String(msg, "UTF-8");
            }
            catch(UnsupportedEncodingException e)
            {
                peripheralTextView.append("Unable to convert message bytes to string. \n");
            }
             */
            //peripheralTextView.append("characteristic value is " + msg + "\n");
        }

        @Override
        public void onConnectionStateChange(final BluetoothGatt gatt, final int status, final int newState) {
            // this will get called when a device connects or disconnects
            System.out.println(newState);
            switch (newState) {
                case 0:
                    MainActivity.this.runOnUiThread(new Runnable() {
                        public void run() {
                            peripheralTextView.append("device disconnected\n");
                            connectToDevice.setVisibility(View.VISIBLE);
                            disconnectDevice.setVisibility(View.INVISIBLE);
                        }
                    });
                    break;
                case 2:
                    MainActivity.this.runOnUiThread(new Runnable() {
                        public void run() {
                            peripheralTextView.append("device connected\n");
                            connectToDevice.setVisibility(View.INVISIBLE);
                            disconnectDevice.setVisibility(View.VISIBLE);
                        }
                    });

                    // discover services and characteristics for this device
                    bluetoothGatt.discoverServices();

                    break;
                default:
                    MainActivity.this.runOnUiThread(new Runnable() {
                        public void run() {
                            peripheralTextView.append("we encounterned an unknown state, uh oh\n");
                        }
                    });
                    break;
            }
        }

        @Override
        public void onServicesDiscovered(final BluetoothGatt gatt, final int status) {
            // this will get called after the client initiates a 			BluetoothGatt.discoverServices() call
            MainActivity.this.runOnUiThread(new Runnable() {
                public void run() {
                    peripheralTextView.append("device services have been discovered\n");
                }
            });

            //displayGattServices(gatt);
            BluetoothGattService gatt_service = gatt.getService(BT_service_ID1);
            BluetoothGattCharacteristic gatt_char = gatt_service.getCharacteristic(char_service_ID1);

            gatt.setCharacteristicNotification(gatt_char, true);

            BluetoothGattDescriptor descriptor = gatt_char.getDescriptor(desc_ID);
            descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);

            gatt.writeDescriptor(descriptor);


            /*
            final boolean charRead = gatt.readCharacteristic(gatt_char);
            MainActivity.this.runOnUiThread(new Runnable() {
                public void run() {
                    peripheralTextView.append("characteristic read returns: "+charRead+"\n");
                }
            });

            final String uuid = gatt_service.getUuid().toString();
            System.out.println("Service discovered: " + uuid);
            MainActivity.this.runOnUiThread(new Runnable() {
                public void run() {
                    peripheralTextView.append("Service disovered: "+uuid+"\n");
                }
            });
            final String charUuid = gatt_char.getUuid().toString();
            System.out.println("Characteristic discovered for service: " + charUuid);
            MainActivity.this.runOnUiThread(new Runnable() {
                public void run() {
                    peripheralTextView.append("Characteristic discovered for service: "+charUuid+"\n");
                }
            });
        */

        }

        @Override
        public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status){
            BluetoothGattService gatt_service = gatt.getService(BT_service_ID1);
            BluetoothGattCharacteristic gatt_char = gatt_service.getCharacteristic(char_service_ID1);

            //gatt_char.setValue(new byte[]{1, 1});
            gatt.writeCharacteristic(gatt_char);
        }

        @Override
        // Result of a characteristic read operation
        public void onCharacteristicRead(BluetoothGatt gatt,
                                         BluetoothGattCharacteristic characteristic,
                                         int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                broadcastUpdate(ACTION_DATA_AVAILABLE, characteristic);
                byte[] value = characteristic.getValue();
                final String v = new String(value);
                MainActivity.this.runOnUiThread(new Runnable() {
                    public void run() {
                        peripheralTextView.append("on characteristic read value is: " + v + "\n");
                    }
                });
            }

        }

    };

    private void broadcastUpdate(final String action,
                                 final BluetoothGattCharacteristic characteristic) {

        System.out.println(characteristic.getUuid());
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case PERMISSION_REQUEST_COARSE_LOCATION: {
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    System.out.println("coarse location permission granted");
                } else {
                    final AlertDialog.Builder builder = new AlertDialog.Builder(this);
                    builder.setTitle("Functionality limited");
                    builder.setMessage("Since location access has not been granted, this app will not be able to discover beacons when in the background.");
                    builder.setPositiveButton(android.R.string.ok, null);
                    builder.setOnDismissListener(new DialogInterface.OnDismissListener() {

                        @Override
                        public void onDismiss(DialogInterface dialog) {
                        }

                    });
                    builder.show();
                }
                return;
            }
        }
    }

    public void startScanning() {
        System.out.println("start scanning");
        btScanning = true;
        deviceIndex = 0;
        devicesDiscovered.clear();
        peripheralTextView.setText("");
        peripheralTextView.append("Started Scanning\n");
        startScanningButton.setVisibility(View.INVISIBLE);
        stopScanningButton.setVisibility(View.VISIBLE);
        AsyncTask.execute(new Runnable() {
            @Override
            public void run() {
                btScanner.startScan(leScanCallback);
            }
        });

        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                stopScanning();
            }
        }, SCAN_PERIOD);
    }

    public void stopScanning() {
        System.out.println("stopping scanning");
        peripheralTextView.append("Stopped Scanning\n");
        btScanning = false;
        startScanningButton.setVisibility(View.VISIBLE);
        stopScanningButton.setVisibility(View.INVISIBLE);
        AsyncTask.execute(new Runnable() {
            @Override
            public void run() {
                btScanner.stopScan(leScanCallback);
            }
        });
    }

    public void connectToDeviceSelected() {
        peripheralTextView.append("Trying to connect to device at index: " + deviceIndexInput.getText() + "\n");
        int deviceSelected = Integer.parseInt(deviceIndexInput.getText().toString());
        bluetoothGatt = devicesDiscovered.get(deviceSelected).connectGatt(this, false, btleGattCallback);
    }

    public void disconnectDeviceSelected() {
        peripheralTextView.append("Disconnecting from device\n");
        bluetoothGatt.disconnect();
    }

    public void setCharacteristicNotification(BluetoothGattCharacteristic character, boolean enabled){
        bluetoothGatt.setCharacteristicNotification(character, enabled);
        BluetoothGattDescriptor descriptor = character.getDescriptor(desc_ID);
        descriptor.setValue(enabled?BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE:BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE);
        bluetoothGatt.writeDescriptor(descriptor);
    }

    private void displayGattServices(BluetoothGatt gatt){
        BluetoothGattService gatt_service = gatt.getService(BT_service_ID1);
        BluetoothGattCharacteristic gatt_char = gatt_service.getCharacteristic(char_service_ID1);

       // setCharacteristicNotification(gatt_char, true);

        //get descriptor from characteristic above using pre-defined descriptor UUID.
        //final BluetoothGattDescriptor desc = gatt_char.getDescriptor(desc_ID);
        //read Descriptor, should return either true or false -> theoretically should trigger onDescriptorRead and subscribe to ble service and update every time characteristic changes.
        //gatt.readDescriptor(desc);
        //set ble notification using the descriptor

        //prints descriptor
        /*
        MainActivity.this.runOnUiThread(new Runnable() {
            public void run() {
                peripheralTextView.append("desc is: "+desc+"\n");
            }
        });
         */
        //set descriptor value, either ENABLE_INDICATION_VALUE or ENABLE_NOTIFICATION_VALUE.
        //desc.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
        //desc.setValue(BluetoothGattDescriptor.ENABLE_INDICATION_VALUE);
        //write back to descriptor.
        //final boolean writeDesc = gatt.writeDescriptor(desc);
        /*
        MainActivity.this.runOnUiThread(new Runnable() {
            public void run() {
                peripheralTextView.append("writeDesc is: "+writeDesc+"\n");
            }
        });
*/
        final boolean charRead = gatt.readCharacteristic(gatt_char);
        MainActivity.this.runOnUiThread(new Runnable() {
            public void run() {
                peripheralTextView.append("characteristic read returns: "+charRead+"\n");
            }
        });

        final String uuid = gatt_service.getUuid().toString();
        System.out.println("Service discovered: " + uuid);
        MainActivity.this.runOnUiThread(new Runnable() {
            public void run() {
                peripheralTextView.append("Service disovered: "+uuid+"\n");
            }
        });
        final String charUuid = gatt_char.getUuid().toString();
        System.out.println("Characteristic discovered for service: " + charUuid);
        MainActivity.this.runOnUiThread(new Runnable() {
            public void run() {
                peripheralTextView.append("Characteristic discovered for service: "+charUuid+"\n");
            }
        });

        //gatt.readCharacteristic(gatt_char);

    }

/*
    private void displayGattServices(List<BluetoothGattService> gattServices) {
        if (gattServices == null) return;

        // Loops through available GATT Services.
        for (BluetoothGattService gattService : gattServices) {


            final String uuid = gattService.getUuid().toString();
            System.out.println("Service discovered: " + uuid);
            MainActivity.this.runOnUiThread(new Runnable() {
                public void run() {
                    peripheralTextView.append("Service disovered: "+uuid+"\n");
                }
            });
            new ArrayList<HashMap<String, String>>();
            List<BluetoothGattCharacteristic> gattCharacteristics =
                    gattService.getCharacteristics();

            // Loops through available Characteristics.
            for (BluetoothGattCharacteristic gattCharacteristic :
                    gattCharacteristics) {
                for (final BluetoothGattDescriptor descriptor:gattCharacteristic.getDescriptors()){
                    MainActivity.this.runOnUiThread(new Runnable() {
                        public void run() {
                            peripheralTextView.append("BluetoothGattDescriptor: "+descriptor.getUuid().toString()+"\n");
                        }
                    });
                }
                final byte[] mValue = gattCharacteristic.getValue();

                final String charUuid = gattCharacteristic.getUuid().toString();
                System.out.println("Characteristic discovered for service: " + charUuid);
                MainActivity.this.runOnUiThread(new Runnable() {
                    public void run() {
                        peripheralTextView.append("Characteristic discovered for service: "+charUuid+"\n");
                    }
                });
                MainActivity.this.runOnUiThread(new Runnable() {
                    public void run() {
                        peripheralTextView.append("Characteristic discovered for service: "+mValue+"\n");
                    }
                });

            }
        }
    }
*/
    @Override
    public void onStart() {
        super.onStart();

//        client.connect();
//        Action viewAction = Action.(
//                Action.TYPE_VIEW, // TODO: choose an action type.
//                "Main Page", // TODO: Define a title for the content shown.
//                // TODO: If you have web page content that matches this app activity's content,
//                // make sure this auto-generated web page URL is correct.
//                // Otherwise, set the URL to null.
//                Uri.parse("http://host/path"),
//                // TODO: Make sure this auto-generated app URL is correct.
//                Uri.parse("android-app://com.example.joelwasserman.androidbleconnectexample/http/host/path")
//        );
//        AppIndex.AppIndexApi.start(client, viewAction);
    }

    @Override
    public void onStop() {

/*        Action viewAction = Action.newAction(
                Action.TYPE_VIEW, // TODO: choose an action type.
                "Main Page", // TODO: Define a title for the content shown.
                // TODO: If you have web page content that matches this app activity's content,
                // make sure this auto-generated web page URL is correct.
                // Otherwise, set the URL to null.
                Uri.parse("http://host/path"),
                // TODO: Make sure this auto-generated app URL is correct.
                Uri.parse("android-app://com.example.joelwasserman.androidbleconnectexample/http/host/path")
        );
        AppIndex.AppIndexApi.end(client, viewAction);
        client.disconnect();*/
        super.onStop();
    }
}