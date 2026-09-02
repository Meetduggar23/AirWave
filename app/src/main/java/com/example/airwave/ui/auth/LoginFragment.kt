package com.example.airwave.ui.auth

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
import com.google.android.material.textfield.TextInputEditText

class LoginFragment : Fragment() {

    private lateinit var etNickname: TextInputEditText
    private lateinit var btnContinue: Button
    private lateinit var btnGuest: Button
    private lateinit var tvSignup: TextView

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_login, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        etNickname = view.findViewById(R.id.etNickname)
        btnContinue = view.findViewById(R.id.btnContinue)
        btnGuest = view.findViewById(R.id.btnGuest)
        tvSignup = view.findViewById(R.id.tvSignup)

        // Pre-fill if returning user
        val savedNickname = PreferencesHelper.nickname
        if (savedNickname.isNotEmpty()) {
            etNickname.setText(savedNickname)
        }

        btnContinue.setOnClickListener {
            val nickname = etNickname.text?.toString()?.trim() ?: ""
            if (nickname.isNotEmpty()) {
                PreferencesHelper.nickname = nickname
                PreferencesHelper.isGuest = false
                PreferencesHelper.onboardingDone = true
                findNavController().navigate(R.id.action_login_to_home)
            } else {
                etNickname.error = getString(R.string.signup_error_empty)
            }
        }

        btnGuest.setOnClickListener {
            PreferencesHelper.nickname = "AirWave User"
            PreferencesHelper.isGuest = true
            PreferencesHelper.onboardingDone = true
            findNavController().navigate(R.id.action_login_to_home)
        }

        tvSignup.setOnClickListener {
            findNavController().navigate(R.id.action_login_to_signup)
        }
    }
}
