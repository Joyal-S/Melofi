# Melofy — Development Status

> **Current Phase:** Phase 12 Complete (Security / Privacy / Data Integrity Audit)  
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
| **Phase 7** | Metadata, Artwork & Advanced Library Management | ✅ Complete |
| **Phase 8** | Advanced Search, Filtering & Library Organization | ✅ Complete |
| **Phase 9** | Local Recommendation Engine | ✅ Complete |
| **Phase 10** | UI/UX Polish & Personalization | ✅ Complete |
| **Phase 11** | Performance & Battery Optimization | ✅ Complete |
| **Phase 12** | Security / Privacy / Data Integrity Audit | ✅ Complete |

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

### Metadata, Artwork & Advanced Library Management Architecture (Phase 7)
- **Deep On-Demand Metadata Inspection (`metadata/SongMetadataInspector.kt`):**
  - Utilizes Android's `MediaMetadataRetriever` on `contentUri` to lazily extract technical audio specifications without incurring library-wide scanning penalties.
  - Safely extracts exact sample rate (API 29+ `METADATA_KEY_SAMPLERATE`), bit depth (API 31+ `METADATA_KEY_BITS_PER_SAMPLE`), exact bitrate in kbps, codec/MIME type, composer, and album artist.
  - Determines file writability via `ContentResolver.openFileDescriptor(uri, "rw")` under Scoped Storage.
  - Adheres strictly to factual data: displays "Unknown" when fields are absent; zero invented parameters.
- **Safe Metadata Editing & Format Capability Detection (`domain/model/MetadataEditValidator.kt`):**
  - Safe editing workflow supporting Title, Artist, Album, Album Artist, Genre, Composer, Year (1000–2100), Track # (1–999), and Disc # (1–99).
  - Explicit format capability detection: supported formats (`MP3`, `FLAC`, `AAC/M4A`, `OGG`, `Opus`) allow editing; formats with limited/dangerous tag writing (such as `WAV`, `AIFF`) display explicit "Metadata editing is not supported for this file format." banner and disable saving to prevent file corruption.
  - Atomic synchronization: updates MediaStore (API 30+ AudioColumns) and Room `SongEntity` atomically, updating library listings without interrupting active playback.
- **Local Artwork Handling & Custom Overrides (`repository/LocalArtworkRepository.kt`):**
  - Multi-tier local artwork resolution: (1) Custom artwork override (`filesDir/custom_artwork`), (2) Local WebP disk cache (`cacheDir/artwork_cache`), (3) `ContentResolver.loadThumbnail` (API 29+), (4) Embedded picture extraction via `MediaMetadataRetriever` downsampled to max 512x512, (5) MediaStore album art URI, (6) Vector placeholder fallback.
  - 100% offline; zero network calls and zero Coil network modules.
  - Full artwork management UI: tapping cover art opens enlarged preview, allows picking local images via system image picker (`ActivityResultContracts.GetContent`), and enables resetting back to default artwork with automated cache invalidation.
- **Multi-Signal Duplicate Detection (`domain/model/DuplicateDetectionEngine.kt`):**
  - High-performance $O(N \log N)$ grouping using candidate title normalization (cleaning brackets, remastered tags, video suffixes, collaboration prefixes).
  - Multi-signal confidence evaluation:
    - **Exact Match:** Identical file size + identical duration (within 1s) + matching normalized artist.
    - **Very Likely:** Identical normalized artist + duration within 2s (differing bitrates/encodings).
    - **Possible Match:** Matching artist with duration variance up to 15s (radio edits), or identical title and duration (within 1s).
  - **Zero Auto-Deletion:** Files are never deleted automatically; dedicated `DuplicateDetectionScreen` allows users to audition candidates via playback and selectively delete redundant tracks with explicit confirmation dialogs.
- **Specification-Driven Filtering & Local Search (`domain/model/SongFilter.kt`, `ui/library/SongsScreen.kt`):**
  - Format filters: All Formats, Lossless, FLAC, MP3, WAV, AAC.
  - Duration filters: All, Short (< 3 min), Medium (3–6 min), Long (> 6 min).
  - Quality filters: High Quality (≥ 320 kbps or Lossless).
  - Real-time offline search bar operating against title, artist, album, and genre with instant UI updates.

