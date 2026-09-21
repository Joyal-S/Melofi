# Auralis — Development Status

> **Current Phase:** Phase 3 Complete (Awaiting User Review / Phase 4 Approval)  
> **Last Updated:** 2026-09-21  

---

## 1. Phase Status Summary

| Phase | Description | Status |
|---|---|---|
| **Phase 0** | Architecture Validation & Dependency Compatibility Audit | ✅ Complete |
| **Phase 1** | Android Foundation & Project Bootstrap | ✅ Complete |
| **Phase 2** | Media Store Scanner, Song Repository & Library UI | ✅ Complete |
| **Phase 3** | Media3 Playback Engine & Background Playback | ✅ Complete |
| **Phase 4** | Playlists, Favorites, Equalizer & Advanced Audio | ⬜ Next |
| **Phase 5** | Listening Stats, History & Final Polish | ⬜ Planned |

---

## 2. Current Architecture & Implementation

### Build & Toolchain
- **Gradle:** 9.6.0 (via Gradle Wrapper)
- **AGP:** 9.4.0 (using official built-in Kotlin support)
- **Kotlin:** 2.2.10 (built-in via AGP 9.4.0)
- **KSP:** 2.2.10-2.0.2 (using `android.disallowKotlinSourceSets=false`)
- **Compose Compiler:** 2.2.10
- **Compose BOM:** 2026.09.00
- **AndroidX Media3:** 1.11.1 (`media3-exoplayer`, `media3-session`)
- **Target SDK / Compile SDK:** targetSdk 36, compileSdk 37 (Android SDK Platform 37.2 installed locally)
- **Min SDK:** 29 (Android 10)
- **JVM Target:** 17

### Playback Architecture Established (Phase 3)
- **Permissions (`AndroidManifest.xml`):**
  - `FOREGROUND_SERVICE`
  - `FOREGROUND_SERVICE_MEDIA_PLAYBACK`
  - `POST_NOTIFICATIONS` (API 33+) with runtime request flow
  - `WAKE_LOCK`
  - Zero network permissions (`INTERNET` strictly absent)
- **Service (`AuralisPlaybackService`):**
  - Extends `androidx.media3.session.MediaSessionService`.
  - Configures `ExoPlayer` instance with `C.USAGE_MEDIA` / `C.CONTENT_TYPE_MUSIC`, `handleAudioFocus = true`, `setHandleAudioBecomingNoisy(true)`, and `setWakeMode(C.WAKE_MODE_LOCAL)`.
  - Configures `MediaSession` with `PendingIntent` pointing to `MainActivity`.
  - Native system foreground service promotion and lock screen / notification media controls.
- **Client & Engine (`PlaybackController`, `PlaybackQueue`, `SeekSanitizer`):**
  - `PlaybackQueue`: Thread-safe in-memory queue supporting indexing, bounds clamping, item additions/removals, and repeat modes (`OFF`, `ONE`, `ALL`).
  - `SeekSanitizer`: Clamps invalid, negative, or duration-exceeding seek requests.
  - `MediaItemMapper`: Deterministic mapping between domain `Song` (Content URI) and Media3 `MediaItem` + `MediaMetadata`.
  - `PlaybackController`: Singleton client binding via `SessionToken` and `MediaController.Builder`. Synchronizes `Player.Listener` state into reactive `PlaybackState` flow. Non-polling position tracking active only while playing.
- **Presentation & UI:**
  - `PlaybackViewModel`: Exposes reactive `StateFlow<PlaybackState>` and user commands (`playSong`, `togglePlayPause`, `seekTo`, `next`, `previous`, `toggleShuffle`, `cycleRepeatMode`).
  - `PlaybackControlsBar`: Persistent Material 3 playback control surface embedded in `SongsScreen` bottom bar with seek slider, elapsed/duration labels, artwork thumbnail, play/pause, prev, next, shuffle, and repeat toggles.
  - Runtime `POST_NOTIFICATIONS` request in `MainActivity` for Android 13+.

---

## 3. Completed Verification

| Verification | Result | Notes |
|---|---|---|
| `./gradlew --version` | ✅ Pass | Gradle 9.6.0, JDK 21.0.10 (Android Studio JBR) |
| `./gradlew clean` | ✅ Pass | Clean build cache & outputs |
| `./gradlew assembleDebug` | ✅ Pass | Generates `app-debug.apk` cleanly without errors |
| `./gradlew testDebugUnitTest` | ✅ Pass | 43/43 unit tests execute and pass in all 10 test suites |
| KSP Annotation Processing | ✅ Pass | Room schema export and Hilt components generate cleanly |
| Hilt Code Generation | ✅ Pass | `DatabaseModule`, `RepositoryModule`, `DispatchersModule`, `PlaybackModule` validated |
| Permissions Audit | ✅ Pass | Storage and media playback permissions verified; zero network access |
| Privacy Audit | ✅ Pass | No analytics, ads, Firebase, or external telemetry libraries |
| Runtime Verification | ⚠️ NOT TESTED | 0 connected physical devices or emulators detected via `adb devices` |

---

## 4. Known Limitations & Deferred Items

- **No Connected Hardware / Emulator:** `adb devices` returned 0 attached devices at verification time; compilation and full unit test suite are verified, but runtime device verification is marked NOT TESTED.
- **Persistent Playlists & Favorites Deferred:** Per specification, Room-backed playlist persistence, favorites, and history tracking are scheduled for subsequent phases.
- **Audio Effects Deferred:** Equalizer, bass boost, and ReplayGain loudness normalization remain planned for Phase 4.

---

## 5. Next Phase: Phase 4

**Focus:** Playlists, Favorites, Equalizer & Advanced Audio Controls
- Room persistence for user playlists and favorite songs.
- Playlist management UI (create, rename, add/remove songs, reorder).
- System equalizer integration and audio effects.
