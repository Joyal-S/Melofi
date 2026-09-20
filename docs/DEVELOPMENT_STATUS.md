# Auralis — Development Status

> **Current Phase:** Phase 1 Complete (Awaiting User Review / Phase 2 Approval)  
> **Last Updated:** 2026-09-21  

---

## 1. Phase Status Summary

| Phase | Description | Status |
|---|---|---|
| **Phase 0** | Architecture Validation & Dependency Compatibility Audit | ✅ Complete |
| **Phase 1** | Android Foundation & Project Bootstrap | ✅ Complete |
| **Phase 2** | Media Store Scanner, Song Repository & Library UI | ⬜ Next |
| **Phase 3** | Queue Management, Playlists & Favorites | ⬜ Planned |
| **Phase 4** | Playback Controls & Media3 Background Service | ⬜ Planned |
| **Phase 5** | Audio Features (Equalizer, ReplayGain, Sleep Timer) | ⬜ Planned |
| **Phase 6** | Polish, UI Details & Performance Optimization | ⬜ Planned |

---

## 2. Current Architecture & Implementation

### Build & Toolchain
- **Gradle:** 9.6.0 (via Gradle Wrapper)
- **AGP:** 9.4.0 (using official built-in Kotlin support)
- **Kotlin:** 2.2.10 (built-in via AGP 9.4.0)
- **KSP:** 2.2.10-2.0.2 (using `android.disallowKotlinSourceSets=false`)
- **Compose Compiler:** 2.2.10
- **Compose BOM:** 2026.09.00
- **Target SDK / Compile SDK:** targetSdk 36, compileSdk 37 (installed locally: Android SDK Platform 37.2)
- **Min SDK:** 29 (Android 10)
- **JVM Target:** 17
- **Technical Debt Elimination:**
  - `android.builtInKotlin=false` REMOVED (AGP 9 built-in Kotlin active).
  - `android.newDsl=false` REMOVED (AGP 9 New DSL active).
  - `org.jetbrains.kotlin.android` REMOVED (no legacy plugin).
  - `-Xskip-metadata-version-check` REMOVED (eliminated via `kotlin-stdlib:2.2.10` dependency alignment).

### Architecture Layers Established
- **DI:** Hilt 2.60.1 configured with `@HiltAndroidApp` in `AuralisApplication` and `@AndroidEntryPoint` in `MainActivity`.
- **UI:** Jetpack Compose + Material 3 with dark/light dynamic theme in `com.auralis.app.core.ui.theme`. Minimal launch screen showing app branding and slogan.
- **Data & Playback Dependencies:** Room 2.8.5, Media3 1.11.1 (ExoPlayer + Session), Coil 3.6.3 (compose module only, no network), DataStore Preferences 1.2.1, Kotlinx Serialization 1.11.0, Coroutines 1.11.0 configured in build files.

---

## 3. Completed Verification

| Verification | Result | Notes |
|---|---|---|
| `./gradlew --version` | ✅ Pass | Gradle 9.6.0, JDK 21.0.10 (Android Studio JBR) |
| `./gradlew clean` | ✅ Pass | Clean build cache & outputs |
| `./gradlew assembleDebug` | ✅ Pass | Generates `app-debug.apk` without errors |
| `./gradlew testDebugUnitTest` | ✅ Pass | Unit test runner executes successfully |
| KSP Annotation Processing | ✅ Pass | `kspDebugKotlin` and `kspDebugUnitTestKotlin` tasks execute cleanly |
| Hilt Code Generation | ✅ Pass | Hilt bytecode transformation and component generation succeed |
| Permissions Audit | ✅ Pass | Zero network or unnecessary permissions in `AndroidManifest.xml` |
| Privacy Audit | ✅ Pass | No analytics, ads, Firebase, or external telemetry libraries |

---

## 4. Known Limitations

- **No Playback or Media Service Implemented:** Media3 dependencies are linked and validated, but the playback service and player controllers are deferred to subsequent phases as per project roadmap.
- **No MediaStore Scanner:** Device audio scanning and database ingestion are scheduled for Phase 2.
- **No Physical Device / Emulator Connected:** `adb devices` returned 0 active devices at verification time; APK generation and compilation are verified, but runtime launch on physical hardware remains to be performed when a device is attached.

---

## 5. Next Phase: Phase 2

**Focus:** Local Audio Discovery & Library Foundation
- Implement `MediaStoreAudioScanner` with support for scoped storage (API 29+).
- Implement Room database schema: `SongEntity`, `AlbumEntity`, `ArtistEntity`, `GenreEntity`.
- Implement `SongRepository` and `GetSongsUseCase`.
- Implement basic Songs list UI in Compose.
- Handle runtime permissions (`READ_MEDIA_AUDIO` on API 33+, `READ_EXTERNAL_STORAGE` on API 29–32).
