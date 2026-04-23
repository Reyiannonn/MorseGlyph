# MorseGlyph — Design Spec
**Date:** 2026-04-23  
**Status:** Approved

---

## Overview

MorseGlyph is a single-screen Android app (Kotlin, Jetpack Compose) that translates user-typed text into Morse code and simultaneously:
1. Flashes the Nothing Phone (3) Glyph Matrix (GlyphMatrix SDK)
2. Plays 700 Hz sine-wave beep tones via `AudioTrack`

TTS is explicitly excluded. The app targets Nothing Phone (3) exclusively for glyph output; all other devices run in audio-only mode.

---

## Decisions Made

| Question | Decision |
|---|---|
| Glyph target | Nothing Phone (3) only (GlyphMatrix SDK); everything else = audio-only |
| TTS | Removed entirely |
| WPM persistence | Yes — SharedPreferences |
| Live indicator | All 3 modes (Symbol / Full String / Per Letter), user picks via segmented button |
| UI framework | Full Jetpack Compose |
| Architecture | Single ViewModel + StateFlow; glyph + audio as sibling coroutines |

---

## Module Boundaries

```
MainActivity
  └── MorseScreen (Compose root, observes MorseViewModel)

MorseViewModel
  Fields: inputText, morseString, transmissionState, activeSymbolIndex,
          wpm, indicatorMode, errorMessage
  Owns: cancellable transmission coroutine job
  Delegates to: MorseTranslator, GlyphController, AudioController

MorseTranslator          — pure Kotlin, zero Android deps, fully unit-testable
  translate(text)        → List<MorseWord>
  toDisplayString(words) → String
  toFlatSymbols(words)   → List<TimedSymbol>

GlyphController          — wraps GlyphMatrix SDK, LifecycleObserver
  flashOn(durationMs) / flashOff(durationMs)
  Graceful no-op if not Nothing Phone (3) or SDK bind fails

AudioController          — AudioTrack sine wave, 700 Hz
  beep(durationMs)
  release()

SharedPrefsRepository    — WPM + IndicatorMode persistence
  getWpm() / saveWpm(Int)
  getIndicatorMode() / saveIndicatorMode(IndicatorMode)
```

**Key invariant:** `MorseTranslator` has zero Android imports. All Android-specific code is isolated in `GlyphController`, `AudioController`, or the ViewModel.

---

## Data Model

```kotlin
sealed class TimedSymbol {
    data class Tone(val durationMs: Long) : TimedSymbol()    // glyph ON + beep
    data class Silence(val durationMs: Long) : TimedSymbol() // glyph OFF, no beep
}

enum class TransmissionState { IDLE, TRANSMITTING }

enum class IndicatorMode { SYMBOL, FULL_STRING, PER_LETTER }
```

### WPM → Timing
Standard PARIS formula: `unitMs = 1200 / wpm`

| Element | Duration |
|---|---|
| Dot | 1 × unitMs |
| Dash | 3 × unitMs |
| Symbol gap (within letter) | 1 × unitMs |
| Letter gap | 3 × unitMs |
| Word gap | 7 × unitMs |

---

## Transmission Loop

```
User taps TRANSMIT
  → validate input (non-empty, ≤ 100 chars)
  → MorseTranslator.toFlatSymbols(text) → List<TimedSymbol>
  → transmissionJob = viewModelScope.launch {
        symbols.forEachIndexed { i, symbol ->
            _state.update { activeSymbolIndex = i }
            when (symbol) {
                is Tone    → coroutineScope {
                                 launch { glyphController.flashOn(symbol.durationMs) }
                                 launch { audioController.beep(symbol.durationMs) }
                             }
                is Silence → coroutineScope {
                                 launch { glyphController.flashOff(symbol.durationMs) }
                                 launch { delay(symbol.durationMs) }
                             }
            }
        }
        _state.update { status = IDLE, activeSymbolIndex = -1 }
    }

User taps STOP
  → transmissionJob.cancel()
  → glyphController.flashOff(0)
  → state resets to IDLE
```

---

## UI Structure (Jetpack Compose)

