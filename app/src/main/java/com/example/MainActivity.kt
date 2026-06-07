package com.example

import android.Manifest
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.ViewModelProvider
import com.example.data.Repository
import com.example.ui.JarvisScreen
import com.example.ui.JarvisViewModel
import com.example.ui.JarvisViewModelFactory
import com.example.ui.theme.JarvisTheme

class MainActivity : ComponentActivity() {

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            Toast.makeText(this, "Acoustic grid authorization granted, sir.", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "Acoustic channels offline. Direct terminal input is active.", Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val app = application as JarvisApp
        val repository = Repository(app.database)
        val factory = JarvisViewModelFactory(repository, applicationContext)
        val viewModel = ViewModelProvider(this, factory)[JarvisViewModel::class.java]

        // Request recording permission on boot
        requestPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)

        setContent {
            JarvisTheme {
                JarvisScreen(viewModel = viewModel)
            }
        }
    }
}
