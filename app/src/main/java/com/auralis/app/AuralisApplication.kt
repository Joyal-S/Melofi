package com.auralis.app

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

/**
 * Auralis application entry point.
 *
 * Annotated with @HiltAndroidApp to trigger Hilt code generation
 * and provide the application-level dependency container.
 */
@HiltAndroidApp
class AuralisApplication : Application()
