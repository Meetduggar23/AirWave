package com.example.airwave.ui.profile

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.airwave.R
import com.example.airwave.bluetooth.BluetoothManager
import com.example.airwave.util.PreferencesHelper
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton

/**
 * "Your AirWave Identity" - there is no account, only the nickname used for
 * the current session and a locally remembered convenience copy.
 */
class ProfileFragment : Fragment() {

    private lateinit var tvNickname: TextView
    private lateinit var tvAvatar: TextView
    private lateinit var tvStatus: TextView

    private val bluetoothManager: BluetoothManager
        get() = BluetoothManager.getInstance(requireContext())

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_profile, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        tvNickname = view.findViewById(R.id.tvNickname)
        tvAvatar = view.findViewById(R.id.ivAvatar)
        tvStatus = view.findViewById(R.id.tvStatus)

        view.findViewById<MaterialToolbar>(R.id.toolbar).setNavigationOnClickListener {
            findNavController().popBackStack()
        }

        view.findViewById<MaterialButton>(R.id.btnChangeName).setOnClickListener {
            findNavController().navigate(R.id.action_profile_to_welcome)
        }
    }

    override fun onResume() {
        super.onResume()
        loadIdentity()
    }

    private fun loadIdentity() {
        val nickname = PreferencesHelper.nickname
        tvNickname.text = nickname
        tvAvatar.text = nickname.trim().firstOrNull()?.uppercaseChar()?.toString() ?: "?"

        // Session status: whether this device is currently open for requests.
        val state = bluetoothManager.connectionState.value
        tvStatus.text = if (state == BluetoothManager.ConnectionState.CONNECTED) {
            getString(R.string.profile_in_chat)
        } else {
            getString(R.string.profile_available)
        }
    }
}
