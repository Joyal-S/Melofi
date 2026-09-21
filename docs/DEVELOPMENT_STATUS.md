# Auralis — Development Status

> **Current Phase:** Phase 2 Complete (Awaiting User Review / Phase 3 Approval)  
> **Last Updated:** 2026-09-21  

---

## 1. Phase Status Summary

| Phase | Description | Status |
|---|---|---|
| **Phase 0** | Architecture Validation & Dependency Compatibility Audit | ✅ Complete |
| **Phase 1** | Android Foundation & Project Bootstrap | ✅ Complete |
| **Phase 2** | Media Store Scanner, Song Repository & Library UI | ✅ Complete |
| **Phase 3** | Queue Management, Playlists & Favorites | ⬜ Next |
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
- **Target SDK / Compile SDK:** targetSdk 36, compileSdk 37 (Android SDK Platform 37.2 installed locally)
- **Min SDK:** 29 (Android 10)
- **JVM Target:** 17

### Architecture Layers Established (Phase 2)
- **Permissions:**
  - `READ_EXTERNAL_STORAGE` (API 29–32 with `maxSdkVersion="32"`)
  - `READ_MEDIA_AUDIO` (API 33+)
  - `PermissionUtils` helper handling runtime checks and rationale flows.
  - Zero network permissions (`INTERNET` absent).
- **Scanner & Normalization:**
  - `MediaStoreAudioScanner` querying `MediaStore.Audio.Media.EXTERNAL_CONTENT_URI` via `ContentResolver`.
  - `AudioMetadataNormalizer` pure Kotlin normalizer ensuring no null/corrupted strings enter the database.
  - `LibrarySyncDiff` computing differential updates (`toInsert`, `toUpdate`, `toDelete`, `unchanged`).
  - Strict duplicate prevention guarantee across repeated scans.
- **Data Persistence (Room 2.8.5):**
  - Entities: `SongEntity`, `AlbumEntity`, `ArtistEntity`, `GenreEntity`.
  - DAOs: `SongDao`, `AlbumDao`, `ArtistDao`, `GenreDao`.
  - Database: `AuralisDatabase` with atomic `@Transaction` sync.
  - Primary audio key: Stable Content URI (`content://media/external/audio/media/<id>`).
- **Domain & Repository:**
  - Domain models: `Song`, `SongSortOrder`, `ScanResult`.
  - Repository: `SongRepository` and `SongRepositoryImpl`.
  - Use cases: `GetSongsUseCase`, `ScanLibraryUseCase`.
- **UI & Presentation:**
  - `SongsViewModel` with reactive `StateFlow<SongsUiState>`.
  - `SongsScreen` with Material 3 TopAppBar, sorting menu, rescan action, permission rationale card, empty state, error view, and `LazyColumn`.
  - `SongListItem` displaying Coil local artwork with vector placeholder fallback, title, artist, album, and formatted duration.

---

## 3. Completed Verification

| Verification | Result | Notes |
|---|---|---|
| `./gradlew --version` | ✅ Pass | Gradle 9.6.0, JDK 21.0.10 (Android Studio JBR) |
| `./gradlew clean` | ✅ Pass | Clean build cache & outputs |
| `./gradlew assembleDebug` | ✅ Pass | Generates `app-debug.apk` without warnings or errors |
| `./gradlew testDebugUnitTest` | ✅ Pass | 30/30 unit tests execute and pass in all suites |
| KSP Annotation Processing | ✅ Pass | Room schema export and Hilt components generate cleanly |
| Hilt Code Generation | ✅ Pass | `DatabaseModule`, `RepositoryModule`, `DispatchersModule` validated |
| Permissions Audit | ✅ Pass | Zero network or unnecessary permissions in `AndroidManifest.xml` |
| Privacy Audit | ✅ Pass | No analytics, ads, Firebase, or external telemetry libraries |
| Runtime Verification | ⚠️ NOT TESTED | No connected physical device or emulator detected via `adb devices` |

---

## 4. Known Limitations

- **No Playback or Media Service Implemented:** Media3 dependencies are linked and validated; playback service, ExoPlayer, MediaSession, and notification controls are scheduled for Phase 4 / Phase 3.
- **No Physical Device / Emulator Connected:** `adb devices` returned 0 attached devices at verification time; compilation and unit test suite are fully verified, but physical runtime launch was not performed.
- **Playlists / Favorites Deferred:** As per project roadmap, playlists and favorites belong to Phase 3.

---

## 5. Next Phase: Phase 3

**Focus:** Queue Management, Playlists & Favorites
- Implement queue engine and queue persistence.
- Implement Playlist and Favorite Room entities and cross-reference tables.
- Implement playlist creation, editing, song addition/removal, and reordering.
- Connect library songs to playlist and queue actions.
