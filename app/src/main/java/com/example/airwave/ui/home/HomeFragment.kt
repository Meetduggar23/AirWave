package com.example.airwave.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.airwave.R
import com.example.airwave.util.PreferencesHelper
import com.google.android.material.card.MaterialCardView

class HomeFragment : Fragment() {

    private lateinit var tvGreeting: TextView
    private lateinit var tvBluetoothStatus: TextView
    private lateinit var tvConnectionStatus: TextView
    private lateinit var btnFindUsers: Button
    private lateinit var btnChats: MaterialCardView
    private lateinit var btnProfile: MaterialCardView
    private lateinit var btnSettings: MaterialCardView
    private lateinit var bluetoothStatusDot: View
    private lateinit var connectionStatusDot: View

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_home, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        tvGreeting = view.findViewById(R.id.tvGreeting)
        tvBluetoothStatus = view.findViewById(R.id.tvBluetoothStatus)
        tvConnectionStatus = view.findViewById(R.id.tvConnectionStatus)
        btnFindUsers = view.findViewById(R.id.btnFindUsers)
        btnChats = view.findViewById(R.id.btnChats)
        btnProfile = view.findViewById(R.id.btnProfile)
        btnSettings = view.findViewById(R.id.btnSettings)
        bluetoothStatusDot = view.findViewById(R.id.bluetoothStatusDot)
        connectionStatusDot = view.findViewById(R.id.connectionStatusDot)

        btnFindUsers.setOnClickListener {
            findNavController().navigate(R.id.action_home_to_nearby)
        }
        btnChats.setOnClickListener {
            findNavController().navigate(R.id.action_home_to_chat_history)
        }
        btnProfile.setOnClickListener {
            findNavController().navigate(R.id.action_home_to_profile)
        }
        btnSettings.setOnClickListener {
            findNavController().navigate(R.id.action_home_to_settings)
        }

        updateUI()
    }

    override fun onResume() {
        super.onResume()
        updateUI()
    }

    private fun updateUI() {
        val nickname = PreferencesHelper.nickname
        tvGreeting.text = getString(R.string.home_greeting, nickname)
        updateBluetoothStatus()
    }

    private fun updateBluetoothStatus() {
        val btManager = requireContext().getSystemService(android.content.Context.BLUETOOTH_SERVICE) as? android.bluetooth.BluetoothManager
        val btAdapter = btManager?.adapter
        val isEnabled = btAdapter?.isEnabled == true

        if (isEnabled) {
            tvBluetoothStatus.text = getString(R.string.bluetooth_on)
            bluetoothStatusDot.setBackgroundResource(R.drawable.status_dot_connected)
        } else {
            tvBluetoothStatus.text = getString(R.string.bluetooth_off)
            bluetoothStatusDot.setBackgroundResource(R.drawable.status_dot_disconnected)
        }

        btnFindUsers.isEnabled = isEnabled
    }
}
