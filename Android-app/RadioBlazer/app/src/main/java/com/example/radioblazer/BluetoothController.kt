package com.example.radioblazer

import android.bluetooth.BluetoothAdapter
import android.content.Context
import android.util.Log
import android.bluetooth.BluetoothSocket
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothServerSocket
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import android.os.Bundle
import android.os.Handler
import java.util.*

/**
 * Created by ramankit on 20/7/17.
 */

class BluetoothRadioService(context: Context, handler: Handler){

    // Member fields
    private var mAdapter: BluetoothAdapter? = null
    private var mHandler: Handler? = null
    private var mConnectThread: ConnectThread? = null
    private var mConnectedThread: ConnectedThread? = null
    private var mState: Int = 0
    private var mNewState: Int = 0

    private val  TAG: String = javaClass.simpleName

    // Unique UUID for this application
    //NOTE: Use the correct UUID for yor device
    private val MY_UUID_SECURE = UUID.fromString("29621b37-e817-485a-a258-52da5261421a")

    //In my case the UUID that worked is the one below, and for some reason only worked with an insecure connection
    private val MY_UUID_INSECURE = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")

    // Name for the SDP record when creating server socket
    private val NAME_SECURE = "BluetoothRadioSecure"
    private val NAME_INSECURE = "BluetoothRadioInsecure"

    // Constants that indicate the current connection state
    companion object {
        val STATE_NONE = 0       // we're doing nothing
        val STATE_LISTEN = 1     // now listening for incoming connections
        val STATE_CONNECTING = 2 // now initiating an outgoing connection
        val STATE_CONNECTED = 3  // now connected to a remote device
    }

    init {

        mAdapter = BluetoothAdapter.getDefaultAdapter()
        mState = STATE_NONE
        mNewState = mState
        mHandler = handler
    }

    /**
     * Return the current connection state.
     */
    @Synchronized fun getState(): Int {
        return mState
    }

    /**
     * Start the radio service.
     */
    @Synchronized fun start() {
        Log.d(TAG, "start")

        // Cancel any thread attempting to make a connection
        if (mConnectThread != null) {
            mConnectThread?.cancel()
            mConnectThread = null
        }

        // Cancel any thread currently running a connection
        if (mConnectedThread != null) {
            mConnectedThread?.cancel()
            mConnectedThread = null
        }
    }


    /**
     * Start the ConnectThread to initiate a connection to a remote device.
     * Secure (true) , Insecure (false)
     */
    @Synchronized fun connect(device: BluetoothDevice?, secure: Boolean) {

        Log.d(TAG, "connecting to: " + device)

        // Cancel any thread attempting to make a connection
        if (mConnectThread != null) {
            mConnectThread?.cancel()
            mConnectThread = null
        }

        // Cancel any thread currently running a connection
        if (mConnectedThread != null) {
            mConnectedThread?.cancel()
            mConnectedThread = null
        }

        // Start the thread to connect with the given device
        mConnectThread = ConnectThread(device, secure)
        mConnectThread?.run()

        // Update UI title
        //updateUserInterfaceTitle()
    }


    /**
      Start the ConnectedThread to begin managing a Bluetooth connection
     */
    @Synchronized fun connected(socket: BluetoothSocket?, device: BluetoothDevice?, socketType: String) {
        Log.d(TAG, "connected, Socket Type:" + socketType)

        // Cancel the thread that completed the connection
        if (mConnectThread != null) {
            mConnectThread?.cancel()
            mConnectThread = null
        }

        // Cancel any thread currently running a connection
        if (mConnectedThread != null) {
            mConnectedThread?.cancel()
            mConnectedThread = null
        }

        // Start the thread to manage the connection and perform transmissions
        mConnectedThread = ConnectedThread(socket, socketType)
        mConnectedThread?.start()

        // Send the name of the connected device back to the UI Activity
        val msg = mHandler?.obtainMessage(Constants.MESSAGE_DEVICE_NAME)
        val bundle = Bundle()
        bundle.putString(Constants.DEVICE_NAME, device?.name)
        msg?.data = bundle
        mHandler?.sendMessage(msg)
        // Update UI title
        //updateUserInterfaceTitle()
    }

    /**
     * Stop all threads
     */
    @Synchronized fun stop() {
        Log.d(TAG, "stop")

        if (mConnectThread != null) {
            mConnectThread?.cancel()
            mConnectThread = null
        }

        if (mConnectedThread != null) {
            mConnectedThread?.cancel()
            mConnectedThread = null
        }

        mState = STATE_NONE
        // Update UI title
        //updateUserInterfaceTitle()
    }

    /**
     * Write to the ConnectedThread in an unsynchronized manner
     */
    fun write(out: ByteArray) {
        // Create temporary object
        var r: ConnectedThread?  = null
        // Synchronize a copy of the ConnectedThread
        synchronized(this) {
            if (mState != STATE_CONNECTED) return
            r = mConnectedThread
        }
        // Perform the write unsynchronized
        r?.write(out)
    }


    /**
     * Indicate that the connection attempt failed and notify the UI Activity.
     */
    private fun connectionFailed() {
        // Send a failure message back to the Activity
        val msg = mHandler?.obtainMessage(Constants.MESSAGE_TOAST)
        val bundle = Bundle()
        bundle.putString(Constants.TOAST, "Unable to connect device")
        msg?.data = bundle
        mHandler?.sendMessage(msg)

        mState = STATE_NONE
        // Update UI title
        //updateUserInterfaceTitle()

        // Start the service over to restart listening mode
        this@BluetoothRadioService.start()
    }

