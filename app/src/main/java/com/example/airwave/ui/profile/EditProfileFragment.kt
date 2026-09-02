package com.example.airwave.ui.profile

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.airwave.R
import com.example.airwave.util.PreferencesHelper
import com.google.android.material.textfield.TextInputEditText

class EditProfileFragment : Fragment() {

    private lateinit var etNickname: TextInputEditText
    private lateinit var etStatus: TextInputEditText
    private lateinit var btnSave: Button

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_edit_profile, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        etNickname = view.findViewById(R.id.etNickname)
        etStatus = view.findViewById(R.id.etStatus)
        btnSave = view.findViewById(R.id.btnSave)

        etNickname.setText(PreferencesHelper.nickname)
        etStatus.setText(PreferencesHelper.status)

        btnSave.setOnClickListener {
            val nickname = etNickname.text?.toString()?.trim() ?: ""
            val status = etStatus.text?.toString()?.trim() ?: "Available nearby"

            when {
                nickname.isEmpty() -> {
                    etNickname.error = getString(R.string.signup_error_empty)
                }
                nickname.length < 2 || nickname.length > 20 -> {
                    etNickname.error = getString(R.string.signup_error_length)
                }
                else -> {
                    PreferencesHelper.nickname = nickname
                    PreferencesHelper.status = status.ifEmpty { "Available nearby" }
                    Toast.makeText(context, R.string.profile_save, Toast.LENGTH_SHORT).show()
                    findNavController().popBackStack()
                }
            }
        }
    }
}
