package com.example.airwave.ui.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatDelegate
import androidx.fragment.app.Fragment
import androidx.navigation.NavOptions
import androidx.navigation.fragment.findNavController
import com.example.airwave.R
import com.example.airwave.bluetooth.BluetoothManager
import com.example.airwave.util.LanguageHelper
import com.example.airwave.util.PreferencesHelper
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.switchmaterial.SwitchMaterial

class SettingsFragment : Fragment() {

    private lateinit var layoutTheme: LinearLayout
    private lateinit var tvThemeValue: TextView
    private lateinit var layoutLanguage: LinearLayout
    private lateinit var tvLanguageValue: TextView
    private lateinit var switchNotifications: SwitchMaterial
    private lateinit var switchDiscoverable: SwitchMaterial
    private lateinit var layoutSessionOnly: LinearLayout
    private lateinit var layoutClearData: LinearLayout
    private lateinit var layoutAbout: LinearLayout

    private val bluetoothManager: BluetoothManager
        get() = BluetoothManager.getInstance(requireContext())

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_settings, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        layoutTheme = view.findViewById(R.id.layoutTheme)
        tvThemeValue = view.findViewById(R.id.tvThemeValue)
        layoutLanguage = view.findViewById(R.id.layoutLanguage)
        tvLanguageValue = view.findViewById(R.id.tvLanguageValue)
        switchNotifications = view.findViewById(R.id.switchNotifications)
        switchDiscoverable = view.findViewById(R.id.switchDiscoverable)
        layoutSessionOnly = view.findViewById(R.id.layoutSessionOnly)
        layoutClearData = view.findViewById(R.id.layoutClearData)
        layoutAbout = view.findViewById(R.id.layoutAbout)

        view.findViewById<MaterialToolbar>(R.id.toolbar).setNavigationOnClickListener {
            findNavController().popBackStack()
        }

        loadSettings()

        layoutTheme.setOnClickListener { showThemeDialog() }
        layoutLanguage.setOnClickListener { showLanguageDialog() }

        switchNotifications.setOnCheckedChangeListener { _, isChecked ->
            PreferencesHelper.notificationsEnabled = isChecked
        }

        switchDiscoverable.setOnCheckedChangeListener { _, isChecked ->
            PreferencesHelper.discoverable = isChecked
        }

        layoutSessionOnly.setOnClickListener {
            AlertDialog.Builder(requireContext())
                .setTitle(R.string.privacy_session_only)
                .setMessage(R.string.privacy_session_only_message)
                .setPositiveButton(R.string.ok, null)
                .show()
        }

        layoutClearData.setOnClickListener {
            AlertDialog.Builder(requireContext())
                .setTitle(R.string.privacy_clear_session)
                .setMessage(R.string.privacy_clear_session_confirm)
                .setPositiveButton(R.string.yes) { _, _ -> clearSession() }
                .setNegativeButton(R.string.no, null)
                .show()
        }

        layoutAbout.setOnClickListener {
            findNavController().navigate(R.id.action_settings_to_about)
        }
    }

    private fun clearSession() {
        bluetoothManager.disconnect()
        PreferencesHelper.clearNickname()
        Toast.makeText(context, R.string.settings_data_cleared, Toast.LENGTH_SHORT).show()
        val navOptions = NavOptions.Builder()
            .setPopUpTo(R.id.homeFragment, true)
            .build()
        val bundle = Bundle().apply { putBoolean("editMode", false) }
        findNavController().navigate(R.id.welcomeFragment, bundle, navOptions)
    }

    private fun loadSettings() {
        tvThemeValue.text = when (PreferencesHelper.themeMode) {
            0 -> getString(R.string.theme_light)
            1 -> getString(R.string.theme_dark)
            else -> getString(R.string.theme_system)
        }
        tvLanguageValue.text = LanguageHelper.getLanguageName(PreferencesHelper.language)
        switchNotifications.isChecked = PreferencesHelper.notificationsEnabled
        switchDiscoverable.isChecked = PreferencesHelper.discoverable
    }

    private fun showThemeDialog() {
        val themes = arrayOf(getString(R.string.theme_light), getString(R.string.theme_dark), getString(R.string.theme_system))
        AlertDialog.Builder(requireContext())
            .setTitle(R.string.settings_theme)
            .setItems(themes) { _, which ->
                PreferencesHelper.themeMode = which
                applyTheme(which)
                tvThemeValue.text = themes[which]
            }
            .show()
    }

    private fun applyTheme(mode: Int) {
        when (mode) {
            0 -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
            1 -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
            else -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
        }
    }

    private fun showLanguageDialog() {
        val languages = LanguageHelper.getAvailableLanguages()
        val names = languages.map { it.second }.toTypedArray()
        AlertDialog.Builder(requireContext())
            .setTitle(R.string.settings_language)
            .setItems(names) { _, which ->
                PreferencesHelper.language = languages[which].first
                tvLanguageValue.text = names[which]
                LanguageHelper.setLocale(requireContext(), languages[which].first)
                requireActivity().recreate()
            }
            .show()
    }
}
