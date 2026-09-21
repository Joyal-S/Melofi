# Auralis — Development Status

> **Current Phase:** Phase 4 Complete (Awaiting User Review / Phase 5 Approval)  
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
| **Phase 5** | Audio Features & Advanced Controls (Equalizer, ReplayGain, Sleep Timer) | ⬜ Next |
| **Phase 6** | Polish, Stats, Smart Playlists & Performance Optimization | ⬜ Planned |

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
- **Presentation & UI:**
  - `MainNavigationScreen`: Root navigation with Material 3 `NavigationBar` (Songs, Favorites, Playlists) and persistent `PlaybackControlsBar`.
  - `PlaylistsScreen`: Displays playlist cards with song counts, create playlist dialog, rename dialog, and delete confirmation dialog.
  - `PlaylistDetailScreen`: Ordered track listing with move up/down reordering, play all, add to queue, and remove track actions.
  - `FavoritesScreen`: Fast favorite list with play all and instant unfavorite.
  - `AddToPlaylistDialog`: In-dialog quick selection or new playlist creation on the fly.
  - `SongListItem`: Reactive favorite heart toggle and 3-dots actions menu ("Add to Playlist", "Play Next").

---

## 3. Completed Verification

| Verification | Result | Notes |
|---|---|---|
| `./gradlew --version` | ✅ Pass | Gradle 9.6.0, JDK 21.0.10 (Android Studio JBR) |
| `./gradlew clean` | ✅ Pass | Clean build cache & outputs |
| `./gradlew assembleDebug` | ✅ Pass | Generates `app-debug.apk` cleanly in 7s without errors |
| `./gradlew testDebugUnitTest` | ✅ Pass | 85/85 unit tests execute and pass in all 15 test suites |
| Schema Export & Validation | ✅ Pass | Room schema `2.json` generated and verified against `MIGRATION_1_2` DDL |
| KSP Annotation Processing | ✅ Pass | Room DAOs, entities, and Hilt components generate cleanly |
| Hilt Code Generation | ✅ Pass | `DatabaseModule`, `RepositoryModule`, `DispatchersModule`, `PlaybackModule` validated |
| Permissions Audit | ✅ Pass | Storage and media playback permissions verified; zero network access |
| Privacy Audit | ✅ Pass | 100% offline; no analytics, ads, Firebase, or external telemetry libraries |
| Runtime Verification | ⚠️ NOT TESTED | 0 connected physical devices or emulators detected via `adb devices` |

---

## 4. Known Limitations & Deferred Items

- **No Connected Hardware / Emulator:** `adb devices` returned 0 attached devices at verification time; compilation and unit test suite are fully verified, but physical device runtime verification was marked NOT TESTED.
- **Audio Effects Deferred (Phase 5):** Equalizer, bass boost, ReplayGain loudness normalization, and crossfade remain scheduled for Phase 5.
- **Listening History & Analytics Deferred (Phase 6):** Listening stats, play history, and smart playlists remain scheduled for Phase 6.

---

## 5. Next Phase: Phase 5

**Focus:** Audio Features & Advanced Controls
- System Equalizer and audio effect integration.
- Bass boost and virtualizer.
- Gapless and crossfade playback configuration.
- Sleep timer.
