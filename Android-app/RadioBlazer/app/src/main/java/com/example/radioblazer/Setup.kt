package com.example.radioblazer

import android.os.Bundle
import com.google.android.material.snackbar.Snackbar
import androidx.appcompat.app.AppCompatActivity;
import android.bluetooth.*
import android.content.Intent
import android.content.DialogInterface
import androidx.appcompat.app.AlertDialog
import android.util.Log
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.ListView
import android.content.Context
import android.view.LayoutInflater
import android.annotation.SuppressLint
import android.view.View
import android.view.ViewGroup

import kotlinx.android.synthetic.main.activity_setup.*

val MY_UUID = ""
val bluetoothAdapter: BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter()

class Setup : AppCompatActivity() {

    private val REQUEST_ENABLE_BT = 1

    companion object {
        val EXTRA_ADDRESS: String = "Device_address"
    }

    private val deviceItemList = ArrayList<BluetoothDevice>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_setup)
        //setSupportActionBar(toolbar)
        getActionBar()?.hide()

        fab.setOnClickListener { view ->
            Snackbar.make(view, "Desarrollado por Esteban Molina. Todos los Derechos Reservados.", Snackbar.LENGTH_LONG)
                .setAction("Action", null).show()
        }

        val bluetoothAdapter: BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter()
        if (bluetoothAdapter == null) {
            AlertDialog.Builder(this)
                .setTitle("Not compatible")
                .setMessage("Your phone does not support Bluetooth")
                .setPositiveButton("Exit", DialogInterface.OnClickListener { dialog, which -> System.exit(0) })
                .setIcon(android.R.drawable.ic_dialog_alert)
                .show()
        }else{
            if (bluetoothAdapter.isEnabled == false) {
                val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
                startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT)
            }else{
                Log.d("DEVICELIST", "Super called for DeviceListFragment onCreate\n")

                //Prueba de listview



                //Fin de prueba de listview
                val listView = findViewById<ListView>(R.id.bluetoothDevices)
                val pairedDevices: Set<BluetoothDevice>? = bluetoothAdapter?.bondedDevices
                pairedDevices?.forEach { device ->
                    //val deviceName = device.name
                    //val deviceHardwareAddress = device.address // MAC address
                    deviceItemList.add(device)
                }
                val adapter = BluetoothListViewAdapter(this, deviceItemList)
                //val adapter = ArrayAdapter<BluetoothDevice>(this, android.R.layout.simple_list_item_2, deviceItemList)
                listView.adapter = adapter
                listView.onItemClickListener = AdapterView.OnItemClickListener { parent, view, i, id ->
                    val bluetoothDevice: BluetoothDevice = deviceItemList[i]
                    val address: String = bluetoothDevice.address
                    val intent = Intent(this, MainActivity::class.java)
                    intent.putExtra(EXTRA_ADDRESS, address)
                    startActivity(intent)
                }

            }
        }


    }
}