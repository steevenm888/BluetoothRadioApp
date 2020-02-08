package com.example.radioblazer

import Data.DataDbHelper
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.bluetooth.*
import android.content.Intent
import android.content.DialogInterface
import androidx.appcompat.app.AlertDialog
import android.util.Log
import android.app.ProgressDialog
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.Context
import android.os.AsyncTask
import java.util.*
import android.bluetooth.BluetoothSocket
import android.os.Handler
import java.io.*
import android.os.Message
import android.annotation.SuppressLint
import android.widget.*


import java.io.IOException
import java.lang.reflect.Array
import java.math.BigInteger
import kotlin.properties.Delegates


class MainActivity : AppCompatActivity() {

    //Companion object with the variables that we will use
    companion object {
        lateinit var m_address: String
        private lateinit var txtConnected: TextView
        private lateinit var txtFreq: TextView
        private var aux = ""
    }

    private var mChatService: BluetoothRadioService? = null
    private var m_bluetoothAdapter: BluetoothAdapter? = null
    private var db: DataDbHelper? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        //Get the address sent from the last activity with the paired devices
        m_address = intent.getStringExtra(Setup?.EXTRA_ADDRESS)

        //Widgets declaration
        txtConnected = findViewById(R.id.txtConnected) as TextView
        val btnBack: ImageButton = findViewById(R.id.btnBack) as ImageButton
        val btnForward: ImageButton = findViewById(R.id.btnForward) as ImageButton
        val btn1: Button = findViewById(R.id.btn1) as Button
        val btn2: Button = findViewById(R.id.btn2) as Button
        val btn3: Button = findViewById(R.id.btn3) as Button
        val btn4: Button = findViewById(R.id.btn4) as Button
        val btn5: Button = findViewById(R.id.btn5) as Button
        val btn6: Button = findViewById(R.id.btn6) as Button
        val mute: Switch = findViewById(R.id.mute) as  Switch
        txtFreq = findViewById(R.id.txtFreq) as TextView
        db = DataDbHelper(this)

        //With this declaration you can run the second method described later
        //ConnectToDevice(this).execute()

        //Declaration of the Bluetooth Radio service to manage the bluetooth connection
        //The mHandler will later help us to get information from other threads linked to the bluetooth radio service
        mChatService = BluetoothRadioService(this, mHandler)
        //Function to connect to our device with our Radio service
        connectDevice(m_address)

