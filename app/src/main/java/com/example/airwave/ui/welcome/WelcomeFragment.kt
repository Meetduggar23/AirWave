package com.example.airwave.ui.welcome

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.airwave.R
import com.example.airwave.bluetooth.BluetoothManager
import com.example.airwave.util.PreferencesHelper
import com.google.android.material.textfield.TextInputEditText

/**
 * The one and only entry screen. AirWave never asks for an email, password,
 * phone number or account - just a nickname for the current session.
 */
class WelcomeFragment : Fragment() {

    private lateinit var etName: TextInputEditText

    private var isEditMode = false
    private var pendingNickname: String? = null

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        // Proceed regardless; missing permissions are handled where they are needed.
        if (isEditMode) {
            findNavController().popBackStack()
        } else {
            goToHome()
        }
    }

    private val enableBluetoothLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        if (!bluetoothManager().isBluetoothEnabled) {
            Toast.makeText(requireContext(), R.string.error_bluetooth_disabled, Toast.LENGTH_SHORT).show()
        }
        requestRuntimePermissions()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_welcome, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        isEditMode = arguments?.getBoolean("editMode", false) ?: false
        etName = view.findViewById(R.id.etName)

        // Remembered name is a convenience only - it is not an account.
        if (isEditMode) {
            etName.setText(PreferencesHelper.nickname)
            etName.setSelection(etName.text?.length ?: 0)
        }

        view.findViewById<View>(R.id.btnContinue).setOnClickListener {
            onContinueClicked()
        }
    }

    private fun bluetoothManager(): BluetoothManager =
        BluetoothManager.getInstance(requireContext())

    private fun onContinueClicked() {
        val nickname = etName.text?.toString()?.trim().orEmpty()
        when {
            nickname.isEmpty() -> {
                etName.error = getString(R.string.welcome_error_empty)
            }
            nickname.length < 2 || nickname.length > 20 -> {
                etName.error = getString(R.string.welcome_error_length)
            }
            else -> {
                pendingNickname = nickname
                PreferencesHelper.nickname = nickname
                bluetoothManager().ensureAirWaveDeviceName()

                if (!bluetoothManager().isBluetoothAvailable) {
                    Toast.makeText(requireContext(), R.string.bluetooth_unavailable, Toast.LENGTH_SHORT).show()
                    return
                }
                if (!bluetoothManager().isBluetoothEnabled) {
                    enableBluetoothLauncher.launch(Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE))
                    return
                }
                requestRuntimePermissions()
            }
        }
    }

    private fun requestRuntimePermissions() {
        val needed = mutableListOf<String>()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.BLUETOOTH_CONNECT)
                != PackageManager.PERMISSION_GRANTED
            ) {
                needed.add(Manifest.permission.BLUETOOTH_CONNECT)
            }
            if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.BLUETOOTH_SCAN)
                != PackageManager.PERMISSION_GRANTED
            ) {
                needed.add(Manifest.permission.BLUETOOTH_SCAN)
            }
            if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.BLUETOOTH_ADVERTISE)
                != PackageManager.PERMISSION_GRANTED
            ) {
                needed.add(Manifest.permission.BLUETOOTH_ADVERTISE)
            }
        } else {
            if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED
            ) {
                needed.add(Manifest.permission.ACCESS_FINE_LOCATION)
            }
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED
        ) {
            needed.add(Manifest.permission.POST_NOTIFICATIONS)
        }

        if (needed.isEmpty()) {
            if (isEditMode) {
                findNavController().popBackStack()
            } else {
                goToHome()
            }
        } else {
            permissionLauncher.launch(needed.toTypedArray())
        }
    }

    private fun goToHome() {
        try {
            findNavController().navigate(R.id.action_welcome_to_home)
        } catch (e: Exception) {
            // Should not happen; ignore
        }
    }
}
