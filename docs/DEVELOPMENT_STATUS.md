# Melofy — Development Status

> **Current Phase:** Phase 6 Complete (Listening History, Statistics, Smart Playlists & Widgets)  
> **Last Updated:** 2026-09-21  

---

## 1. Phase Status Summary

| Phase | Description | Status |
|---|---|---|
| **Phase 0** | Architecture Validation & Dependency Compatibility Audit | ✅ Complete |
| **Phase 1** | Android Foundation & Project Bootstrap | ✅ Complete |
| **Phase 2** | Media Store Scanner, Song Repository & Library UI | ✅ Complete |
| **Phase 3** | Media3 Playback Engine & Background Playback | ✅ Complete |
| **Phase 4** | Playlists, Favorites, Membership, Ordering & Queue Persistence | ✅ Complete |
| **Phase 5** | Audio Features & Advanced Controls (Equalizer, ReplayGain, Sleep Timer) | ✅ Complete |
| **Phase 6** | Listening History, Statistics, Smart Playlists & Home Screen Widgets | ✅ Complete |

---

## 2. Current Architecture & Implementation

### Build & Toolchain
- **Gradle:** 9.6.0 (via Gradle Wrapper)
- **AGP:** 9.4.0 (using official built-in Kotlin support)
- **Kotlin:** 2.2.10 (built-in via AGP 9.4.0)
- **KSP:** 2.2.10-2.0.2 (using `android.disallowKotlinSourceSets=false`)
- **Compose Compiler:** 2.2.10
- **Compose BOM:** 2026.09.00
- **Room:** 2.8.5 (Schema version 2 exported)
- **AndroidX Media3:** 1.11.1 (`media3-exoplayer`, `media3-session`)
- **Target SDK / Compile SDK:** targetSdk 36, compileSdk 37 (Android SDK Platform 37.2 installed locally)
- **Min SDK:** 29 (Android 10)
- **JVM Target:** 17

### Organization & Persistence Architecture (Phase 4)
- **Room Database Schema v2 & Migration (`AuralisDatabase.kt`):**
  - Schema upgraded from version 1 to 2 via deterministic `MIGRATION_1_2`.
  - `playlists` table: `@PrimaryKey(autoGenerate = true) id: Long`, `name: String`, `createdAt: Long`, `updatedAt: Long`.
  - `favorites` table: `@PrimaryKey songId: Long`, `addedAt: Long`. Foreign key to `songs(id)` with `ON DELETE CASCADE`.
  - `playlist_songs` cross-reference table: composite primary key `(playlistId, songId)`, `position: Int`, `addedAt: Long`. Foreign keys to both `playlists(id)` and `songs(id)` with `ON DELETE CASCADE`.
  - Foreign key constraints enabled in SQLite via `RoomDatabase.Callback.onOpen`.
  - Deleting a song via MediaStore rescan cascades to remove its favorite status and playlist membership without leaving orphaned rows or deleting playlists.
  - Deleting a playlist removes its memberships without deleting library songs.
- **Data Access & Performance:**
  - `FavoriteDao`: Exposes `getFavoriteSongIds(): Flow<List<Long>>` for $O(1)$ in-memory hash set lookup in song lists, eliminating N+1 database queries.
  - `PlaylistDao`: Single-query joined counts and song retrieval with deterministic `position ASC` ordering and position compaction transactions.
- **Domain & Use Cases:**
  - Favorites: `ToggleFavoriteUseCase`, `GetFavoriteSongsUseCase`, `GetFavoriteSongIdsUseCase`, `IsFavoriteUseCase`.
  - Playlists: `CreatePlaylistUseCase`, `RenamePlaylistUseCase`, `DeletePlaylistUseCase`, `GetPlaylistsUseCase`, `GetPlaylistSongsUseCase`, `AddSongToPlaylistUseCase`, `RemoveSongFromPlaylistUseCase`, `ReorderPlaylistSongsUseCase`.
  - `PlaylistValidator`: Validates non-empty names, 100-character limits, whitespace trimming, and duplicate name prevention.
