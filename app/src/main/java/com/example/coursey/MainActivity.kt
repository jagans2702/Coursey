package com.example.coursey

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.coursey.navigation.AppNavHost
import com.example.coursey.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val container = (application as LearningApp).container
        setContent {
            MyApplicationTheme {
                AppNavHost(container = container)
            }
        }
    }
}
