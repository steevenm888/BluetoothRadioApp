package com.example.radioblazer

import android.app.Activity
import android.widget.BaseAdapter
import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.view.ViewGroup
import android.view.View
import android.view.LayoutInflater
import android.content.Context
import android.widget.TextView
import android.widget.TwoLineListItem
import androidx.core.content.ContextCompat.getSystemService




class BluetoothListViewAdapter(private val activity: Activity, bluetoothList: List<BluetoothDevice>) : BaseAdapter() {

    private var bluetoothList = ArrayList<BluetoothDevice>()

    init {
        this.bluetoothList = bluetoothList as ArrayList<BluetoothDevice>
    }

    override fun getCount(): Int {
        return bluetoothList.size
    }

    override fun getItem(i: Int): Any {
        return bluetoothList[i]
    }

    override fun getItemId(i: Int): Long {
        return i.toLong()
    }

    @SuppressLint("InflateParams", "ViewHolder")
    override fun getView(i: Int, convertView: View?, viewGroup: ViewGroup): View {
        val twoLineListItem: TwoLineListItem
        if (convertView == null) {
            val inflater = activity
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
            twoLineListItem = inflater.inflate(
                android.R.layout.simple_list_item_2, null
            ) as TwoLineListItem
        } else {
            twoLineListItem = convertView as TwoLineListItem
        }
        val text1 = twoLineListItem.text1
        val text2 = twoLineListItem.text2

        text1.setText(bluetoothList.get(i).name)
        text2.text = "" + bluetoothList.get(i).address

        return twoLineListItem
        /*var vi: View
        val inflater = activity.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        vi = inflater.inflate(R.layout.listviewitem, null)
        val name = vi.findViewById(R.id.txtName) as TextView
        val address = vi.findViewById(R.id.txtAddress) as TextView
        name.text = bluetoothList[i].name
        address.text = bluetoothList[i].address
        return vi*/
    }
}