- **Playback Integration:**
  - Extended Phase 3 `PlaybackController` and `PlaybackViewModel` with `addSongsToQueue(songs: List<Song>)` and `playSongs(songs: List<Song>, startIndex: Int)`.
  - "Play Playlist" and "Play Favorites" route through existing `PlaybackController` and `PlaybackQueue` without duplicating player instances.

### Audio Features & Playback Architecture (Phase 5)
- **Hardware Audio Effects Architecture (`playback/audiofx`):**
  - `AudioEffectCapabilityDetector`: Safely queries `AudioEffect.queryEffects()` for standard UUIDs (`EFFECT_TYPE_EQUALIZER`, `EFFECT_TYPE_BASS_BOOST`, `EFFECT_TYPE_VIRTUALIZER`) while catching all OS/HAL exceptions.
  - `AudioEffectsManager`: Manages instances of `Equalizer`, `BassBoost`, and `Virtualizer` directly bound to ExoPlayer's active `audioSessionId` via `AnalyticsListener.onAudioSessionIdChanged`. Rebinds effects on session transitions and releases native resources on service teardown.
  - Graceful Fallback: All effect interactions are wrapped in `runCatching` to safeguard against vendor HAL crashes. Unsupported effects display explicit "Not supported on this device" states in the UI.
  - `EqualizerModels`: Dynamic band discovery (center frequencies, hardware min/max level ranges, hardware presets), level clamping, and human-friendly formatting (`60 Hz`, `1 kHz`, `+3 dB`).
  - `BassBoostState` & `VirtualizerState`: Strength mapping ($0$ to $1000$ millibels mapped to $0$–$100\%$) with strict clamping.
- **Loudness Normalization (ReplayGain):**
  - `ReplayGainParser`: Decodes ID3v2 `TXXX` frames, `VorbisComment` tags, and stream header tags into normalized track/album gain and peak values.
  - `ReplayGainManager`: Listens to `AnalyticsListener.onMetadata` and applies real-time linear volume scaling to `ExoPlayer.volume` without modifying, transcoding, or rewriting source audio files.
  - Peak-Based Anti-Clipping: When $\text{gainLinear} \times \text{peak} > 1.0$, gain is reduced to $1.0 / \text{peak}$ to prevent digital distortion.
  - Modes: `OFF` (unity gain 1.0x), `TRACK` (independent track normalization), `ALBUM` (preserves intra-album dynamics with track fallback). When metadata is absent, defaults to unity gain without inventing values.
- **Gapless Playback & Crossfade Transparency:**
  - Gapless Playback: Verified automatic support via Media3 ExoPlayer for compatible containers (FLAC, WAV, AAC, and MP3 with Xing/LAME or iTunSMPB metadata).
  - Crossfade: Verified that single-player Media3 1.11.1 does not provide native dual-stream crossfading. Parallel dual-player workarounds are deliberately avoided to prevent glitches and CPU overhead; platform limitation is transparently documented in the UI and docs.
- **Sleep Timer:**
  - `SleepTimerManager`: Structured-concurrency timer with 1-second countdown ticks exposing `StateFlow<SleepTimerState>` (`Inactive`, `Running`, `Completed`). Automatically pauses playback via `PlaybackController` upon expiration. Cancels and resets cleanly without coroutine leaks.
- **Persistent Audio Preferences:**
  - `AudioPreferencesRepository`: Uses AndroidX DataStore Preferences (`audio_preferences`) to persist user settings (equalizer state, band levels, preset, bass boost, virtualizer, ReplayGain mode).
- **Presentation & UI:**
  - `AudioSettingsScreen`: Material 3 settings screen with capability-driven cards for Equalizer (presets + dynamic band sliders), Bass Boost, Virtualizer, ReplayGain (mode chips + live track metadata status), Playback Engine info, and Sleep Timer (preset chips + live countdown + custom duration dialog).
  - `MainNavigationScreen`: Integrated 4th root navigation tab (`Audio`) with `ic_tune` vector icon.