```
MorseScreen (Column, black background)
  ├── DotMatrixHeader          — decorative Canvas dot grid, Nothing aesthetic
  ├── MessageInputField        — outlined, max 100 chars, char counter, error state
  ├── MorseOutputField         — read-only, RobotoMono, shows translated Morse string
  ├── WpmSlider                — range 5–30, labeled, persists on change
  ├── IndicatorModeSelector    — segmented button: Symbol | Full String | Per Letter
  ├── LiveIndicator            — switches between:
  │     ├── SymbolIndicator      (single pulsing • or —)
  │     ├── FullStringIndicator  (full Morse string, active symbol in accent color)
  │     └── PerLetterIndicator   (current letter + symbols, active highlighted)
  └── TransmitStopRow
        ├── TRANSMIT button      (disabled during TX)
        └── STOP button          (always present, enabled only during TX — avoids layout jump)
```

### Nothing OS Visual Language
- Background: `#000000`, surface cards: `#0D0D0D`
- Accent (active): `#FFFFFF`, inactive: `#3A3A3A`, error: `#FF3B30`
- Font: `RobotoMono` — ndot-compatible slot (swap font asset to upgrade later)
- Corner radius: 0dp everywhere (sharp corners)
- Borders: 1dp `#1F1F1F` dividers
- Active symbol animates with `animateFloatAsState` brightness pulse

---

## Error Handling

| Scenario | Behavior |
|---|---|
| Empty input | Red underline + "Enter a message" error text, no TRANSMIT |
| Input > 100 chars | Char counter turns red, TRANSMIT disabled |
| Unknown characters (`@`, `#`, etc.) | Skipped silently; shown as `?` in Morse output |
| GlyphMatrix SDK bind failure | Snackbar "Glyph unavailable — audio only"; `GlyphController` becomes no-op |
| Not Nothing Phone (3) | Same as SDK bind failure |
| AudioTrack init failure | Snackbar "Audio unavailable"; `AudioController` becomes no-op |
| STOP mid-transmission | Job cancelled, matrix cleared, state resets, input preserved |
| App backgrounded | Audio continues; glyph released via `LifecycleObserver.onStop()` |
| Back press during TX | Transmission cancelled, all resources released |

---

## Project File Structure

```
app/
  src/main/
    java/com/morseglyph/
      MainActivity.kt
      ui/
        MorseScreen.kt
        components/
          DotMatrixHeader.kt
          MessageInputField.kt
          MorseOutputField.kt
          WpmSlider.kt
          IndicatorModeSelector.kt
          LiveIndicator.kt          (dispatches to 3 sub-composables)
          SymbolIndicator.kt
          FullStringIndicator.kt
          PerLetterIndicator.kt
          TransmitStopRow.kt
      viewmodel/
        MorseViewModel.kt
        MorseUiState.kt
      morse/
        MorseTranslator.kt
        MorseSymbol.kt             (Dot, Dash, LetterGap, WordGap, SymbolGap)
        TimedSymbol.kt
      glyph/
        GlyphController.kt
      audio/
        AudioController.kt
      data/
        SharedPrefsRepository.kt
  src/test/java/com/morseglyph/
    MorseTranslatorTest.kt
  libs/
    glyph-matrix-sdk.aar           (manually placed — see README)
build.gradle (app)
README.md
```

---

## Dependencies (app/build.gradle)

```kotlin
// Jetpack Compose BOM
implementation(platform("androidx.compose:compose-bom:2024.09.00"))
implementation("androidx.compose.ui:ui")
implementation("androidx.compose.material3:material3")
implementation("androidx.compose.ui:ui-tooling-preview")
implementation("androidx.activity:activity-compose:1.9.2")

// Coroutines
implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")

// ViewModel
implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.5")

// GlyphMatrix SDK (local AAR)
implementation(files("libs/glyph-matrix-sdk.aar"))

// Test
testImplementation("junit:junit:4.13.2")
testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.8.1")
```

---

## Testing Plan

- `MorseTranslatorTest` — unit tests for A–Z, 0–9, space, mixed case, unknown chars, WPM timing math
- Manual: glyph flash on physical Nothing Phone (3)
- Manual: audio-only mode on non-Nothing device
- Manual: STOP mid-transmission, app background/foreground cycle

---

## Out of Scope

- Nothing Phone (1) / (2) / (2a) glyph strip support
- Text-to-Speech
- Saving/sharing transmission history
- Custom Morse mappings
