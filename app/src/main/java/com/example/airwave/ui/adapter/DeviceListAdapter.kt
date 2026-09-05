package com.example.airwave.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.airwave.R
import com.example.airwave.model.AirWaveUser

class DeviceListAdapter(
    users: List<AirWaveUser> = emptyList(),
    private val onConnectClick: (AirWaveUser) -> Unit
) : ListAdapter<AirWaveUser, DeviceListAdapter.ViewHolder>(DIFF) {

    init {
        submitList(users)
    }

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvAvatar: TextView = view.findViewById(R.id.tvAvatar)
        val tvDeviceName: TextView = view.findViewById(R.id.tvDeviceName)
        val tvDeviceStatus: TextView = view.findViewById(R.id.tvDeviceStatus)
        val btnConnect: View = view.findViewById(R.id.btnConnect)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_device, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val user = getItem(position)
        holder.tvAvatar.text = user.initial.toString()
        holder.tvDeviceName.text = user.name
        holder.btnConnect.setOnClickListener { onConnectClick(user) }
    }

    /** Replaces the backing list and refreshes the list. Called whenever discovery finds new users. */
    fun updateData(newUsers: List<AirWaveUser>) {
        submitList(newUsers)
    }

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<AirWaveUser>() {
            override fun areItemsTheSame(oldItem: AirWaveUser, newItem: AirWaveUser): Boolean {
                // The Bluetooth address is the stable identity of a device.
                return oldItem.address == newItem.address
            }

            override fun areContentsTheSame(oldItem: AirWaveUser, newItem: AirWaveUser): Boolean {
                return oldItem.name == newItem.name && oldItem.address == newItem.address
            }
        }
    }
}