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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

/**
 * This class does all the work for setting up and managing Bluetooth
 * connections with other devices. It has a thread that listens for
 * incoming connections, a thread for connecting with a device, and a
 * thread for performing data transmissions when connected.
 */
public class BluetoothChatService {
    // Debugging
    private static final String TAG = "BluetoothChatService";
    private static final boolean D = true;

    // Name for the SDP record when creating server socket
    //當創建Socket服務時的SDP名稱
    private static final String NAME = "BluetoothChat";

    // Unique UUID for this application
    //應用程式唯一的UUID
    private static final UUID MY_UUID = UUID.fromString("fa87c0d0-afac-11de-8a39-0800200c9a66");

    // Member fields
    //本地的藍芽選配器
    private final BluetoothAdapter mAdapter;
    private final Handler mHandler;
    
    //請求監聽過程
    private AcceptThread mAcceptThread;
    
    //連接一個設備
    private ConnectThread mConnectThread;
    
    //已經連接後
    private ConnectedThread mConnectedThread;
    
    //各種狀態
    private int mState;

    // Constants that indicate the current connection state
    public static final int STATE_NONE = 0;       // we're doing nothing
    public static final int STATE_LISTEN = 1;     // now listening for incoming connections
    public static final int STATE_CONNECTING = 2; // now initiating an outgoing connection
    public static final int STATE_CONNECTED = 3;  // now connected to a remote device

    /**
     * Constructor. Prepares a new BluetoothChat session.
     * @param context  The UI Activity Context
     * @param handler  A Handler to send messages back to the UI Activity
     */
    public BluetoothChatService(Context context, Handler handler) {
    	//得到本地藍芽選配器
        mAdapter = BluetoothAdapter.getDefaultAdapter();
        
        //設置狀態
        mState = STATE_NONE;
        
        //設置Handler
        mHandler = handler;
    }

    /**
     * Set the current state of the chat connection
     * @param state  An integer defining the current connection state
     */
    private synchronized void setState(int state) {
    	
        if (D) Log.d(TAG, "setState() " + mState + " -> " + state);
        mState = state;

        // Give the new state to the Handler so the UI Activity can update
        //狀態更新之後，UI Activity也需要更新
        mHandler.obtainMessage(BluetoothChat.MESSAGE_STATE_CHANGE, state, -1).sendToTarget();
    }

    /**
     * Return the current connection state. */
    public synchronized int getState() {
        return mState;
    }

    /**
     * Start the chat service. Specifically start AcceptThread to begin a
     * session in listening (server) mode. Called by the Activity onResume() */
    public synchronized void start() {
        if (D) Log.d(TAG, "start");

        // Cancel any thread attempting to make a connection
        //取消任何試圖建立一個連接
        if (mConnectThread != null) {
        	mConnectThread.cancel(); 
        	mConnectThread = null;
        }

        // Cancel any thread currently running a connection
        //取消正在運行的連接
        if (mConnectedThread != null) {
        	mConnectedThread.cancel(); 
        	mConnectedThread = null;
        }

        // Start the thread to listen on a BluetoothServerSocket
        //啟動mAcceptThread來監聽BluetoothServerSocket
        if (mAcceptThread == null) {
            mAcceptThread = new AcceptThread();
            mAcceptThread.start();
        }
        
        //設置狀態為監聽，等待連接...
        setState(STATE_LISTEN);
    }

    /**
     * Start the ConnectThread to initiate a connection to a remote device.
     * @param device  The BluetoothDevice to connect
     */
    public synchronized void connect(BluetoothDevice device) {
        if (D) Log.d(TAG, "connect to: " + device);

        // Cancel any thread attempting to make a connection
        //取消任何Thread試圖建立一個連接
        if (mState == STATE_CONNECTING) {
            if (mConnectThread != null) {
            	mConnectThread.cancel(); 
            	mConnectThread = null;
            }
        }

        // Cancel any thread currently running a connection
        //取消任何正在運行的Thread
        if (mConnectedThread != null) {
        	mConnectedThread.cancel(); 
        	mConnectedThread = null;
        }

        // Start the thread to connect with the given device
        //啟動一個連接Thread去連接指定的設備
        mConnectThread = new ConnectThread(device);
        mConnectThread.start();
        setState(STATE_CONNECTING);
    }