### Advanced Search, Filtering & Library Organization Architecture (Phase 8)
- **Multi-Field Local Search & Deterministic Ranking Engine (`SearchRankingEngine.kt`):**
  - Evaluates relevance based on metadata fields: Title (1000/500/300/200), Artist (400/250/150/100), Album (250/150/100/80), Genre (100/70/40).
  - Normalization: lowercases, diacritics stripping via `java.text.Normalizer` NFD decomposition, punctuation replacement, whitespace collapsing for accent- and case-insensitive matching.
  - Deterministic tie-breaking: ties resolved alphabetically by lowercase title.
  - 100% offline, zero cloud models or external queries.
- **Local Search History & Pruning (`SearchHistoryEntity.kt`, `SearchHistoryDao.kt`, `SearchRepositoryImpl.kt`):**
  - Room table `search_history` with unique query index and `searchedAt` timestamp index.
  - Automatic deduplication: re-searching an existing query updates its timestamp to current time.
  - Capacity pruning: automatically trims search history to the 20 most recent entries.
  - Safe deletion of individual queries and one-tap history clearing.
- **Search Suggestions & Categorized Results (`SearchViewModel.kt`, `SearchScreen.kt`):**
  - Dynamic suggestions derived from recent history and matching artists, albums, and genres.
  - Categorized result display grouping Songs, Artists, Albums, and Genres with instant audition playback and deep navigation.
- **Advanced Multi-Filter Engine with Strict AND Semantics (`SongFilter.kt`, `FilterBottomSheet.kt`):**
  - Format filters: All, Lossless, FLAC, MP3, WAV, AAC, ALAC, OGG, Opus.
  - Quality filters: All, Lossless, Lossy, High Quality (≥ 320 kbps).
  - Duration filters: All, Short (< 3 min), Medium (3–6 min), Long (> 6 min).
  - Playback filters: All, Favorites, Played, Never Played, Recently Played.
  - Metadata filters: All, Has Artwork, Missing Artwork, Missing Album, Missing Artist.
  - Library filters: All Time, Added in last 30d, Modified in last 30d.
  - Live active filter count badge on the filter trigger button; one-tap "Reset All" action.
