package com.idoelbak.tracker

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import com.idoelbak.tracker.ui.TrackerApp
import com.idoelbak.tracker.ui.TrackerViewModel

/**
 * AppCompat rather than ComponentActivity so `AppCompatDelegate.setApplicationLocales` can switch
 * the app between English and Hebrew without restarting the process.
 */
class MainActivity : AppCompatActivity() {

    private val model by lazy { ViewModelProvider(this)[TrackerViewModel::class.java] }

    private val askForNotifications =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        requestNotificationPermission()
        setContent { TrackerApp(model) }
    }

    /**
     * Asked once, on Android 13+. Reminders are the whole point of the app, but a refusal is not
     * fatal -- everything else keeps working, and Settings can turn them back on later.
     */
    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        val granted = ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
        if (granted != PackageManager.PERMISSION_GRANTED) {
            askForNotifications.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    /**
     * Days cannot close themselves while the app is shut, so every return to the app settles
     * whatever finished in the meantime and re-reads which day it now is.
     */
    override fun onResume() {
        super.onResume()
        model.refresh()
    }
}