    /**
     * Indicate that the connection was lost and notify the UI Activity.
     */
    private fun connectionLost() {
        // Send a failure message back to the Activity
        val msg = mHandler?.obtainMessage(Constants.MESSAGE_TOAST)
        val bundle = Bundle()
        bundle.putString(Constants.TOAST, "Device connection was lost")
        msg?.data = bundle
        mHandler?.sendMessage(msg)

        mState = STATE_NONE
        // Update UI title
        // updateUserInterfaceTitle()

        // Start the service over to restart listening mode
        this@BluetoothRadioService.start()
    }


    /**
     * This thread runs while attempting to make an outgoing connection
     * with a device. It runs straight through; the connection either
     * succeeds or fails.
     */
    private inner class ConnectThread(private val mmDevice: BluetoothDevice?, secure: Boolean) : Thread() {
        private val mmSocket: BluetoothSocket?
        private val mSocketType: String

        init {
            var tmp: BluetoothSocket? = null
            mSocketType = if (secure) "Secure" else "Insecure"

            // Get a BluetoothSocket for a connection with the
            // given BluetoothDevice
            try {
                if (secure) {
                    tmp = mmDevice?.createRfcommSocketToServiceRecord(
                        MY_UUID_SECURE)
                    Log.e(TAG, "Socket Type: " + mSocketType + "UUID created as Secure")
                } else {
                    tmp = mmDevice?.createInsecureRfcommSocketToServiceRecord(
                        MY_UUID_INSECURE)
                    Log.e(TAG, "Socket Type: " + mSocketType + "UUID created as Insecure")
                }
            } catch (e: IOException) {
                Log.e(TAG, "Socket Type: " + mSocketType + "create() failed", e)
            }

            mmSocket = tmp
            mState = STATE_CONNECTING
        }

        override fun run() {

            Log.i(TAG, "BEGIN mConnectThread SocketType:" + mSocketType)
            name = "ConnectThread" + mSocketType

            // Always cancel discovery because it will slow down a connection
            mAdapter?.cancelDiscovery()

            // Make a connection to the BluetoothSocket
            Log.i(TAG, "Cancelled Discovery")
            try {
                // This is a blocking call and will only return on a
                // successful connection or an exception
                mmSocket!!.connect()
                val msg = mHandler?.obtainMessage(Constants.DEVICE_CONNECTED)
                val bundle = Bundle()
                bundle.putString(Constants.TOAST, "Connected to Device")
                msg?.data = bundle
                mHandler?.sendMessage(msg)
                Log.e(TAG, "Connected")

            } catch (e: IOException) {
                // Close the socket
                try {
                    mmSocket?.close()
                    Log.e(TAG, "Couldn't connect to bluetooth device, Sockect CLosed. Error: " + e)
                } catch (e2: IOException) {
                    Log.e(TAG, "unable to close() " + mSocketType +
                            " socket during connection failure", e2)
                }

                connectionFailed()
                return
            }

            // Reset the ConnectThread because we're done
            synchronized(this@BluetoothRadioService) {
                mConnectThread = null
            }

            // Start the connected thread
            connected(mmSocket, mmDevice, mSocketType)
        }

        fun cancel() {
            try {
                mmSocket?.close()
            } catch (e: IOException) {
                Log.e(TAG, "close() of connect $mSocketType socket failed", e)
            }

        }
    }

    /**
     * This thread runs during a connection with a remote device.
     * It handles all incoming and outgoing transmissions.
     */
    private inner class ConnectedThread(private val mmSocket: BluetoothSocket?, socketType: String) : Thread() {

        private val mmInStream: InputStream?
        private val mmOutStream: OutputStream?

        init {
            Log.d(TAG, "create ConnectedThread: " + socketType)
            var tmpIn: InputStream? = null
            var tmpOut: OutputStream? = null

            // Get the BluetoothSocket input and output streams
            try {
                tmpIn = mmSocket?.inputStream
                tmpOut = mmSocket?.outputStream
            } catch (e: IOException) {
                Log.e(TAG, "temp sockets not created", e)
            }

            mmInStream = tmpIn
            mmOutStream = tmpOut
            mState = STATE_CONNECTED
        }

        override fun run() {
            Log.i(TAG, "BEGIN mConnectedThread")
            val buffer = ByteArray(1024)
            var bytes: Int

            // Keep listening to the InputStream while connected
            while (mState == STATE_CONNECTED) {
                try {
                    // Read from the InputStream
                    bytes = mmInStream?.read(buffer) ?: 0

                    // Send the obtained bytes to the UI Activity
                    mHandler?.obtainMessage(Constants.MESSAGE_READ, bytes, -1, buffer)
                        ?.sendToTarget()
                } catch (e: IOException) {
                    Log.e(TAG, "disconnected", e)
                    connectionLost()
                    break
                }

            }
        }

        /**
         * Write to the connected OutStream.
         */
        fun write(buffer: ByteArray) {
            try {
                mmOutStream?.write(buffer)

                // Share the sent message back to the UI Activity
                mHandler?.obtainMessage(Constants.MESSAGE_WRITE, -1, -1, buffer)
                    ?.sendToTarget()
            } catch (e: IOException) {
                Log.e(TAG, "Exception during write", e)
            }

        }

        fun cancel() {
            try {
                mmSocket?.close()
            } catch (e: IOException) {
                Log.e(TAG, "close() of connect socket failed", e)
            }

        }
    }


}