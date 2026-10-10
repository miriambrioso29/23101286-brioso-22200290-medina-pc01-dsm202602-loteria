package com.example.pc1

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.pc1.presentation.navigation.AppNavGraph
import com.example.pc1.ui.theme.PC1Theme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            PC1Theme {
                AppNavGraph()
            }
        }
    }
}