    /**
     * Start the ConnectedThread to begin managing a Bluetooth connection
     * @param socket  The BluetoothSocket on which the connection was made
     * @param device  The BluetoothDevice that has been connected
     */
    public synchronized void connected(BluetoothSocket socket, BluetoothDevice device) {
        if (D) Log.d(TAG, "connected");
        
        /*	為了避免重覆連線，
			先檢查有沒有已存在的ConectThread、ConnectedThrad和AcceptThread。
			如果有，一律先關掉。
		*/

        // Cancel the thread that completed the connection
        //取消所有ConnectThread
        if (mConnectThread != null) {
        	mConnectThread.cancel(); 
        	mConnectThread = null;
        }

        // Cancel any thread currently running a connection
        //取消所有正在連接的Thread
        if (mConnectedThread != null) {
        	mConnectedThread.cancel(); 
        	mConnectedThread = null;
        }

        // Cancel the accept thread because we only want to connect to one device
        //取消所有監聽Thread，因為已經連接了一個設備
        if (mAcceptThread != null) {
        	mAcceptThread.cancel(); 
        	mAcceptThread = null;
        }

        // 	Start the thread to manage the connection and perform transmissions
        //然後，啟動ConnectedThread管理連接&執行翻譯
        mConnectedThread = new ConnectedThread(socket);
        mConnectedThread.start();

        
        // Send the name of the connected device back to the UI Activity
        //發送連接的設備名稱到UI Activity介面
        Message msg = mHandler.obtainMessage(BluetoothChat.MESSAGE_DEVICE_NAME);
        Bundle bundle = new Bundle();
        bundle.putString(BluetoothChat.DEVICE_NAME, device.getName());
        msg.setData(bundle);
        mHandler.sendMessage(msg);

        //狀態為已經連接，正在運行中...
        setState(STATE_CONNECTED);
    }

    /**
     * Stop all threads
     */
    //取消所有Thread
    public synchronized void stop() {
        if (D) Log.d(TAG, "stop");
        if (mConnectThread != null) {mConnectThread.cancel(); mConnectThread = null;}
        if (mConnectedThread != null) {mConnectedThread.cancel(); mConnectedThread = null;}
        if (mAcceptThread != null) {mAcceptThread.cancel(); mAcceptThread = null;}
        
        //設置為準備狀態
        setState(STATE_NONE);
    }

    /**
     * Write to the ConnectedThread in an unsynchronized manner
     * @param out The bytes to write
     * @see ConnectedThread#write(byte[])
     */
    //寫入自己要發送的消息
    public void write(byte[] out) {
        // Create temporary object

    		ConnectedThread r;
    		// Synchronize a copy of the ConnectedThread
    		synchronized (this) {
        	//判斷是否處於已經連接狀態
            if (mState != STATE_CONNECTED) return;
            	r = mConnectedThread;
            
    		}
    		
    		// Perform the write unsynchronized
    		//執行寫出去
    		r.write(out);
    	
    }
    
    /**
     * Indicate that the connection attempt failed and notify the UI Activity.
     */
    private void connectionFailed() {
        setState(STATE_LISTEN);

        // Send a failure message back to the Activity
        //發送連接失敗的消息到UI介面
        Message msg = mHandler.obtainMessage(BluetoothChat.MESSAGE_TOAST);
        Bundle bundle = new Bundle();
        bundle.putString(BluetoothChat.TOAST, "Unable to connect device");
        msg.setData(bundle);
        mHandler.sendMessage(msg);
    }

