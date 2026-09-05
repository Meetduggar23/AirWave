package com.example.airwave.ui.home

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import com.example.airwave.R
import com.example.airwave.bluetooth.BluetoothManager
import com.example.airwave.ui.verify.VerifyBottomSheetFragment
import com.example.airwave.util.PreferencesHelper
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.materialswitch.MaterialSwitch

class HomeFragment : Fragment() {

    private lateinit var tvGreeting: TextView
    private lateinit var tvBluetoothStatus: TextView
    private lateinit var swBluetooth: MaterialSwitch
    private lateinit var tvConnectionStatus: TextView
    private lateinit var tvActiveChat: TextView
    private lateinit var tvIdentityName: TextView
    private lateinit var ivIdentityInitial: TextView
    private lateinit var btnFindUsers: MaterialButton
    private lateinit var btnIdentity: MaterialCardView
    private lateinit var btnSettings: MaterialCardView
    private lateinit var cardActiveChat: MaterialCardView
    private lateinit var bluetoothStatusDot: View
    private lateinit var connectionStatusDot: View

    private val bluetoothManager: BluetoothManager
        get() = BluetoothManager.getInstance(requireContext())

    private var receiverRegistered = false

    /** Guards the switch while the UI is being synced to the real adapter state. */
    private var syncingSwitch = false

    private val connectionObserver = Observer<BluetoothManager.ConnectionState?> {
        updateConnectionUi()
    }

    private val peerObserver = Observer<String?> {
        updateConnectionUi()
    }

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
        tvActiveChat = view.findViewById(R.id.tvActiveChat)
        tvIdentityName = view.findViewById(R.id.tvIdentityName)
        ivIdentityInitial = view.findViewById(R.id.ivIdentityInitial)
        btnFindUsers = view.findViewById(R.id.btnFindUsers)
        btnIdentity = view.findViewById(R.id.btnIdentity)
        btnSettings = view.findViewById(R.id.btnSettings)
        cardActiveChat = view.findViewById(R.id.cardActiveChat)
        bluetoothStatusDot = view.findViewById(R.id.bluetoothStatusDot)
        connectionStatusDot = view.findViewById(R.id.connectionStatusDot)
        swBluetooth = view.findViewById(R.id.swBluetooth)

        swBluetooth.setOnCheckedChangeListener { _, checked ->
            if (!syncingSwitch) handleBluetoothToggle(checked)
        }

        btnFindUsers.setOnClickListener { onFindUsersClicked() }
        btnIdentity.setOnClickListener {
            findNavController().navigate(R.id.action_home_to_profile)
        }
        btnSettings.setOnClickListener {
            findNavController().navigate(R.id.action_home_to_settings)
        }
        cardActiveChat.setOnClickListener {
            val peer = bluetoothManager.peerName.value
            val bundle = Bundle().apply {
                if (!peer.isNullOrBlank()) putString("deviceName", peer)
            }
            findNavController().navigate(R.id.action_home_to_chat, bundle)
        }

