# MUSIIEA 🎵

A **fully offline**, lightweight, dark-first local music player for Android —
developed by **AVIOIVA**. Built with Kotlin, Jetpack Compose
(Material 3 / Material You) and Media3 ExoPlayer. **The app has no INTERNET
permission at all**: it physically cannot phone home.

---

## ✨ Features

**Core playback**
- Broad format support via ExoPlayer: **MP3, FLAC, AAC/M4A, OGG (Vorbis &
  Opus), WAV, ALAC, MKA** and more (see [Format notes](#-format-notes))
- Play / pause / next / previous, **shuffle** and **repeat** (all / **repeat-one**)
- **Gapless album playback** — the whole queue is prepared up front, so track
  transitions are pre-buffered and seamless; MP3/FLAC encoder delay/padding is
  trimmed automatically (perfect for live albums and concept albums)
- **Background playback** — music keeps playing when you switch apps or lock
  the screen, with full **lock-screen / notification controls** (via Media3
  MediaSession) and headset-button support
- In-app **volume slider** in Now Playing (plus normal hardware volume)
- Pauses automatically when headphones are unplugged

**Library & organisation**
- Browse by **Albums** (with cover art) and by **Folders**
- "All songs" list with shuffle-everything button
- **Custom playlists**: create, rename, delete, add/remove tracks, play, shuffle
- **Search** (albums, artists, folders) and **sorting** (title / artist /
  album / duration / track order)

**Audio customisation**
- **Multi-band Equalizer** with your device's presets (typically 5 bands),
  editable per-band sliders with dB readout
- **Bass Boost**, **Treble** (±6 dB high-shelf) and **3D Surround**
  (Virtualizer) effects
- Effects apply live to playback and are remembered between sessions

**Quality of life**
- **Sleep timer** (5–90 min) with optional **volume fade-out** — great for
  falling asleep to music or audiobooks
- **YT Music hand-off** — a "YT Music" button in Now Playing opens the
  official YouTube Music app (or the web player in your browser) with the
  current track pre-filled in search, so switching to streaming takes one
  tap. MUSIIEA itself stays 100% offline: it merely launches the other app,
  and has no INTERNET permission of its own.
- **About page** — tap the round AVIOIVA logo badge in any top bar for brand
  info, app version and contact details

**Privacy & footprint**
- 100% **offline**: no INTERNET permission, no analytics, no tracking
- Tiny APK (R8-shrunk), no heavy images or databases — playlists are a small
  JSON file, library is read straight from Android's MediaStore

---

## 🤖 Build it with the GitHub robot (no tools needed on your PC)

The repository ships with a GitHub Actions workflow (`.github/workflows/build.yml`)
that builds the APKs for you on every push:

1. Create a new repository on GitHub (e.g. `musiiea`), **upload this
   project** (keep the folder structure — `.github/`, `app/`, `gradle/`, etc.
   must be at the repo root). Easiest ways:
   - GitHub web UI: "uploading an existing file", or
   - `git init && git add . && git commit -m "MUSIIEA" && git remote add
     origin https://github.com/<you>/musiiea.git && git push -u origin main`
2. Open the **Actions** tab on your repo. A run named **"Build APKs"** starts
   automatically (you can also press **"Run workflow"** manually).
3. Wait ~5-10 minutes for the green check.
4. Open the run, scroll to **Artifacts**, and download:
   - **MUSIIEA-debug-apk** → install this one; it's ready to use
   - **MUSIIEA-release-unsigned-apk** → smaller build; needs your own
     signing key before it can install
5. Copy the APK to your phone, tap it, allow "install from unknown sources",
   done.

> Note: downloading artifacts requires being logged in to GitHub.

---

## 🛠️ Build it locally (optional)

Requirements: **Android Studio** (recent version) — everything else is
downloaded automatically.

1. Open Android Studio → **Open** → select this project folder.
2. Let it sync (first sync downloads Gradle 8.13 + dependencies).
3. **Run ▶** on a device, or **Build → Build APK(s)**.

Or from the command line:

```bash
./gradlew assembleDebug     # APK at app/build/outputs/apk/debug/
./gradlew assembleRelease   # unsigned release APK
```

Toolchain: JDK 17 · Gradle 8.13 · AGP 8.9.2 · Kotlin 2.2.10 · compileSdk 36 ·
minSdk 26 (Android 8.0+) · Media3 1.11.1 · Compose BOM 2025.08.00

---

## 📁 Project layout

```
AVIOIVAPlayer/
├── .github/workflows/build.yml        ← the "robot" that builds APKs on GitHub
├── app/src/main/
│   ├── AndroidManifest.xml            ← permissions + playback service
│   ├── res/                           ← strings, theme, adaptive icon
│   └── java/com/avioiva/player/
│       ├── MainActivity.kt            ← single Compose activity
│       ├── AvioivaApplication.kt       ← app-wide singletons
│       ├── data/
│       │   ├── MusicLibrary.kt        ← MediaStore scanner (albums/folders)
│       │   ├── PlaylistStore.kt       ← offline JSON playlists
│       │   ├── AlbumArtLoader.kt      ← embedded cover art + cache
│       │   └── Models.kt
│       ├── playback/
│       │   ├── PlaybackService.kt     ← background Media3 service (gapless)
│       │   ├── PlayerConnection.kt    ← MediaController bridge → StateFlows
│       │   ├── EqualizerManager.kt    ← EQ + BassBoost + Treble + Virtualizer
│       │   └── SleepTimer.kt          ← countdown with fade-out
│       ├── util/
│       │   ├── Format.kt              ← time/size formatting
│       │   └── YTMusic.kt             ← "YT Music" deep-link hand-off
│       └── ui/
│           ├── AppRoot.kt             ← tabs, mini-player, permission gate
│           ├── PlayerViewModel.kt
│           ├── theme/Theme.kt         ← dark Material You
│           └── screens/               ← Albums, Folders, Playlists,
│                                        Equalizer, NowPlaying, About, details
└── gradle/wrapper/                    ← Gradle 8.13 wrapper
```

---

## 🎼 Format notes

| Format | Support |
|---|---|
| MP3, AAC, M4A, WAV, OGG Vorbis, OGG Opus, FLAC, ALAC, MKA | ✅ Full |
| Hi-res FLAC (24-bit/96kHz+) | ✅ (device-dependent output path) |
| APE (Monkey's Audio) | ❌ Android/ExoPlayer has no APE decoder; convert to FLAC (lossless, e.g. with `ffmpeg -i x.ape x.flac`) |

Cover art is read from the files' **embedded artwork** (ID3 / FLAC / M4A), so
nothing needs to be downloaded.

---

## 🔧 Customisation

- **Rename the app / change package**: change `applicationId` + `namespace` in
  `app/build.gradle.kts`, the `app_name` in `res/values/strings.xml`, and the
  package folders.
- **Versions**: all dependency versions are pinned at the top of
  `app/build.gradle.kts`.
- **Light theme**: `AVIOIVATheme` in `ui/theme/Theme.kt` is dark-first; swap
  `dynamicDarkColorScheme` for `dynamicLightColorScheme` if you prefer.

## 📜 License

MIT — see [LICENSE](LICENSE).