    /**
     * Indicate that the connection was lost and notify the UI Activity.
     */
    
    private void connectionLost() {
        setState(STATE_LISTEN);

        // Send a failure message back to the Activity
        //發送消息失敗到UI介面
        Message msg = mHandler.obtainMessage(BluetoothChat.MESSAGE_TOAST);
        Bundle bundle = new Bundle();
        bundle.putString(BluetoothChat.TOAST, "Device connection was lost");
        msg.setData(bundle);
        mHandler.sendMessage(msg);
    }

    /**
     * This thread runs while listening for incoming connections. It behaves
     * like a server-side client. It runs until a connection is accepted
     * (or until cancelled).
     */
    //這個AcceptThread存在的目的，是因為程式先假設每臺裝置都有可能想要跟它做藍芽連線。 
    private class AcceptThread extends Thread {
        // The local server socket
    	//本地Socket服務
        private final BluetoothServerSocket mmServerSocket;

        public AcceptThread() {
            BluetoothServerSocket tmp = null;

            // Create a new listening server socket
            //創建一個新的Socket服務監聽
            try {
                tmp = mAdapter.listenUsingRfcommWithServiceRecord(NAME, MY_UUID);
            } catch (IOException e) {
                Log.e(TAG, "listen() failed", e);
            }
            mmServerSocket = tmp;
        }

        public void run() {
            if (D) Log.d(TAG, "BEGIN mAcceptThread" + this);
            setName("AcceptThread");
            
        	//BluetoothSocket可以讓我們做到資料交換的功能。
            BluetoothSocket socket = null;

            // Listen to the server socket if we're not connected
            //如果當前沒有連接，則一直監聽Socket服務
            while (mState != STATE_CONNECTED) {
                try {
                    // This is a blocking call and will only return on a
                    // successful connection or an exception
                	//如果有請求就接受，這是一個阻塞調用，將之返回連接成功和一個異常
                    socket = mmServerSocket.accept();
                } catch (IOException e) {
                    Log.e(TAG, "accept() failed", e);
                    break;
                }

                // If a connection was accepted
                //若接受連接
                if (socket != null) {
                    synchronized (BluetoothChatService.this) {
                        switch (mState) {
                        	case STATE_LISTEN:
                        	case STATE_CONNECTING:
                        		// Situation normal. Start the connected thread.
                        		/*	因為在Service onStart()呼叫AcceptThread.start()後，
									馬上將藍芽狀態設定成setState(STATE_LISTEN);
									因此，在switch迴圈中，
									程式執行了connected()函式。
								*/
                        		//若狀態為監聽或正在連接中，則調用connected來連接
                        		connected(socket, socket.getRemoteDevice());
                        		break;
                        		
                        	case STATE_NONE:
                        	case STATE_CONNECTED:
	                            // Either not ready or already connected. Terminate new socket.
	                            //若沒有準備或已經連接，這終止該Socket
                        		try {
	                                socket.close();
	                            } catch (IOException e) {
	                                Log.e(TAG, "Could not close unwanted socket", e);
	                            }
                            break;
                        }
                    }
                }
            }
            if (D) Log.i(TAG, "END mAcceptThread");
        }

