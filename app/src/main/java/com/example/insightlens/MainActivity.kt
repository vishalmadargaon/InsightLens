package com.example.insightlens // <--- CHANGE THIS to match your other files!

import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import com.example.insightlens.ui.theme.InsightLensTheme // If this is red, delete this line.

class MainActivity : ComponentActivity() {

    // 1. This handles the "Allow Camera?" popup
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            startCamera() // If they say yes, start the app!
        } else {
            // If they say no, we can't do anything.
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 2. Check if we already have permission
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            startCamera()
        } else {
            // 3. If not, ask for it
            requestPermissionLauncher.launch(android.Manifest.permission.CAMERA)
        }
    }

    private fun startCamera() {
        setContent {
            // 4. HERE IS THE SWITCH!
            // We load our custom CameraPreview instead of "Hello Android"
            CameraPreview()
        }
    }
}