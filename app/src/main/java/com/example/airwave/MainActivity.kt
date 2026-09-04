package com.example.airwave

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.os.Build
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import com.example.airwave.bluetooth.BluetoothManager
import com.example.airwave.util.LanguageHelper
import com.example.airwave.util.PreferencesHelper

class pushMainActivity : AppCompatActivity() {

    private lateinit var navController: NavController
    private lateinit var bluetoothManager: BluetoothManager

    @Volatile
    private var isResumed = false

    private var requestDialog: AlertDialog? = null

    override fun attachBaseContext(newBase: Context) {
        val prefs = newBase.getSharedPreferences("airwave_prefs", Context.MODE_PRIVATE)
        val lang = prefs.getString("language", "en") ?: "en"
        val context = LanguageHelper.setLocale(newBase, lang)
        super.attachBaseContext(context)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.nav_host_fragment)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, 0)
            insets
        }

        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        navController = navHostFragment.navController

        bluetoothManager = BluetoothManager.getInstance(this)

        bluetoothManager.incomingRequest.observe(this) { request ->
            if (request == null) {
                requestDialog?.dismiss()
                requestDialog = null
            } else {
                if (isResumed) {
                    showIncomingRequestDialog(request.peerName)
                } else {
                    notifyIncomingRequest(request.peerName)
                }
            }
        }

        navController.addOnDestinationChangedListener { _, destination, _ ->
            if (destination.id == R.id.chatFragment) {
                requestDialog?.dismiss()
                requestDialog = null
            }
        }
    }

    override fun onStart() {
        super.onStart()
        isResumed = true
        // If a request arrived while backgrounded, surface it now.
        bluetoothManager.incomingRequest.value?.let { request ->
            showIncomingRequestDialog(request.peerName)
        }
    }

    override fun onStop() {
        super.onStop()
        isResumed = false
    }

    override fun onDestroy() {
        super.onDestroy()
        // Only tear down Bluetooth resources when the activity is truly finishing
        // (not on configuration changes, which would kill a live chat session).
        if (isFinishing) {
            bluetoothManager.destroy()
        }
    }

    private fun showIncomingRequestDialog(peerName: String) {
        if (requestDialog?.isShowing == true) return
        if (navController.currentDestination?.id == R.id.chatFragment) return
        requestDialog = AlertDialog.Builder(this)
            .setTitle(R.string.connection_request_title)
            .setMessage(getString(R.string.connection_request_message, peerName))
            .setPositiveButton(R.string.connection_accept) { _, _ ->
                bluetoothManager.acceptIncomingRequest()
                openChatForAcceptedConnection()
            }
            .setNegativeButton(R.string.connection_reject) { _, _ ->
                bluetoothManager.rejectIncomingRequest()
            }
            .setNeutralButton(R.string.connection_block) { _, _ ->
                bluetoothManager.blockIncomingRequest()
            }
            .setOnDismissListener {
                requestDialog = null
            }
            .show()
    }

    private fun openChatForAcceptedConnection() {
        if (!isResumed) return
        val navOptions = androidx.navigation.NavOptions.Builder()
            .setPopUpTo(R.id.homeFragment, false)
            .setLaunchSingleTop(true)
            .build()
        try {
            navController.navigate(R.id.chatFragment, Bundle(), navOptions)
        } catch (e: Exception) {
            // Navigation not possible right now (e.g. still on welcome); ignore.
        }
    }

    private fun notifyIncomingRequest(peerName: String) {
        if (!PreferencesHelper.notificationsEnabled || !PreferencesHelper.connectionNotifications) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) != android.content.pm.PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        try {
            val intent = packageManager.getLaunchIntentForPackage(packageName)?.apply {
                addFlags(android.content.Intent.FLAG_ACTIVITY_SINGLE_TOP)
            }
            val pendingIntent = PendingIntent.getActivity(
                this, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            val notification: Notification = Notification.Builder(this, AirWaveApp.CHANNEL_CONNECTIONS)
                .setSmallIcon(R.drawable.ic_bluetooth)
                .setContentTitle(getString(R.string.connection_request_title))
                .setContentText(getString(R.string.connection_request_message, peerName))
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .build()
            val manager = getSystemService(NotificationManager::class.java)
            manager.notify(1001, notification)
        } catch (e: Exception) {
            // Notification could not be posted; not critical.
        }
    }
}
