package com.auralis.app

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.LaunchedEffect
import androidx.core.content.ContextCompat
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.auralis.app.core.ui.theme.AuralisTheme
import com.auralis.app.ui.library.SongsScreen
import com.auralis.app.ui.library.SongsViewModel
import com.auralis.app.ui.playback.PlaybackViewModel
import dagger.hilt.android.AndroidEntryPoint

/**
 * Single-activity host for the Auralis application.
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AuralisTheme {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    val notificationPermissionLauncher = rememberLauncherForActivityResult(
                        contract = ActivityResultContracts.RequestPermission()
                    ) { /* permission result handled */ }

                    LaunchedEffect(Unit) {
                        if (ContextCompat.checkSelfPermission(
                                this@MainActivity,
                                Manifest.permission.POST_NOTIFICATIONS
                            ) != PackageManager.PERMISSION_GRANTED
                        ) {
                            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        }
                    }
                }

                val songsViewModel: SongsViewModel = hiltViewModel()
                val playbackViewModel: PlaybackViewModel = hiltViewModel()
                SongsScreen(
                    viewModel = songsViewModel,
                    playbackViewModel = playbackViewModel
                )
            }
        }
    }
}
