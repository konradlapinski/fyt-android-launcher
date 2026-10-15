package com.konradlapinski.fytlauncher

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.konradlapinski.fytlauncher.data.LauncherConfigStore
import com.konradlapinski.fytlauncher.ui.LauncherApp

class MainActivity : ComponentActivity() {
    private val configStore by lazy { LauncherConfigStore(applicationContext) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            LauncherApp(activity = this, configStore = configStore)
        }
    }
}