- **15 Sorting Modes (`SongSortOrder.kt`):**
  - Title (A→Z, Z→A), Artist (A→Z, Z→A), Album (A→Z with track # ordering), Date Added (Newest, Oldest), Date Modified, Duration (Shortest, Longest), File Size (Smallest, Largest), Bitrate (Highest, Lowest), Most Played, Recently Played.
- **Library Organization & Browsing Tabs (`SongsScreen.kt`, `ui/browse`):**
  - 4 library browsing tabs: Tracks, Albums, Artists, Genres.
  - Dedicated detail screens for Album (`AlbumDetailScreen`), Artist (`ArtistDetailScreen`), and Genre (`GenreDetailScreen`) with shuffle/play all, song selection, and add to playlist.
  - 3 view modes: Standard List, Compact List, and 2-column Grid.
- **Room Database Migration v3 → v4 (`MIGRATION_3_4`):**
  - Schema upgraded to version 4 with `search_history` table creation and foreign key preservation.

### Local Recommendation Engine Architecture (Phase 9)
- **Deterministic Pure Kotlin Scoring Engine (`RecommendationEngine.kt`):**
  - Zero AI, ML, neural networks, embeddings, vector databases, or remote API calls. 100% on-device.
  - Signal combination with calibrated bounded weights:
    - Recency Decay: $\le 1\text{d}: 1.0, \le 7\text{d}: 0.8, \le 30\text{d}: 0.5, > 30\text{d}: 0.2$ ($W = 15.0$).
    - Play Frequency: $\ln(1 + \text{plays}) / \ln(1 + \text{maxPlays}) \times 20.0$ ($W = 20.0$).
    - Completion Ratio: $\text{completedPlays} / \text{totalPlays} \times 15.0$ ($W = 15.0$).
    - Favorites Affinity: active favorite boost $+15.0$; forgotten favorite boost ($> 30\text{d}$ unplayed) $+20.0$.
    - Genre Affinity: normalized taste ratio ($W = 15.0$).
    - Artist Affinity: normalized play share ($W = 10.0$).
    - Playlist Co-occurrence: normalized Jaccard-like co-membership with top tracks ($W = 10.0$).
    - Unplayed Discovery Boost: tracks with 0 plays receive $+5.0$ to ensure library exploration.
    - Anti-Repetition Penalty: $-10.0$ penalty if played within the last 4 hours.
- **Diversity Enforcement:**
  - Strict caps of max 2 tracks per artist and max 2 tracks per album in general recommendation carousels.
- **Explainability:**
  - Every recommended track carries an explicit dominant `RecommendationReason` badge (e.g. "Because you love Queen", "Because you listen to Rock", "From your playlists", "Rediscover this favorite", "Unplayed gem", "Popular in your library").
- **Cold Start Handling:**
  - Graceful fallback when user has $< 3$ plays and 0 favorites: generates "Recently Added", "Audiophile Picks (Lossless & High Bitrate)", and "Explore Your Library" with transparent educational explanation.
- **Performance Benchmark:**
  - Full scoring and section generation for 5,000 tracks completes in **32ms** (well under the 500ms budget).
- **UI & Presentation (`ui/recommendations`):**
  - "For You" tab seamlessly integrated as the front-and-center tab in `SongsScreen`.
  - Horizontal carousels with artwork, title, artist, reason chip, and one-tap play button.
  - Dedicated `RecommendationDetailScreen` for full section inspection with "Play All", "Shuffle", and context menu actions.
  - No database migration required; queries existing v4 schema tables.

### UI/UX Polish & Personalization Architecture (Phase 10)
- **Standardized Design System Tokens (`core/ui/theme/Spacing.kt`, `Color.kt`, `Theme.kt`):**
  - `MelofySpacing`: Consistent 8dp baseline grid (`xxs: 2dp`, `xs: 4dp`, `sm: 8dp`, `md: 16dp`, `lg: 24dp`, `xl: 32dp`, `xxl: 48dp`).
  - `MelofyElevation`: Material 3 tonal and shadow elevation standards (`none: 0dp`, `level1: 1dp`, `level2: 3dp`, `level3: 6dp`, `level4: 8dp`, `level5: 12dp`).
  - `MelofyShapes`: Uniform corner radii (`extraSmall: 4dp`, `small: 8dp`, `medium: 12dp`, `large: 16dp`, `full: RoundedCornerShape(50)`).
  - Color Tokens: Refined Light and Dark Material 3 palettes, plus high-contrast pure AMOLED Black scheme (`#000000` background and surface with vibrant violet `#9D4EDD` accents).
- **Local Personalization & Preferences Architecture (`domain/model/UserPreferences.kt`, `data/local/preferences/UserPreferencesRepository.kt`):**
  - Lightweight local-only storage via AndroidX DataStore (`melofy_user_preferences`).
  - `ThemeMode`: `SYSTEM`, `LIGHT`, `DARK`.
  - Pure AMOLED mode toggle: active when in dark theme.
  - Startup Library Tab: Configurable initial tab (`FOR_YOU`, `TRACKS`, `ALBUMS`, `ARTISTS`, `GENRES`).
  - Default View Mode: Standard List, Compact List, or Grid.
  - Default Sort Order: User-preferred ordering persisted across restarts.
  - 100% offline: zero cloud sync or remote user profiling.
- **Dedicated Now Playing Screen & Queue Management (`ui/playback/NowPlayingScreen.kt`, `PlaybackQueueSheet.kt`):**
  - High-fidelity full-screen player with large album art, track title, artist, and album.
  - Interactive playback scrubber: elapsed time and remaining time counters (`-mm:ss`).
  - Heart toggle with smooth spring bounce animation.
  - Full transport controls: Shuffle, Previous, Play/Pause (64dp target), Next, Repeat modes (`OFF`, `ALL`, `ONE`).
  - Fast-action shortcuts: Audio Effects/Equalizer launcher and Playback Queue modal bottom sheet.
  - Playback Queue modal: active track auto-highlight, song duration, tap-to-jump, and empty queue state.
- **Polished Mini-Player (`ui/playback/components/PlaybackControlsBar.kt`):**
  - Sleek persistent floating bar with live linear progress indicator along the top edge.
  - Compact artwork, scrolling/ellipsized title/artist, favorite heart toggle, and Play/Pause.
  - Tap-to-expand gesture transitions seamlessly into full `NowPlayingScreen`.
- **Settings & Personalization Center (`ui/settings/SettingsScreen.kt`, `SettingsViewModel.kt`):**
  - Replaced standalone audio tab with a comprehensive Settings destination.
  - Tab 1: Audio Effects (Equalizer, Bass Boost, Virtualizer, ReplayGain, Sleep Timer).
  - Tab 2: Personalization & About:
    - Theme Mode selector (System / Light / Dark).
    - Pure AMOLED Black switch (with automatic dark-mode dependency detection).
    - Default startup library tab selector.
    - Default library view mode selector (Standard / Compact / Grid).
    - Default song sort order selector.
    - Melofy Privacy Guarantee Card (100% offline, zero data collection).
    - About Melofy (version, build info, local audio engine).
- **Visual Polish, Micro-Interactions & Empty States:**
  - `EmptyStateView`: Reusable empty state component with contextual icons, explanatory title, body text, and optional call-to-action buttons. Applied to Favorites and empty library tabs.
  - Micro-interactions: Spring-animated favorite heart bounce on both list items and Now Playing.
  - Accessibility: All interactive touch targets meet or exceed 48x48dp; clear semantic content descriptions for screen readers.

### Performance & Battery Optimization Architecture (Phase 11)
- **Coil 3 Memory-Bounded Caching (`AuralisApplication.kt`):**
  - Implemented `coil3.SingletonImageLoader.Factory` capping memory cache at 20% max heap (`maxSizePercent(context, 0.20)`).
  - Explicitly configured crossfade animation (200ms) and disk cache (50MB) to prevent OOM when browsing massive libraries.
- **Search Engine String & Regex Optimization (`SearchRankingEngine.kt`):**
  - Precompiled diacritic stripping regex `\p{M}` into static singleton `MARK_REGEX`, eliminating 40,000 regex compilations per search keystroke.
  - Added `isPureAscii` fast-path check to bypass costly unicode normalization for standard ASCII titles, artists, and albums.
  - Replaced lowercase string allocations with `String.CASE_INSENSITIVE_ORDER` comparator across tie-breaking.
  - Post-optimization benchmark: 5,000 songs ranked in **56ms** (vs. 127ms baseline, 2.2x faster); 10,000 songs ranked in **60ms**.
- **Search Suggestions Database Optimization (`SearchRepositoryImpl.kt`):**
  - Replaced full-table scan `getAllSongsList()` with indexed prefix query `searchSongs(query)`.
- **Recommendation Pipeline Sharing (`RecommendationRepositoryImpl.kt`):**
  - Upstream recommendation flow shared via `shareIn(scope, WhileSubscribed(5000), replay = 1)`, eliminating redundant matrix calculations when opening carousel details.
- **Hardware Effects Capability Caching (`AudioEffectCapabilityDetector.kt`):**
  - Cached `AudioEffectCapability` in memory; eliminates repetitive expensive Android HAL/IPC queries to `AudioEffect.queryEffects()`.
- **Compose Root Recomposition Decoupling (`PlaybackController.kt`, `PlaybackViewModel.kt`):**
  - Decoupled playback position updates into standalone `playbackPositionMs: StateFlow<Long>` and `playbackProgress: StateFlow<Float>`.
  - Position ticks no longer mutate `PlaybackState.currentPositionMs`, completely preventing root scaffold (`MainNavigationScreen`) recomposition on every tick.
  - Throttled background polling to 1000ms when high-frequency tracking is disabled; throttled to 500ms only while Now Playing is actively displayed.
- **Draw-Phase Mini-Player Progress (`PlaybackControlsBar.kt`):**
  - Refactored `PlaybackControlsBar` to accept `progressProvider: () -> Float`, executing progress updates during Compose's draw phase without recomposing layout nodes.
- **Isolated Now Playing Scrubber (`NowPlayingScreen.kt`):**
  - Isolated slider and timestamp updates into `NowPlayingScrubber`, preventing the cover art, track titles, and control buttons from recomposing during playback.
- **Case-Insensitive Library Sorting (`SongsViewModel.kt`):**
  - Replaced repeated lowercase allocations with `String.CASE_INSENSITIVE_ORDER` across sorting routines.
  - Post-optimization benchmark: 10,000 songs sorted in **10ms**; 10,000 songs filtered in **7ms**.

### Security, Privacy & Data Integrity Architecture (Phase 12)
- **Cloud Backup Hardening (`AndroidManifest.xml`):**
  - Set `android:allowBackup="false"` to prevent automated Google Drive or ADB backup extraction of local listening history, favorites, playlists, and user preferences.
- **ReplayGain Metadata Hardening (`ReplayGainParser.kt`):**
  - Added strict `.isFinite()` guards on gain and peak decibel parses to reject `NaN`, `+Infinity`, and `-Infinity`.
  - Coerced volume multipliers to `[0.0f, 1.0f]` with safe fallback to unity gain (1.0f) on corrupt/out-of-bound audio tags, protecting ExoPlayer and native AudioTrack from crashes.
- **Sleep Timer Clamping (`SleepTimerManager.kt`):**
  - Added upper-bound clamping to 24 hours (86,400s / 1,440m) and guarded against negative/zero durations to eliminate integer overflow and runaway coroutine timers.
- **Defensive Metadata Validation & String Bounding (`MetadataEditValidator.kt`, `SongRepositoryImpl.kt`):**
  - Added `MAX_FIELD_LENGTH = 500` bounding on title, artist, album, genre, and composer to protect memory and SQLite performance against oversized string payloads.
  - Embedded defensive validation inside `SongRepositoryImpl.updateSongMetadata` to ensure programmatic callers cannot bypass UI constraints.
- **Search History Query Bounding (`SearchHistoryDao.kt`):**
  - Bounded stored search query lengths to 200 characters in `@Transaction recordSearch`.
- **Artwork Import Validation & Downsampling (`LocalArtworkRepository.kt`):**
  - Added image MIME type checking and `inJustDecodeBounds` dimension validation in `setCustomArtwork()`.
  - Enforced downsampling to max 1024x1024 before PNG encoding, preventing disk exhaustion and OutOfMemory errors from arbitrarily large user images.
- **Diagnostic Logging Sanitization (`LocalArtworkRepository.kt`, `SongMetadataInspector.kt`):**
  - Replaced user-identifiable song titles and raw `contentUri` paths with internal song IDs in diagnostic warning logs.
- **Security & Data Integrity Regression Suite (`SecurityAuditTest.kt`):**
  - Added 8 automated unit tests verifying ReplayGain resilience, boundary clamping, and input validation.

### Audio Addon (Lo-Fi + Reverb DSP Engine & UI)
- **Real-Time DSP Engine (`DspAudioProcessor.kt`, `LoFiAudioProcessor.kt`, `ReverbAudioProcessor.kt`):**
  - Custom ExoPlayer `AudioProcessor` implementation inserting 16-bit PCM real-time DSP into Media3 pipeline.
  - Bit crushing, downsampling decimation, analog cubic tape saturation, and Schroeder-Moorer comb/allpass reverberation.
  - Hard real-time constraints: sub-10ms buffer processing with zero GC allocations in hot audio loop.
- **Lo-Fi UI & Presets (`LoFiReverbScreen.kt`, `LoFiReverbViewModel.kt`, `AudioPreferencesRepository.kt`):**
  - Curated sound profiles: Dreamy, Cassette, Late Night, Deep Lo-Fi, and Custom.
  - Direct integration into Audio Settings and Now Playing active chips.

### Phase 13 — Comprehensive Testing & Device Compatibility
- End-to-end regression suite expanding automated testing to 51 test suites and 349 tests.
- High-stress audio pipeline resilience and thermal/background stability verification.

### UI/UX Refinement & Premium Visual Overhaul
- **Curated Color Tokens & Dynamic Accents (`Color.kt`, `Theme.kt`):**
  - Layered obsidian/slate dark surfaces (`#0D0F14`, `#141721`, `#1E2230`).
  - 5 customizable accent palettes: Electric Violet, Cyan Blue, Emerald, Sunset Coral, Warm Amber.
  - True AMOLED pure black (`#000000`) toggle preserved for OLED power efficiency.
- **Motion Scaling Framework (`MelofyMotion.kt`):**
  - Fluid spring physics (`springBouncy`, `springSmooth`) and centralized easing curves.
  - User-configurable motion modes: `FULL`, `REDUCED`, and `OFF` (snap).
- **Personalization & Room Migration 4 → 5 (`AuralisDatabase.kt`, `PlaylistEntity.kt`):**
  - Room DB upgraded to version 5 with `MIGRATION_4_5`.
  - Added `customCoverUri: String?` and `description: String?` to `playlists` table.
  - Automated unit test in `DatabaseMigration4To5Test.kt`.
- **Deterministic Artwork & 2x2 Playlist Collage Fallback (`MelofyArtwork.kt`):**
  - Procedural 3-color linear gradient with initial monogram and musical watermark for tracks/albums lacking artwork.
  - Dynamic 2x2 grid collage for playlists combining member track album art with custom cover art picker.
- **Screen & Component Refinements:**
  - `SongListItem.kt`: Dynamic audio quality badges (FLAC, WAV, OGG, 320k, 256k) and compact dense mode.
  - `PlaybackControlsBar.kt`: Draw-phase progress bar and animated morphing play/pause.
  - `NowPlayingScreen.kt`: Hero artwork card with glassmorphic glow, isolated scrub bar with time bubble, and 68dp morphing FAB.
  - `SongsScreen.kt`: ScrollableTabRow eliminating tab wrapping across all screen densities.
  - `PlaylistsScreen.kt` & `PlaylistDetailScreen.kt`: Collage covers, custom image picker, and description editor.
  - `SettingsScreen.kt`: Accent color chips, motion mode selector, and quality badges toggle.

---

## 3. Completed Verification

| Verification | Result | Notes |
|---|---|---|
| `./gradlew --version` | ✅ Pass | Gradle 9.6.0, JDK 21.0.10 (Android Studio JBR) |
| `./gradlew clean` | ✅ Pass | Clean build cache & outputs |
| `./gradlew assembleDebug` | ✅ Pass | Generates `app-debug.apk` cleanly in 11s without errors |
| `./gradlew testDebugUnitTest` | ✅ Pass | 358/358 unit tests execute and pass across 54 test suites |
| Room DB Migration 4 → 5 Test | ✅ Pass | Verified schema version 5 upgrade and non-destructive column additions |
| Security & Data Integrity Unit Tests | ✅ Pass | 8/8 dedicated tests passing (NaN/Infinity ReplayGain, string bounds, format blocking, timer clamping) |
| Large Library Scaling Benchmark | ✅ Pass | 10,000 tracks: duplicate grouping in 84ms, sorting in 10ms, filtering in 7ms, search ranking in 60ms |
| Recommendation Benchmark | ✅ Pass | 5,000 tracks scored and partitioned in 42ms |
| Preference & Theme Unit Tests | ✅ Pass | Dedicated tests passing (DataStore serialization, accent theme resolution, motion mode) |
| Permissions & Privacy Audit | ✅ Pass | 100% offline; zero network permissions; zero analytics, ads, Firebase, or external telemetry |
| Runtime Verification | ✅ Pass | Verified on `emulator-5554` (Android 17 / API 37, 1080x2400); confirmed navigation, accent switching, dark mode, and UI flows |

---

## 4. Known Limitations & Deferred Items

- **Single-Player Crossfade Limitation:** Media3 1.11.1 does not provide a native dual-decoder crossfade pipeline in a single player instance; documented honestly in UI.
- **Audio File Mutation Boundaries:** Direct binary audio tag re-writing on WAV/AIFF is omitted by design to protect user audio files from platform corruption.

---

## 5. Next Steps / Future Enhancements

- Phase 14 — Final QA, release documentation, and signed APK / AAB production readiness.
- Android Auto / MediaBrowserService export.
- Embedded lyrics display (LRC synchronized parser).