### Listening History, Statistics, Smart Playlists & Widgets Architecture (Phase 6)
- **Room Database Schema v3 & Migration (`AuralisDatabase.kt`):**
  - Upgraded schema to version 3 via deterministic `MIGRATION_2_3`.
  - `playback_history` table: `@PrimaryKey(autoGenerate = true) id: Long`, `songId: Long`, `timestamp: Long`, `durationPlayedMs: Long`, `completed: Boolean`. Foreign key to `songs(id)` with `ON DELETE CASCADE`.
  - Indexed on `songId`, `timestamp`, and `completed` for high-throughput query performance.
- **Accurate Playback Session Tracking (`PlaybackSessionTracker.kt`):**
  - Session-aware duration tracker integrated directly into `AuralisPlaybackService`.
  - Accurately accumulates active playback time while avoiding duplicate counts during pauses, seeks, buffering, or track changes.
  - Automatically flags a play as `completed` when listening time meets or exceeds standard thresholds (80% of song duration or 30 seconds for long tracks).
- **Smart Playlists Engine (`SmartPlaylistRepositoryImpl.kt`):**
  - Dynamic on-the-fly generation of smart playlists without altering static user-created playlists:
    - **Most Played**: Songs ordered descending by total play counts.
    - **Recently Played**: Chronological log of recently played tracks.
    - **Forgotten Favorites**: Favorited tracks that haven't been played in >30 days or ever.
    - **Heavy Rotation**: Tracks with multiple repeated plays in the last 7 days.
  - Full playback queue integration via `PlaybackController`.
- **Listening Insights & Statistics UI (`ui/stats`):**
  - Dedicated Stats navigation tab in `MainNavigationScreen`.
  - Timeframe filtering (All Time, Last 30 Days, Last 7 Days).
  - Summary KPI cards: Total listening time (formatted into hours and minutes), total completed plays, average completion rate.
  - Interactive top rankings: Top Songs, Top Artists, and Top Genres with play counts and duration metrics.
  - Daily listening trend visualization and history management (clearing/resetting history with user confirmation).
- **Home Screen App Widget (`MelofyWidgetProvider.kt`):**
  - AppWidgetProvider with dedicated layout (`widget_playback.xml`) featuring album artwork, track title, artist, and remote playback controls (Play/Pause, Skip, Previous).
  - Broadcast-driven updates synchronized directly with `AuralisPlaybackService` state changes.

---

## 3. Completed Verification

| Verification | Result | Notes |
|---|---|---|
| `./gradlew --version` | ✅ Pass | Gradle 9.6.0, JDK 21.0.10 (Android Studio JBR) |
| `./gradlew clean` | ✅ Pass | Clean build cache & outputs |
| `./gradlew assembleDebug` | ✅ Pass | Generates `app-debug.apk` cleanly in 9s without errors |
| `./gradlew testDebugUnitTest` | ✅ Pass | 172/172 unit tests execute and pass across all 29 test suites |
| KSP Annotation Processing | ✅ Pass | Room DAOs, entities, and Hilt components generate cleanly |
| Hilt Code Generation | ✅ Pass | `DatabaseModule`, `RepositoryModule`, `DispatchersModule`, `PlaybackModule`, `AudioModule` validated |
| Permissions Audit | ✅ Pass | Storage and playback permissions verified; `ACCESS_NETWORK_STATE` explicitly removed via `tools:node="remove"`; 0 network permissions |
| Privacy Audit | ✅ Pass | 100% offline; no analytics, ads, Firebase, or external telemetry libraries |
| Runtime Verification | ⚠️ NOT TESTED | 0 connected physical devices or emulators detected via `adb devices` |

---

## 4. Known Limitations & Deferred Items

- **No Connected Hardware / Emulator:** `adb devices` returned 0 attached devices at verification time; compilation and unit test suite are fully verified, but physical device runtime verification was marked NOT TESTED.
- **Single-Player Crossfade Limitation:** Media3 1.11.1 does not provide a native dual-decoder crossfade pipeline in a single player instance; documented honestly in UI.

---

## 5. Next Steps / Future Enhancements

- Physical hardware and emulator UI/audio quality testing.
- UI theme customization (custom color schemes, AMOLED pure black theme).
- Android Auto / MediaBrowserService export.
- Embedded lyrics display (LRC synchronized parser).
