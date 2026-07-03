package com.sim.lavis

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.sim.lavis.ui.LavisApp
import com.sim.lavis.ui.theme.LavisTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private var hasAudioPermission by mutableStateOf(false)

    private val permissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { grants ->
            hasAudioPermission = grants[Manifest.permission.READ_MEDIA_AUDIO] == true
            if (hasAudioPermission) scanLibrary()
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        hasAudioPermission = ContextCompat.checkSelfPermission(
            this, Manifest.permission.READ_MEDIA_AUDIO
        ) == PackageManager.PERMISSION_GRANTED

        if (hasAudioPermission) {
            scanLibrary()
        } else {
            permissionLauncher.launch(
                arrayOf(
                    Manifest.permission.READ_MEDIA_AUDIO,
                    Manifest.permission.POST_NOTIFICATIONS
                )
            )
        }

        setContent {
            LavisTheme {
                if (hasAudioPermission) {
                    LavisApp(playerManager = (application as LavisApplication).playerManager)
                } else {
                    Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                        Text(
                            text = "$ error: audio permission denied\n\n" +
                                "lavis needs access to your music files.\n" +
                                "grant it in settings > apps > lavis.",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(innerPadding).padding(24.dp)
                        )
                    }
                }
            }
        }
    }

    private fun scanLibrary() {
        val app = application as LavisApplication
        lifecycleScope.launch { app.mediaScanner.sync() }
    }
}
