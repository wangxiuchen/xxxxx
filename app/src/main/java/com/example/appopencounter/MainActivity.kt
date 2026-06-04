package com.example.appopencounter

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.appopencounter.ui.navigation.AppNavHost
import com.example.appopencounter.ui.theme.AppOpenCounterTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            AppOpenCounterTheme {
                AppNavHost()
            }
        }
    }
}
