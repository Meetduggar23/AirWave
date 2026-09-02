package com.example.airwave.ui.profile

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.airwave.R
import com.example.airwave.util.PreferencesHelper

class ProfileFragment : Fragment() {

    private lateinit var ivProfile: ImageView
    private lateinit var tvNickname: TextView
    private lateinit var tvStatus: TextView
    private lateinit var tvIdentity: TextView
    private lateinit var btnEdit: Button

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_profile, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        ivProfile = view.findViewById(R.id.ivProfile)
        tvNickname = view.findViewById(R.id.tvNickname)
        tvStatus = view.findViewById(R.id.tvStatus)
        tvIdentity = view.findViewById(R.id.tvIdentity)
        btnEdit = view.findViewById(R.id.btnEdit)

        loadProfile()

        btnEdit.setOnClickListener {
            findNavController().navigate(R.id.action_profile_to_edit)
        }
    }

    override fun onResume() {
        super.onResume()
        loadProfile()
    }

    private fun loadProfile() {
        tvNickname.text = PreferencesHelper.nickname.ifEmpty { "AirWave User" }
        tvStatus.text = PreferencesHelper.status
        val btManager = requireContext().getSystemService(android.content.Context.BLUETOOTH_SERVICE) as? android.bluetooth.BluetoothManager
        val address = btManager?.adapter?.address?.takeLast(8) ?: "Unknown"
        tvIdentity.text = "AirWave ID: $address"
    }
}
