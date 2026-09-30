package com.sim.lavis

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.sim.lavis.ui.LavisApp
import com.sim.lavis.ui.components.EmptyState
import com.sim.lavis.ui.components.LavisIcons
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
        // The app is always dark, so always use light system-bar icons.
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT)
        )

        hasAudioPermission = ContextCompat.checkSelfPermission(
            this, Manifest.permission.READ_MEDIA_AUDIO
        ) == PackageManager.PERMISSION_GRANTED

        if (hasAudioPermission) scanLibrary() else requestPermissions()

        setContent {
            LavisTheme {
                if (hasAudioPermission) {
                    LavisApp(playerManager = (application as LavisApplication).playerManager)
                } else {
                    PermissionScreen(
                        onGrant = ::requestPermissions,
                        onOpenSettings = ::openAppSettings
                    )
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        // Re-attach to the playback service if it went away while we were in the background.
        if (hasAudioPermission) (application as LavisApplication).playerManager.connect()
    }

    override fun onResume() {
        super.onResume()
        // Permission may have been granted from system settings while we were away.
        if (!hasAudioPermission &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_AUDIO) ==
            PackageManager.PERMISSION_GRANTED
        ) {
            hasAudioPermission = true
            scanLibrary()
        }
    }

    private fun requestPermissions() {
        permissionLauncher.launch(
            arrayOf(
                Manifest.permission.READ_MEDIA_AUDIO,
                Manifest.permission.POST_NOTIFICATIONS
            )
        )
    }

    private fun openAppSettings() {
        startActivity(
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", packageName, null))
        )
    }

    private fun scanLibrary() {
        val app = application as LavisApplication
        lifecycleScope.launch { app.mediaScanner.sync() }
    }
}

@Composable
private fun PermissionScreen(onGrant: () -> Unit, onOpenSettings: () -> Unit) {
    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            EmptyState(
                icon = LavisIcons.Headphones,
                title = "Lavis needs your music",
                message = "Allow access to audio files so Lavis can find the songs in your Music folder."
            )
            Button(onClick = onGrant) { Text("Allow access") }
            TextButton(onClick = onOpenSettings, modifier = Modifier.padding(top = 8.dp)) {
                Text("Open settings")
            }
        }
    }
}
