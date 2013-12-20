/*
 * Copyright (C) 2009 The Android Open Source Project
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

package BluetoothChat.cc;

import java.util.Set;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.AdapterView.OnItemClickListener;

/**
 * This Activity appears as a dialog. It lists any paired devices and
 * devices detected in the area after discovery. When a device is chosen
 * by the user, the MAC address of the device is sent back to the parent
 * Activity in the result Intent.
 */
public class DeviceListActivity extends Activity {
    // Debugging
    private static final String TAG = "DeviceListActivity";
    private static final boolean D = true;

    // Return Intent extra
    public static String EXTRA_DEVICE_ADDRESS = "device_address";//�Ū޿�t��

    // Member fields
    private BluetoothAdapter mBtAdapter;						//�w�g�t��L���Ū޳]��
    private ArrayAdapter<String> mPairedDevicesArrayAdapter;	//�s���Ū޳]��
    private ArrayAdapter<String> mNewDevicesArrayAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Setup the window(�]�m���f�A�ݭn�@�Ӷi�ױ��A���y�ɥΪ�)
        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
        setContentView(R.layout.device_list);

        // Set result CANCELED incase the user backs out
        setResult(Activity.RESULT_CANCELED);

        // Initialize the button to perform device discovery
        Button scanButton = (Button) findViewById(R.id.button_scan);
        scanButton.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                doDiscovery();	//���y
                v.setVisibility(View.GONE);
            }
        });

        // Initialize array adapters. One for already paired devices and
        // one for newly discovered devices
        //��l��ArrayAdapter�A�@�ӬO�w�t�諸�]�ơA�@�ӬO�s�o�{���]��
        mPairedDevicesArrayAdapter = new ArrayAdapter<String>(this, R.layout.device_name);
        mNewDevicesArrayAdapter = new ArrayAdapter<String>(this, R.layout.device_name);

        // Find and set up the ListView for paired devices
        //�˴��ó]�m�w�t�諸�]��ListView
        ListView pairedListView = (ListView) findViewById(R.id.paired_devices);
        pairedListView.setAdapter(mPairedDevicesArrayAdapter);
        pairedListView.setOnItemClickListener(mDeviceClickListener);

        // Find and set up the ListView for newly discovered devices
        //�˴��ó]�m�o�{���]��ListView
        ListView newDevicesListView = (ListView) findViewById(R.id.new_devices);
        newDevicesListView.setAdapter(mNewDevicesArrayAdapter);
        newDevicesListView.setOnItemClickListener(mDeviceClickListener);

        // Register for broadcasts when a device is discovered
        //��@�ӳ]�ƳQ�o�{�ɡA�ݭn���U�@�Ӽs��
        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        this.registerReceiver(mReceiver, filter);

        // Register for broadcasts when discovery has finished
        //������ˬd�����ɡA�ݭn���U�@�Ӽs��
        filter = new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        this.registerReceiver(mReceiver, filter);

        // Get the local Bluetooth adapter
        //�o�쥻�a���Ū޿�t��
        mBtAdapter = BluetoothAdapter.getDefaultAdapter();

        // Get a set of currently paired devices
        //��getBondedDevices()���o�w�t�諸�Ū޳]��
        Set<BluetoothDevice> pairedDevices = mBtAdapter.getBondedDevices();

        // If there are paired devices, add each one to the ArrayAdapter
        //�p�G���t�令�\�A�]�ƲK�[��ArrayAdapter
        if (pairedDevices.size() > 0) {
            findViewById(R.id.title_paired_devices).setVisibility(View.VISIBLE);
            for (BluetoothDevice device : pairedDevices) {
                mPairedDevicesArrayAdapter.add(device.getName() + "\n" + device.getAddress());
            }
        //�_�h�K�[�@�ӨS���Q�t�諸�r��    
        } else {
            String noDevices = getResources().getText(R.string.none_paired).toString();
            mPairedDevicesArrayAdapter.add(noDevices);
        }
    }

    @Override
    //�P���ާ@
    protected void onDestroy() {
        super.onDestroy();

        // Make sure we're not doing discovery anymore
        //�T�O�S���o�{�A�ˬd�]��
        if (mBtAdapter != null) {
            mBtAdapter.cancelDiscovery();
        }

        // Unregister broadcast listeners
        //�����ҵ��U���s��
        this.unregisterReceiver(mReceiver);
    }

    /**
     * Start device discover with the BluetoothAdapter
     */
    //���y��Q�o�{���]��
    private void doDiscovery() {
    	
        if (D) Log.d(TAG, "doDiscovery()");

        // Indicate scanning in the title
        //��ܶi�ױ��A�]�mTitle�����y���A
        setProgressBarIndeterminateVisibility(true);
        setTitle(R.string.scanning);

        // Turn on sub-title for new devices
        //�]�m�s�]�ƪ��l���D
        findViewById(R.id.title_new_devices).setVisibility(View.VISIBLE);

        // If we're already discovering, stop it
        //�Y�w�g�b���ˤF�A��������
        if (mBtAdapter.isDiscovering()) {
            mBtAdapter.cancelDiscovery();
        }

        // Request discover from BluetoothAdapter
        //�ШD�q�Ū޿�t���o���Q�o�{���]��
        mBtAdapter.startDiscovery();
    }

    //The on-click listener for all devices in the ListViews
    //ListView���Ҧ��]�ƪ��I���ƥ��ť
    private OnItemClickListener mDeviceClickListener = new OnItemClickListener() {
        public void onItemClick(AdapterView<?> av, View v, int arg2, long arg3) {
            // Cancel discovery because it's costly and we're about to connect
            //�����˴����y�]�ƪ��L�{�A�]���D�`�Ӹ귽
        	mBtAdapter.cancelDiscovery();

            // Get the device MAC address, which is the last 17 chars in the View
            //�o��Mac��m
        	String info = ((TextView) v).getText().toString();
            String address = info.substring(info.length() - 17);
            
            v.getDrawableState();
            
            // Create the result Intent and include the MAC address
            //�ؤ@�Ӧ�Mac��m��Intent
            Intent intent = new Intent();
            intent.putExtra(EXTRA_DEVICE_ADDRESS, address);

            // Set result and finish this Activity
            //�]�mresult�A�õ���Activity
            setResult(Activity.RESULT_OK, intent);
            finish();
        }
    };

    // The BroadcastReceiver that listens for discovered devices and
    // changes the title when discovery is finished
    //��ť���y�Ū޳]��
    //
    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            // When discovery finds a device
            //��o�{�@�ӳ]�Ʈ�
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                // Get the BluetoothDevice object from the Intent
            	//�qIntent�o���Ū޳]�ƹ�H
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                
                // If it's already paired, skip it, because it's been listed already
                //�p�G�t��L�h���L�A�]�����{�b�w�g�b�]�ƦC���
                if (device.getBondState() != BluetoothDevice.BOND_BONDED) {
                	//�K�[��]�ƦC��
                    mNewDevicesArrayAdapter.add(device.getName() + "\n" + device.getAddress());
                }
            // When discovery is finished, change the Activity title
            //���y��������A����Avtivity��Title
            } else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
                //�]�m�i�ױ�������ܡA�]�mTitle
            	setProgressBarIndeterminateVisibility(false);         	
                setTitle(R.string.select_device);
                
                //�Y�p�ƾ���0�A�h��ܨS�o�{�Ū�
                if (mNewDevicesArrayAdapter.getCount() == 0) {
                    String noDevices = getResources().getText(R.string.none_found).toString();
                    mNewDevicesArrayAdapter.add(noDevices);
                }
            }
        }
    };

}
