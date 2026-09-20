# Auralis — Project Specification

> **Working Name:** Auralis  
> **Type:** Native Android Application  
> **Vision:** A local-first, offline music player focused on high-quality audio playback and a polished modern user experience.  
> **Status:** Phase 1 — Foundation & Project Bootstrap Complete  
> **Last Updated:** 2026-09-21

---

## Table of Contents

1. [Product Vision](#1-product-vision)
2. [Technology Stack & Versions](#2-technology-stack--versions)
3. [Module Structure](#3-module-structure)
4. [Architecture Overview](#4-architecture-overview)
5. [Package Structure](#5-package-structure)
6. [Feature Boundaries](#6-feature-boundaries)
7. [Data / Domain / UI Layers](#7-data--domain--ui-layers)
8. [Playback Architecture (Media3)](#8-playback-architecture-media3)
9. [Room Database Design](#9-room-database-design)
10. [Navigation Structure](#10-navigation-structure)
11. [Local Audio Discovery & Import](#11-local-audio-discovery--import)
12. [Background Playback ↔ UI Communication](#12-background-playback--ui-communication)
13. [Testing Strategy](#13-testing-strategy)
14. [Android-Specific Risks](#14-android-specific-risks)
15. [Privacy & Security](#15-privacy--security)
16. [Naming Conventions](#16-naming-conventions)
17. [Project Rules](#17-project-rules)
18. [Phased Roadmap](#18-phased-roadmap)

---

## 1. Product Vision

Auralis is a completely local Android music player. It allows users to import and play audio files stored on their device. The application works without an account, backend, subscription, advertisements, or internet connection.

### Core Principles

- **Offline-first.** Local data is the single source of truth.
- **Privacy-respecting.** No analytics, no cloud, no tracking.
- **High-quality audio.** Preserve original files; never transcode unnecessarily.
- **Honest metadata.** Display actual codec, bitrate, sample rate, bit depth. Never imply quality enhancement that did not occur.
- **Clean architecture.** Separated UI, business logic, data, and playback layers.

---

## 2. Technology Stack & Versions (Verified)

> **Audit Date:** 2026-09-20  
> **Audit Method:** Authoritative web sources, official documentation, Maven Central, GitHub releases, build verification.  
> **Previous errors corrected:** AGP was listed as 9.4.1 (does not exist; correct is 9.4.0). Kotlin was listed as 2.4.20 (incompatible; AGP 9.4.0 bundles Kotlin 2.2.10). KSP was updated to 2.2.10-2.0.2 for Kotlin 2.2.10.

### 2.1 Verified Dependency Matrix

| Component | Version | Compatibility | Classification | Evidence |
|---|---|---|---|---|
| **Android Gradle Plugin** | 9.4.0 | ✅ Verified | A — Verified compatible | Official release (Sep 2026). Requires Gradle ≥ 9.6.0, JDK ≥ 17. |
| **Kotlin (KGP)** | 2.2.10 | ✅ Verified | A — Verified compatible | Bundled by AGP 9.4.0. Cannot independently upgrade to 2.3/2.4 without build instability risk. |
| **Gradle** | 9.6.0 | ✅ Verified | A — Verified compatible | AGP 9.4.0 minimum/default = Gradle 9.6.0. Using the documented default. |
| **KSP** | 2.2.10-2.0.2 | ✅ Verified (Phase 1 correction) | A — Verified compatible | KSP 2.2.10-2.0.2 matches Kotlin 2.2.10 compiler. |
| **Compose BOM** | 2026.09.00 | ✅ Verified (Phase 1 correction) | A — Verified compatible | BOM 2026.09.00 resolves to Compose 1.12.1 requiring compileSdk 37 (Platform 37.2 installed). |
| **Compose Compiler Plugin** | 2.2.10 | ✅ Verified | A — Verified compatible | `org.jetbrains.kotlin.plugin.compose` version MUST match Kotlin version exactly. |
| **Material 3** | Via Compose BOM | ✅ Verified | A — Verified compatible | `androidx.compose.material3:material3` — version managed by BOM. |
| **AndroidX Media3** | 1.11.1 | ✅ Verified | A — Verified compatible | Supports minSdk 21+, compileSdk 37, AGP 9. |
| **Room** | 2.8.5 | ✅ Verified | A — Verified compatible | KSP-based. Compatible with Kotlin 2.2.x and KSP 2.2.10-2.0.2. |
| **Hilt (Dagger)** | 2.60.1 | ✅ Verified | A — Verified compatible | Requires AGP ≥ 9 → needs Hilt ≥ 2.59. KSP support is stable since 2.60. |
| **Hilt Navigation Compose** | 1.4.0 | ✅ Verified | A — Verified compatible | `androidx.hilt:hilt-navigation-compose`. Independent versioning from Dagger. |
| **Navigation Compose** | 2.10.1 | ✅ Verified | A — Verified compatible | Supports type-safe routes via `@Serializable`. Requires kotlinx-serialization plugin. |
| **Coil** | 3.6.3 | ✅ Verified | A — Verified compatible | Latest stable (Sep 18, 2026). Only `coil-compose` module needed. No INTERNET permission. |
| **DataStore** | 1.2.1 | ✅ Verified | A — Verified compatible | Stable Preferences DataStore. No known issues with AGP 9 / Kotlin 2.2. |
| **Kotlin Coroutines** | 1.11.0 | ✅ Verified | A — Verified compatible | `kotlinx-coroutines-android`. Compatible with Kotlin 2.2.x. |
| **Kotlin Serialization JSON** | 1.11.0 | ✅ Verified | A — Verified compatible | Runtime library. Plugin version = Kotlin version (2.2.10). |
| **Lifecycle** | 2.11.0 | ✅ Verified | A — Verified compatible | `lifecycle-runtime-compose`, `lifecycle-viewmodel-compose`. Stable. |
| **Activity Compose** | 1.13.0 | ✅ Verified | A — Verified compatible | `androidx.activity:activity-compose`. Stable. |
| **Core** | 1.19.0 | ✅ Verified | A — Verified compatible | `androidx.core:core` (not `core-ktx` — KTX merged into main artifact). |

### 2.2 SDK & Build Configuration

| Setting | Value | Evidence |
|---|---|---|
| **minSdk** | 29 | Android 10. Native scoped storage. ~92%+ device coverage. |
| **targetSdk** | 36 | Latest stable target platform. |
| **compileSdk** | 37 | Platform 37.2 installed locally. Required by Compose BOM 2026.09.00 and AndroidX libraries. |
| **Build Tools** | 36.0.0 | AGP 9.4.0 default. Installed locally. |
| **NDK** | 28.2.13676358 | AGP 9.4.0 default. Installed locally. Not actively needed. |
| **JVM Target** | 17 | AGP 9.4.0 baseline requirement. JDK 17 is the minimum for Gradle 9.6+. |

### 2.3 JDK / Gradle Configuration

| Property | Value | Rationale |
|---|---|---|
| **Gradle JDK** | Android Studio JBR (OpenJDK 21.0.10) | `JAVA_HOME = C:\Program Files\Android\Android Studio\jbr`. This is the JDK Gradle uses to execute. |
| **JVM Target for Kotlin compilation** | 17 | AGP 9.4 baseline. Using 17 (not 21) ensures broadest compatibility. |
| **System JDK (Oracle 26)** | NOT used for Gradle | Present on PATH but should not be used. Gradle will use `JAVA_HOME` or `org.gradle.java.home`. |
| **`org.gradle.java.home`** | Set in `gradle.properties` | Explicitly point to AS JBR to avoid accidental use of system JDK 26. |

### 2.4 Detected Local Environment

| Tool | Detected Version |
|---|---|
| Java (system) | Oracle JDK 26 |
| Java (Android Studio JBR) | OpenJDK 21.0.10 |
| Android SDK | `C:\Users\joyal\AppData\Local\Android\Sdk` |
| Installed Platforms | android-34, android-35, android-36, android-36.1 |
| Installed Build Tools | 35.0.0, 36.0.0 |
| NDK | 28.2.13676358 |
| CMake | 3.22.1 |
| Global Kotlin CLI | Not installed (handled by AGP 9 built-in) |
| Global Gradle CLI | Not installed (handled by Gradle Wrapper) |

### 2.5 Exact Dependency Artifacts

#### Gradle Plugins (declared in root `build.gradle.kts` / version catalog)

| Plugin | Artifact ID | Version |
|---|---|---|
| Android Application | `com.android.application` | 9.4.0 |
| Compose Compiler | `org.jetbrains.kotlin.plugin.compose` | 2.2.10 |
| KSP | `com.google.devtools.ksp` | 2.3.12 |
| Hilt | `com.google.dagger.hilt.android` | 2.60.1 |
| Kotlin Serialization | `org.jetbrains.kotlin.plugin.serialization` | 2.2.10 |

> **NOTE:** Do NOT apply `org.jetbrains.kotlin.android`. AGP 9.4.0 provides Kotlin 2.2.10 built-in.

#### App Dependencies

```toml
# gradle/libs.versions.toml — Versions
[versions]
agp = "9.4.0"
kotlin = "2.2.10"
ksp = "2.3.12"
composeBom = "2026.07.00"
media3 = "1.11.1"
room = "2.8.5"
hilt = "2.60.1"
hiltNavigationCompose = "1.4.0"
navigationCompose = "2.10.1"
coil = "3.6.3"
datastore = "1.2.1"
coroutines = "1.11.0"
serialization = "1.11.0"
lifecycle = "2.11.0"
activityCompose = "1.13.0"
core = "1.19.0"
```

#### Media3 Modules (exact artifacts)

| Artifact | Purpose | Required? |
|---|---|---|
| `androidx.media3:media3-exoplayer` | Core playback engine | ✅ Yes |
| `androidx.media3:media3-session` | MediaSession, background playback, notifications, Bluetooth | ✅ Yes |
| `androidx.media3:media3-ui` | Pre-built PlayerView / controls (may use sparingly with Compose) | ⚠️ Optional — evaluate during Phase 1 |

> Do NOT add: `media3-datasource-okhttp`, `media3-cast`, `media3-exoplayer-dash`, `media3-exoplayer-hls`, or any streaming/network modules.

#### Coil Modules (exact artifacts)

| Artifact | Purpose | Required? |
|---|---|---|
| `io.coil-kt.coil3:coil-compose` | Local image loading for Compose (includes core transitively) | ✅ Yes |

> Do NOT add: `coil-network-okhttp`, `coil-network-ktor`, or any network image loader.  
> Coil does NOT inject `INTERNET` permission into the manifest. No manifest merging concern.

#### Room Setup (KSP)

```kotlin
// In app/build.gradle.kts
dependencies {
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)
}
```

#### Hilt Setup (KSP)

```kotlin
// In app/build.gradle.kts
plugins {
    alias(libs.plugins.hilt)
    alias(libs.plugins.ksp)
}
dependencies {
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.hilt.navigation.compose)
}
```

#### Compose Compiler Setup

```kotlin
// In app/build.gradle.kts
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.compose.compiler)
    // NO org.jetbrains.kotlin.android — built-in with AGP 9
}

// Kotlin compiler options (NOT inside android { } block)
kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}
```

### 2.6 Critical AGP 9.4.0 Build Rules

1. **Do NOT apply** `org.jetbrains.kotlin.android` — causes `Cannot add extension with name 'kotlin'` build failure.
2. **Do NOT use KAPT** — incompatible with AGP 9 built-in Kotlin. Use KSP exclusively.
3. **Do apply** `org.jetbrains.kotlin.plugin.compose` version `2.2.10` — Compose compiler is still a separate plugin from built-in Kotlin.
4. **Do apply** `org.jetbrains.kotlin.plugin.serialization` version `2.2.10` — for type-safe navigation routes.
5. **Kotlin compiler options** go in the top-level `kotlin { compilerOptions { ... } }` block, NOT inside `android { kotlinOptions { } }`.
6. **JAVA_HOME** must point to Android Studio JBR: `C:\Program Files\Android\Android Studio\jbr`.
7. **Set `org.gradle.java.home`** in `gradle.properties` to the AS JBR path to prevent system JDK 26 from being used by Gradle.
8. **JVM target is 17** (not 21). AGP 9.4.0 requires JDK 17 as baseline.
9. **`core-ktx` is obsolete** — use `androidx.core:core:1.19.0` instead. KTX extensions merged into main artifact.

### 2.7 Why Not Kotlin 2.4.20?

The Phase 0 specification incorrectly proposed Kotlin 2.4.20. This is **not compatible** with AGP 9.4.0's built-in Kotlin.

| Approach | Outcome | Risk |
|---|---|---|
| Use AGP 9.4.0 built-in (Kotlin 2.2.10) | ✅ Safe, tested by Google | None |
| Override KGP to 2.4.20 via buildscript | ⚠️ May work | `ClassCastException`, compiler crashes, untested combination |
| Set `android.builtInKotlin=false` + manual 2.4.20 | ⚠️ Works but deprecated | Will break in AGP 10.0; creates migration debt |

**Decision: Use Kotlin 2.2.10 (bundled).** Kotlin 2.2.10 is a stable, production-ready release. It has all features needed for this project. Upgrading to 2.3/2.4 can happen when a future AGP version bundles them natively.

### 2.8 Offline / Privacy Verification

| Concern | Status |
|---|---|
| Firebase | ❌ Not included |
| Supabase | ❌ Not included |
| Analytics SDK | ❌ Not included |
| Advertising SDK | ❌ Not included |
| Authentication | ❌ Not included |
| Cloud database | ❌ Not included |
| Cloud storage | ❌ Not included |
| Network audio streaming | ❌ Not included (no Media3 network modules) |
| `INTERNET` permission | ❌ Not required. Coil does not inject it. No dependency requests it. |
| All dependencies offline-capable | ✅ Verified |

---

## 3. Module Structure

A single-module architecture initially, with clear internal package boundaries ready for future modularization.

```
auralis/                          (root project)
├── app/                          (main application module)
│   ├── src/main/
│   │   ├── java/com/auralis/app/
│   │   ├── res/
│   │   └── AndroidManifest.xml
│   ├── src/test/                 (unit tests)
│   ├── src/androidTest/          (instrumentation tests)
│   └── build.gradle.kts
├── gradle/
│   ├── wrapper/
│   │   ├── gradle-wrapper.jar
│   │   └── gradle-wrapper.properties
│   └── libs.versions.toml        (version catalog)
├── build.gradle.kts              (root build file)
├── settings.gradle.kts
├── gradle.properties
├── local.properties              (gitignored — SDK path)
├── .gitignore
├── PROJECT_SPECIFICATION.md
└── README.md
```

### Future Multi-Module Plan

When the codebase grows, extract into feature modules:

| Module | Purpose |
|---|---|
| `:core:data` | Room database, DAOs, repositories |
| `:core:domain` | Use cases, domain models |
| `:core:playback` | Media3 service, MediaSession, audio engine |
| `:core:ui` | Shared Compose components, theme, design system |
| `:feature:library` | Library browsing (songs, albums, artists, genres) |
| `:feature:player` | Now-playing screen, mini-player |
| `:feature:playlists` | Playlist management |
| `:feature:search` | Search functionality |
| `:feature:settings` | App settings, equalizer, audio controls |

> **Rule:** Do not modularize prematurely. Extract modules only when clear boundaries justify the build-system overhead.

---

## 4. Architecture Overview

### Pattern: MVVM + Repository + Use Cases

```
┌─────────────────────────────────────────────────┐
│                   UI Layer                       │
│  (Compose Screens, ViewModels, UI State)         │
├─────────────────────────────────────────────────┤
│                 Domain Layer                     │
│  (Use Cases, Domain Models, Repository Interfaces)│
├─────────────────────────────────────────────────┤
│                  Data Layer                      │
│  (Repository Implementations, Room DAOs,         │
│   DataStore, MediaStore Scanner, Media3 Service) │
└─────────────────────────────────────────────────┘
```

### Key Architectural Rules

1. **ViewModels** expose `StateFlow` / `SharedFlow` to Compose UI. They do not hold Android `Context`.
2. **Repositories** are the single gateway between domain/UI and data sources. They are injected via Hilt.
3. **Use Cases** encapsulate single-purpose business logic. Simple CRUD may bypass use cases initially.
4. **Room DAOs** return `Flow<List<T>>` for reactive data. `suspend` functions for writes.
5. **The playback engine** (Media3 `MediaSessionService`) is a separate Android service. UI communicates with it via `MediaController` / `MediaBrowser`, never by direct service binding.
6. **Navigation** uses Jetpack Compose Navigation with type-safe routes.
7. **Dependency injection** uses Hilt throughout. `@HiltViewModel`, `@Inject constructor`, `@Module`, `@InstallIn`.

---

## 5. Package Structure

Base package: `com.auralis.app`

```
com.auralis.app/
├── AuralisApplication.kt              (@HiltAndroidApp)
├── MainActivity.kt                    (@AndroidEntryPoint, single-activity host)
│
├── data/
│   ├── local/
│   │   ├── db/
│   │   │   ├── AuralisDatabase.kt     (Room database)
│   │   │   ├── dao/                   (Room DAOs)
│   │   │   ├── entity/               (Room entities)
│   │   │   └── converter/            (Room type converters)
│   │   ├── datastore/
│   │   │   └── UserPreferences.kt    (DataStore preferences)
│   │   └── scanner/
│   │       └── MediaStoreScanner.kt  (MediaStore audio file discovery)
│   ├── repository/                    (Repository implementations)
│   └── mapper/                        (Entity ↔ Domain model mappers)
│
├── domain/
│   ├── model/                         (Domain models: Song, Album, Artist, etc.)
│   ├── repository/                    (Repository interfaces)
│   └── usecase/                       (Use cases)
│
├── playback/
│   ├── AuralisPlaybackService.kt      (MediaSessionService)
│   ├── AuralisMediaController.kt      (MediaController wrapper)
│   ├── PlaybackState.kt               (Playback state models)
│   └── audio/                         (Audio effects: EQ, bass boost, etc.)
│
├── ui/
│   ├── navigation/
│   │   ├── AuralisNavHost.kt          (Navigation graph)
│   │   └── Route.kt                   (Type-safe route definitions)
│   ├── theme/
│   │   ├── Theme.kt
│   │   ├── Color.kt
│   │   ├── Type.kt
│   │   └── Shape.kt
│   ├── components/                    (Shared Compose components)
│   │   ├── MiniPlayer.kt
│   │   ├── SongListItem.kt
│   │   └── ...
│   ├── library/                       (Library screens + ViewModels)
│   │   ├── LibraryScreen.kt
│   │   ├── LibraryViewModel.kt
│   │   ├── songs/
│   │   ├── albums/
│   │   ├── artists/
│   │   └── genres/
│   ├── player/                        (Now-playing screen)
│   │   ├── PlayerScreen.kt
│   │   └── PlayerViewModel.kt
│   ├── playlists/                     (Playlist management)
│   ├── search/                        (Search)
│   └── settings/                      (Settings, audio controls)
│
└── di/                                (Hilt modules)
    ├── DatabaseModule.kt
    ├── RepositoryModule.kt
    ├── PlaybackModule.kt
    └── DataStoreModule.kt
```

---

## 6. Feature Boundaries

### Phase 1 — Foundation (First Implementation)

| Feature | Scope |
|---|---|
| **Media Scanner** | Discover audio files via MediaStore; populate Room DB |
| **Library: Songs** | Display all songs; sort by title/artist/album/date |
| **Basic Playback** | Play/pause, seek, previous/next via Media3 |
| **Background Playback** | MediaSessionService, foreground notification |
| **Now Playing** | Full-screen player with album art, controls, seek bar |
| **Mini Player** | Persistent bottom bar with current track info |
| **Theme** | Material 3 dynamic color, dark/light mode |

### Phase 2 — Library Expansion

| Feature | Scope |
|---|---|
| **Albums** | Album browsing, album detail screen |
| **Artists** | Artist browsing, artist detail screen |
| **Genres** | Genre browsing |
| **Album Art** | Embedded art extraction, Coil loading |
| **Search** | Full-text search across songs/albums/artists |
| **Sorting & Filtering** | Multiple sort criteria, filters |

### Phase 3 — Queue & Playlists

| Feature | Scope |
|---|---|
| **Queue** | View/edit play queue, drag-to-reorder |
| **Shuffle & Repeat** | Shuffle modes, repeat one/all/off |
| **Playlists** | Create/edit/delete user playlists |
| **Favorites** | Mark songs as favorites |
| **Recently Played** | Auto-tracked play history |

### Phase 4 — Audio Controls & Personalization

| Feature | Scope |
|---|---|
| **Equalizer** | System EQ integration |
| **Bass Boost** | Android AudioEffect |
| **Crossfade** | Cross-fade between tracks |
| **Gapless Playback** | Media3 gapless support |
| **Sleep Timer** | Countdown or end-of-track timer |
| **ReplayGain** | Loudness normalization |
| **Play Counts** | Track-level play counts |
| **Listening Stats** | Dashboard with listening trends |
| **Smart Playlists** | Auto-generated based on history |

### Phase 5 — Polish & Advanced

| Feature | Scope |
|---|---|
| **Audio Metadata Display** | Codec, bitrate, sample rate, bit depth, channels |
| **Balance Control** | L/R stereo balance |
| **Bluetooth Controls** | AVRCP metadata, media button handling |
| **Android Auto** | (Stretch goal) Media browser integration |
| **Widget** | Home screen playback widget |

---

## 7. Data / Domain / UI Layers

### Data Layer

| Component | Responsibility |
|---|---|
| `AuralisDatabase` | Room database; single instance via Hilt |
| `SongDao`, `AlbumDao`, etc. | Data access; return `Flow` for reads, `suspend` for writes |
| `SongEntity`, `AlbumEntity`, etc. | Room entities mapped to SQLite tables |
| `MediaStoreScanner` | Queries Android `MediaStore.Audio` for file discovery |
| `UserPreferences` | DataStore for settings (theme, sort order, last played, etc.) |
| `SongRepositoryImpl`, etc. | Implements domain repository interfaces |

### Domain Layer

| Component | Responsibility |
|---|---|
| `Song`, `Album`, `Artist`, `Genre` | Plain Kotlin data classes — no Room/Android annotations |
| `SongRepository`, `AlbumRepository` | Interfaces defining data contracts |
| `GetSongsUseCase`, `SearchLibraryUseCase` | Encapsulate business logic; injected into ViewModels |

### UI Layer

| Component | Responsibility |
|---|---|
| `LibraryViewModel`, `PlayerViewModel` | Hold UI state as `StateFlow`; call use cases/repositories |
| `LibraryScreen`, `PlayerScreen` | Compose functions observing ViewModel state |
| `MiniPlayer`, `SongListItem` | Reusable Compose components |
| `AuralisNavHost` | Navigation graph; type-safe route handling |
| `Theme`, `Color`, `Type` | Material 3 design system |

### Data Flow Example

```
MediaStore → MediaStoreScanner → SongRepository → SongDao → Room/SQLite
                                       ↑
                              GetSongsUseCase
                                       ↑
                              LibraryViewModel (StateFlow<UiState>)
                                       ↑
                              LibraryScreen (Compose)
```

---

## 8. Playback Architecture (Media3)

### Overview

The playback stack is built on **AndroidX Media3**, which unifies ExoPlayer, MediaSession, and the media notification system.

### Components

```
┌──────────────────────────────────────────────────┐
│              AuralisPlaybackService              │
│         (extends MediaSessionService)            │
│                                                  │
│  ┌────────────┐  ┌──────────────┐  ┌──────────┐ │
│  │  ExoPlayer  │  │ MediaSession │  │  Notif.  │ │
│  │  (Player)   │←→│              │←→│ (auto)   │ │
│  └────────────┘  └──────────────┘  └──────────┘ │
│         ↑                ↑                       │
│   AudioFocus        MediaButton                  │
│   Handling           Handling                    │
└──────────────────────────────────────────────────┘
         ↑ MediaController (IPC)
┌──────────────────────────────────────────────────┐
│              UI Layer (Compose)                   │
│                                                  │
│  PlayerViewModel ← MediaController.Listener      │
│       ↓                                          │
│  PlayerScreen / MiniPlayer                       │
└──────────────────────────────────────────────────┘
```

### Key Design Decisions

1. **`AuralisPlaybackService`** extends `MediaSessionService` (not `MediaBrowserServiceCompat`). Media3's `MediaSessionService` handles the foreground service lifecycle, notification, and MediaSession automatically.

2. **`ExoPlayer`** is the `Player` implementation. Created inside the service. Configured with:
   - `AudioAttributes` for `USAGE_MEDIA` / `CONTENT_TYPE_MUSIC`
   - `handleAudioFocus = true` (automatic audio focus via Media3)
   - `handleAudioBecomingNoisy = true` (auto-pause on headphone disconnect)

3. **UI connects via `MediaController`**, obtained through `MediaController.Builder` with a `SessionToken`. This is an IPC boundary — the UI never holds a direct reference to the `ExoPlayer` instance.

4. **Playback state** is observed via `Player.Listener` on the `MediaController`. The `PlayerViewModel` translates these callbacks into `StateFlow<PlaybackUiState>`.

5. **Media notification** is managed automatically by Media3's `MediaSessionService`. Custom actions (favorites, etc.) can be added via `MediaSession.Callback`.

6. **Lock screen / Bluetooth / Android Auto** controls are handled automatically by `MediaSession`.

### Audio Focus Strategy

| Event | Behavior |
|---|---|
| Transient loss (e.g., navigation prompt) | Duck volume or pause, auto-resume |
| Permanent loss (e.g., another music app) | Pause playback |
| Audio becoming noisy (headphones unplugged) | Pause immediately |
| Phone call (via `AUDIOFOCUS_LOSS_TRANSIENT`) | Pause, resume after call |

> Media3 handles all of these when `handleAudioFocus = true` and proper `AudioAttributes` are set.

### Gapless & Crossfade

- **Gapless**: ExoPlayer supports gapless natively for compatible formats (MP3 with LAME headers, FLAC, Opus, etc.).
- **Crossfade**: Requires a custom `Player` wrapper or dual-ExoPlayer approach. Deferred to Phase 4.

---

## 9. Room Database Design

### Entity Relationship Diagram

```
┌─────────────┐     ┌──────────────┐     ┌─────────────┐
│   SongEntity │────→│ AlbumEntity  │     │ ArtistEntity│
│              │     │              │     │             │
│  albumId (FK)│     │  artistId(FK)│────→│             │
│  artistId(FK)│     └──────────────┘     └─────────────┘
│  genreId (FK)│
└──────┬───────┘     ┌──────────────┐
       │             │  GenreEntity  │
       └────────────→│              │
                     └──────────────┘

┌───────────────────┐     ┌──────────────────────┐
│  PlaylistEntity   │←───→│ PlaylistSongCrossRef │
│                   │     │  playlistId (FK)      │
└───────────────────┘     │  songId (FK)          │
                          │  position             │
                          └──────────────────────┘

┌──────────────────┐     ┌──────────────────────┐
│ PlayHistoryEntity│     │  FavoriteEntity       │
│  songId (FK)     │     │  songId (FK)          │
│  playedAt        │     │  addedAt              │
└──────────────────┘     └──────────────────────┘

┌──────────────────┐
│  QueueItemEntity │
│  songId (FK)     │
│  position        │
│  isCurrent       │
└──────────────────┘
```

### Phase 1 Entities (Implement First)

```kotlin
@Entity(tableName = "songs")
data class SongEntity(
    @PrimaryKey val id: Long,              // MediaStore ID
    val title: String,
    val artistId: Long?,
    val artistName: String,                // Denormalized for fast display
    val albumId: Long?,
    val albumName: String,                 // Denormalized for fast display
    val genreId: Long?,
    val genreName: String?,                // Denormalized
    val duration: Long,                    // Milliseconds
    val trackNumber: Int?,
    val discNumber: Int?,
    val year: Int?,
    val filePath: String,                  // Content URI or file path
    val contentUri: String,                // content:// URI (scoped storage)
    val mimeType: String?,
    val size: Long,                        // File size in bytes
    val bitrate: Int?,                     // kbps, if available
    val sampleRate: Int?,                  // Hz, if available
    val channelCount: Int?,
    val dateAdded: Long,                   // Unix timestamp
    val dateModified: Long,
    val albumArtUri: String?,              // content:// URI for album art
)

@Entity(tableName = "albums")
data class AlbumEntity(
    @PrimaryKey val id: Long,              // MediaStore Album ID
    val name: String,
    val artistId: Long?,
    val artistName: String?,
    val year: Int?,
    val songCount: Int,
    val artUri: String?,
)

@Entity(tableName = "artists")
data class ArtistEntity(
    @PrimaryKey val id: Long,              // MediaStore Artist ID
    val name: String,
    val albumCount: Int,
    val songCount: Int,
)

@Entity(tableName = "genres")
data class GenreEntity(
    @PrimaryKey val id: Long,              // MediaStore Genre ID
    val name: String,
    val songCount: Int,
)
```

### Phase 3 Entities (Implement Later)

```kotlin
@Entity(tableName = "playlists")
data class PlaylistEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val description: String?,
    val createdAt: Long,
    val modifiedAt: Long,
    val songCount: Int,
    val artUri: String?,                   // First song's art or custom
)

@Entity(
    tableName = "playlist_songs",
    primaryKeys = ["playlistId", "songId"],
    foreignKeys = [ /* FK to playlists, FK to songs */ ]
)
data class PlaylistSongCrossRef(
    val playlistId: Long,
    val songId: Long,
    val position: Int,
    val addedAt: Long,
)

@Entity(tableName = "favorites")
data class FavoriteEntity(
    @PrimaryKey val songId: Long,
    val addedAt: Long,
)

@Entity(tableName = "play_history")
data class PlayHistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val songId: Long,
    val playedAt: Long,
    val durationPlayed: Long,              // How long the user actually listened
)

@Entity(tableName = "queue_items")
data class QueueItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val songId: Long,
    val position: Int,
    val isCurrent: Boolean,
)
```

> **Note:** These entity definitions are architectural specifications. Do not create the Room classes until their corresponding feature phase begins.

---

## 10. Navigation Structure

### Screen Hierarchy

```
MainActivity (SingleActivity, @AndroidEntryPoint)
└── AuralisNavHost
    ├── BottomNavBar
    │   ├── Library (start destination)
    │   │   ├── Songs Tab
    │   │   ├── Albums Tab
    │   │   ├── Artists Tab
    │   │   └── Genres Tab
    │   ├── Search
    │   ├── Playlists
    │   └── Settings
    │
    ├── AlbumDetail(albumId)
    ├── ArtistDetail(artistId)
    ├── GenreDetail(genreId)
    ├── PlaylistDetail(playlistId)
    │
    └── NowPlaying (full-screen overlay / bottom sheet)
        └── Queue (sub-screen)
```

### Navigation Strategy

- **Bottom navigation bar** with 4 tabs: Library, Search, Playlists, Settings.
- **Mini player** sits above the bottom nav bar; persists across all screens.
- **Now Playing** screen is a full-screen overlay (expanded from mini player); can be a bottom-sheet-style reveal.
- Detail screens (album, artist, genre, playlist) push onto the navigation stack.
- **Type-safe routes** using Kotlin serializable classes (Compose Navigation 2.8+ pattern).

```kotlin
// Route definitions (Phase 1)
@Serializable object LibraryRoute
@Serializable object SearchRoute
@Serializable object PlaylistsRoute
@Serializable object SettingsRoute
@Serializable data class AlbumDetailRoute(val albumId: Long)
@Serializable data class ArtistDetailRoute(val artistId: Long)
@Serializable data class GenreDetailRoute(val genreId: Long)
@Serializable data class PlaylistDetailRoute(val playlistId: Long)
```

---

## 11. Local Audio Discovery & Import

### Strategy: MediaStore Query

Audio files are discovered via `ContentResolver` querying `MediaStore.Audio.Media`.

### Discovery Flow

```
App Launch / Manual Refresh
        ↓
MediaStoreScanner.scan()
        ↓
ContentResolver.query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, ...)
        ↓
Map cursor rows → SongEntity list
        ↓
Diff against existing Room data (insert new, update changed, soft-delete removed)
        ↓
Room database updated → Flows emit → UI recomposes
```

### Key Implementation Details

1. **Scoped Storage (API 29+):** Use `content://` URIs, not raw file paths. `MediaStore.Audio.Media.EXTERNAL_CONTENT_URI` is the correct authority.

2. **Projections:** Query only needed columns for performance:
   - `_ID`, `TITLE`, `ARTIST`, `ARTIST_ID`, `ALBUM`, `ALBUM_ID`, `DURATION`, `TRACK`, `YEAR`, `DATA` (path, for display only), `MIME_TYPE`, `SIZE`, `DATE_ADDED`, `DATE_MODIFIED`

3. **Album art:** Retrieved via `ContentUris.withAppendedId(MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI, albumId)` or via the `MediaMetadataRetriever` for embedded art.

4. **Extended metadata** (bitrate, sample rate, bit depth, channel count): Not always available from MediaStore. For accurate data, use `MediaMetadataRetriever` or Media3's `MetadataRetriever` on the content URI. This is expensive — run in a background coroutine, cache results in Room.

5. **Incremental scan:** Track `DATE_MODIFIED` to avoid re-scanning unchanged files. Store a `lastScanTimestamp` in DataStore.

6. **Supported formats:** MP3, AAC (M4A), OGG/Vorbis, Opus, FLAC, WAV, and anything MediaStore indexes + ExoPlayer can decode. No format filtering — let ExoPlayer handle decoding; gracefully report unsupported files.

7. **Permissions:**
   - API 29–32: `READ_EXTERNAL_STORAGE` (or `READ_MEDIA_AUDIO` on 33+)
   - API 33+: `READ_MEDIA_AUDIO` (granular media permission)

---

## 12. Background Playback ↔ UI Communication

### Architecture

```
┌───────────────────────────────────────┐
│        AuralisPlaybackService         │
│       (MediaSessionService)           │
│                                       │
│  ExoPlayer ←→ MediaSession            │
│                    ↓                  │
│             SessionToken              │
└───────────────────┬───────────────────┘
                    │ IPC (Binder via Media3)
┌───────────────────┴───────────────────┐
│          MediaController              │
│    (obtained via SessionToken)        │
│                                       │
│  PlayerViewModel observes:            │
│  - Player.Listener callbacks          │
│  - isPlaying, currentMediaItem,       │
│    playbackState, position, duration  │
│                                       │
│  PlayerViewModel commands:            │
│  - play(), pause(), seekTo(),         │
│    seekToNext(), seekToPrevious(),    │
│    setMediaItems(), setShuffleMode()  │
└───────────────────────────────────────┘
```

### Communication Contract

| Direction | Mechanism | Examples |
|---|---|---|
| **UI → Service** | `MediaController` commands | `play()`, `pause()`, `seekTo(ms)`, `setMediaItems(list)` |
| **Service → UI** | `Player.Listener` callbacks | `onIsPlayingChanged`, `onMediaItemTransition`, `onPlaybackStateChanged` |
| **Persistent state** | Room database | Queue persisted to `queue_items` table; restored on app restart |
| **User preferences** | DataStore | Shuffle/repeat mode, last position, volume |

### Lifecycle

1. **Service starts** when the user initiates playback. Media3 handles foreground promotion automatically.
2. **Service persists** through screen-off, app backgrounding, and task-switch.
3. **Service stops** when the user explicitly stops playback or dismisses the notification.
4. **UI reconnects** to the existing session via `SessionToken` when the app returns to foreground.
5. **Notification** is automatically managed by `MediaSessionService`. Custom media buttons are added via `MediaSession.Builder.setCustomLayout()`.

---

## 13. Testing Strategy

### Unit Tests (`:app/src/test/`)

| Target | Tools | Coverage |
|---|---|---|
| ViewModels | JUnit 5, Turbine (Flow testing), MockK | State transitions, use case invocations |
| Use Cases | JUnit 5, MockK | Business logic |
| Repositories | JUnit 5, MockK | Data mapping, error handling |
| Mappers | JUnit 5 | Entity ↔ domain model mapping |
| Room DAOs | Robolectric + in-memory Room DB | Query correctness |

### Instrumentation Tests (`:app/src/androidTest/`)

| Target | Tools | Coverage |
|---|---|---|
| Room Database | AndroidX Test, in-memory Room | Migration, complex queries |
| Compose UI | Compose UI Test (`createComposeRule`) | Screen rendering, interaction |
| MediaStore Scanner | AndroidX Test | Integration with ContentResolver |
| Playback Service | Media3 TestPlayer | Service lifecycle, MediaSession |

### Key Libraries

```toml
# Test dependencies (in libs.versions.toml)
junit5 = { module = "org.junit.jupiter:junit-jupiter", version = "5.x" }
mockk = { module = "io.mockk:mockk", version = "1.x" }
turbine = { module = "app.cash.turbine:turbine", version = "1.x" }
compose-ui-test = { module = "androidx.compose.ui:ui-test-junit4" }  # via BOM
robolectric = { module = "org.robolectric:robolectric", version = "4.x" }
```

### Testing Rules

1. **Do not fabricate test results.** Only report tests that have actually been run.
2. **Playback tests** require an actual or emulated Android device. Unit-test ViewModels with a mock `MediaController`.
3. **Database tests** use in-memory Room databases.
4. **UI tests** use Compose test rules with semantic matchers.
5. **No flaky tests.** Use `Turbine` for Flow assertions with timeouts. Avoid `Thread.sleep` in tests.

---

## 14. Android-Specific Risks

### 14.1 Foreground Service Requirements

| API Level | Requirement |
|---|---|
| API 26+ | Must call `startForeground()` within 5 seconds of `startForegroundService()` |
| API 31+ | Foreground service type must be declared: `android:foregroundServiceType="mediaPlayback"` |
| API 34+ | Runtime permission `FOREGROUND_SERVICE_MEDIA_PLAYBACK` required in manifest |

> **Mitigation:** Media3's `MediaSessionService` handles foreground promotion automatically. Declare the service type and permissions in `AndroidManifest.xml`.

### 14.2 Audio Focus

| Risk | Mitigation |
|---|---|
| Other apps stealing focus | Handle `AUDIOFOCUS_LOSS` — pause playback |
| Transient interruptions | Handle `AUDIOFOCUS_LOSS_TRANSIENT` — pause then resume |
| Ducking requests | Handle `AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK` — lower volume |

> **Mitigation:** Set `AudioAttributes` and `handleAudioFocus = true` on ExoPlayer. Media3 handles this.

### 14.3 MediaSession Lifecycle

| Risk | Mitigation |
|---|---|
| Session not released | Release `MediaSession` and `ExoPlayer` in `onDestroy()` of `MediaSessionService` |
| Multiple sessions | Ensure only one `MediaSession` exists. Media3's architecture prevents duplicates. |
| Session persisting after user stops | Call `stopSelf()` when playback ends and notification is dismissed |

### 14.4 Notification Permissions

| API Level | Requirement |
|---|---|
| API < 33 | Notifications allowed by default |
| API 33+ | Must request `POST_NOTIFICATIONS` runtime permission |

> **Mitigation:** Request `POST_NOTIFICATIONS` at runtime on API 33+. Gracefully degrade if denied (playback still works, just no notification controls).

### 14.5 Scoped Storage

| API Level | Behavior |
|---|---|
| API 29–32 | Scoped storage; `READ_EXTERNAL_STORAGE` or `requestLegacyExternalStorage` |
| API 33+ | Granular permissions: `READ_MEDIA_AUDIO` |

> **Mitigation:** Target API 29 minimum. Use `MediaStore` queries with `content://` URIs. Never rely on raw file paths for playback.

### 14.6 Battery Optimization

| Risk | Mitigation |
|---|---|
| Doze mode killing service | Foreground service with notification exempts from Doze |
| App Standby buckets | Active playback keeps the app in an active bucket |
| OEM-specific battery savers | Document potential issues; provide user guidance |
| `WakeLock` needs | ExoPlayer acquires a partial wake lock during playback via `setWakeMode()` |

### 14.7 Bluetooth / Media Controls

| Risk | Mitigation |
|---|---|
| AVRCP metadata not showing | `MediaSession` metadata must be set. Media3 does this automatically from `MediaItem.mediaMetadata`. |
| Media buttons (play/pause/skip) | `MediaSession.Callback` handles these. Media3 routes them to the `Player`. |
| A2DP audio routing | ExoPlayer handles output routing. No special code needed. |
| Bluetooth disconnection | `AudioManager.ACTION_AUDIO_BECOMING_NOISY` — handled by `handleAudioBecomingNoisy = true` |

### 14.8 Android Version Differences

| Concern | API 29 | API 31 | API 33 | API 34+ |
|---|---|---|---|---|
| Storage permission | `READ_EXTERNAL_STORAGE` | `READ_EXTERNAL_STORAGE` | `READ_MEDIA_AUDIO` | `READ_MEDIA_AUDIO` |
| Notification permission | Auto-granted | Auto-granted | `POST_NOTIFICATIONS` | `POST_NOTIFICATIONS` |
| Foreground service type | Optional | Required | Required | Required + manifest permission |
| Exact alarms (sleep timer) | Allowed | `SCHEDULE_EXACT_ALARM` | `SCHEDULE_EXACT_ALARM` | `USE_EXACT_ALARM` or inexact |
| Dynamic color (Material You) | ❌ | ❌ | ✅ | ✅ |

---

## 15. Privacy & Security

### Commitments

- ❌ No network permissions in `AndroidManifest.xml` (unless Coil needs it for local content URIs — verify).
- ❌ No analytics SDK (Firebase Analytics, Amplitude, etc.)
- ❌ No crash reporting SDK (Firebase Crashlytics, Sentry, etc.)
- ❌ No advertising SDK
- ❌ No authentication / accounts
- ❌ No cloud database (Supabase, Firebase, MySQL, etc.)
- ❌ No remote API calls
- ✅ All data stored locally in Room / DataStore
- ✅ Music files remain under user control (never copied/transcoded without consent)
- ✅ Listening history stays on-device

### Permissions Required

| Permission | Purpose | Runtime? |
|---|---|---|
| `READ_MEDIA_AUDIO` (API 33+) | Discover audio files | Yes |
| `READ_EXTERNAL_STORAGE` (API 29–32) | Discover audio files | Yes |
| `FOREGROUND_SERVICE` | Background playback | No (manifest only) |
| `FOREGROUND_SERVICE_MEDIA_PLAYBACK` (API 34+) | Foreground service type | No (manifest only) |
| `POST_NOTIFICATIONS` (API 33+) | Playback notification | Yes |
| `WAKE_LOCK` | Prevent sleep during playback | No (manifest only) |

---

## 16. Naming Conventions

| Element | Convention | Example |
|---|---|---|
| Package | lowercase, dot-separated | `com.auralis.app.data.local.db` |
| Class | PascalCase | `AuralisPlaybackService` |
| Interface | PascalCase (no `I` prefix) | `SongRepository` |
| Function | camelCase | `getSongsByAlbum()` |
| Variable | camelCase | `currentSong` |
| Constant | UPPER_SNAKE_CASE | `DATABASE_NAME` |
| Room Entity | `*Entity` suffix | `SongEntity` |
| Room DAO | `*Dao` suffix | `SongDao` |
| ViewModel | `*ViewModel` suffix | `LibraryViewModel` |
| Use Case | verb phrase + `UseCase` | `GetSongsUseCase` |
| Composable Screen | `*Screen` suffix | `LibraryScreen` |
| Composable Component | Descriptive PascalCase | `MiniPlayer`, `SongListItem` |
| Hilt Module | `*Module` suffix | `DatabaseModule` |
| State class | `*UiState` suffix | `LibraryUiState` |
| Route | `*Route` suffix | `AlbumDetailRoute` |
| Resource files | snake_case | `ic_play_arrow.xml` |
| String resources | snake_case | `@string/now_playing_title` |

---

## 17. Project Rules

### Must Follow

1. **Do not add** Firebase, Supabase, PHP, MySQL, authentication, subscriptions, or cloud services.
2. **Do not add** `INTERNET` permission unless technically required for a local-only feature (verify first).
3. **Do not transcode** or modify imported audio files.
4. **Do not fabricate** successful builds, tests, or functionality claims.
5. **Do not use KAPT.** Use KSP exclusively for annotation processing.
6. **Do not apply** `org.jetbrains.kotlin.android` plugin — it conflicts with AGP 9's built-in Kotlin.
7. **Do not create fake implementations** for core functionality. Use `TODO()` markers with clear descriptions if a real implementation is deferred.
8. **Do not silently swallow exceptions.** Log errors appropriately; surface user-facing errors via UI state.
9. **Do not hold `Context` references** in ViewModels. Use `@ApplicationContext` via Hilt where absolutely needed.
10. **Do not directly bind** to the playback service from Compose. Use `MediaController` via `SessionToken`.

### Should Follow

1. **Use version catalog** (`libs.versions.toml`) for all dependency management.
2. **Use `StateFlow`** for ViewModel → UI state. Avoid `LiveData` in new code.
3. **Use `Flow`** for Room DAO read queries. Use `suspend` for writes.
4. **Mark placeholders** with `// TODO: Phase N — description` comments.
5. **Keep Composables stateless** where possible. Hoist state to ViewModels.
6. **Use Hilt `@Inject constructor`** for all injectable classes. Avoid manual instantiation.
7. **Write mappers** between data entities and domain models. Do not leak Room entities into the UI layer.
8. **Test before claiming.** Run `./gradlew build` and report actual results.

---

## 18. Phased Roadmap

| Phase | Focus | Status |
|---|---|---|
| **Phase 0** | Architecture, project setup, specification | ✅ Complete |
| **Phase 1** | Android Foundation & Project Bootstrap | ✅ Complete |
| **Phase 2** | Library: MediaStore scanner, songs, albums, artists, UI | ⬜ Next |
| **Phase 3** | Queue, playlists, favorites, shuffle/repeat, history | ⬜ Planned |
| **Phase 4** | Audio controls: EQ, bass boost, crossfade, gapless, sleep timer | ⬜ Planned |
| **Phase 5** | Polish: metadata display, stats, smart playlists, widget | ⬜ Planned |

---

> **This document is the source of truth for Auralis architecture and project rules. Update it as decisions are made and the project evolves.**
