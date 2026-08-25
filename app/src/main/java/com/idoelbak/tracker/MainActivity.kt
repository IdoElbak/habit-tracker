package com.idoelbak.tracker

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.idoelbak.tracker.ui.TrackerApp
import com.idoelbak.tracker.ui.TrackerViewModel

/**
 * AppCompat rather than ComponentActivity so `AppCompatDelegate.setApplicationLocales` can switch
 * the app between English and Hebrew without restarting the process.
 */
class MainActivity : AppCompatActivity() {

    private val model by lazy { ViewModelProvider(this)[TrackerViewModel::class.java] }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent { TrackerApp(model) }
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
