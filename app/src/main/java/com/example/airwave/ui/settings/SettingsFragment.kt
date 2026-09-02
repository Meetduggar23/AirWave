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
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.airwave.R
import com.example.airwave.data.local.DatabaseHelper
import com.example.airwave.util.LanguageHelper
import com.example.airwave.util.PreferencesHelper
import com.google.android.material.switchmaterial.SwitchMaterial
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SettingsFragment : Fragment() {

    private lateinit var layoutTheme: LinearLayout
    private lateinit var tvThemeValue: TextView
    private lateinit var layoutAccent: LinearLayout
    private lateinit var tvAccentValue: TextView
    private lateinit var layoutContrast: LinearLayout
    private lateinit var tvContrastValue: TextView
    private lateinit var layoutTextSize: LinearLayout
    private lateinit var tvTextSizeValue: TextView
    private lateinit var layoutLanguage: LinearLayout
    private lateinit var tvLanguageValue: TextView
    private lateinit var switchNotifications: SwitchMaterial
    private lateinit var switchDiscoverable: SwitchMaterial
    private lateinit var layoutClearChats: LinearLayout
    private lateinit var layoutClearData: LinearLayout
    private lateinit var layoutAbout: LinearLayout

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
        layoutAccent = view.findViewById(R.id.layoutAccent)
        tvAccentValue = view.findViewById(R.id.tvAccentValue)
        layoutContrast = view.findViewById(R.id.layoutContrast)
        tvContrastValue = view.findViewById(R.id.tvContrastValue)
        layoutTextSize = view.findViewById(R.id.layoutTextSize)
        tvTextSizeValue = view.findViewById(R.id.tvTextSizeValue)
        layoutLanguage = view.findViewById(R.id.layoutLanguage)
        tvLanguageValue = view.findViewById(R.id.tvLanguageValue)
        switchNotifications = view.findViewById(R.id.switchNotifications)
        switchDiscoverable = view.findViewById(R.id.switchDiscoverable)
        layoutClearChats = view.findViewById(R.id.layoutClearChats)
        layoutClearData = view.findViewById(R.id.layoutClearData)
        layoutAbout = view.findViewById(R.id.layoutAbout)

        loadSettings()

        layoutTheme.setOnClickListener { showThemeDialog() }
        layoutAccent.setOnClickListener { showAccentDialog() }
        layoutContrast.setOnClickListener { showContrastDialog() }
        layoutTextSize.setOnClickListener { showTextSizeDialog() }
        layoutLanguage.setOnClickListener { showLanguageDialog() }

        switchNotifications.setOnCheckedChangeListener { _, isChecked ->
            PreferencesHelper.notificationsEnabled = isChecked
        }

        switchDiscoverable.setOnCheckedChangeListener { _, isChecked ->
            PreferencesHelper.discoverable = isChecked
        }

        layoutClearChats.setOnClickListener {
            AlertDialog.Builder(requireContext())
                .setMessage(R.string.settings_clear_chats_confirm)
                .setPositiveButton(R.string.yes) { _, _ ->
                    clearAllChats()
                }
                .setNegativeButton(R.string.no, null)
                .show()
        }

        layoutClearData.setOnClickListener {
            AlertDialog.Builder(requireContext())
                .setMessage(R.string.privacy_clear_data_confirm)
                .setPositiveButton(R.string.yes) { _, _ ->
                    PreferencesHelper.clearAll()
                    loadSettings()
                    Toast.makeText(context, R.string.settings_data_cleared, Toast.LENGTH_SHORT).show()
                }
                .setNegativeButton(R.string.no, null)
                .show()
        }

        layoutAbout.setOnClickListener {
            findNavController().navigate(R.id.action_settings_to_about)
        }
    }

    private fun loadSettings() {
        tvThemeValue.text = when (PreferencesHelper.themeMode) {
            0 -> getString(R.string.theme_light)
            1 -> getString(R.string.theme_dark)
            else -> getString(R.string.theme_system)
        }
        tvAccentValue.text = getAccentName(PreferencesHelper.accentColor)
        tvContrastValue.text = if (PreferencesHelper.contrastMode == 0) getString(R.string.contrast_normal) else getString(R.string.contrast_high)
        tvTextSizeValue.text = getTextSizeName(PreferencesHelper.textSize)
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

    private fun showAccentDialog() {
        val colors = arrayOf(
            getString(R.string.accent_default),
            getString(R.string.accent_blue),
            getString(R.string.accent_purple),
            getString(R.string.accent_green),
            getString(R.string.accent_orange),
            getString(R.string.accent_red),
            getString(R.string.accent_teal),
            getString(R.string.accent_pink)
        )
        AlertDialog.Builder(requireContext())
            .setTitle(R.string.settings_accent_color)
            .setItems(colors) { _, which ->
                PreferencesHelper.accentColor = which
                tvAccentValue.text = colors[which]
            }
            .show()
    }

    private fun showContrastDialog() {
        val options = arrayOf(getString(R.string.contrast_normal), getString(R.string.contrast_high))
        AlertDialog.Builder(requireContext())
            .setTitle(R.string.settings_contrast)
            .setItems(options) { _, which ->
                PreferencesHelper.contrastMode = which
                tvContrastValue.text = options[which]
            }
            .show()
    }

    private fun showTextSizeDialog() {
        val sizes = arrayOf(getString(R.string.text_size_small), getString(R.string.text_size_default), getString(R.string.text_size_large), getString(R.string.text_size_extra_large))
        AlertDialog.Builder(requireContext())
            .setTitle(R.string.settings_text_size)
            .setItems(sizes) { _, which ->
                PreferencesHelper.textSize = which
                tvTextSizeValue.text = sizes[which]
            }
            .show()
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

    private fun clearAllChats() {
        val db = DatabaseHelper.getInstance(requireContext())
        viewLifecycleOwner.lifecycleScope.launch {
            withContext(Dispatchers.IO) {
                db.deleteAllMessages()
                db.deleteAllConversations()
            }
            if (isAdded) {
                Toast.makeText(context, R.string.settings_all_chats_cleared, Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun getAccentName(index: Int): String {
        return when (index) {
            0 -> getString(R.string.accent_default)
            1 -> getString(R.string.accent_blue)
            2 -> getString(R.string.accent_purple)
            3 -> getString(R.string.accent_green)
            4 -> getString(R.string.accent_orange)
            5 -> getString(R.string.accent_red)
            6 -> getString(R.string.accent_teal)
            7 -> getString(R.string.accent_pink)
            else -> getString(R.string.accent_default)
        }
    }

    private fun getTextSizeName(index: Int): String {
        return when (index) {
            0 -> getString(R.string.text_size_small)
            1 -> getString(R.string.text_size_default)
            2 -> getString(R.string.text_size_large)
            3 -> getString(R.string.text_size_extra_large)
            else -> getString(R.string.text_size_default)
        }
    }
}
