package com.example.airwave.ui.splash

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.airwave.R
import com.example.airwave.util.PreferencesHelper

class SplashFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_splash, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Handler(Looper.getMainLooper()).postDelayed({
            if (!isAdded) return@postDelayed
            navigate()
        }, 1200)
    }

    private fun navigate() {
        try {
            val dest = if (PreferencesHelper.nickname.isBlank()) {
                R.id.action_splash_to_welcome
            } else {
                R.id.action_splash_to_home
            }
            findNavController().navigate(dest)
        } catch (e: Exception) {
            // Navigation might fail if the view is destroyed
        }
    }
}
