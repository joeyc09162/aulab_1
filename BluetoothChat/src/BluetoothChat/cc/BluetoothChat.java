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


import java.util.StringTokenizer;

import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.View.OnClickListener;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

/**
 * This is the main Activity that displays the current chat session.
 */
public class BluetoothChat extends Activity {
    // Debugging
    private static final String TAG = "BluetoothChat";
    private static final boolean D = true;	
    
    // Message types sent from the BluetoothChatService Handler
    //嚙緬BluetoothChatService Handler嚙緻嚙箴嚙踝蕭嚙踝蕭嚙踝蕭嚙踝蕭
    public static final int MESSAGE_STATE_CHANGE = 1;
    public static final int MESSAGE_READ = 2;
    public static final int MESSAGE_WRITE = 3;
    public static final int MESSAGE_DEVICE_NAME = 4;
    public static final int MESSAGE_TOAST = 5;

    // Key names received from the BluetoothChatService Handler
    public static final String DEVICE_NAME = "device_name";
    public static final String TOAST = "toast";

    // Intent request codes
    //Intent嚙請求嚙瞇嚙碼
    private static final int REQUEST_CONNECT_DEVICE = 1;
    private static final int REQUEST_ENABLE_BT = 2;	//嚙請求嚙踝蕭嚙罷嚙褐芽迎蕭Intent嚙瞇嚙碼



   
    // Name of the connected device
    //嚙編嚙踝蕭嚙踝蕭嚙稽嚙複名嚙踝蕭
    private String mConnectedDeviceName = null;
    
    // Array adapter for the conversation thread
    private ArrayAdapter<String> mConversationArrayAdapter;
    
    // String buffer for outgoing messages
    //嚙瞇嚙緯嚙緻嚙箴嚙碼嚙篁嚙踝蕭嚙緝嚙踝蕭
    private static StringBuffer mOutStringBuffer;
    
    // Local Bluetooth adapter
    //嚙踝蕭嚙窮嚙褐芽選蕭t嚙踝蕭
    private BluetoothAdapter mBluetoothAdapter = null;
    
    // Member object for the chat services
    //嚙踝蕭悛A嚙褓迎蕭嚙踝蕭H
    private static BluetoothChatService mChatService = null;

    public static int candraw=1;
    public static int hasball=1;
    public static int isRight=1;
    public static int ball_x=0;
    public static int ball_y=0;
    public static int isconnect=0;    
    public static int issend=0;    
    public static int dir=0;
    
    protected static final int GUIUPDATEIDENTIFIER = 0x101;
    Thread myRefreshThread = null;
    BounceView myBounceView =  null;
    Handler myGUIUpdateHandler = new Handler(){
    	public void handleMessage(Message msg){
    		switch(msg.what){
    		case BluetoothChat.GUIUPDATEIDENTIFIER:
    			if(candraw==1){
    				myBounceView.invalidate();
    			}
    			break;
    		}
    		super.handleMessage(msg);
    	}
    };
    
