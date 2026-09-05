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

    // Held as fields so the delayed navigation can be cancelled when the
    // fragment is destroyed (otherwise the Runnable keeps the fragment alive
    // until the 1200ms delay fires).
    private val splashHandler = Handler(Looper.getMainLooper())
    private val splashRunnable = Runnable {
        if (!isAdded) return@Runnable
        navigate()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_splash, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        splashHandler.postDelayed(splashRunnable, 1200)
    }

    override fun onDestroyView() {
        splashHandler.removeCallbacks(splashRunnable)
        super.onDestroyView()
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