        //onClick Listeners that change stations, forward or backward.
        //Additionally we get the frequency that our radio currently is in.
        //I'll leave all the commands compatible with this particular radio board in the readme
        btnBack.setOnClickListener{
            sendCommand("AT+SEEKDOWN")
        }
        btnForward.setOnClickListener {
            sendCommand("AT+SEEKUP")
        }
        btn1.setOnClickListener {
            //val freq = db!!.getData(1)
            sendCommand("AT+FREQ=945")
        }
        btn2.setOnClickListener {
            //val freq = db!!.getData(1)
            sendCommand("AT+FREQ=965")
        }
        btn3.setOnClickListener {
            //val freq = db!!.getData(1)
            sendCommand("AT+FREQ=977")
        }
        btn4.setOnClickListener {
            //val freq = db!!.getData(1)
            sendCommand("AT+FREQ=1021")
        }
        btn5.setOnClickListener {
            //val freq = db!!.getData(1)
            sendCommand("AT+FREQ=1033")
        }
        btn6.setOnClickListener {
            //val freq = db!!.getData(1)
            sendCommand("AT+FREQ=1077")
        }
        //mute command sent to the radio when the switch state is changed
        mute?.setOnCheckedChangeListener({ _ , isChecked ->
            val message = if (isChecked) sendCommand("AT+MUTE=NO") else sendCommand("AT+MUTE=YES")
        })
    }

    private fun connectDevice(address: String) {

        // Cancel discovery because it's costly and we're about to connect
        m_bluetoothAdapter?.cancelDiscovery()
        val deviceAddress = m_address
        Log.d("connectionDevice", "connect to: " + deviceAddress)
        m_bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
        val device: BluetoothDevice = m_bluetoothAdapter!!.getRemoteDevice(deviceAddress)

        //txtConnected.text = "Conectando..."
        //connectionDot.setImageDrawable(getDrawable(R.drawable.ic_circle_connecting))

        // Attempt to connect to the device
        mChatService?.connect(device, false)
    }

    /**
     * Handler that gets information back from the BluetoothChatService Threads
     */
    private val mHandler: Handler = @SuppressLint("HandlerLeak")
    object : Handler() {
        override fun handleMessage(msg: Message) {

            when (msg.what) {

                Constants.MESSAGE_STATE_CHANGE -> {

                    Log.d("Handler", "State changed")

                    when (msg.arg2) {

                        BluetoothRadioService.STATE_CONNECTED -> {

                            txtConnected.text = "Conectado"
                        }

                        BluetoothRadioService.STATE_CONNECTING -> {
                            txtConnected.text = "Conectando..."
                        }

                        BluetoothRadioService.STATE_LISTEN, BluetoothRadioService.STATE_NONE -> {
                            txtConnected.text = "Desconectado"
                        }
                    }
                }

                Constants.MESSAGE_WRITE -> {
                    val writeBuf = msg.obj as ByteArray
                    // construct a string from the buffer
                    val writeMessage = String(writeBuf)
                    //Toast.makeText(this@MainActivity,"Me: $writeMessage",Toast.LENGTH_SHORT).show()
                    //mConversationArrayAdapter.add("Me:  " + writeMessage)
                    val milliSecondsTime = System.currentTimeMillis()
                    //chatFragment.communicate(com.webianks.bluechat.Message(writeMessage,milliSecondsTime,Constants.MESSAGE_TYPE_SENT))

                }
                Constants.MESSAGE_READ -> {
                    val readBuf = msg.obj as ByteArray
                    // construct a string from the valid bytes in the buffer
                    val readMessage = String(readBuf, 0, msg.arg1)
                    //concat al the messages the radio sends
                    aux = aux + readMessage;
                    val milliSecondsTime = System.currentTimeMillis()

                    //if the radio contains a "!" and ("FREQ or RREQ") it calls the function to print the frequency
                    if(aux.contains("!") && (aux.contains("FREQ") || aux.contains("RREQ"))){
                        ShowFrequency()
                    }


                }
                Constants.MESSAGE_TOAST -> {
                    txtConnected.text = "Desconectado"
                }
                Constants.DEVICE_CONNECTED -> {
                    txtConnected.text = "Conectado"
                    sendCommand("AT+STATUS=?")
                }
                Constants.DEVICE_DISCONNECTED -> {
                    txtConnected.text = "Desconectado"
                }
            }
        }
    }

    //Function to print the frequency sent from the radio
    //the format that my radio sends the frequency is "FREQ = 1033 OK!" or "RREQ = 1033 OK!"
    private fun ShowFrequency(){
        Log.d("aux = ", aux)
        val temp: List<String>
        //split the string accordingly to the frase that follows the frequency
        if(aux.contains("FREQ")){
            temp = aux.split("FREQ")
        }else{
            temp = aux.split("RREQ")
        }
        //once we have "FREQ = 1033 OK!" or "RREQ = 1033 OK!" we split it when a space is found
        val temp2 = temp[1].split(" ")
        Log.d("aux = ", temp2[1])
        //we add a . to the before the last caracter of the frequency since the format is 1033
        //it changes from 1033 to 103.3 Hz
        val freqF = temp2[2]
        val temp3 = temp2[2].toCharArray()
        if(freqF.length == 3){
            txtFreq.text = temp3[0].toString()+temp3[1] + "." + temp3[2] + " Hz"
        }else{
            txtFreq.text = temp3[0].toString()+temp3[1] + temp3[2] + "." + temp3[3] + " Hz"
        }
        aux = ""
    }

    //Function to send commands to our Radio
    private fun sendCommand(message: String) {

        // Check that we're actually connected before trying anything
        if (mChatService?.getState() != BluetoothRadioService.STATE_CONNECTED) {
            Toast.makeText(this, "Desconectado", Toast.LENGTH_SHORT).show();
            return
        }

        // Check that there's actually something to send
        if (message.isNotEmpty()) {
            // Get the message bytes and tell the BluetoothChatService to write
            val send = message.toByteArray()
            mChatService?.write(send)

        }
    }


    //Below you can find an alternative way to connect with a Bluetooth device as a Client.
    //This code is perfect if all you want to do is connect to an Arduino and send commands to it.
    //For example yo can make a bluetooth controlled car ad send the commands to go forward, backward, left or right
    //with the senCommand function described below
    //In this case we are using a AsyncTask class that makes the connection and changes the
    //text on the main activity to connected if so, or disconnected if anything occurs while attempting
    //a connection


    //This function allows you to send commands or just any text to your device
    /*private fun sendCommand(input: String) {
        //Validation of the Bluetooth Socket
        if (m_bluetoothSocket != null) {
            //Try to send the command via an outputstream
            try{
                m_bluetoothSocket!!.outputStream.write(input.toByteArray())
            } catch(e: IOException) {
                //exception if couldn't send message
                e.printStackTrace()
            }
        }
    }*/


    //function to disconnect from the bluetooth device if needed
    /*private fun disconnect() {
        if (m_bluetoothSocket != null) {
            try {
                m_bluetoothSocket!!.close()
                m_bluetoothSocket = null
                m_isConnected = false
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
        finish()
    }*/

    //This is the AsyncTask to connect to Any bluetooth device.
    //In the main Activity above you will have the correct way to call the function to make this method work

    //Declaration of the class as a AsyncTask
    //Note: an AsyncTask is an easy way to manage multiple threads in a Async way. This class
    //will help you manage the thread that will connect to the bluetooth device in the background while your main
    //Activity is still running

    /*private class ConnectToDevice(c: Context) : AsyncTask<Void, Void, String>() {
        public var connectSuccess: Boolean = true
        private val context: Context

        init {
            this.context = c
        }

        //This function is the preparation for the background task that will be execute later,
        //here you must declare any variables, actions, or messages you would like the user to see
        override fun onPreExecute() {
            super.onPreExecute()
            //As I said you can pop a progress dialog to let the user know that the connection is taking
            //place in the background
            //Note: Spanish is the language of my country (Ecuador) you can change the text to your own message
            m_progress = ProgressDialog.show(context, "Conectando...", "Por Favor Espere")
        }

        //This function runs the task i the background, doing any process that you need.
        //You can not change any Widgets from this function, you can only retrieve any data you
        //might need and store it to later send it to your target.
        override fun doInBackground(vararg p0: Void?): String? {
            try {
                if (m_bluetoothSocket == null || !m_isConnected) {
                    m_bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
                    val device: BluetoothDevice = m_bluetoothAdapter!!.getRemoteDevice(m_address)
                    m_bluetoothSocket = device.createInsecureRfcommSocketToServiceRecord(m_myUUID)
                    BluetoothAdapter.getDefaultAdapter().cancelDiscovery()
                    m_bluetoothSocket!!.connect()
                }
            } catch (e: IOException) {
                connectSuccess = false
                e.printStackTrace()
            }
            return null
        }

        //In this function your background task already finished, so you can manipulate any data you got
        //and send it for example to a textview widget in the main activity as I do below
        override fun onPostExecute(result: String?) {
            super.onPostExecute(result)
            if (!connectSuccess) {
                //This line sends a LOG to yor Logcat to help you see whats going in your app while doing any tests
                Log.i("data", "couldn't connect")
                txtConnected.text = "Desconectado"
            } else {
                Log.i("data", "connected")
                txtConnected.text = "Conectado"
                m_isConnected = true
            }
            m_progress.dismiss()
        }
    }*/

}




