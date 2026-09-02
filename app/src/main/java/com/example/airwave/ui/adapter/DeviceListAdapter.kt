package com.example.airwave.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.airwave.R
import com.example.airwave.bluetooth.BluetoothManager

class DeviceListAdapter(
    private val devices: List<BluetoothManager.AirWaveDevice>,
    private val onConnectClick: (BluetoothManager.AirWaveDevice) -> Unit
) : RecyclerView.Adapter<DeviceListAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvDeviceName: TextView = view.findViewById(R.id.tvDeviceName)
        val tvDeviceAddress: TextView = view.findViewById(R.id.tvDeviceAddress)
        val btnConnect: Button = view.findViewById(R.id.btnConnect)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_device, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val device = devices[position]
        holder.tvDeviceName.text = device.name
        holder.tvDeviceAddress.text = device.address
        holder.btnConnect.setOnClickListener {
            onConnectClick(device)
        }
    }

    override fun getItemCount() = devices.size
}