        val toolbar = view.findViewById<MaterialToolbar>(R.id.toolbar)
        toolbar.inflateMenu(R.menu.menu_home)
        toolbar.menu.findItem(R.id.action_verify)?.icon?.setTint(
            ContextCompat.getColor(requireContext(), R.color.aw_text_primary)
        )
        toolbar.setOnMenuItemClickListener { item ->
            if (item.itemId == R.id.action_verify) {
                openVerifySheet()
                true
            } else {
                false
            }
        }
    }

    private fun openVerifySheet() {
        VerifyBottomSheetFragment().show(childFragmentManager, VerifyBottomSheetFragment.TAG)
    }

    override fun onResume() {
        super.onResume()
        registerBluetoothStateReceiver()
        updateGreeting()
        updateBluetoothStatus()

        // Keep this device reachable and visible for the whole session.
        bluetoothManager.ensureAirWaveDeviceName()
        bluetoothManager.startListening()
        requestDiscoverabilityOnce()

        bluetoothManager.connectionState.observe(viewLifecycleOwner, connectionObserver)
        bluetoothManager.peerName.observe(viewLifecycleOwner, peerObserver)
        updateConnectionUi()
    }

    override fun onPause() {
        super.onPause()
        unregisterBluetoothStateReceiver()
    }

    private fun updateGreeting() {
        val nickname = PreferencesHelper.nickname.ifBlank { "there" }
        tvGreeting.text = getString(R.string.home_greeting, nickname)
        tvIdentityName.text = nickname
        ivIdentityInitial.text = nickname.trim().firstOrNull()?.uppercaseChar()?.toString() ?: "?"
    }

    private fun updateConnectionUi() {
        val state = bluetoothManager.connectionState.value
        val peer = bluetoothManager.peerName.value
        when (state) {
            BluetoothManager.ConnectionState.CONNECTED -> {
                tvConnectionStatus.text = peer ?: getString(R.string.bluetooth_connected)
                connectionStatusDot.setBackgroundResource(R.drawable.status_dot_connected)
                cardActiveChat.visibility = View.VISIBLE
                tvActiveChat.text = getString(R.string.home_active_chat_with, peer ?: "")
            }
            BluetoothManager.ConnectionState.CONNECTING -> {
                tvConnectionStatus.text = getString(R.string.bluetooth_connecting)
                connectionStatusDot.setBackgroundResource(R.drawable.status_dot_disconnected)
                cardActiveChat.visibility = View.GONE
            }
            else -> {
                tvConnectionStatus.text = getString(R.string.home_not_connected)
                connectionStatusDot.setBackgroundResource(R.drawable.status_dot_disconnected)
                cardActiveChat.visibility = View.GONE
            }
        }
    }

    private fun updateBluetoothStatus() {
        val adapter = btAdapter()
        if (adapter == null) {
            tvBluetoothStatus.text = getString(R.string.bluetooth_unavailable)
            bluetoothStatusDot.setBackgroundResource(R.drawable.status_dot_disconnected)
            setSwitchSynced(false)
            swBluetooth.isEnabled = false
            return
        }
        swBluetooth.isEnabled = true
        val enabled = adapter.isEnabled
        setSwitchSynced(enabled)
        if (enabled) {
            tvBluetoothStatus.text = getString(R.string.bluetooth_on)
            bluetoothStatusDot.setBackgroundResource(R.drawable.status_dot_connected)
        } else {
            tvBluetoothStatus.text = getString(R.string.bluetooth_off)
            bluetoothStatusDot.setBackgroundResource(R.drawable.status_dot_disconnected)
        }
    }

    /** Updates the switch without triggering the toggle handler. */
    private fun setSwitchSynced(checked: Boolean) {
        syncingSwitch = true
        swBluetooth.isChecked = checked
        syncingSwitch = false
    }

    private fun handleBluetoothToggle(checked: Boolean) {
        if (!isAdded) return
        val adapter = btAdapter()
        if (adapter == null) {
            updateBluetoothStatus()
            return
        }
        if (checked) {
            if (!adapter.isEnabled) {
                ensureBluetoothConnectPermissionThenEnable()
            }
        } else if (adapter.isEnabled) {
            // Apps are not allowed to disable Bluetooth directly (deprecated in
            // API 33); route the user to the official system Bluetooth settings.
            openBluetoothSettings()
        }
    }

    private fun ensureBluetoothConnectPermissionThenEnable() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
            ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.BLUETOOTH_CONNECT)
            != PackageManager.PERMISSION_GRANTED
        ) {
            toggleConnectPermissionLauncher.launch(Manifest.permission.BLUETOOTH_CONNECT)
            return
        }
        launchBluetoothEnableRequest()
    }

    private val toggleConnectPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (isAdded && granted) {
            launchBluetoothEnableRequest()
        } else {
            // Permission denied - snap the switch back to the real (off) state.
            updateBluetoothStatus()
        }
    }

    /** Uses Android's official enable request (no hidden APIs, no force-enable). */
    private fun launchBluetoothEnableRequest() {
        if (!isAdded) return
        try {
            toggleEnableBluetoothLauncher.launch(Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE))
        } catch (e: Exception) {
            updateBluetoothStatus()
        }
    }

    /** Apps cannot disable Bluetooth directly; open the official system settings. */
    private fun openBluetoothSettings() {
        if (!isAdded) return
        try {
            startActivity(Intent(Settings.ACTION_BLUETOOTH_SETTINGS))
        } catch (e: Exception) {
            updateBluetoothStatus()
        }
    }

    private val toggleEnableBluetoothLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        // Re-sync with the real adapter state. If the user cancelled the system
        // enable request, the switch snaps back to OFF.
        updateBluetoothStatus()
    }

    private fun btAdapter(): BluetoothAdapter? {
        val btManager = requireContext()
            .getSystemService(Context.BLUETOOTH_SERVICE) as? android.bluetooth.BluetoothManager
        return btManager?.adapter
    }

    private fun registerBluetoothStateReceiver() {
        if (receiverRegistered) return
        val filter = IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED)
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                requireContext().registerReceiver(bluetoothStateReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
            } else {
                requireContext().registerReceiver(bluetoothStateReceiver, filter)
            }
            receiverRegistered = true
        } catch (e: Exception) {
            // Ignore registration failures
        }
    }

    private fun unregisterBluetoothStateReceiver() {
        if (!receiverRegistered) return
        try {
            requireContext().unregisterReceiver(bluetoothStateReceiver)
        } catch (e: Exception) {
            // Ignore
        }
        receiverRegistered = false
    }

    private val bluetoothStateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == BluetoothAdapter.ACTION_STATE_CHANGED) {
                val state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR)
                if (state == BluetoothAdapter.STATE_ON) {
                    bluetoothManager.startListening()
                    bluetoothManager.ensureAirWaveDeviceName()
                    requestDiscoverabilityOnce()
                }
                updateBluetoothStatus()
            }
        }
    }

    private fun onFindUsersClicked() {
        if (!bluetoothManager.isBluetoothAvailable) {
            Toast.makeText(requireContext(), R.string.bluetooth_unavailable, Toast.LENGTH_SHORT).show()
            return
        }
        if (!bluetoothManager.isBluetoothEnabled) {
            enableBluetoothLauncher.launch(Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE))
            return
        }
        proceedToNearby()
    }

    /** Ensures scan permissions are granted, then opens the Nearby Users screen. */
    private fun proceedToNearby() {
        if (hasScanPermission()) {
            findNavController().navigate(R.id.action_home_to_nearby)
        } else {
            permissionLauncher.launch(scanPermissions())
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
        val needed = scanPermissions()
        return needed.all {
            ContextCompat.checkSelfPermission(requireContext(), it) == PackageManager.PERMISSION_GRANTED
        }
    }

    private val enableBluetoothLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        updateBluetoothStatus()
        // The user enabled Bluetooth to find nearby people - take them straight there.
        if (result.resultCode == android.app.Activity.RESULT_OK && isAdded) {
            proceedToNearby()
        }
    }

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) {
        if (isAdded && hasScanPermission()) {
            findNavController().navigate(R.id.action_home_to_nearby)
        }
    }

    private val discoverableLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        // The user may have declined; the app keeps working either way.
    }

    private fun requestDiscoverabilityOnce() {
        if (!PreferencesHelper.discoverable) return
        if (discoverableRequestedThisRun) return
        val adapter = btAdapter() ?: return
        if (!adapter.isEnabled) return
        // Becoming discoverable needs BLUETOOTH_ADVERTISE on Android 12+.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
            ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.BLUETOOTH_ADVERTISE)
            != PackageManager.PERMISSION_GRANTED
        ) {
            discoverablePermissionLauncher.launch(Manifest.permission.BLUETOOTH_ADVERTISE)
            return
        }
        launchDiscoverableRequest()
    }

    private val discoverablePermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted && isAdded) launchDiscoverableRequest()
    }

    private fun launchDiscoverableRequest() {
        if (discoverableRequestedThisRun) return
        discoverableRequestedThisRun = true
        try {
            val intent = Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE).apply {
                putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300)
            }
            discoverableLauncher.launch(intent)
        } catch (e: Exception) {
            // Some devices do not allow the request; ignore.
        }
    }

    // Per-fragment flag so a denial can be retried after re-entering the screen
    // (a companion/static flag would permanently block re-asking in one process).
    private var discoverableRequestedThisRun = false
}
