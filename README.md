# MorseGlyph

Translates text into Morse code and transmits it through the Nothing Phone Glyph Matrix — light and sound at the same time.

---

## Requirements

- Nothing Phone with Glyph Matrix support (Phone 2 / 2a / 3 series)
- `glyph-matrix-sdk-2.0.aar` placed in `app/libs/` — download from the [GlyphMatrix Developer Kit](https://github.com/Nothing-Developer-Programme/GlyphMatrix-Developer-Kit)
- Android Studio Hedgehog or newer

The project won't compile without the AAR. On non-Nothing devices it falls back to audio only.

---

## Glyph setup

Go to **Settings → Glyph Interface → Glyph Toys**, find MorseGlyph and enable it. Grant the permission on first launch.

---

## Features

- Type any message (up to 100 characters) and see the Morse translation live
- Three indicator modes: **Symbol** (dot/dash pixel pattern per tone), **Full** (solid flash), **Per-letter** (full letter shown as pixel grid while it plays)
- Adjustable speed from 5 to 30 WPM — persists between sessions
- **Loop mode** — keeps repeating until you press stop
- **SOS button** — one tap, no typing needed
- Paste from clipboard directly into the input
- Last 10 transmitted messages saved as quick-select chips
- Persistent banner if the Glyph service isn't available, with a shortcut to Nothing Settings
- First-launch onboarding (3 pages, never shown again after that)

---

## How it works

The Glyph Matrix is a grid of individually addressable LEDs. MorseGlyph maps each Morse symbol to a pixel pattern — a small circle for a dot, a horizontal bar for a dash — and pushes that frame to the matrix for exactly the duration of the tone, then clears it. Per-letter mode subdivides the full grid into N equal cells (one per symbol in the letter) and renders them all at once.

Audio runs in parallel via `AudioTrack` — a 700 Hz sine wave with a 5 ms fade-in/out to avoid clicks.

The Glyph service connection lives on `onCreate`/`onDestroy`, not `onStart`/`onStop`, so turning the screen off mid-transmission doesn't kill it. A `PARTIAL_WAKE_LOCK` keeps the CPU running while the screen is off.

---

## Architecture

```
MainActivity
  ├── OnboardingScreen       first launch only
  └── MorseScreen            main UI, driven by StateFlow
       └── MorseViewModel
            ├── MorseTranslator        pure Kotlin, no Android deps
            ├── GlyphController        GlyphMatrixManager, LifecycleObserver
            ├── AudioController        AudioTrack, 700 Hz sine wave
            └── SharedPrefsRepository  settings + history + onboarding flag

MorseGlyphToyService         registered as a Nothing Glyph Toy
```
