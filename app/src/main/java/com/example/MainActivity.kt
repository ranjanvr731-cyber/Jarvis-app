package com.example

import android.Manifest
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import com.example.ui.JarvisScreen
import com.example.ui.JarvisViewModel
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
    private val viewModel: JarvisViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Register permission requests launcher at startup
        val permissionLauncher = registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { results ->
            // Audio recording is core to JARVIS voice systems
            val audioAllowed = results[Manifest.permission.RECORD_AUDIO] ?: false
            if (!audioAllowed) {
                // Log/Handle state in ViewModel or show warning to user
                viewModel.processIncomingInput("Alert: Microphone permission denied. Some vocal facilities are temporarily offline, sir.")
            }
        }

        setContent {
            MyApplicationTheme {
                // Request permissions safely at composition launch
                LaunchedEffect(Unit) {
                    permissionLauncher.launch(
                        arrayOf(
                            Manifest.permission.RECORD_AUDIO,
                            Manifest.permission.CALL_PHONE,
                            Manifest.permission.SEND_SMS
                        )
                    )
                }

                JarvisScreen(
                    viewModel = viewModel,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}
