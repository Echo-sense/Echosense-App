package com.example.echosense;

import androidx.appcompat.app.AppCompatActivity;

/*import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.icu.util.Output;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import org.w3c.dom.Text;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {

    //instantiate the UI for the app (Button, ListView, etc.)
    private TextView mBTstat;
    private TextView mRB;
    private Button mScan;
    private Button mOff;
    private Button mPaired;
    private Button mDiscover;
    private BluetoothAdapter mBTAdap;
    private Set<BluetoothDevice> mPairedDevices;
    private ArrayAdapter<String> mBTArrAdap;
    private ListView mDeviceListView;

    //Handler and connection
    private Handler mHandler;
    private ConnectedThread mConnectedThread;
    private BluetoothSocket mBTSocket = null;

    private ScanCallback mScanCallback;
    private BluetoothAdapter.LeScanCallback mLeScanCallback;
    private BluetoothLeScanner mLeScanner;

    private final static int REQUEST_EN_BT = 1;
    private final static int MSG_RD = 2;
    private final static int CNCTING_STAT = 3;

    private static final UUID BTmoduleUUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mBTstat = (TextView)findViewById(R.id.bluetoothStatus);
        mRB = (TextView)findViewById(R.id.readBuffer);
        mScan = (Button)findViewById(R.id.scan);
        mOff = (Button)findViewById(R.id.off);
        mDiscover = (Button)findViewById(R.id.discover);
        mPaired = (Button)findViewById(R.id.PairedBtn);

        mBTArrAdap = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1);
        mBTAdap = BluetoothAdapter.getDefaultAdapter();

        mDeviceListView = (ListView)findViewById(R.id.devicesListView);
        mDeviceListView.setAdapter(mBTArrAdap);
        mDeviceListView.setOnItemClickListener(mDeviceClickListener);

        if(Build.VERSION.SDK_INT >= 21){
            mScanCallback = new ScanCallback() {
                @Override
                public void onScanResult(int callbackType, ScanResult result) {
                    super.onScanResult(callbackType, result);
                    mBTArrAdap.add(result.getDevice().getAddress());
                }
            };
        }
        else{
            mLeScanCallback = new BluetoothAdapter.LeScanCallback() {
                @Override
                public void onLeScan(BluetoothDevice device, int rssi, byte[] scanRecord) {
                    mBTArrAdap.add(device.getAddress());
                }
            };
        }


        mHandler = new Handler() {

            public void msgHandle(android.os.Message message) {
                if (message.what == MSG_RD) {
                    String rdMsg = null;
                    try {
                        rdMsg = new String((byte[]) message.obj, "UTF-8");
                    } catch (UnsupportedEncodingException e) {
                        e.printStackTrace();
                    }
                    mRB.setText(rdMsg);
                }
                if (message.what == CNCTING_STAT) {
                    if (message.arg1 == 1) {
                        mBTstat.setText("Connected to Device: " + (String) (message.obj));
                    } else {
                        mBTstat.setText("Connection Failed");
                    }
                }
            }
        };

        if(mBTArrAdap == null){
            mBTstat.setText("Bluetooth not available");
        }
        else{
            mScan.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    bluetoothOn(v);
                }
            });

            mOff.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    bluetoothOff(v);
                }
            });

            mPaired.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    listPaired(v);
                }
            });

            mDiscover.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    discover(v);
                }
            });
        }
    }

    private void bluetoothOn(View v){
        if(!mBTAdap.isEnabled()){
            Intent BTen = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(BTen, REQUEST_EN_BT);
            mBTstat.setText("Bluetooth Enabled");
        }
        else{
            mBTstat.setText("Bluetooth is already ON");
        }
    }

    @Override
    protected void onActivityResult(int request_code, int result_code, Intent D){
        if(request_code == REQUEST_EN_BT){
            if(request_code == RESULT_OK){
                mBTstat.setText("Enabled");
            }
            else{
                mBTstat.setText("Disabled");
            }
        }
    }

    private void bluetoothOff(View v){
        mBTAdap.disable();
        mBTstat.setText("Bluetooth Turned Off");
    }

    private void discover(View v){
        if(mBTAdap.isDiscovering()){
            mBTAdap.cancelDiscovery();
        }
        else{
            if(mBTAdap.isEnabled()){
                mBTArrAdap.clear();
                startScanDevice();
                registerReceiver(blReceiver, new IntentFilter(BluetoothDevice.ACTION_FOUND));
            }
            else{
                mBTstat.setText("Bluetooth is not turned on");
            }
        }
    }

    public void startScanDevice(){
        if(Build.VERSION.SDK_INT < 21) {
            mBTAdap.startLeScan(mLeScanCallback);
        }
        else{
            if(mLeScanner == null){
                mLeScanner = mBTAdap.getBluetoothLeScanner();
                mLeScanner.startScan(new ArrayList<ScanFilter>(), new ScanSettings.Builder().setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY).build(), mScanCallback);
            }
        }
    }

    final BroadcastReceiver blReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if(BluetoothDevice.ACTION_FOUND.equals(action)){
                BluetoothDevice BTdevice = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                mBTArrAdap.add(BTdevice.getName() + "\n" + BTdevice.getAddress());
                mBTArrAdap.notifyDataSetChanged();
            }
        }
    };

    private void listPaired(View v){
        mPairedDevices = mBTAdap.getBondedDevices();
        if(mBTAdap.isEnabled()){
            for (BluetoothDevice BTdevice : mPairedDevices){
                mBTArrAdap.add(BTdevice.getName() + "\n" + BTdevice.getAddress());
            }
        }
        else{
            Toast.makeText(this, "Bluetooth not on", Toast.LENGTH_SHORT).show();
        }
    }

    private AdapterView.OnItemClickListener mDeviceClickListener = new AdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            if(!mBTAdap.isEnabled()){
                Toast.makeText(MainActivity.this, "Bluetooth not on", Toast.LENGTH_SHORT).show();
                return;
            }

            mBTstat.setText("Connecting...");

            String info = ((TextView) view).getText().toString();
            final String addr = info.substring(info.length() - 17);
            final String name = info.substring(0, info.length() - 17);

            new Thread(){
                public void run() {
                    boolean fail = false;
                    BluetoothDevice BTdevice = mBTAdap.getRemoteDevice(addr);

                    try{
                        mBTSocket = createBluetoothSocket(BTdevice);
                    }
                    catch (IOException e){
                        fail = true;
                        Toast.makeText(MainActivity.this, "Failed creating socket", Toast.LENGTH_SHORT).show();
                    }

                    try{
                        mBTSocket.connect();
                    }
                    catch (IOException e){
                        try {
                            fail = true;
                            mBTSocket.close();
                            mHandler.obtainMessage(CNCTING_STAT, -1, -1).sendToTarget();
                        }
                        catch(IOException e2){
                            Toast.makeText(MainActivity.this, "Failed creating socket", Toast.LENGTH_SHORT).show();
                        }
                    }

                    if(fail == false) {
                        mConnectedThread = new ConnectedThread(mBTSocket);
                        mConnectedThread.start();

                        mHandler.obtainMessage(CNCTING_STAT, 1, -1, name).sendToTarget();
                    }
                }
            }.start();
        }
    };

    private BluetoothSocket createBluetoothSocket(BluetoothDevice BTdevice) throws IOException{
        return BTdevice.createRfcommSocketToServiceRecord(BTmoduleUUID);
    }

    private class ConnectedThread extends Thread{

        private final BluetoothSocket mmSocket;
        private final InputStream mmIn;
        private final OutputStream mmOut;

        public ConnectedThread(BluetoothSocket socket){
            mmSocket = socket;
            InputStream tempIN = null;
            OutputStream tempOUT = null;

            try {
                tempIN = socket.getInputStream();
                tempOUT = socket.getOutputStream();
            }
            catch (IOException e){

            }
            mmIn = tempIN;
            mmOut = tempOUT;
        }

        public void run(){
            byte[] buffer = new byte[1024];
            int bytes;
            while(true){
                try{
                    bytes = mmIn.available();
                    if(bytes != 0){
                        SystemClock.sleep(100);
                        bytes = mmIn.available();
                        bytes = mmIn.read(buffer, 0, bytes);
                        mHandler.obtainMessage(MSG_RD, bytes, -1, buffer).sendToTarget();
                    }
                }
                catch(IOException e){
                    e.printStackTrace();

                    break;
                }
            }
        }

        public void write(String input){
            byte[] bytes = input.getBytes();
            try{
                mmOut.write(bytes);
            }
            catch(IOException e){

            }
        }

        public void cancel(){
            try{
                mmSocket.close();
            }
            catch(IOException e){

            }
        }
    }
}*/

import android.Manifest;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
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



import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
            displayGattServices(bluetoothGatt.getServices());
        }

        @Override
        // Result of a characteristic read operation
        public void onCharacteristicRead(BluetoothGatt gatt,
                                         BluetoothGattCharacteristic characteristic,
                                         int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                broadcastUpdate(ACTION_DATA_AVAILABLE, characteristic);
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

                final String charUuid = gattCharacteristic.getUuid().toString();
                System.out.println("Characteristic discovered for service: " + charUuid);
                MainActivity.this.runOnUiThread(new Runnable() {
                    public void run() {
                        peripheralTextView.append("Characteristic discovered for service: "+charUuid+"\n");
                    }
                });

            }
        }
    }

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