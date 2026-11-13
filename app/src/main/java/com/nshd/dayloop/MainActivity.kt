package com.nshd.dayloop

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.nshd.dayloop.ui.DayLoopApp
import com.nshd.dayloop.ui.theme.DayLoopTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            DayLoopTheme {
                DayLoopApp()
            }
        }
    }
}
