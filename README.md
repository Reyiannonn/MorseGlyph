# MorseGlyph

A Nothing OS–inspired Android app that translates text into Morse code and simultaneously:
- Flashes the **Nothing Phone (3) Glyph Matrix** (dots = short flash, dashes = long flash)
- Plays **700 Hz sine-wave beep tones** via AudioTrack

---

## Prerequisites

### 1. Download the GlyphMatrix SDK AAR

1. Visit: https://github.com/Nothing-Developer-Programme/GlyphMatrix-Developer-Kit
2. Download the latest release AAR (e.g. `glyph-matrix-sdk-x.x.x.aar`)
3. Copy it to `app/libs/` (rename if needed — the build picks up all `*.aar` files in that directory)

> **Without the AAR the project will not compile.** The app runs in audio-only mode on non-Nothing-Phone-3 devices.

### 2. Verify SDK API

After downloading, open the AAR's bundled docs or `classes.jar` and confirm the package name used in `GlyphController.kt` (`com.nothing.ketchum`) matches what the AAR exports. Adjust imports/calls in `GlyphController.kt` if they differ.

---

## Enable Glyph on Nothing Phone (3)

1. Go to **Settings → Glyph Interface → Glyph Toys**
2. Find **MorseGlyph** and enable it
3. Grant the Glyph permission prompt when the app first runs

---

## Build & Run

```bash
# From project root
./gradlew assembleDebug
adb install app/build/outputs/apk/debug/app-debug.apk
```

Or open in **Android Studio** → click **Run**.

Minimum Android: **8.0 (API 26)**. Target: **API 35**.

---

## Usage

1. Type a message in the input field (max 100 characters)
2. The Morse translation appears below automatically
3. Pick an **indicator mode**: Symbol / Full / Letter
4. Adjust **WPM** (5–30, persists between launches)
5. Tap **TRANSMIT** — glyph flashes + beeps play simultaneously
6. Tap **STOP** to cancel at any time

---

## Architecture

```
MainActivity
  └── MorseScreen (Compose)
       └── MorseViewModel (StateFlow)
            ├── MorseTranslator  (pure Kotlin, zero Android deps)
            ├── GlyphController  (GlyphMatrix SDK via reflection, LifecycleObserver)
            ├── AudioController  (AudioTrack, 700 Hz sine wave)
            └── SharedPrefsRepository (WPM + indicator mode persistence)
```

---

## Screenshots

| Idle | Transmitting — Full String | Transmitting — Per Letter |
|------|---------------------------|--------------------------|
| _(add screenshot)_ | _(add screenshot)_ | _(add screenshot)_ |