        public void cancel() {
            if (D) Log.d(TAG, "cancel " + this);
            try {
                mmServerSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "close() of server failed", e);
            }
        }
    }


    /**
     * This thread runs while attempting to make an outgoing connection
     * with a device. It runs straight through; the connection either
     * succeeds or fails.
     */
    //ConnectThread的目的是要主動連接其它已開啟藍芽的裝置。 
    private class ConnectThread extends Thread {
    	//藍芽Socket
        private final BluetoothSocket mmSocket;
        
        //藍芽設備
        private final BluetoothDevice mmDevice;

        public ConnectThread(BluetoothDevice device) {
            mmDevice = device;
            BluetoothSocket tmp = null;

            // Get a BluetoothSocket for a connection with the
            // given BluetoothDevice
            //得到一個指定藍芽設備的BluetoothSocket
            try {
                tmp = device.createRfcommSocketToServiceRecord(MY_UUID);
            } catch (IOException e) {
                Log.e(TAG, "create() failed", e);
            }
            mmSocket = tmp;
        }

        public void run() {
            Log.i(TAG, "BEGIN mConnectThread");
            setName("ConnectThread");

            // Always cancel discovery because it will slow down a connection
            //取消可見狀態，將會進行連接
            mAdapter.cancelDiscovery();

            // Make a connection to the BluetoothSocket
            //創建一個BluetoothSocket連接
            try {
                // This is a blocking call and will only return on a
                // successful connection or an exception
            	//同樣是阻塞調用，將之返回連接成功和一個異常
                mmSocket.connect();
            } catch (IOException e) {
            	//連接失敗
                connectionFailed();
                
                // Close the socket
                //如果異常，則關掉Socket
                try {
                    mmSocket.close();
                } catch (IOException e2) {
                    Log.e(TAG, "unable to close() socket during connection failure", e2);
                }
                
                // Start the service over to restart listening mode
                //重新啟動監聽服務狀態
                BluetoothChatService.this.start();
                return;
            }

            // Reset the ConnectThread because we're done
            //完成則重置ConnectThread
            synchronized (BluetoothChatService.this) {
                mConnectThread = null;
            }

            // Start the connected thread
            //開啟ConnectThread中...
            connected(mmSocket, mmDevice);
        }

        //取消ConnectThread
        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "close() of connect socket failed", e);
            }
        }
    }

    /**
     * This thread runs during a connection with a remote device.
     * It handles all incoming and outgoing transmissions.
     */
    //在做資料互傳的監聽工作。
    private class ConnectedThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;

        public ConnectedThread(BluetoothSocket socket) {
            Log.d(TAG, "create ConnectedThread");
            mmSocket = socket;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;

            // Get the BluetoothSocket input and output streams
            //得到BluetoothSocket的InputStream、OutputStream
            try {
            	//ConnectedThread正在用BluetoothSocket取得InputStream和OutputStream
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
            } catch (IOException e) {
                Log.e(TAG, "temp sockets not created", e);
            }

            mmInStream = tmpIn;
            mmOutStream = tmpOut;
        }

        public void run() {
            Log.i(TAG, "BEGIN mConnectedThread");
            byte[] buffer = new byte[1024];
            int bytes;

            // Keep listening to the InputStream while connected
            //監聽InputStream
            while (true) {
                try {

                    // Read from the InputStream
                	//從InputStream中讀取數據
                    bytes = mmInStream.read(buffer);

                    // Send the obtained bytes to the UI Activity
                    //發送一個訊息到UI Activity進行更新
                    mHandler.obtainMessage(BluetoothChat.MESSAGE_READ, bytes, -1, buffer)
                            .sendToTarget();

                	
                } catch (IOException e) {
                	//出現異常，則連接遺失
                    Log.e(TAG, "disconnected", e);
                    connectionLost();
                    break;
                }
            }
        }

        /**
         * Write to the connected OutStream.
         * 寫入要發送的訊息
         * @param buffer  The bytes to write
         */
        //透過旗下的write()和read()在做2隻藍芽裝置的溝通
        public void write(byte[] buffer) {
            try {
                mmOutStream.write(buffer);

                // Share the sent message back to the UI Activity
                //將寫的訊息同時傳遞給UI介面
                mHandler.obtainMessage(BluetoothChat.MESSAGE_WRITE, -1, -1, buffer)
                        .sendToTarget();

            } catch (IOException e) {
                Log.e(TAG, "Exception during write", e);
            }
        }
        

    	//取消ConnectedThread
        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "close() of connect socket failed", e);
            }
        }
    }
}
