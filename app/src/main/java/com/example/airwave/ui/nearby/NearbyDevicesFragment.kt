package com.example.airwave.ui.nearby

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.airwave.R
import com.example.airwave.bluetooth.BluetoothManager
import com.example.airwave.ui.adapter.DeviceListAdapter

class NearbyDevicesFragment : Fragment() {

    private lateinit var bluetoothManager: BluetoothManager
    private lateinit var rvDevices: RecyclerView
    private lateinit var progressBar: ProgressBar
    private lateinit var tvStatus: TextView
    private lateinit var tvEmpty: TextView
    private lateinit var btnScan: Button
    private lateinit var layoutEmpty: LinearLayout

    private val devices = mutableListOf<BluetoothManager.AirWaveDevice>()
    private lateinit var adapter: DeviceListAdapter
    private val handler = Handler(Looper.getMainLooper())
    private val SCAN_DURATION = 15000L

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_nearby_devices, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        bluetoothManager = BluetoothManager(requireContext().applicationContext)

        rvDevices = view.findViewById(R.id.rvDevices)
        progressBar = view.findViewById(R.id.progressBar)
        tvStatus = view.findViewById(R.id.tvStatus)
        tvEmpty = view.findViewById(R.id.tvEmpty)
        btnScan = view.findViewById(R.id.btnScan)
        layoutEmpty = view.findViewById(R.id.layoutEmpty)

        adapter = DeviceListAdapter(devices) { device ->
            bluetoothManager.stopDiscovery()
            val bundle = Bundle().apply {
                putString("deviceAddress", device.address)
                putString("deviceName", device.name)
            }
            findNavController().navigate(R.id.action_nearby_to_chat, bundle)
        }

        rvDevices.layoutManager = LinearLayoutManager(requireContext())
        rvDevices.adapter = adapter

        bluetoothManager.onDeviceFound = { device ->
            handler.post {
                if (devices.none { it.address == device.address }) {
                    devices.add(device)
                    adapter.notifyItemInserted(devices.size - 1)
                    updateEmptyState()
                }
            }
        }

        bluetoothManager.onDiscoveryStarted = {
            handler.post {
                progressBar.visibility = View.VISIBLE
                tvStatus.text = getString(R.string.nearby_scanning)
                btnScan.text = getString(R.string.nearby_stop)
                layoutEmpty.visibility = View.GONE
                rvDevices.visibility = View.VISIBLE
            }
        }

        bluetoothManager.onDiscoveryFinished = {
            handler.post {
                progressBar.visibility = View.GONE
                btnScan.text = getString(R.string.nearby_scan_again)
                if (devices.isEmpty()) {
                    layoutEmpty.visibility = View.VISIBLE
                    rvDevices.visibility = View.GONE
                    tvEmpty.text = getString(R.string.nearby_no_devices)
                }
            }
        }

        btnScan.setOnClickListener {
            if (bluetoothManager.isBluetoothEnabled) {
                startScanning()
            }
        }

        startScanning()
    }

    @SuppressLint("MissingPermission")
    private fun startScanning() {
        devices.clear()
        adapter.notifyDataSetChanged()
        bluetoothManager.startDiscovery()
        handler.postDelayed({
            bluetoothManager.stopDiscovery()
        }, SCAN_DURATION)
    }

    private fun updateEmptyState() {
        if (devices.isEmpty()) {
            layoutEmpty.visibility = View.VISIBLE
            rvDevices.visibility = View.GONE
        } else {
            layoutEmpty.visibility = View.GONE
            rvDevices.visibility = View.VISIBLE
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        bluetoothManager.stopDiscovery()
        handler.removeCallbacksAndMessages(null)
    }
}
