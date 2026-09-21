# Melofy

> "Your music. Your device. Your control."  
> A local-first, offline Android music player focused on high-quality audio playback and a polished modern experience.

---

## Vision

Melofy is a completely local music player for Android. It plays audio files stored on your device — no account, no backend, no subscription, no ads, no internet required.

- **Privacy-first.** No analytics, no cloud sync, no tracking.
- **High-quality audio.** Supports MP3, AAC, OGG, Opus, FLAC, WAV, and more. Preserves original files — never transcodes.
- **Honest about quality.** Shows real codec info, bitrate, sample rate. Never claims to enhance low-quality sources.
- **Modern Android.** Jetpack Compose, Material 3, Media3/ExoPlayer, Room, Hilt.

---

## Status

**Phase 9 Complete — Local Recommendation Engine**

Melofy features a complete local-first music library with Media3 background playback, custom playlists, reactive favorites, hardware-accelerated audio effects (Equalizer, Bass Boost, Virtualizer, ReplayGain), listening history, statistics, home screen widget, deep on-demand metadata inspection, safe metadata editing, offline artwork management with custom overrides, multi-signal duplicate detection, multi-field local search with deterministic ranking, persistent search history, dynamic suggestions, categorized results, advanced multi-filter engine with strict AND semantics, 15 sorting modes, library organization across Tracks, Albums, Artists, and Genres, and a 100% on-device deterministic recommendation engine with zero AI/ML, cold start handling, explainable recommendations, and "For You" carousels. See [docs/DEVELOPMENT_STATUS.md](docs/DEVELOPMENT_STATUS.md) and [PROJECT_SPECIFICATION.md](PROJECT_SPECIFICATION.md) for full technical documentation.

---

## Tech Stack

| Component | Technology |
|---|---|
| Language | Kotlin |
| UI | Jetpack Compose + Material 3 |
| Playback | AndroidX Media3 (ExoPlayer + MediaSession) |
| Database | Room (SQLite) |
| Settings | DataStore Preferences |
| DI | Hilt (Dagger) |
| Images | Coil |
| Async | Kotlin Coroutines + Flow |
| Build | Gradle Kotlin DSL with Version Catalog |
| Architecture | MVVM + Repository + Use Cases |

---

## Requirements

- Android Studio (latest stable)
- Android SDK 37 (Platform 37.2)
- JDK 21 (bundled with Android Studio)
- Minimum device: Android 10 (API 29)

---

## Building

```bash
# Clone the repository
git clone <repo-url>
cd spotifly

# Build (uses Gradle Wrapper — no global Gradle needed)
./gradlew assembleDebug

# Run tests
./gradlew test
```

---

## Project Structure

```
melofy/
├── app/                          Main application module
├── gradle/
│   ├── wrapper/                  Gradle Wrapper
│   └── libs.versions.toml       Version catalog
├── PROJECT_SPECIFICATION.md      Architecture & spec (source of truth)
└── README.md                     This file
```

See [PROJECT_SPECIFICATION.md](PROJECT_SPECIFICATION.md) for detailed architecture, module structure, database design, navigation, and phased roadmap.

---

## Privacy

- ❌ No internet permission
- ❌ No analytics / crash reporting
- ❌ No advertising
- ❌ No accounts or authentication
- ❌ No cloud services
- ✅ All data stays on your device
- ✅ Your music files are never modified

---

## License

TBD

---

## Contributing

This project is currently in early development. Contribution guidelines will be established once the foundation is in place.
