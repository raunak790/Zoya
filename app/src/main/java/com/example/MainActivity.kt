package com.example

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import com.example.service.ZoyaController
import com.example.service.ZoyaVoiceService
import com.example.ui.ZoyaMainScreen
import com.example.ui.theme.ZoyaTheme
import com.example.viewmodel.ZoyaViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: ZoyaViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Check initial permissions
        val hasAudio = ContextCompat.checkSelfPermission(
            this,
            android.Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
        viewModel.onPermissionsUpdated(hasAudio)

        handleIntent(intent)

        setContent {
            ZoyaTheme {
                ZoyaMainScreen(
                    viewModel = viewModel,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        if (intent?.action == ZoyaVoiceService.ACTION_AWAKEN) {
            ZoyaController.awakenZoya(applicationContext)
        }
    }
}
