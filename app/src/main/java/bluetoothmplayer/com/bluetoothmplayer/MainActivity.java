package bluetoothmplayer.com.bluetoothmplayer;

import android.app.Activity;
import android.app.ListActivity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.view.Menu;
import android.view.MenuItem;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.UUID;

import android.app.ListActivity;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

public class MainActivity extends Activity {

    private ArrayList<String> results = new ArrayList<String>();
    private final static UUID uuid = UUID.fromString("fc5ffc49-00e3-4c8b-9cf1-6b72aad1001a");
    private String CurrentDeviceMacAddress = "";
    private HashMap<String, String> requestDetails = new HashMap<String, String>();
    private String tableName = DBHelper.tableName;
    private String requesttoSent = "";
    private BluetoothAdapter bluetoothAdapter;
    private ToggleButton toggleButton;
    private ArrayAdapter adapter;
    private static final int ENABLE_BT_REQUEST_CODE = 1;
    private static final int DISCOVERABLE_BT_REQUEST_CODE = 2;
    private static final int DISCOVERABLE_DURATION = 300;
    private SQLiteDatabase newDB;
    private String SongName = "";
    private String Owner = "";
    private String MacId = "";
    private String deviceMacId = "";
    private ListView listView;

    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        adapter = new ArrayAdapter(this, android.R.layout.simple_list_item_1);
        //listview.setAdapter(adapter);
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        startBluetooth();
        toggleButton = (ToggleButton) findViewById(R.id.toggleButton);
        CurrentDeviceMacAddress = BluetoothAdapter.getDefaultAdapter().getAddress();
        setDeviceMacId(CurrentDeviceMacAddress);
        Toast.makeText(getApplicationContext(), getDeviceMacId(), Toast.LENGTH_SHORT).show();
        openAndQueryDatabase();

    }

    public void onToggleClicked(View view) {
        ToggleButton toggleButton = (ToggleButton) view;
        if (toggleButton.isChecked()) {
            displayResultList();
            listView.setVisibility(View.VISIBLE);
        } else {
            listView.setVisibility(View.GONE);
        }

    }

    private void displayResultList() {
        listView = (ListView) findViewById((R.id.listview1));
        listView.setAdapter(new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, results));
        listView.setTextFilterEnabled(true);
        listView.setClickable(true);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Object o = listView.getItemAtPosition(position);
                String str = (String) o;//As you are using Default String Adapter
                Toast.makeText(getApplicationContext(), str, Toast.LENGTH_SHORT).show();
                String[] request = str.split(",");
                requestDetails.put("source", CurrentDeviceMacAddress);
                requestDetails.put("songname", request[0].split("=")[1]);
                requestDetails.put("destination", request[2].split("=")[1]);
                requesttoSent = requestDetails.get("source") + "|" + requestDetails.get("songname") + "|" + requestDetails.get("destination");
                Log.d("request = ", requesttoSent);
                String relay = openAndQueryOwnDeviceRelayDetails(requestDetails.get("destination"));
                Log.d("relay mac   ",relay);
                if (!relay.equals(requestDetails.get("destination"))) {
                    BluetoothDevice bluetoothDevice = bluetoothAdapter.getRemoteDevice(relay);
                    Log.d("relay mac   ",relay);
                    // Initiate a connection request in a separate thread
                    ConnectingThread t = new ConnectingThread(bluetoothDevice);
                    t.start();
                }
            }
        });


    }

    private void openAndQueryDatabase() {
        try {
            DBHelper dbHelper = new DBHelper(this.getApplicationContext());
            newDB = dbHelper.getWritableDatabase();
            Cursor c = newDB.rawQuery("SELECT SongName,Owner,MacId FROM " + tableName, null);
            if (c != null) {
                if (c.moveToFirst()) {
                    do {
                        SongName = c.getString(c.getColumnIndex("SongName"));
                        Owner = c.getString(c.getColumnIndex("Owner"));
                        MacId = c.getString(c.getColumnIndex("MacId"));
                        results.add("SongName=" + SongName + ",Owner=" + Owner + ",MacId=" + MacId);
                    } while (c.moveToNext());
                }
            }
        } catch (SQLiteException se) {
            Log.e(getClass().getSimpleName(), "Could not create or Open the database");
        } finally {
            if (newDB != null)
                newDB.execSQL("DELETE FROM " + tableName);
            newDB.close();
        }

    }

    private String openAndQueryOwnDeviceRelayDetails(String DestinationMacId) {
        String RelayMacId = "";
        Log.d("destination = ",DestinationMacId);
        try {
            DBHelper dbHelper = new DBHelper(this.getApplicationContext());
            newDB = dbHelper.getWritableDatabase();
            Cursor b = newDB.rawQuery("SELECT relay FROM RelayDetails where destination = '" + DestinationMacId+"'", null);
            if (b != null) {
                if (b.moveToFirst()) {
                    do {
                        RelayMacId = b.getString(b.getColumnIndex("relay"));
                    } while (b.moveToNext());
                }
            }
        } catch (SQLiteException se) {
            Log.e(getClass().getSimpleName(), "Could not create or Open the database");
        } finally {
            if (newDB != null)
                newDB.execSQL("DELETE FROM " + tableName);
            newDB.close();
        }
        return RelayMacId;
    }


    private void startBluetooth() {
        adapter.clear();
        if (bluetoothAdapter == null) {
            // Device does not support Bluetooth
            Toast.makeText(getApplicationContext(), "Oop! Your device does not support Bluetooth",
                    Toast.LENGTH_SHORT).show();
        } else {

            if (!bluetoothAdapter.isEnabled()) {
                // A dialog will appear requesting user permission to enable Bluetooth
                Intent enableBluetoothIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableBluetoothIntent, ENABLE_BT_REQUEST_CODE);
            } else {
                Toast.makeText(getApplicationContext(), "Your device has already been enabled." +
                                "\n" + "Scanning for remote Bluetooth devices...",
                        Toast.LENGTH_SHORT).show();
                // To discover remote Bluetooth devices
                //discoverDevices();
                // Make local device discoverable by other devices
                makeDiscoverable();
            }
        }
    }

    protected void makeDiscoverable() {
        // Make local device discoverable
        Intent discoverableIntent = new
                Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
        discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, DISCOVERABLE_DURATION);
        startActivityForResult(discoverableIntent, DISCOVERABLE_BT_REQUEST_CODE);
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {

        if (requestCode == ENABLE_BT_REQUEST_CODE) {

            // Bluetooth successfully enabled!
            if (resultCode == Activity.RESULT_OK) {
                Toast.makeText(getApplicationContext(), "Ha! Bluetooth is now enabled." +
                                "\n" + "Scanning for remote Bluetooth devices...",
                        Toast.LENGTH_SHORT).show();

                // To discover remote Bluetooth devices
               // discoverDevices();

                // Make local device discoverable by other devices
                makeDiscoverable();

                // Start a thread to create a  server socket to listen
                // for connection request
                ListeningThread t = new ListeningThread();
                t.start();

            } else { // RESULT_CANCELED as user refused or failed to enable Bluetooth
                Toast.makeText(getApplicationContext(), "Bluetooth is not enabled.",
                        Toast.LENGTH_SHORT).show();

                // Turn off togglebutton
                //            toggleButton.setChecked(false);
            }
        } else if (requestCode == DISCOVERABLE_BT_REQUEST_CODE) {

            if (resultCode == DISCOVERABLE_DURATION) {
                Toast.makeText(getApplicationContext(), "Your device is now discoverable by other devices for " +
                                DISCOVERABLE_DURATION + " seconds",
                        Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(getApplicationContext(), "Fail to enable discoverability on your device.",
                        Toast.LENGTH_SHORT).show();
            }
        }
    }

    private class ConnectingThread extends Thread {
        private final BluetoothSocket bluetoothSocket;
        private final BluetoothDevice bluetoothDevice;

        public ConnectingThread(BluetoothDevice device) {

            BluetoothSocket temp = null;
            bluetoothDevice = device;

            // Get a BluetoothSocket to connect with the given BluetoothDevice
            try {
                temp = bluetoothDevice.createRfcommSocketToServiceRecord(uuid);
            } catch (IOException e) {
                e.printStackTrace();
            }
            bluetoothSocket = temp;
        }

        public void run() {
            // Cancel discovery as it will slow down the connection
            //bluetoothAdapter.cancelDiscovery();

            try {
                // This will block until it succeeds in connecting to the device
                // through the bluetoothSocket or throws an exception
                Log.d("YOUR_APP_LOG_TAG1", "I got an error");
                bluetoothSocket.connect();
                Log.d("YOUR_APP_LOG_TAG1", "I got an error");
            } catch (IOException connectException) {
                connectException.printStackTrace();
                try {
                    bluetoothSocket.close();
                } catch (IOException closeException) {
                    closeException.printStackTrace();
                }
            }

            // Code to manage the connection in a separate thread
            /*
               manageBluetoothConnection(bluetoothSocket);
            */
        }

        // Cancel an open connection and terminate the thread
        public void cancel() {
            try {
                bluetoothSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private class ListeningThread extends Thread {
        private final BluetoothServerSocket bluetoothServerSocket;

        public ListeningThread() {
            BluetoothServerSocket temp = null;
            try {
                temp = bluetoothAdapter.listenUsingRfcommWithServiceRecord(getString(R.string.app_name), uuid);


            } catch (IOException e) {
                e.printStackTrace();
            }
            bluetoothServerSocket = temp;
        }

        public void run() {
            BluetoothSocket bluetoothSocket;
            // This will block while listening until a BluetoothSocket is returned
            // or an exception occurs
            while (true) {
                try {
                    Log.d("YOUR_APP_LOG_TAG1", "done1122");
                    bluetoothSocket = bluetoothServerSocket.accept();
                    Log.d("YOUR_APP_LOG_TAG1", "done11");
                } catch (IOException e) {
                    break;
                }
                // If a connection is accepted
                if (bluetoothSocket != null) {
                    Log.d("YOUR_APP_LOG_TAG1", "done11");
                    runOnUiThread(new Runnable() {
                        public void run() {
                            Toast.makeText(getApplicationContext(), "A connection has been accepted.",
                                    Toast.LENGTH_SHORT).show();
                        }
                    });

                    // Code to manage the connection in a separate thread
                   /*
                       manageBluetoothConnection(bluetoothSocket);
                   */

                    try {
                        bluetoothServerSocket.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    break;
                }
            }
        }

        // Cancel the listening socket and terminate the thread
        public void cancel() {
            try {
                bluetoothServerSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }


    public void setDeviceMacId(String deviceMacId1) {
        deviceMacId = deviceMacId1;
    }

    public String getDeviceMacId() {
        return deviceMacId;
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
