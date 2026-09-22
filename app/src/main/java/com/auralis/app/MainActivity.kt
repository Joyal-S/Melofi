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
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.auralis.app.core.ui.theme.AuralisTheme
import com.auralis.app.data.local.preferences.UserPreferencesRepository
import com.auralis.app.domain.model.ThemeMode
import com.auralis.app.domain.model.UserPreferences
import com.auralis.app.ui.MainNavigationScreen
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.auralis.app.core.ui.theme.MelofyAnimationMode
import com.auralis.app.ui.launch.MelofiLaunchScreen

/**
 * Single-activity host for the Melofi application.
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var userPreferencesRepository: UserPreferencesRepository

    companion object {
        /**
         * Cold start vs warm start tracking.
         * Reset to false on process death; retains true for warm starts (returning from background,
         * device rotation, multi-window toggling).
         */
        var hasCompletedInitialLaunch: Boolean = false
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val systemDark = isSystemInDarkTheme()
            val userPreferences by userPreferencesRepository.preferencesFlow
                .collectAsStateWithLifecycle(initialValue = UserPreferences())

            val isDark = when (userPreferences.themeMode) {
                ThemeMode.SYSTEM -> systemDark
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
            }

            var showLaunchScreen by remember {
                mutableStateOf(!hasCompletedInitialLaunch && userPreferences.animationMode != MelofyAnimationMode.OFF)
            }

            AuralisTheme(
                darkTheme = isDark,
                amoledBlack = userPreferences.amoledBlack,
                accent = userPreferences.accentColor,
                animationMode = userPreferences.animationMode,
                artworkCornerStyle = userPreferences.artworkCornerStyle
            ) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    val notificationPermissionLauncher = rememberLauncherForActivityResult(
                        contract = ActivityResultContracts.RequestPermission()
                    ) { /* permission result handled */ }

                    LaunchedEffect(showLaunchScreen) {
                        if (!showLaunchScreen && ContextCompat.checkSelfPermission(
                                this@MainActivity,
                                Manifest.permission.POST_NOTIFICATIONS
                            ) != PackageManager.PERMISSION_GRANTED
                        ) {
                            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        }
                    }
                }

                Box(modifier = Modifier.fillMaxSize()) {
                    // Underneath: Main application is fully composed immediately.
                    // ViewModels, database flows, and MediaStore scanning start asynchronously
                    // without any blocking main-thread delay.
                    MainNavigationScreen()

                    // Overlay: Premium Melofi launch experience on cold start
                    if (showLaunchScreen) {
                        MelofiLaunchScreen(
                            isDark = isDark,
                            isAmoled = userPreferences.amoledBlack,
                            animationMode = userPreferences.animationMode,
                            onLaunchComplete = {
                                hasCompletedInitialLaunch = true
                                showLaunchScreen = false
                            }
                        )
                    }
                }
            }
        }
    }
}
