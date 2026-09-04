package com.example.airwave.ui.nearby

import android.Manifest
import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.airwave.R
import com.example.airwave.bluetooth.BluetoothManager
import com.example.airwave.model.AirWaveUser
import com.example.airwave.ui.adapter.DeviceListAdapter
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton

class NearbyDevicesFragment : Fragment() {

    private lateinit var rvUsers: RecyclerView
    private lateinit var progressBar: ProgressBar
    private lateinit var tvStatus: TextView
    private lateinit var tvEmpty: TextView
    private lateinit var btnScan: MaterialButton
    private lateinit var layoutEmpty: LinearLayout

    private lateinit var adapter: DeviceListAdapter

    private val bluetoothManager: BluetoothManager
        get() = BluetoothManager.getInstance(requireContext())

    private val usersObserver = Observer<List<AirWaveUser>> { users ->
        if (!isAdded) return@Observer
        adapter.updateData(users)
        updateEmptyState(users)
    }

    private val scanningObserver = Observer<Boolean> { scanning ->
        if (!isAdded) return@Observer
        if (scanning == true) {
            progressBar.visibility = View.VISIBLE
            tvStatus.text = getString(R.string.nearby_scanning)
            btnScan.text = getString(R.string.nearby_stop)
        } else {
            progressBar.visibility = View.GONE
            btnScan.text = getString(R.string.nearby_scan_again)
            tvStatus.text = ""
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_nearby_devices, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        rvUsers = view.findViewById(R.id.rvDevices)
        progressBar = view.findViewById(R.id.progressBar)
        tvStatus = view.findViewById(R.id.tvStatus)
        tvEmpty = view.findViewById(R.id.tvEmpty)
        btnScan = view.findViewById(R.id.btnScan)
        layoutEmpty = view.findViewById(R.id.layoutEmpty)

        adapter = DeviceListAdapter(emptyList()) { user ->
            showConnectPreview(user)
        }
        rvUsers.layoutManager = LinearLayoutManager(requireContext())
        rvUsers.adapter = adapter

        view.findViewById<MaterialToolbar>(R.id.toolbar).setNavigationOnClickListener {
            findNavController().popBackStack()
        }

        btnScan.setOnClickListener {
            if (bluetoothManager.isScanning.value == true) {
                bluetoothManager.stopDiscovery()
            } else {
                startScanning()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        bluetoothManager.discoveredUsers.observe(viewLifecycleOwner, usersObserver)
        bluetoothManager.isScanning.observe(viewLifecycleOwner, scanningObserver)
        adapter.updateData(bluetoothManager.discoveredUsers.value.orEmpty())
        updateEmptyState(bluetoothManager.discoveredUsers.value.orEmpty())

        if (bluetoothManager.isScanning.value != true) {
            startScanning()
        }
    }

    override fun onPause() {
        super.onPause()
        bluetoothManager.stopDiscovery()
    }

    private fun startScanning() {
        if (!bluetoothManager.isBluetoothEnabled) {
            enableBluetoothLauncher.launch(Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE))
            return
        }
        if (!hasScanPermission()) {
            permissionLauncher.launch(scanPermissions())
            return
        }
        bluetoothManager.startDiscovery()
    }

    private val enableBluetoothLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (!isAdded) return@registerForActivityResult
        if (result.resultCode == Activity.RESULT_OK) {
            startScanning()
        } else {
            tvStatus.text = getString(R.string.bluetooth_off)
        }
    }

    private fun scanPermissions(): Array<String> {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            // ADVERTISE is needed to make this device discoverable by others.
            arrayOf(
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.BLUETOOTH_CONNECT,
                Manifest.permission.BLUETOOTH_ADVERTISE
            )
        } else {
            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    private fun hasScanPermission(): Boolean {
        return scanPermissions().all {
            ContextCompat.checkSelfPermission(requireContext(), it) == PackageManager.PERMISSION_GRANTED
        }
    }

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) {
        if (isAdded && hasScanPermission()) {
            bluetoothManager.startDiscovery()
        }
    }

    private fun updateEmptyState(users: List<AirWaveUser>) {
        if (users.isEmpty()) {
            layoutEmpty.visibility = View.VISIBLE
            rvUsers.visibility = View.GONE
            tvEmpty.text = getString(R.string.nearby_no_devices)
        } else {
            layoutEmpty.visibility = View.GONE
            rvUsers.visibility = View.VISIBLE
        }
    }

    /** Preview step: confirm before a connection is attempted. */
    private fun showConnectPreview(user: AirWaveUser) {
        if (bluetoothManager.isConnected) {
            AlertDialog.Builder(requireContext())
                .setMessage(R.string.nearby_busy)
                .setPositiveButton(R.string.ok, null)
                .show()
            return
        }
        AlertDialog.Builder(requireContext())
            .setTitle(user.name)
            .setMessage(getString(R.string.nearby_connect_confirm, user.name))
            .setPositiveButton(R.string.nearby_connect) { _, _ ->
                bluetoothManager.stopDiscovery()
                val bundle = Bundle().apply {
                    putString("deviceAddress", user.address)
                    putString("deviceName", user.name)
                }
                findNavController().navigate(R.id.action_nearby_to_chat, bundle)
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }
}
