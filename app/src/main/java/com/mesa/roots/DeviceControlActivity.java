/*
 * Copyright (C) 2013 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.mesa.roots;

import android.app.Activity;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.ImageButton;
import android.widget.Switch;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

import android.widget.SeekBar;
import android.widget.Toast;

/**
 * For a given BLE device, this Activity provides the user interface to connect, display data,
 * and display GATT services and characteristics supported by the device.  The Activity
 * communicates with {@code BluetoothLeService}, which in turn interacts with the
 * Bluetooth LE API.
 */
public class DeviceControlActivity extends Activity {
    private final static String TAG = DeviceControlActivity.class.getSimpleName();

    public static final String EXTRAS_DEVICE_NAME = "DEVICE_NAME";
    public static final String EXTRAS_DEVICE_ADDRESS = "DEVICE_ADDRESS";
    private int[] RGBFrame = {0,0,0};
    private int dimmer_value = 0;
    //private TextView isSerial;
    private TextView mConnectionState;
    private TextView mDataField;
    private Switch mOnOffswitch;
    private Switch mDLASwitch;
    private Switch mFadeInOutSwitch;
    private ImageButton up_imageButton;
    private ImageButton down_imageButton;
    private SeekBar dimmer_seekBar;
    private String mDeviceName;
    private String mDeviceAddress;
  //  private ExpandableListView mGattServicesList;
    private BluetoothLeService mBluetoothLeService;
     private boolean mConnected = false;
    private BluetoothGattCharacteristic characteristicTX;
    private BluetoothGattCharacteristic characteristicRX;

    private String mModuleName;
    private int mStartUpSeq = 1;
    private String mCurReceivedData;
    private String mLastSentData;

    public final static UUID HM_RX_TX =
            UUID.fromString(SampleGattAttributes.HM_RX_TX);

    private final String LIST_NAME = "NAME";
    private final String LIST_UUID = "UUID";

