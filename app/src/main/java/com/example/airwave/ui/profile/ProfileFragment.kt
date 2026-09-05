package com.example.airwave.ui.profile

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.airwave.R
import com.example.airwave.bluetooth.BluetoothManager
import com.example.airwave.util.PreferencesHelper
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText

/**
 * "Your AirWave Identity" - there is no account, only the nickname used for
 * the current session and a locally remembered convenience copy.
 *
 * The nickname is edited in place on this screen (the "Change Name" button,
 * which carries the pencil icon, toggles the inline editor); it is never
 * edited by navigating to another screen.
 */
class ProfileFragment : Fragment() {

    private lateinit var tvNickname: TextView
    private lateinit var tvAvatar: TextView
    private lateinit var tvStatus: TextView
    private lateinit var etName: TextInputEditText
    private lateinit var layoutNameEditor: View
    private lateinit var btnChangeName: MaterialButton

    private var isEditingName = false

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
        etName = view.findViewById(R.id.etName)
        layoutNameEditor = view.findViewById(R.id.layoutNameEditor)
        btnChangeName = view.findViewById(R.id.btnChangeName)

        view.findViewById<MaterialToolbar>(R.id.toolbar).setNavigationOnClickListener {
            findNavController().popBackStack()
        }

        btnChangeName.setOnClickListener {
            if (isEditingName) saveName() else startEditName()
        }
        etName.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                saveName()
                true
            } else {
                false
            }
        }

        // Back while editing cancels the edit instead of leaving the screen.
        requireActivity().onBackPressedDispatcher.addCallback(
            viewLifecycleOwner,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    if (isEditingName) {
                        cancelEditName()
                    } else {
                        isEnabled = false
                        findNavController().popBackStack()
                    }
                }
            }
        )
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

    // ---------------- Inline name editing ----------------

    private fun startEditName() {
        isEditingName = true
        etName.setText(PreferencesHelper.nickname)
        etName.setSelection(etName.text?.length ?: 0)
        etName.error = null
        tvNickname.visibility = View.GONE
        layoutNameEditor.visibility = View.VISIBLE
        btnChangeName.text = getString(R.string.profile_save)
        btnChangeName.icon = null
        etName.requestFocus()
        etName.postDelayed({ showKeyboard() }, 100)
    }

    private fun saveName() {
        val nickname = etName.text?.toString()?.trim().orEmpty()
        when {
            nickname.isEmpty() -> {
                etName.error = getString(R.string.welcome_error_empty)
            }
            nickname.length < PreferencesHelper.MIN_NICKNAME_LENGTH ||
                nickname.length > PreferencesHelper.MAX_NICKNAME_LENGTH -> {
                etName.error = getString(R.string.welcome_error_length)
            }
            else -> {
                // Same single source of truth used everywhere else in the app.
                PreferencesHelper.nickname = nickname
                // Re-advertise the updated identity to nearby AirWave users.
                bluetoothManager.ensureAirWaveDeviceName()
                exitEditName()
                loadIdentity()
                Toast.makeText(requireContext(), R.string.profile_name_saved, Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun cancelEditName() {
        exitEditName()
    }

    private fun exitEditName() {
        isEditingName = false
        etName.error = null
        layoutNameEditor.visibility = View.GONE
        tvNickname.visibility = View.VISIBLE
        btnChangeName.text = getString(R.string.profile_change_name)
        btnChangeName.setIconResource(R.drawable.ic_edit)
        hideKeyboard()
    }

    private fun showKeyboard() {
        if (!isAdded) return
        val imm = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.showSoftInput(etName, InputMethodManager.SHOW_IMPLICIT)
    }

    private fun hideKeyboard() {
        if (!isAdded) return
        val imm = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(view?.windowToken, 0)
    }
}
