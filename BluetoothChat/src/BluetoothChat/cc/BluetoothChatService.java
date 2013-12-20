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
    //��Ы�Socket�A�Ȯɪ�SDP�W��
    private static final String NAME = "BluetoothChat";

    // Unique UUID for this application
    //���ε{���ߤ@��UUID
    private static final UUID MY_UUID = UUID.fromString("fa87c0d0-afac-11de-8a39-0800200c9a66");

    // Member fields
    //���a���Ū޿�t��
    private final BluetoothAdapter mAdapter;
    private final Handler mHandler;
    
    //�ШD��ť�L�{
    private AcceptThread mAcceptThread;
    
    //�s���@�ӳ]��
    private ConnectThread mConnectThread;
    
    //�w�g�s����
    private ConnectedThread mConnectedThread;
    
    //�U�ت��A
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
    	//�o�쥻�a�Ū޿�t��
        mAdapter = BluetoothAdapter.getDefaultAdapter();
        
        //�]�m���A
        mState = STATE_NONE;
        
        //�]�mHandler
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
        //���A��s����AUI Activity�]�ݭn��s
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
        //��������չϫإߤ@�ӳs��
        if (mConnectThread != null) {
        	mConnectThread.cancel(); 
        	mConnectThread = null;
        }

        // Cancel any thread currently running a connection
        //�������b�B�檺�s��
        if (mConnectedThread != null) {
        	mConnectedThread.cancel(); 
        	mConnectedThread = null;
        }

        // Start the thread to listen on a BluetoothServerSocket
        //�Ұ�mAcceptThread�Ӻ�ťBluetoothServerSocket
        if (mAcceptThread == null) {
            mAcceptThread = new AcceptThread();
            mAcceptThread.start();
        }
        
        //�]�m���A����ť�A���ݳs��...
        setState(STATE_LISTEN);
    }

    /**
     * Start the ConnectThread to initiate a connection to a remote device.
     * @param device  The BluetoothDevice to connect
     */
    public synchronized void connect(BluetoothDevice device) {
        if (D) Log.d(TAG, "connect to: " + device);

        // Cancel any thread attempting to make a connection
        //��������Thread�չϫإߤ@�ӳs��
        if (mState == STATE_CONNECTING) {
            if (mConnectThread != null) {
            	mConnectThread.cancel(); 
            	mConnectThread = null;
            }
        }

        // Cancel any thread currently running a connection
        //�������󥿦b�B�檺Thread
        if (mConnectedThread != null) {
        	mConnectedThread.cancel(); 
        	mConnectedThread = null;
        }

        // Start the thread to connect with the given device
        //�Ұʤ@�ӳs��Thread�h�s�����w���]��
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
        
        /*	���F�קK���гs�u�A
			���ˬd���S���w�s�b��ConectThread�BConnectedThrad�MAcceptThread�C
			�p�G���A�@�ߥ������C
		*/

        // Cancel the thread that completed the connection
        //�����Ҧ�ConnectThread
        if (mConnectThread != null) {
        	mConnectThread.cancel(); 
        	mConnectThread = null;
        }

        // Cancel any thread currently running a connection
        //�����Ҧ����b�s����Thread
        if (mConnectedThread != null) {
        	mConnectedThread.cancel(); 
        	mConnectedThread = null;
        }

        // Cancel the accept thread because we only want to connect to one device
        //�����Ҧ���ťThread�A�]���w�g�s���F�@�ӳ]��
        if (mAcceptThread != null) {
        	mAcceptThread.cancel(); 
        	mAcceptThread = null;
        }

        // 	Start the thread to manage the connection and perform transmissions
        //�M��A�Ұ�ConnectedThread�޲z�s��&����½Ķ
        mConnectedThread = new ConnectedThread(socket);
        mConnectedThread.start();

        
        // Send the name of the connected device back to the UI Activity
        //�o�e�s�����]�ƦW�٨�UI Activity����
        Message msg = mHandler.obtainMessage(BluetoothChat.MESSAGE_DEVICE_NAME);
        Bundle bundle = new Bundle();
        bundle.putString(BluetoothChat.DEVICE_NAME, device.getName());
        msg.setData(bundle);
        mHandler.sendMessage(msg);

        //���A���w�g�s���A���b�B�椤...
        setState(STATE_CONNECTED);
    }

    /**
     * Stop all threads
     */
    //�����Ҧ�Thread
    public synchronized void stop() {
        if (D) Log.d(TAG, "stop");
        if (mConnectThread != null) {mConnectThread.cancel(); mConnectThread = null;}
        if (mConnectedThread != null) {mConnectedThread.cancel(); mConnectedThread = null;}
        if (mAcceptThread != null) {mAcceptThread.cancel(); mAcceptThread = null;}
        
        //�]�m���ǳƪ��A
        setState(STATE_NONE);
    }

    /**
     * Write to the ConnectedThread in an unsynchronized manner
     * @param out The bytes to write
     * @see ConnectedThread#write(byte[])
     */
    //�g�J�ۤv�n�o�e������
    public void write(byte[] out) {
        // Create temporary object

    		ConnectedThread r;
    		// Synchronize a copy of the ConnectedThread
    		synchronized (this) {
        	//�P�_�O�_�B��w�g�s�����A
            if (mState != STATE_CONNECTED) return;
            	r = mConnectedThread;
            
    		}
    		
    		// Perform the write unsynchronized
    		//����g�X�h
    		r.write(out);
    	
    }
    
    /**
     * Indicate that the connection attempt failed and notify the UI Activity.
     */
    private void connectionFailed() {
        setState(STATE_LISTEN);

        // Send a failure message back to the Activity
        //�o�e�s�����Ѫ�������UI����
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
        //�o�e�������Ѩ�UI����
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
    //�o��AcceptThread�s�b���ت��A�O�]���{�������]�C�O�˸m�����i��Q�n�򥦰��Ū޳s�u�C 
    private class AcceptThread extends Thread {
        // The local server socket
    	//���aSocket�A��
        private final BluetoothServerSocket mmServerSocket;

        public AcceptThread() {
            BluetoothServerSocket tmp = null;

            // Create a new listening server socket
            //�Ыؤ@�ӷs��Socket�A�Ⱥ�ť
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
            
        	//BluetoothSocket�i�H���ڭ̰����ƥ洫���\��C
            BluetoothSocket socket = null;

            // Listen to the server socket if we're not connected
            //�p�G��e�S���s���A�h�@����ťSocket�A��
            while (mState != STATE_CONNECTED) {
                try {
                    // This is a blocking call and will only return on a
                    // successful connection or an exception
                	//�p�G���ШD�N�����A�o�O�@�Ӫ���եΡA�N����^�s�����\�M�@�Ӳ��`
                    socket = mmServerSocket.accept();
                } catch (IOException e) {
                    Log.e(TAG, "accept() failed", e);
                    break;
                }

                // If a connection was accepted
                //�Y�����s��
                if (socket != null) {
                    synchronized (BluetoothChatService.this) {
                        switch (mState) {
                        	case STATE_LISTEN:
                        	case STATE_CONNECTING:
                        		// Situation normal. Start the connected thread.
                        		/*	�]���bService onStart()�I�sAcceptThread.start()��A
									���W�N�Ūު��A�]�w��setState(STATE_LISTEN);
									�]���A�bswitch�j�餤�A
									�{������Fconnected()�禡�C
								*/
                        		//�Y���A����ť�Υ��b�s�����A�h�ե�connected�ӳs��
                        		connected(socket, socket.getRemoteDevice());
                        		break;
                        		
                        	case STATE_NONE:
                        	case STATE_CONNECTED:
	                            // Either not ready or already connected. Terminate new socket.
	                            //�Y�S���ǳƩΤw�g�s���A�o�פ��Socket
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
    //ConnectThread���ت��O�n�D�ʳs���䥦�w�}���Ūު��˸m�C 
    private class ConnectThread extends Thread {
    	//�Ū�Socket
        private final BluetoothSocket mmSocket;
        
        //�Ū޳]��
        private final BluetoothDevice mmDevice;

        public ConnectThread(BluetoothDevice device) {
            mmDevice = device;
            BluetoothSocket tmp = null;

            // Get a BluetoothSocket for a connection with the
            // given BluetoothDevice
            //�o��@�ӫ��w�Ū޳]�ƪ�BluetoothSocket
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
            //�����i�����A�A�N�|�i��s��
            mAdapter.cancelDiscovery();

            // Make a connection to the BluetoothSocket
            //�Ыؤ@��BluetoothSocket�s��
            try {
                // This is a blocking call and will only return on a
                // successful connection or an exception
            	//�P�ˬO����եΡA�N����^�s�����\�M�@�Ӳ��`
                mmSocket.connect();
            } catch (IOException e) {
            	//�s������
                connectionFailed();
                
                // Close the socket
                //�p�G���`�A�h����Socket
                try {
                    mmSocket.close();
                } catch (IOException e2) {
                    Log.e(TAG, "unable to close() socket during connection failure", e2);
                }
                
                // Start the service over to restart listening mode
                //���s�Ұʺ�ť�A�Ȫ��A
                BluetoothChatService.this.start();
                return;
            }

            // Reset the ConnectThread because we're done
            //�����h���mConnectThread
            synchronized (BluetoothChatService.this) {
                mConnectThread = null;
            }

            // Start the connected thread
            //�}��ConnectThread��...
            connected(mmSocket, mmDevice);
        }

        //����ConnectThread
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
    //�b����Ƥ��Ǫ���ť�u�@�C
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
            //�o��BluetoothSocket��InputStream�BOutputStream
            try {
            	//ConnectedThread���b��BluetoothSocket���oInputStream�MOutputStream
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
            //��ťInputStream
            while (true) {
                try {

                    // Read from the InputStream
                	//�qInputStream��Ū���ƾ�
                    bytes = mmInStream.read(buffer);

                    // Send the obtained bytes to the UI Activity
                    //�o�e�@�ӰT����UI Activity�i���s
                    mHandler.obtainMessage(BluetoothChat.MESSAGE_READ, bytes, -1, buffer)
                            .sendToTarget();

                	
                } catch (IOException e) {
                	//�X�{���`�A�h�s����
                    Log.e(TAG, "disconnected", e);
                    connectionLost();
                    break;
                }
            }
        }

        /**
         * Write to the connected OutStream.
         * �g�J�n�o�e���T��
         * @param buffer  The bytes to write
         */
        //�z�L�X�U��write()�Mread()�b��2���Ū޸˸m�����q
        public void write(byte[] buffer) {
            try {
                mmOutStream.write(buffer);

                // Share the sent message back to the UI Activity
                //�N�g���T���P�ɶǻ���UI����
                mHandler.obtainMessage(BluetoothChat.MESSAGE_WRITE, -1, -1, buffer)
                        .sendToTarget();

            } catch (IOException e) {
                Log.e(TAG, "Exception during write", e);
            }
        }
        

    	//����ConnectedThread
        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "close() of connect socket failed", e);
            }
        }
    }
}
