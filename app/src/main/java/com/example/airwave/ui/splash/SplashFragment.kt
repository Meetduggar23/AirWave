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
        }, 2000)
    }

    private fun navigate() {
        try {
            when {
                !PreferencesHelper.onboardingDone -> {
                    findNavController().navigate(R.id.action_splash_to_onboarding)
                }
                PreferencesHelper.nickname.isEmpty() && PreferencesHelper.isGuest -> {
                    findNavController().navigate(R.id.action_splash_to_login)
                }
                else -> {
                    findNavController().navigate(R.id.action_splash_to_home)
                }
            }
        } catch (e: Exception) {
            // Navigation might fail if view is destroyed
        }
    }
}
