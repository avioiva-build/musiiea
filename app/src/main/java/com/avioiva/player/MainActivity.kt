package com.avioiva.player

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.avioiva.player.ui.AppRoot
import com.avioiva.player.ui.theme.AVIOIVATheme

/**
 * Single-activity app. The entire UI is Jetpack Compose, edge-to-edge, with a
 * dark-first Material You theme.
 */
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            AVIOIVATheme {
                AppRoot()
            }
        }
    }
}