    // Code to manage Service lifecycle.
    private final ServiceConnection mServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service) {
            mBluetoothLeService = ((BluetoothLeService.LocalBinder) service).getService();
            if (!mBluetoothLeService.initialize()) {
                Log.e(TAG, "Unable to initialize Bluetooth");
                finish();
            }
            // Automatically connects to the device upon successful start-up initialization.
            mBluetoothLeService.connect(mDeviceAddress);
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            mBluetoothLeService = null;
        }
    };

    // Handles various events fired by the Service.
    // ACTION_GATT_CONNECTED: connected to a GATT server.
    // ACTION_GATT_DISCONNECTED: disconnected from a GATT server.
    // ACTION_GATT_SERVICES_DISCOVERED: discovered GATT services.
    // ACTION_DATA_AVAILABLE: received data from the device.  This can be a result of read
    //                        or notification operations.
    private final BroadcastReceiver mGattUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (BluetoothLeService.ACTION_GATT_CONNECTED.equals(action)) {
                mConnected = true;
                mOnOffswitch.setEnabled(true);

                updateConnectionState(R.string.connected);
                invalidateOptionsMenu();
            }
            else if (BluetoothLeService.ACTION_GATT_DISCONNECTED.equals(action)) {
                mConnected = false;
                mOnOffswitch.setChecked(false);
                mDLASwitch.setChecked(false);
                mFadeInOutSwitch.setChecked(false);
                dimmer_seekBar.setProgress(0);
                dimmer_value = 0;

                mOnOffswitch.setEnabled(false);
                dimmer_seekBar.setEnabled(false);
                up_imageButton.setEnabled(false);
                down_imageButton.setEnabled(false);
                mDLASwitch.setEnabled(false);
                mFadeInOutSwitch.setEnabled(false);
                up_imageButton.setEnabled(false);
                down_imageButton.setEnabled(false);

                updateConnectionState(R.string.disconnected);
                invalidateOptionsMenu();
                clearUI();
            } else if (BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED.equals(action)) {
                // Show all the supported services and characteristics on the user interface.
                displayGattServices(mBluetoothLeService.getSupportedGattServices());
            } else if (BluetoothLeService.ACTION_DATA_AVAILABLE.equals(action)) {
                displayData(intent.getStringExtra(mBluetoothLeService.EXTRA_DATA));
            }
        }
    };

    private void clearUI() {
        mDataField.setText(R.string.no_data);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.gatt_services_characteristics);

        final Intent intent = getIntent();
        mDeviceName = intent.getStringExtra(EXTRAS_DEVICE_NAME);
        mDeviceAddress = intent.getStringExtra(EXTRAS_DEVICE_ADDRESS);

        // Sets up UI references.
        //((TextView) findViewById(R.id.device_address)).setText(mDeviceAddress);
         mConnectionState = (TextView) findViewById(R.id.connection_state);
        // is serial present?
        //isSerial = (TextView) findViewById(R.id.isSerial);
   
        mDataField = (TextView) findViewById(R.id.data_value);
        dimmer_seekBar = (SeekBar) findViewById(R.id.dimmer_seekBar);
        //mGreen = (SeekBar) findViewById(R.id.dimmer_seekBar);
        //mBlue = (SeekBar) findViewById(R.id.dimmer_seekBar);

        mOnOffswitch = (Switch) findViewById(R.id.on_off_switch);
        mDLASwitch = (Switch) findViewById(R.id.dla_switch);
        mFadeInOutSwitch = (Switch) findViewById(R.id.fadeinout_switch);

        up_imageButton = (ImageButton) findViewById(R.id.up_imageButton);
        down_imageButton = (ImageButton) findViewById(R.id.down_imageButton);

        //readSeek(dimmer_seekBar,0);
        //readSeek(mGreen,1);
        //readSeek(mBlue,2);

        dimmer_seekBar.setMax(100);

        getActionBar().setTitle(mDeviceName);
        getActionBar().setDisplayHomeAsUpEnabled(true);
        Intent gattServiceIntent = new Intent(this, BluetoothLeService.class);
        bindService(gattServiceIntent, mServiceConnection, BIND_AUTO_CREATE);

        dimmer_seekBar.setEnabled(false);
        up_imageButton.setEnabled(false);
        down_imageButton.setEnabled(false);
        mDLASwitch.setEnabled(false);
        mFadeInOutSwitch.setEnabled(false);
        up_imageButton.setEnabled(false);
        down_imageButton.setEnabled(false);

    dimmer_seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
        @Override
        public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
            if (fromUser==true)
            {
                dimmer_value = progress;

            }
        }

        @Override
        public void onStartTrackingTouch(SeekBar seekBar) {

        }

        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {
            SendDimmerMode();
        }
    });

    mOnOffswitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener()
    {
        @Override
        public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        if (isChecked) {
            mStartUpSeq = 1;

            dimmer_seekBar.setEnabled(true);
            up_imageButton.setEnabled(true);
            down_imageButton.setEnabled(true);
            mDLASwitch.setEnabled(true);
            mFadeInOutSwitch.setEnabled(true);
            up_imageButton.setEnabled(true);
            down_imageButton.setEnabled(true);

            SendOnOff(true);
        }
        else {

            mDLASwitch.setChecked(false);
            mFadeInOutSwitch.setChecked(false);
            dimmer_seekBar.setProgress(0);
            dimmer_value = 0;

            dimmer_seekBar.setEnabled(false);
            up_imageButton.setEnabled(false);
            down_imageButton.setEnabled(false);
            mDLASwitch.setEnabled(false);
            mFadeInOutSwitch.setEnabled(false);
            up_imageButton.setEnabled(false);
            down_imageButton.setEnabled(false);

            mStartUpSeq = 0;
            SendOnOff(false);
        }
    }
    });

    mFadeInOutSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener()
    {
        @Override
        public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
            if (isChecked)
                SendFadeInFadeOut(true);
            else
                SendFadeInFadeOut(false);
        }
    });

    mDLASwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener()
    {
        @Override
        public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        if (isChecked)
        {
            dimmer_seekBar.setEnabled(false);
            up_imageButton.setEnabled(false);
            down_imageButton.setEnabled(false);

            // DLA ON
            SendDLAMode(true);

        }
        else
        {
            dimmer_seekBar.setEnabled(true);
            up_imageButton.setEnabled(true);
            down_imageButton.setEnabled(true);

            // DLA OFF
            SendDLAMode(false);
        }

    }
    });

        up_imageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v)
            {
                if(dimmer_value > 99)
                {
                    dimmer_value = 100;
                }
                else
                {
                    dimmer_value = dimmer_value + 1;

                }

                dimmer_seekBar.setProgress(dimmer_value);

                SendDimmerMode();
            }
        });

        down_imageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v)
            {
                if(dimmer_value < 1 )
                {
                    dimmer_value = 0;
                }
                else
                {
                    dimmer_value = dimmer_value - 1;
                }

                dimmer_seekBar.setProgress(dimmer_value);

                SendDimmerMode();
            }
        });

    }

    @Override
    protected void onResume() {
        super.onResume();
        registerReceiver(mGattUpdateReceiver, makeGattUpdateIntentFilter());
        if (mBluetoothLeService != null) {
            final boolean result = mBluetoothLeService.connect(mDeviceAddress);
            Log.d(TAG, "Connect request result=" + result);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(mGattUpdateReceiver);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unbindService(mServiceConnection);
        mBluetoothLeService = null;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.gatt_services, menu);
        if (mConnected) {
            menu.findItem(R.id.menu_connect).setVisible(false);
            menu.findItem(R.id.menu_disconnect).setVisible(true);
        } else {
            menu.findItem(R.id.menu_connect).setVisible(true);
            menu.findItem(R.id.menu_disconnect).setVisible(false);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()) {
            case R.id.menu_connect:
                mBluetoothLeService.connect(mDeviceAddress);
                return true;
            case R.id.menu_disconnect:
                mBluetoothLeService.disconnect();
                return true;
            case android.R.id.home:
                onBackPressed();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void updateConnectionState(final int resourceId) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mConnectionState.setText(resourceId);
            }
        });
    }

    private void displayData(String data) {

        if (data != null) {
            mDataField.setText("\n"+ data);

            if(data.startsWith("R") && data.endsWith("Q"))
            {
                if (mStartUpSeq == 1)
                {
                    mModuleName = data.substring(1,data.length()-1);

                    if (mModuleName.equals("MESUT"))
                    {
                        //SendAckState(true);

                        SendModuleNameCheck(mModuleName);

                        mLastSentData = mModuleName;

                        //Toast.makeText(this, "Module Name OK", Toast.LENGTH_SHORT).show();
                        Log.d(TAG, "Module Name Test Sent= " + data);
                    }
                    else
                    {
                        //SendAckState(false);
                        //Toast.makeText(this, "Module Name NOT OK"+data, Toast.LENGTH_SHORT).show();
                        Log.d(TAG, "Module Name Test not Sent= " + data);
                    }

                    mStartUpSeq = 0;
                }
                else
                {
                    mCurReceivedData = data.substring(1,data.length()-1);

                    if (mCurReceivedData.equals(mLastSentData))
                    {
                        SendAckState(true);
                        //Toast.makeText(this, "Last Pack OK", Toast.LENGTH_SHORT).show();
                        Log.d(TAG, "Last Pack OK= " + data);
                    }
                    else
                    {
                        SendAckState(false);
                        //Toast.makeText(this, "Last Pack NOT OK"+data, Toast.LENGTH_SHORT).show();
                        Log.d(TAG, "Last Pack NOT OK= " + data);
                    }

                }

            }
            else if (data.endsWith("Q"))
            {
                if (mLastSentData == mModuleName)
                {
                    if (data.startsWith("1"))
                    {
                        Log.d(TAG, "Module Name OK= " + data);
                        getActionBar().setTitle(mDeviceName+" : "+mModuleName);
                    }
                    else if(data.startsWith("0"))
                    {
                        Log.d(TAG, "Module Name FAIL= " + data);
                    }
                    else
                        Log.d(TAG, "Module Name CHECK FAIL= " + data);

                }
            }
            else
            {
                //Toast.makeText(this, "RX FAIL:"+data, Toast.LENGTH_SHORT).show();
                Log.d(TAG, "RX FAIL= " + data);
            }
        }
    }


    // Demonstrates how to iterate through the supported GATT Services/Characteristics.
    // In this sample, we populate the data structure that is bound to the ExpandableListView
    // on the UI.
    private void displayGattServices(List<BluetoothGattService> gattServices) {
        if (gattServices == null) return;
        String uuid = null;
        String unknownServiceString = getResources().getString(R.string.unknown_service);
        ArrayList<HashMap<String, String>> gattServiceData = new ArrayList<HashMap<String, String>>();

 
        // Loops through available GATT Services.
        for (BluetoothGattService gattService : gattServices) {
            HashMap<String, String> currentServiceData = new HashMap<String, String>();
            uuid = gattService.getUuid().toString();
            currentServiceData.put(
                    LIST_NAME, SampleGattAttributes.lookup(uuid, unknownServiceString));
            
            // If the service exists for HM 10 Serial, say so.
            if(SampleGattAttributes.lookup(uuid, unknownServiceString) == "HM 10 Serial")
            {
                //isSerial.setText("UUID OK");
                //Toast.makeText(this, "UUID OK", Toast.LENGTH_SHORT).show();
                Log.d(TAG, "UUID OK");
            }
            else
            {
                //isSerial.setText("UUID FAIL");
                //Toast.makeText(this, "UUID FAIL", Toast.LENGTH_SHORT).show();
                Log.d(TAG, "UUID FAIL");
            }
            currentServiceData.put(LIST_UUID, uuid);
            gattServiceData.add(currentServiceData);

     		// get characteristic when UUID matches RX/TX UUID
    		 characteristicTX = gattService.getCharacteristic(BluetoothLeService.UUID_HM_RX_TX);
    		 characteristicRX = gattService.getCharacteristic(BluetoothLeService.UUID_HM_RX_TX);
        }
        
    }

    private static IntentFilter makeGattUpdateIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_CONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_DISCONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED);
        intentFilter.addAction(BluetoothLeService.ACTION_DATA_AVAILABLE);
        return intentFilter;
    }

    // on change of bars write char 
    private void SendDLAMode(boolean dla_state) {
        int i = (dla_state==true) ? 1 : 0;
        String str = "P200" + i + "Q";
        String mCheck = str.substring(1,str.length()-1);;
        mLastSentData = mCheck;

        Log.d(TAG, "Sending result=" + str);
        final byte[] tx = str.getBytes();
        if(mConnected) {
            characteristicTX.setValue(tx);
            mBluetoothLeService.writeCharacteristic(characteristicTX);
            mBluetoothLeService.setCharacteristicNotification(characteristicRX,true);
        }
    }

    // on change of bars write char
    private void SendModuleNameCheck(String moduleName) {
        String str = "P" + moduleName + "Q";
        String mCheck = str.substring(1,str.length()-1);;
        mLastSentData = mCheck;

        Log.d(TAG, "Sending result=" + str);
        final byte[] tx = str.getBytes();
        if(mConnected) {
            characteristicTX.setValue(tx);
            mBluetoothLeService.writeCharacteristic(characteristicTX);
            mBluetoothLeService.setCharacteristicNotification(characteristicRX,true);
        }
    }

    // on change of bars write char
    private void SendFadeInFadeOut(boolean fade_in_out_state) {
        int i = (fade_in_out_state==true) ? 1 : 0;

        String str = "P100" + i + "Q";
        String mCheck = str.substring(1,str.length()-1);;
        mLastSentData = mCheck;

        Log.d(TAG, "Sending result=" + str);
        final byte[] tx = str.getBytes();
        if(mConnected) {
            characteristicTX.setValue(tx);
            mBluetoothLeService.writeCharacteristic(characteristicTX);
            mBluetoothLeService.setCharacteristicNotification(characteristicRX,true);
        }
    }

    // on change of bars write char
    private void SendOnOff(boolean on_off_state) {
        int i = (on_off_state==true) ? 1 : 0;

        String str = "P900" + i + "Q";
        String mCheck = str.substring(1,str.length()-1);;
        mLastSentData = mCheck;

        Log.d(TAG, "Sending result=" + str);
        final byte[] tx = str.getBytes();
        if(mConnected) {
            characteristicTX.setValue(tx);
            mBluetoothLeService.writeCharacteristic(characteristicTX);
            mBluetoothLeService.setCharacteristicNotification(characteristicRX,true);
        }
    }

    // on change of bars write char
    private void SendDimmerMode() {
        byte[] result = new byte[3];

        if(dimmer_value<=100) {
            result[2] = (byte) (((dimmer_value - 99)==1) ? 1:0);
            result[1] = (byte) (dimmer_value / 10);
            result[0] = (byte) (dimmer_value % 10/*>> 0*/);
        }
        else
        {
            result[2] = 1;
            result[1] = 0;
            result[0] = 0;
        }

        Log.d(TAG, "Dimmer_value: " + dimmer_value + " Dimmer_char: "+result[2]+" "+result[1]+" "+result[0]);
        String str = "P0" + result[2]+result[1]+result[0] + "Q";
        String mCheck = str.substring(1,str.length()-1);;
        mLastSentData = mCheck;
        Log.d(TAG, "Sending result=" + str);
        final byte[] tx = str.getBytes();
        if(mConnected) {
            characteristicTX.setValue(tx);
            mBluetoothLeService.writeCharacteristic(characteristicTX);
            mBluetoothLeService.setCharacteristicNotification(characteristicRX,true);
        }
    }

    // on change of bars write char
    private void SendAckState(boolean ack_state) {
        int i = (ack_state==true) ? 1 : 0;

        String str = i + "Q";
        Log.d(TAG, "Sending result=" + str);
        final byte[] tx = str.getBytes();
        if(mConnected) {
            characteristicTX.setValue(tx);
            mBluetoothLeService.writeCharacteristic(characteristicTX);
            mBluetoothLeService.setCharacteristicNotification(characteristicRX,true);
        }
    }


}