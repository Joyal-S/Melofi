package com.auralis.app.core.util

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat

/**
 * Utility functions for handling Android scoped storage media permissions across OS versions.
 */
object PermissionUtils {

    /**
     * Determines the appropriate storage read permission for the current Android version.
     *
     * - Android 13+ (API 33+): [Manifest.permission.READ_MEDIA_AUDIO]
     * - Android 10-12 (API 29-32): [Manifest.permission.READ_EXTERNAL_STORAGE]
     */
    fun getRequiredStoragePermission(): String {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_AUDIO
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }
    }

    /**
     * Checks whether the required storage permission has been granted by the user.
     */
    fun hasStoragePermission(context: Context): Boolean {
        val permission = getRequiredStoragePermission()
        return ContextCompat.checkSelfPermission(
            context,
            permission
        ) == PackageManager.PERMISSION_GRANTED
    }
}