    class RefreshRunner implements Runnable{
    	public void run(){
    		while(!Thread.currentThread().isInterrupted()){
    			Message message = new Message();
    			message.what = BluetoothChat.GUIUPDATEIDENTIFIER;
    			BluetoothChat.this.myGUIUpdateHandler.sendMessage(message);
    			try{
    				Thread.sleep(1);
    			}catch(InterruptedException e){
    				Thread.currentThread();
    			}
    		}
    	}
    }
    
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,  WindowManager.LayoutParams.FLAG_FULLSCREEN);
        if(D) Log.e(TAG, "+++ ON CREATE +++");
        
        /*
       // Set up the window layout
        //嚙稽嚙練嚙踝蕭嚙篆嚙瘦嚙踝蕭
        requestWindowFeature(Window.FEATURE_CUSTOM_TITLE);
        setContentView(R.layout.main);
        getWindow().setFeatureInt(Window.FEATURE_CUSTOM_TITLE, R.layout.custom_title);
    */  
        
        this.myBounceView = new BounceView(this);
        this.setContentView(this.myBounceView);
        new Thread(new RefreshRunner()).start();
        Log.e(TAG, "=============================");
        
        
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        
        // If the adapter is null, then Bluetooth is not supported
        //嚙磐嚙踝蕭t嚙踝蕭嚙踝蕭null嚙璀嚙篁嚙踝蕭嚙賭援嚙褐迎蕭
        if (mBluetoothAdapter == null) {
            Toast.makeText(this, "嚙踝蕭嚙賭援嚙褐迎蕭", Toast.LENGTH_LONG).show();
            finish();
            return;
        }
    }

    //嚙誼湛蕭嚙褐芽佗蕭嚙磅嚙踝蕭嚙踝蕭嚙罷嚙瘠嚙磅嚙踝蕭嚙踝蕭嚙罷嚙璀嚙篁嚙請求嚙踝蕭嚙罷嚙瘤嚙踝蕭嚙踝蕭嚙豌，嚙瞇嚙箠嚙瘡嚙稽嚙練嚙瑾嚙褒莎蕭扆T嚙踝蕭嚙踝蕭嚙褒備工嚙瑾
    @Override
    public void onStart() {
        super.onStart();
        if(D) Log.e(TAG, "++ ON START ++");

        //嚙緘嚙瘦嚙磅嚙踝蕭嚙課堆蕭嚙褐芽，嚙篁嚙緯嚙瘩嚙誕用者開嚙踝蕭嚙褐芽。
        if (!mBluetoothAdapter.isEnabled()) {
            Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
       
        //嚙磐嚙踝蕭嚙璀嚙稽嚙練嚙踝蕭挶|嚙踝蕭    
        } else {
            if (mChatService == null) setupChat();
        }
    }

    //
    @Override
    public synchronized void onResume() {
        super.onResume();
        if(D) Log.e(TAG, "+ ON RESUME +");
        //嚙緘嚙瘦嚙誼查嚙磅嚙踝蕭嚙罷嚙踝蕭嚙褐迎蕭BluetoothChatService嚙瘢嚙踝蕭嚙璀嚙褓，嚙篁嚙璀嚙踝蕭嚙罷嚙締嚙諉服嚙踝蕭.
        if (mChatService != null) {
            // Only if the state is STATE_NONE, do we know that we haven't started already
            //嚙緘嚙瘦嚙踝蕭e嚙踝蕭STATE_NONE嚙璀嚙篁嚙豎要嚙罷嚙踝蕭嚙褐芽莎蕭悛A嚙踝蕭
        	if (mChatService.getState() == BluetoothChatService.STATE_NONE) {
              // Start the Bluetooth chat services
        	  //嚙罷嚙課一嚙踝蕭嚙褐芽莎蕭悛A嚙踝蕭
              mChatService.start();
            }
        }
    }
    
    
    
    
    private void setupChat() {
        Log.d(TAG, "setupChat()");

        // Initialize the array adapter for the conversation thread
        //嚙踝蕭l嚙複對蕭僊L嚙緹
        mConversationArrayAdapter = new ArrayAdapter<String>(this, R.layout.message);
        //嚙踝蕭l嚙複對蕭嚙踝蕭嚙豌列嚙踝蕭
  //      mConversationView = (ListView) findViewById(R.id.in);
        //嚙稽嚙練嚙踝蕭嚙踝蕭嚙豌列嚙踝蕭
 //       mConversationView.setAdapter(mConversationArrayAdapter);

        // Initialize the BluetoothChatService to perform bluetooth connections
        //嚙踝蕭l嚙踝蕭BluetoothChatService嚙衛堆蕭嚙踝蕭嚙褐芽連嚙踝蕭
        mChatService = new BluetoothChatService(this, mHandler);

        // Initialize the buffer for outgoing messages
        //嚙踝蕭l嚙複將嚙緯嚙緻嚙箴嚙踝蕭嚙緝嚙踝蕭
        mOutStringBuffer = new StringBuffer("");
        

       
        Log.e(TAG, "***************************************");
     
    }

    @Override
    public synchronized void onPause() {
        super.onPause();
        if(D) Log.e(TAG, "- ON PAUSE -");
    }

    @Override
    public void onStop() {
        super.onStop();
        if(D) Log.e(TAG, "-- ON STOP --");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        // Stop the Bluetooth chat services
        if (mChatService != null) mChatService.stop();
        if(D) Log.e(TAG, "--- ON DESTROY ---");
    }
    
    private void ensureDiscoverable() {
        if(D) Log.d(TAG, "ensure discoverable");
        
        //嚙瞑嚙稻嚙踝蕭嚙緙嚙課佗蕭嚙箠嚙瞋嚙緻嚙緹嚙磅嚙箠嚙瞋嚙編嚙踝蕭
        if (mBluetoothAdapter.getScanMode() !=
            BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE) {
        	
        	//嚙請求嚙箠嚙踝蕭嚙踝蕭嚙璀
            Intent discoverableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
            
            //嚙皺嚙稼嚙踝蕭嚙稼嚙豎性，嚙箠嚙踝蕭嚙踝蕭嚙璀嚙踝蕭嚙褕塚蕭
            discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300);
            startActivity(discoverableIntent);
        }
    }    
    
    
    /**
     * Sends a message.
     * @param message  A string of text to send.
     */
   static public void sendMessage(String message) {
    	
        // Check that we're actually connected before trying anything
    	//嚙誼查嚙瞌嚙稻嚙畿嚙踝蕭s嚙踝蕭嚙踝蕭嚙璀
        if (mChatService.getState() != BluetoothChatService.STATE_CONNECTED) {
         //   Toast.makeText(this, R.string.not_connected, Toast.LENGTH_SHORT).show();
            return;
        }

        // Check that there's actually something to send
        //嚙磐嚙確嚙踝蕭嚙踝蕭嚙踝蕭嚙褐才嚙緻嚙箴嚙璀嚙稻嚙篁嚙踝蕭嚙緻嚙箴
        if (message.length() > 0) {
            // Get the message bytes and tell the BluetoothChatService to write
            byte[] send = message.getBytes();
            mChatService.write(send);

            // Reset out string buffer to zero and clear the edit text field
            mOutStringBuffer.setLength(0);

        }
    }

    // The action listener for the EditText widget, to listen for the return key
    private TextView.OnEditorActionListener mWriteListener = new TextView.OnEditorActionListener() {
        public boolean onEditorAction(TextView view, int actionId, KeyEvent event) {
            // If the action is a key-up event on the return key, send the message
            //嚙踝蕭嚙磊Enter&嚙線嚙稻嚙褕發嚙箴嚙確嚙踝蕭
        	if (actionId == EditorInfo.IME_NULL && event.getAction() == KeyEvent.ACTION_UP) {
            	
                String message = view.getText().toString();
                sendMessage(message);
            }
            if(D) Log.i(TAG, "END onEditorAction");
            return true;
        }
    };

    // The Handler that gets information back from the BluetoothChatService
    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
	            case MESSAGE_STATE_CHANGE:
	                if(D) Log.i(TAG, "MESSAGE_STATE_CHANGE: " + msg.arg1);
	                switch (msg.arg1) {
	                	
	                	case BluetoothChatService.STATE_CONNECTED:
	                		//嚙稽嚙練嚙踝蕭嚙璀嚙緩嚙篇嚙編嚙踝蕭
	        //        		mTitle.setText(R.string.title_connected_to);
	                		
	                		//嚙皺嚙稼嚙稽嚙複名嚙踝蕭
	        //        		mTitle.append(mConnectedDeviceName);
	                		
	                		//嚙瞎嚙緲嚙踝蕭扆O嚙踝蕭
	                		mConversationArrayAdapter.clear();
	                		break;
	                		
	                		
	                	case BluetoothChatService.STATE_CONNECTING:
	                		//嚙稽嚙練嚙踝蕭嚙箭嚙編嚙踝蕭
	          //      		mTitle.setText(R.string.title_connecting);
	                		break;
	                		
	                	case BluetoothChatService.STATE_LISTEN:
	                	case BluetoothChatService.STATE_NONE:
	                		//嚙畿嚙踝蕭嚙褐伐蕭嚙踝蕭A嚙畿嚙磅嚙踝蕭嚙褒備迎蕭嚙璀嚙璀嚙篁嚙踝蕭雰S嚙踝蕭嚙編嚙踝蕭
	             //   		mTitle.setText(R.string.title_not_connected);
	                		break;
	                }
	                break;
	                
	                
	            case MESSAGE_WRITE:
	            	
	            	
	                break;
	                
	                
	            case MESSAGE_READ:
	            	
	            	
	                byte[] readBuf = (byte[]) msg.obj;
	                
	                // construct a string from the valid bytes in the buffer
	                //嚙踝蕭o嚙踝蕭嚙箴嚙衛添嚙稼嚙踝蕭嚙豌�
	                String readMessage = new String(readBuf, 0, msg.arg1);
	                Log.e("readmessage", readMessage);
	               StringTokenizer  str = new StringTokenizer(readMessage," ");
	               ball_x = Integer.parseInt(str.nextToken());
	               ball_y = Integer.parseInt(str.nextToken());
	               dir = Integer.parseInt(str.nextToken());
	               BluetoothChat.hasball = 1;
	               BluetoothChat.candraw =1;
	               BluetoothChat. issend=1;

	 //              new Thread(new RefreshRunner()).start();
	//                mConversationArrayAdapter.add(mConnectedDeviceName+":  " + readMessage);
	                
	                break;
	                
	            case MESSAGE_DEVICE_NAME:
	            	isconnect=1;
	                // save the connected device's name
	            	//嚙瞌嚙編嚙編嚙踝蕭嚙踝蕭嚙稽嚙複名嚙誶，嚙踝蕭嚙踝蕭雂@嚙踝蕭Toast嚙踝蕭嚙踝蕭
	                mConnectedDeviceName = msg.getData().getString(DEVICE_NAME);
	/*                Toast.makeText(getApplicationContext(), "Connected to "
	                               + mConnectedDeviceName, Toast.LENGTH_SHORT).show();
	*/                break;
	                
	            case MESSAGE_TOAST:
	            	//嚙畿嚙緲嚙編嚙踝蕭(嚙緻嚙箴)嚙踝蕭嚙諸迎蕭嚙確嚙踝蕭
	            	
	                Toast.makeText(getApplicationContext(), msg.getData().getString(TOAST),
	                               Toast.LENGTH_SHORT).show();
	                break;
            }
        }
    };

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(D) Log.d(TAG, "onActivityResult " + resultCode);
        switch (requestCode) {
        case REQUEST_CONNECT_DEVICE:
            // When DeviceListActivity returns with a device to connect
        	//嚙踝蕭DeviceListActivity嚙稷嚙褒一嚙諉設嚙複連嚙踝蕭嚙踝蕭
            if (resultCode == Activity.RESULT_OK) {
                // Get the device MAC address
            	//嚙緬Intent嚙踝蕭嚙緻嚙踝蕭]嚙複迎蕭Mac嚙踝蕭m
                String address = data.getExtras()
                                     .getString(DeviceListActivity.EXTRA_DEVICE_ADDRESS);
                
                // Get the BLuetoothDevice object
                //嚙緻嚙踝蕭嚙褐芽設嚙複對蕭H
                BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
                
               
                // Attempt to connect to the device
                //嚙踝蕭嚙調連嚙踝蕭嚙緻嚙踝蕭嚙褐芽設嚙踝蕭
                mChatService.connect(device);
            }
            break;
            
        case REQUEST_ENABLE_BT:
            // When the request to enable Bluetooth returns
        	//嚙箭嚙請求嚙踝蕭嚙罷嚙褐芽時迎蕭嚙瞇嚙碼
            if (resultCode == Activity.RESULT_OK) {
                // Bluetooth is now enabled, so set up a chat session
            	//嚙褐芽伐蕭嚙罷嚙璀嚙課以嚙稽嚙練嚙瑾嚙踝蕭嚙褐芽會嚙踝蕭
                setupChat();
            } else {
                // User did not enable Bluetooth or an error occured
            	//嚙請求嚙踝蕭嚙罷嚙褐芽出嚙踝蕭
                Log.d(TAG, "BT not enabled");
                Toast.makeText(this, R.string.bt_not_enabled_leaving, Toast.LENGTH_SHORT).show();
                finish();
            }
        }
    }

    //嚙請建一嚙諉目選蕭
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.option_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case R.id.scan:
            // Launch the DeviceListActivity to see devices and do scan
        	//嚙課堆蕭DeviceListActivity嚙範嚙豎設嚙複並梧蕭嚙緙
            Intent serverIntent = new Intent(this, DeviceListActivity.class);
            startActivityForResult(serverIntent, REQUEST_CONNECT_DEVICE);
            return true;
            
        case R.id.discoverable:
            // Ensure this device is discoverable by others
        	//嚙確嚙瞌嚙踝蕭嚙璀嚙踝蕭i嚙踝蕭嚙踝蕭嚙璀
            ensureDiscoverable();
            return true;
        }
        return false;
    }
    
   
}