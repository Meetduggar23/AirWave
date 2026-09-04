package com.example.airwave.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.airwave.R
import com.example.airwave.model.AirWaveUser

class DeviceListAdapter(
    users: List<AirWaveUser> = emptyList(),
    private val onConnectClick: (AirWaveUser) -> Unit
) : RecyclerView.Adapter<DeviceListAdapter.ViewHolder>() {

    private val items: MutableList<AirWaveUser> = ArrayList(users)

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
        val user = items[position]
        holder.tvAvatar.text = user.initial.toString()
        holder.tvDeviceName.text = user.name
        holder.btnConnect.setOnClickListener { onConnectClick(user) }
    }

    override fun getItemCount() = items.size

    /** Replaces the backing list and refreshes the list. Called whenever discovery finds new users. */
    fun updateData(newUsers: List<AirWaveUser>) {
        items.clear()
        items.addAll(newUsers)
        notifyDataSetChanged()
    }
}
