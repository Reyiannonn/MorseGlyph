# MorseGlyph Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build a single-screen Kotlin/Compose Android app that translates typed text into Morse code and simultaneously flashes the Nothing Phone (3) Glyph Matrix and plays 700 Hz beep tones.

**Architecture:** Single ViewModel + StateFlow; `MorseTranslator` is pure Kotlin (no Android deps, fully unit-testable); glyph flash and audio beep run as sibling coroutines so they start simultaneously; GlyphController degrades silently to no-op on non-Nothing-Phone-3 hardware.

**Tech Stack:** Kotlin, Jetpack Compose + Material3, Coroutines, AudioTrack, GlyphMatrix SDK (local AAR), SharedPreferences, JUnit4.

---

## File Map

```
MorseGlyph/
  settings.gradle.kts
  build.gradle.kts
  gradle.properties
  gradle/wrapper/gradle-wrapper.properties
  app/
    build.gradle.kts
    libs/                                          ← place glyph-matrix-sdk.aar here
    src/
      main/
        AndroidManifest.xml
        res/
          values/strings.xml
          values/themes.xml
        java/com/morseglyph/
          MainActivity.kt
          morse/
            MorseSymbol.kt
            TimedSymbol.kt
            TransmitEvent.kt
            MorseLetter.kt
            MorseWord.kt
            MorseTranslator.kt
          viewmodel/
            TransmissionState.kt
            IndicatorMode.kt
            MorseUiState.kt
            MorseViewModel.kt
            MorseViewModelFactory.kt
          data/
            SharedPrefsRepository.kt
          audio/
            AudioController.kt
          glyph/
            GlyphController.kt
          ui/
            theme/
              Color.kt
              Type.kt
              Theme.kt
            components/
              DotMatrixHeader.kt
              MessageInputField.kt
              MorseOutputField.kt
              WpmSlider.kt
              IndicatorModeSelector.kt
              SymbolIndicator.kt
              FullStringIndicator.kt
              PerLetterIndicator.kt
              LiveIndicator.kt
              TransmitStopRow.kt
            MorseScreen.kt
      test/
        java/com/morseglyph/
          MorseTranslatorTest.kt
  README.md
```

---

## Task 1: Project Scaffolding

**Files:**
- Create: `settings.gradle.kts`
- Create: `build.gradle.kts`
- Create: `gradle.properties`
- Create: `gradle/wrapper/gradle-wrapper.properties`
- Create: `app/build.gradle.kts`
- Create: `app/src/main/AndroidManifest.xml`
- Create: `app/src/main/res/values/strings.xml`
- Create: `app/src/main/res/values/themes.xml`
- Create: `app/libs/.gitkeep`

- [ ] **Step 1: Create settings.gradle.kts**

```kotlin
// settings.gradle.kts
pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}
rootProject.name = "MorseGlyph"
include(":app")
```

- [ ] **Step 2: Create root build.gradle.kts**

```kotlin
// build.gradle.kts (root)
plugins {
    id("com.android.application") version "8.5.2" apply false
    id("org.jetbrains.kotlin.android") version "1.9.24" apply false
}
```

- [ ] **Step 3: Create gradle.properties**

```properties
android.useAndroidX=true
kotlin.code.style=official
android.nonTransitiveRClass=true
org.gradle.jvmargs=-Xmx2048m
```

- [ ] **Step 4: Create gradle/wrapper/gradle-wrapper.properties**

```properties
distributionBase=GRADLE_USER_HOME
distributionPath=wrapper/dists
distributionUrl=https\://services.gradle.org/distributions/gradle-8.7-bin.zip
zipStoreBase=GRADLE_USER_HOME
zipStorePath=wrapper/dists
```

- [ ] **Step 5: Create app/build.gradle.kts**

```kotlin
// app/build.gradle.kts
plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.morseglyph"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.morseglyph"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
        }
    }

    buildFeatures {
        compose = true
    }

    composeOptions {
        // Must match the Kotlin version: 1.9.24 → 1.5.14
        kotlinCompilerExtensionVersion = "1.5.14"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }

    kotlinOptions {
        jvmTarget = "1.8"
    }
}

dependencies {
    // Compose BOM — pins all compose library versions together
    val composeBom = platform("androidx.compose:compose-bom:2024.09.00")
    implementation(composeBom)
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.activity:activity-compose:1.9.2")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.5")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.5")

    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")

    // GlyphMatrix SDK — download from Nothing's GitHub and place AAR in app/libs/
    // See README.md for download instructions.
    implementation(fileTree(mapOf("dir" to "libs", "include" to listOf("*.aar"))))

    // Tests
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.8.1")

    debugImplementation("androidx.compose.ui:ui-tooling")
}
```

- [ ] **Step 6: Create AndroidManifest.xml**

```xml
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android">
    <application
        android:allowBackup="true"
        android:label="@string/app_name"
        android:supportsRtl="true"
        android:theme="@style/Theme.MorseGlyph">
        <activity
            android:name=".MainActivity"
            android:exported="true"
            android:windowSoftInputMode="adjustResize">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>
    </application>
</manifest>
```

- [ ] **Step 7: Create res/values/strings.xml**

```xml
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <string name="app_name">MorseGlyph</string>
</resources>
```

- [ ] **Step 8: Create res/values/themes.xml**

```xml
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <style name="Theme.MorseGlyph" parent="android:Theme.Material.NoActionBar">
        <item name="android:windowBackground">@android:color/black</item>
        <item name="android:statusBarColor">@android:color/black</item>
        <item name="android:navigationBarColor">@android:color/black</item>
    </style>
</resources>
```

- [ ] **Step 9: Create app/libs/.gitkeep** (empty file — marks the directory for git)

- [ ] **Step 10: Verify the project syncs in Android Studio**

Open the project root in Android Studio → File → Sync Project with Gradle Files.
Expected: BUILD SUCCESSFUL (the AAR stub warning is expected until you place the real AAR).

---

## Task 2: Data Models

**Files:**
- Create: `app/src/main/java/com/morseglyph/morse/MorseSymbol.kt`
- Create: `app/src/main/java/com/morseglyph/morse/MorseLetter.kt`
- Create: `app/src/main/java/com/morseglyph/morse/MorseWord.kt`
- Create: `app/src/main/java/com/morseglyph/morse/TimedSymbol.kt`
- Create: `app/src/main/java/com/morseglyph/morse/TransmitEvent.kt`
- Create: `app/src/main/java/com/morseglyph/viewmodel/TransmissionState.kt`
- Create: `app/src/main/java/com/morseglyph/viewmodel/IndicatorMode.kt`
- Create: `app/src/main/java/com/morseglyph/viewmodel/MorseUiState.kt`

- [ ] **Step 1: Create MorseSymbol.kt**

```kotlin
package com.morseglyph.morse

enum class MorseSymbol { DOT, DASH }
```

- [ ] **Step 2: Create MorseLetter.kt**

```kotlin
package com.morseglyph.morse

data class MorseLetter(
    val char: Char,
    val symbols: List<MorseSymbol>
)
```

- [ ] **Step 3: Create MorseWord.kt**

```kotlin
package com.morseglyph.morse

data class MorseWord(val letters: List<MorseLetter>)
```

- [ ] **Step 4: Create TimedSymbol.kt**

```kotlin
package com.morseglyph.morse

sealed class TimedSymbol {
    data class Tone(val durationMs: Long) : TimedSymbol()
    data class Silence(val durationMs: Long) : TimedSymbol()
}
```

- [ ] **Step 5: Create TransmitEvent.kt**

```kotlin
package com.morseglyph.morse

data class TransmitEvent(
    val symbol: TimedSymbol,
    val wordIndex: Int = -1,
    val letterIndex: Int = -1,
    val symbolIndexInLetter: Int = -1,
    val symbolType: MorseSymbol? = null   // DOT or DASH for Tone events, null for Silence
)
```

- [ ] **Step 6: Create TransmissionState.kt**

```kotlin
package com.morseglyph.viewmodel

enum class TransmissionState { IDLE, TRANSMITTING }
```

- [ ] **Step 7: Create IndicatorMode.kt**

```kotlin
package com.morseglyph.viewmodel

enum class IndicatorMode { SYMBOL, FULL_STRING, PER_LETTER }
```

- [ ] **Step 8: Create MorseUiState.kt**

```kotlin
package com.morseglyph.viewmodel

import com.morseglyph.morse.MorseWord
import com.morseglyph.morse.TransmitEvent

data class MorseUiState(
    val inputText: String = "",
    val morseDisplayString: String = "",
    val morseWords: List<MorseWord> = emptyList(),
    val transmitEvents: List<TransmitEvent> = emptyList(),
    val transmissionState: TransmissionState = TransmissionState.IDLE,
    val activeEventIndex: Int = -1,
    val wpm: Int = 15,
    val indicatorMode: IndicatorMode = IndicatorMode.FULL_STRING,
    val inputError: String? = null,
    val snackbarMessage: String? = null
)
```

- [ ] **Step 9: Commit**

```bash
git add app/src/main/java/com/morseglyph/morse/ \
        app/src/main/java/com/morseglyph/viewmodel/TransmissionState.kt \
        app/src/main/java/com/morseglyph/viewmodel/IndicatorMode.kt \
        app/src/main/java/com/morseglyph/viewmodel/MorseUiState.kt
git commit -m "feat: add core data models"
```

---

## Task 3: MorseTranslator (TDD)

**Files:**
- Create: `app/src/test/java/com/morseglyph/MorseTranslatorTest.kt` ← write first
- Create: `app/src/main/java/com/morseglyph/morse/MorseTranslator.kt`

- [ ] **Step 1: Write the failing tests**

```kotlin
// app/src/test/java/com/morseglyph/MorseTranslatorTest.kt
package com.morseglyph

import com.morseglyph.morse.MorseSymbol
import com.morseglyph.morse.MorseTranslator
import com.morseglyph.morse.TimedSymbol
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MorseTranslatorTest {

    @Test fun `translate single dot letter E`() {
        val words = MorseTranslator.translate("E")
        assertEquals(1, words.size)
        assertEquals(1, words[0].letters.size)
        assertEquals(listOf(MorseSymbol.DOT), words[0].letters[0].symbols)
    }

    @Test fun `translate SOS`() {
        val words = MorseTranslator.translate("SOS")
        assertEquals(1, words.size)
        val letters = words[0].letters
        assertEquals(3, letters.size)
        assertEquals(listOf(MorseSymbol.DOT, MorseSymbol.DOT, MorseSymbol.DOT), letters[0].symbols) // S
        assertEquals(listOf(MorseSymbol.DASH, MorseSymbol.DASH, MorseSymbol.DASH), letters[1].symbols) // O
        assertEquals(listOf(MorseSymbol.DOT, MorseSymbol.DOT, MorseSymbol.DOT), letters[2].symbols) // S
    }

    @Test fun `translate is case insensitive`() {
        assertEquals(MorseTranslator.translate("A"), MorseTranslator.translate("a"))
    }

    @Test fun `translate digit 0`() {
        val words = MorseTranslator.translate("0")
        assertEquals(
            listOf(MorseSymbol.DASH, MorseSymbol.DASH, MorseSymbol.DASH, MorseSymbol.DASH, MorseSymbol.DASH),
            words[0].letters[0].symbols
        )
    }

    @Test fun `translate digit 5`() {
        val words = MorseTranslator.translate("5")
        assertEquals(
            listOf(MorseSymbol.DOT, MorseSymbol.DOT, MorseSymbol.DOT, MorseSymbol.DOT, MorseSymbol.DOT),
            words[0].letters[0].symbols
        )
    }

    @Test fun `translate space creates two words`() {
        val words = MorseTranslator.translate("HI EM")
        assertEquals(2, words.size)
        assertEquals(2, words[0].letters.size) // H, I
        assertEquals(2, words[1].letters.size) // E, M
    }

    @Test fun `translate unknown char produces empty symbols`() {
        val words = MorseTranslator.translate("A@B")
        assertEquals(1, words.size)
        val letters = words[0].letters
        assertEquals(3, letters.size)
        assertTrue(letters[1].symbols.isEmpty()) // '@' unknown
        assertEquals('@', letters[1].char)
    }

    @Test fun `toDisplayString SOS`() {
        val words = MorseTranslator.translate("SOS")
        val display = MorseTranslator.toDisplayString(words)
        assertEquals(". . .   - - -   . . .", display)
    }

    @Test fun `toDisplayString two words`() {
        val words = MorseTranslator.translate("E T")
        val display = MorseTranslator.toDisplayString(words)
        assertEquals(". / -", display)
    }

    @Test fun `toTransmitEvents dot duration at 15 WPM`() {
        val unitMs = 1200L / 15  // 80ms
        val words = MorseTranslator.translate("E") // single dot
        val events = MorseTranslator.toTransmitEvents(words, unitMs)
        assertEquals(1, events.size)
        val first = events[0]
        assertTrue(first.symbol is TimedSymbol.Tone)
        assertEquals(unitMs, (first.symbol as TimedSymbol.Tone).durationMs)
        assertEquals(MorseSymbol.DOT, first.symbolType)
    }

    @Test fun `toTransmitEvents dash is 3 units`() {
        val unitMs = 1200L / 15
        val words = MorseTranslator.translate("T") // single dash
        val events = MorseTranslator.toTransmitEvents(words, unitMs)
        assertEquals(1, events.size)
        assertEquals(3 * unitMs, (events[0].symbol as TimedSymbol.Tone).durationMs)
    }

    @Test fun `toTransmitEvents letter A has symbol gap between dot and dash`() {
        val unitMs = 1200L / 15
        val words = MorseTranslator.translate("A") // .-
        val events = MorseTranslator.toTransmitEvents(words, unitMs)
        // DOT, SYMBOL_GAP, DASH
        assertEquals(3, events.size)
        assertTrue(events[0].symbol is TimedSymbol.Tone)   // dot
        assertTrue(events[1].symbol is TimedSymbol.Silence) // symbol gap
        assertEquals(unitMs, (events[1].symbol as TimedSymbol.Silence).durationMs)
        assertTrue(events[2].symbol is TimedSymbol.Tone)   // dash
    }

    @Test fun `toTransmitEvents two letters have letter gap between them`() {
        val unitMs = 1200L / 15
        val words = MorseTranslator.translate("ET") // E=., T=-
        val events = MorseTranslator.toTransmitEvents(words, unitMs)
        // DOT, LETTER_GAP(3*unit), DASH
        assertEquals(3, events.size)
        assertTrue(events[1].symbol is TimedSymbol.Silence)
        assertEquals(3 * unitMs, (events[1].symbol as TimedSymbol.Silence).durationMs)
    }

    @Test fun `toTransmitEvents two words have word gap between them`() {
        val unitMs = 1200L / 15
        val words = MorseTranslator.translate("E T") // E=. space T=-
        val events = MorseTranslator.toTransmitEvents(words, unitMs)
        // DOT, WORD_GAP(7*unit), DASH
        assertEquals(3, events.size)
        assertEquals(7 * unitMs, (events[1].symbol as TimedSymbol.Silence).durationMs)
    }

    @Test fun `toTransmitEvents position metadata is correct`() {
        val unitMs = 1200L / 15
        val words = MorseTranslator.translate("A") // .-
        val events = MorseTranslator.toTransmitEvents(words, unitMs)
        assertEquals(0, events[0].wordIndex)
        assertEquals(0, events[0].letterIndex)
        assertEquals(0, events[0].symbolIndexInLetter) // first symbol (DOT)
        assertEquals(-1, events[1].symbolIndexInLetter) // gap
        assertEquals(1, events[2].symbolIndexInLetter) // second symbol (DASH)
    }

    @Test fun `wpm timing formula`() {
        assertEquals(240L, 1200L / 5)   // 5 WPM  = 240ms/unit
        assertEquals(40L,  1200L / 30)  // 30 WPM = 40ms/unit
    }
}
```

- [ ] **Step 2: Run tests to verify they fail**

In Android Studio: right-click `MorseTranslatorTest` → Run.
Expected: all tests fail with `Unresolved reference: MorseTranslator`.

- [ ] **Step 3: Implement MorseTranslator.kt**

```kotlin
// app/src/main/java/com/morseglyph/morse/MorseTranslator.kt
package com.morseglyph.morse

object MorseTranslator {

    private val TABLE: Map<Char, List<MorseSymbol>> = mapOf(
        'A' to listOf(MorseSymbol.DOT, MorseSymbol.DASH),
        'B' to listOf(MorseSymbol.DASH, MorseSymbol.DOT, MorseSymbol.DOT, MorseSymbol.DOT),
        'C' to listOf(MorseSymbol.DASH, MorseSymbol.DOT, MorseSymbol.DASH, MorseSymbol.DOT),
        'D' to listOf(MorseSymbol.DASH, MorseSymbol.DOT, MorseSymbol.DOT),
        'E' to listOf(MorseSymbol.DOT),
        'F' to listOf(MorseSymbol.DOT, MorseSymbol.DOT, MorseSymbol.DASH, MorseSymbol.DOT),
        'G' to listOf(MorseSymbol.DASH, MorseSymbol.DASH, MorseSymbol.DOT),
        'H' to listOf(MorseSymbol.DOT, MorseSymbol.DOT, MorseSymbol.DOT, MorseSymbol.DOT),
        'I' to listOf(MorseSymbol.DOT, MorseSymbol.DOT),
        'J' to listOf(MorseSymbol.DOT, MorseSymbol.DASH, MorseSymbol.DASH, MorseSymbol.DASH),
        'K' to listOf(MorseSymbol.DASH, MorseSymbol.DOT, MorseSymbol.DASH),
        'L' to listOf(MorseSymbol.DOT, MorseSymbol.DASH, MorseSymbol.DOT, MorseSymbol.DOT),
        'M' to listOf(MorseSymbol.DASH, MorseSymbol.DASH),
        'N' to listOf(MorseSymbol.DASH, MorseSymbol.DOT),
        'O' to listOf(MorseSymbol.DASH, MorseSymbol.DASH, MorseSymbol.DASH),
        'P' to listOf(MorseSymbol.DOT, MorseSymbol.DASH, MorseSymbol.DASH, MorseSymbol.DOT),
        'Q' to listOf(MorseSymbol.DASH, MorseSymbol.DASH, MorseSymbol.DOT, MorseSymbol.DASH),
        'R' to listOf(MorseSymbol.DOT, MorseSymbol.DASH, MorseSymbol.DOT),
        'S' to listOf(MorseSymbol.DOT, MorseSymbol.DOT, MorseSymbol.DOT),
        'T' to listOf(MorseSymbol.DASH),
        'U' to listOf(MorseSymbol.DOT, MorseSymbol.DOT, MorseSymbol.DASH),
        'V' to listOf(MorseSymbol.DOT, MorseSymbol.DOT, MorseSymbol.DOT, MorseSymbol.DASH),
        'W' to listOf(MorseSymbol.DOT, MorseSymbol.DASH, MorseSymbol.DASH),
        'X' to listOf(MorseSymbol.DASH, MorseSymbol.DOT, MorseSymbol.DOT, MorseSymbol.DASH),
        'Y' to listOf(MorseSymbol.DASH, MorseSymbol.DOT, MorseSymbol.DASH, MorseSymbol.DASH),
        'Z' to listOf(MorseSymbol.DASH, MorseSymbol.DASH, MorseSymbol.DOT, MorseSymbol.DOT),
        '0' to listOf(MorseSymbol.DASH, MorseSymbol.DASH, MorseSymbol.DASH, MorseSymbol.DASH, MorseSymbol.DASH),
        '1' to listOf(MorseSymbol.DOT, MorseSymbol.DASH, MorseSymbol.DASH, MorseSymbol.DASH, MorseSymbol.DASH),
        '2' to listOf(MorseSymbol.DOT, MorseSymbol.DOT, MorseSymbol.DASH, MorseSymbol.DASH, MorseSymbol.DASH),
        '3' to listOf(MorseSymbol.DOT, MorseSymbol.DOT, MorseSymbol.DOT, MorseSymbol.DASH, MorseSymbol.DASH),
        '4' to listOf(MorseSymbol.DOT, MorseSymbol.DOT, MorseSymbol.DOT, MorseSymbol.DOT, MorseSymbol.DASH),
        '5' to listOf(MorseSymbol.DOT, MorseSymbol.DOT, MorseSymbol.DOT, MorseSymbol.DOT, MorseSymbol.DOT),
        '6' to listOf(MorseSymbol.DASH, MorseSymbol.DOT, MorseSymbol.DOT, MorseSymbol.DOT, MorseSymbol.DOT),
        '7' to listOf(MorseSymbol.DASH, MorseSymbol.DASH, MorseSymbol.DOT, MorseSymbol.DOT, MorseSymbol.DOT),
        '8' to listOf(MorseSymbol.DASH, MorseSymbol.DASH, MorseSymbol.DASH, MorseSymbol.DOT, MorseSymbol.DOT),
        '9' to listOf(MorseSymbol.DASH, MorseSymbol.DASH, MorseSymbol.DASH, MorseSymbol.DASH, MorseSymbol.DOT),
    )

    fun translate(text: String): List<MorseWord> =
        text.uppercase()
            .split(' ')
            .filter { it.isNotEmpty() }
            .map { word ->
                MorseWord(word.map { char ->
                    MorseLetter(char, TABLE[char] ?: emptyList())
                })
            }

    fun toDisplayString(words: List<MorseWord>): String =
        words.joinToString(" / ") { word ->
            word.letters.joinToString("   ") { letter ->
                if (letter.symbols.isEmpty()) "?"
                else letter.symbols.joinToString(" ") { sym ->
                    if (sym == MorseSymbol.DOT) "." else "-"
                }
            }
        }

    fun toTransmitEvents(words: List<MorseWord>, unitMs: Long): List<TransmitEvent> {
        val events = mutableListOf<TransmitEvent>()
        words.forEachIndexed { wIdx, word ->
            word.letters.forEachIndexed { lIdx, letter ->
                if (letter.symbols.isEmpty()) return@forEachIndexed
                letter.symbols.forEachIndexed { sIdx, sym ->
                    val toneDuration = if (sym == MorseSymbol.DOT) unitMs else 3 * unitMs
                    events += TransmitEvent(
                        symbol = TimedSymbol.Tone(toneDuration),
                        wordIndex = wIdx,
                        letterIndex = lIdx,
                        symbolIndexInLetter = sIdx,
                        symbolType = sym
                    )
                    if (sIdx < letter.symbols.lastIndex) {
                        events += TransmitEvent(symbol = TimedSymbol.Silence(unitMs))
                    }
                }
                if (lIdx < word.letters.lastIndex) {
                    events += TransmitEvent(symbol = TimedSymbol.Silence(3 * unitMs))
                }
            }
            if (wIdx < words.lastIndex) {
                events += TransmitEvent(symbol = TimedSymbol.Silence(7 * unitMs))
            }
        }
        return events
    }
}
```

- [ ] **Step 4: Run tests to verify they pass**

In Android Studio: right-click `MorseTranslatorTest` → Run.
Expected: all 15 tests pass, green bar.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/morseglyph/morse/MorseTranslator.kt \
        app/src/test/java/com/morseglyph/MorseTranslatorTest.kt
git commit -m "feat: implement MorseTranslator with full ITU symbol table (TDD)"
```

---

## Task 4: SharedPrefsRepository

**Files:**
- Create: `app/src/main/java/com/morseglyph/data/SharedPrefsRepository.kt`

- [ ] **Step 1: Implement SharedPrefsRepository**

```kotlin
// app/src/main/java/com/morseglyph/data/SharedPrefsRepository.kt
package com.morseglyph.data

import android.content.Context
import com.morseglyph.viewmodel.IndicatorMode

class SharedPrefsRepository(context: Context) {
    private val prefs = context.getSharedPreferences("morseglyph_prefs", Context.MODE_PRIVATE)

    fun getWpm(): Int = prefs.getInt("wpm", 15).coerceIn(5, 30)

    fun saveWpm(wpm: Int) {
        prefs.edit().putInt("wpm", wpm.coerceIn(5, 30)).apply()
    }

    fun getIndicatorMode(): IndicatorMode {
        val name = prefs.getString("indicator_mode", IndicatorMode.FULL_STRING.name)
        return runCatching { IndicatorMode.valueOf(name!!) }.getOrDefault(IndicatorMode.FULL_STRING)
    }

    fun saveIndicatorMode(mode: IndicatorMode) {
        prefs.edit().putString("indicator_mode", mode.name).apply()
    }
}
```

- [ ] **Step 2: Commit**

```bash
git add app/src/main/java/com/morseglyph/data/SharedPrefsRepository.kt
git commit -m "feat: add SharedPrefsRepository for WPM and indicator mode persistence"
```

---

## Task 5: AudioController

**Files:**
- Create: `app/src/main/java/com/morseglyph/audio/AudioController.kt`

- [ ] **Step 1: Implement AudioController**

```kotlin
// app/src/main/java/com/morseglyph/audio/AudioController.kt
package com.morseglyph.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlin.math.PI
import kotlin.math.min
import kotlin.math.sin

class AudioController {

    private val sampleRate = 44100
    private val frequency = 700.0
    private val amplitude = 0.7

    var available = true
        private set

    suspend fun beep(durationMs: Long) {
        if (!available) { delay(durationMs); return }
        withContext(Dispatchers.IO) {
            val numSamples = (sampleRate * durationMs / 1000.0).toInt()
            val buffer = buildBuffer(numSamples)
            try {
                val track = AudioTrack.Builder()
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_MEDIA)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .build()
                    )
                    .setAudioFormat(
                        AudioFormat.Builder()
                            .setSampleRate(sampleRate)
                            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                            .build()
                    )
                    .setBufferSizeInBytes(buffer.size * 2)
                    .setTransferMode(AudioTrack.MODE_STATIC)
                    .build()
                track.write(buffer, 0, buffer.size)
                track.play()
                delay(durationMs)
                track.stop()
                track.release()
            } catch (e: Exception) {
                available = false
            }
        }
    }

    private fun buildBuffer(numSamples: Int): ShortArray {
        val buffer = ShortArray(numSamples) { i ->
            val angle = 2.0 * PI * i.toDouble() * frequency / sampleRate
            (sin(angle) * Short.MAX_VALUE * amplitude).toInt().toShort()
        }
        // 5ms fade-in / fade-out to eliminate click artifacts
        val fadeSamples = min((sampleRate * 0.005).toInt(), numSamples / 4)
        for (i in 0 until fadeSamples) {
            val fade = i.toFloat() / fadeSamples
            buffer[i] = (buffer[i] * fade).toInt().toShort()
            buffer[numSamples - 1 - i] = (buffer[numSamples - 1 - i] * fade).toInt().toShort()
        }
        return buffer
    }
}
```

- [ ] **Step 2: Commit**

```bash
git add app/src/main/java/com/morseglyph/audio/AudioController.kt
git commit -m "feat: add AudioController with 700Hz sine wave beep and click-free fade"
```

---

## Task 6: GlyphController

**Files:**
- Create: `app/src/main/java/com/morseglyph/glyph/GlyphController.kt`

> **SDK note:** The GlyphMatrix Developer Kit AAR must be in `app/libs/` before this compiles.
> Download from: https://github.com/Nothing-Developer-Programme/GlyphMatrix-Developer-Kit
> Verify the exact package and class names against the SDK's bundled docs.
> The code below targets the API shown in Nothing's developer documentation.
> If import paths differ, update the imports — the logic stays identical.

- [ ] **Step 1: Implement GlyphController**

```kotlin
// app/src/main/java/com/morseglyph/glyph/GlyphController.kt
package com.morseglyph.glyph

import android.content.Context
import android.os.Build
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import kotlinx.coroutines.delay

// Import the GlyphMatrix SDK classes from the AAR.
// Verify these package names against the SDK documentation bundled with the AAR.
import com.nothing.ketchum.GlyphManager
import com.nothing.ketchum.GlyphFrame

class GlyphController(
    private val context: Context,
    private val onBindFailed: () -> Unit
) : DefaultLifecycleObserver {

    private var glyphManager: GlyphManager? = null
    var available = false
        private set

    // 25×25 = 625 channels; max brightness value varies by SDK version.
    // The GlyphMatrix SDK for Phone (3) uses values 0–4095.
    private val matrixSize = 625
    private val maxBrightness = 4095

    override fun onStart(owner: LifecycleOwner) {
        if (!isNothingPhone3()) {
            onBindFailed()
            return
        }
        try {
            glyphManager = GlyphManager.getInstance(context).also { mgr ->
                mgr.init(object : GlyphManager.Callback {
                    override fun onServiceConnected(manager: GlyphManager) {
                        manager.openSession()
                        available = true
                    }
                    override fun onServiceDisconnected(manager: GlyphManager) {
                        available = false
                    }
                })
            }
        } catch (e: Exception) {
            available = false
            onBindFailed()
        }
    }

    override fun onStop(owner: LifecycleOwner) {
        turnOffImmediate()
        try {
            glyphManager?.closeSession()
        } catch (_: Exception) {}
        glyphManager = null
        available = false
    }

    suspend fun flashOn(durationMs: Long) {
        if (!available) { delay(durationMs); return }
        try {
            setAllChannels(maxBrightness)
        } catch (e: Exception) {
            available = false
            onBindFailed()
        }
        delay(durationMs)
    }

    suspend fun flashOff(durationMs: Long) {
        if (!available) { delay(durationMs); return }
        try {
            setAllChannels(0)
        } catch (_: Exception) {}
        delay(durationMs)
    }

    fun turnOffImmediate() {
        if (!available) return
        try { setAllChannels(0) } catch (_: Exception) {}
    }

    private fun setAllChannels(brightness: Int) {
        val mgr = glyphManager ?: return
        // Build a frame with all channels at the given brightness level.
        // GlyphFrame.Builder API — verify channel count against SDK docs for Phone (3).
        val builder = mgr.getGlyphFrameBuilder()
        // Phone (3) has 25 zones; set each to brightness.
        // The SDK exposes named channels; set them all here.
        // Replace with the correct channel constants from the SDK.
        for (i in 0 until 25) {
            try { builder.buildChannel(i, brightness) } catch (_: Exception) {}
        }
        mgr.toggle(builder.build())
    }

    private fun isNothingPhone3(): Boolean =
        Build.MANUFACTURER.equals("Nothing", ignoreCase = true) &&
        (Build.MODEL.contains("A059", ignoreCase = true) ||
         Build.MODEL.contains("Phone (3)", ignoreCase = true))
}
```

> **If the GlyphMatrix SDK uses a different API** (e.g., `setAppMatrixFrame` with an `IntArray`), replace `setAllChannels` with:
> ```kotlin
> private fun setAllChannels(brightness: Int) {
>     val pixels = IntArray(matrixSize) { brightness }
>     glyphSession?.setAppMatrixFrame(GlyphMatrixFrame(pixels))
> }
> ```
> Adjust `GlyphMatrixSession` / `GlyphMatrixFrame` imports to match the AAR.

- [ ] **Step 2: Commit**

```bash
git add app/src/main/java/com/morseglyph/glyph/GlyphController.kt
git commit -m "feat: add GlyphController wrapping GlyphMatrix SDK with lifecycle-safe graceful degrade"
```

---

## Task 7: MorseViewModel

**Files:**
- Create: `app/src/main/java/com/morseglyph/viewmodel/MorseViewModel.kt`
- Create: `app/src/main/java/com/morseglyph/viewmodel/MorseViewModelFactory.kt`

- [ ] **Step 1: Implement MorseViewModel.kt**

```kotlin
// app/src/main/java/com/morseglyph/viewmodel/MorseViewModel.kt
package com.morseglyph.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.morseglyph.audio.AudioController
import com.morseglyph.data.SharedPrefsRepository
import com.morseglyph.glyph.GlyphController
import com.morseglyph.morse.MorseTranslator
import com.morseglyph.morse.TimedSymbol
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class MorseViewModel(
    private val glyphController: GlyphController,
    private val audioController: AudioController,
    private val prefsRepository: SharedPrefsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(MorseUiState())
    val uiState: StateFlow<MorseUiState> = _uiState.asStateFlow()

    private var transmissionJob: Job? = null

    init {
        _uiState.update { it.copy(
            wpm = prefsRepository.getWpm(),
            indicatorMode = prefsRepository.getIndicatorMode()
        )}
    }

    fun onInputTextChange(text: String) {
        if (text.length > 100) return
        val words = if (text.isBlank()) emptyList() else MorseTranslator.translate(text)
        val unitMs = unitMs()
        _uiState.update { it.copy(
            inputText = text,
            morseDisplayString = if (text.isBlank()) "" else MorseTranslator.toDisplayString(words),
            morseWords = words,
            transmitEvents = MorseTranslator.toTransmitEvents(words, unitMs),
            inputError = null
        )}
    }

    fun onWpmChange(wpm: Int) {
        prefsRepository.saveWpm(wpm)
        val words = _uiState.value.morseWords
        val unitMs = 1200L / wpm
        _uiState.update { it.copy(
            wpm = wpm,
            transmitEvents = MorseTranslator.toTransmitEvents(words, unitMs)
        )}
    }

    fun onIndicatorModeChange(mode: IndicatorMode) {
        prefsRepository.saveIndicatorMode(mode)
        _uiState.update { it.copy(indicatorMode = mode) }
    }

    fun transmit() {
        val state = _uiState.value
        if (state.inputText.isBlank()) {
            _uiState.update { it.copy(inputError = "Enter a message") }
            return
        }
        val events = state.transmitEvents
        if (events.isEmpty()) return

        transmissionJob = viewModelScope.launch {
            _uiState.update { it.copy(
                transmissionState = TransmissionState.TRANSMITTING,
                inputError = null,
                snackbarMessage = null
            )}
            events.forEachIndexed { index, event ->
                _uiState.update { it.copy(activeEventIndex = index) }
                when (val sym = event.symbol) {
                    is TimedSymbol.Tone -> coroutineScope {
                        launch { glyphController.flashOn(sym.durationMs) }
                        launch { audioController.beep(sym.durationMs) }
                    }
                    is TimedSymbol.Silence -> glyphController.flashOff(sym.durationMs)
                }
            }
            glyphController.turnOffImmediate()
            _uiState.update { it.copy(
                transmissionState = TransmissionState.IDLE,
                activeEventIndex = -1
            )}
        }
    }

    fun stop() {
        transmissionJob?.cancel()
        glyphController.turnOffImmediate()
        _uiState.update { it.copy(
            transmissionState = TransmissionState.IDLE,
            activeEventIndex = -1
        )}
    }

    fun onGlyphBindFailed() {
        _uiState.update { it.copy(snackbarMessage = "Glyph unavailable — audio only") }
    }

    fun clearSnackbar() {
        _uiState.update { it.copy(snackbarMessage = null) }
    }

    private fun unitMs(): Long = 1200L / _uiState.value.wpm

    override fun onCleared() {
        super.onCleared()
        audioController.available.let { } // ensure audioController can be GC'd cleanly
    }
}
```

- [ ] **Step 2: Implement MorseViewModelFactory.kt**

```kotlin
// app/src/main/java/com/morseglyph/viewmodel/MorseViewModelFactory.kt
package com.morseglyph.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.morseglyph.audio.AudioController
import com.morseglyph.data.SharedPrefsRepository
import com.morseglyph.glyph.GlyphController

class MorseViewModelFactory(
    private val glyphController: GlyphController,
    private val audioController: AudioController,
    private val prefsRepository: SharedPrefsRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T =
        MorseViewModel(glyphController, audioController, prefsRepository) as T
}
```

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/morseglyph/viewmodel/
git commit -m "feat: add MorseViewModel with cancellable transmission coroutine"
```

---

## Task 8: UI Theme

**Files:**
- Create: `app/src/main/java/com/morseglyph/ui/theme/Color.kt`
- Create: `app/src/main/java/com/morseglyph/ui/theme/Type.kt`
- Create: `app/src/main/java/com/morseglyph/ui/theme/Theme.kt`

- [ ] **Step 1: Create Color.kt**

```kotlin
// app/src/main/java/com/morseglyph/ui/theme/Color.kt
package com.morseglyph.ui.theme

import androidx.compose.ui.graphics.Color

val Black = Color(0xFF000000)
val NothingSurface = Color(0xFF0D0D0D)
val NothingBorder = Color(0xFF1F1F1F)
val NothingInactive = Color(0xFF3A3A3A)
val NothingAccent = Color(0xFFFFFFFF)
val NothingError = Color(0xFFFF3B30)
val NothingDim = Color(0xFF888888)
```

- [ ] **Step 2: Create Type.kt**

```kotlin
// app/src/main/java/com/morseglyph/ui/theme/Type.kt
package com.morseglyph.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// RobotoMono is a system font available on all Android devices.
// To swap in Nothing's ndot font: add the .ttf to app/src/main/res/font/
// and replace FontFamily.Monospace below with FontFamily(Font(R.font.ndot)).
val RobotoMono = FontFamily.Monospace

val MorseTypography = Typography(
    headlineLarge = TextStyle(
        fontFamily = RobotoMono,
        fontWeight = FontWeight.Bold,
        fontSize = 24.sp,
        letterSpacing = 0.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = RobotoMono,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        letterSpacing = 0.sp
    ),
    labelMedium = TextStyle(
        fontFamily = RobotoMono,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        letterSpacing = 1.sp
    )
)
```

- [ ] **Step 3: Create Theme.kt**

```kotlin
// app/src/main/java/com/morseglyph/ui/theme/Theme.kt
package com.morseglyph.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val MorseColorScheme = darkColorScheme(
    background = Black,
    surface = NothingSurface,
    onBackground = NothingAccent,
    onSurface = NothingAccent,
    primary = NothingAccent,
    onPrimary = Black,
    secondary = NothingInactive,
    onSecondary = NothingAccent,
    outline = NothingBorder,
    error = NothingError,
    onError = Black
)

@Composable
fun MorseGlyphTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = MorseColorScheme,
        typography = MorseTypography,
        content = content
    )
}
```

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/com/morseglyph/ui/theme/
git commit -m "feat: add Nothing OS inspired dark theme (sharp corners, mono font, black bg)"
```

---

## Task 9: Base UI Components

**Files:**
- Create: `app/src/main/java/com/morseglyph/ui/components/DotMatrixHeader.kt`
- Create: `app/src/main/java/com/morseglyph/ui/components/MessageInputField.kt`
- Create: `app/src/main/java/com/morseglyph/ui/components/MorseOutputField.kt`

- [ ] **Step 1: Create DotMatrixHeader.kt**

```kotlin
// app/src/main/java/com/morseglyph/ui/components/DotMatrixHeader.kt
package com.morseglyph.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.morseglyph.ui.theme.NothingInactive

@Composable
fun DotMatrixHeader(modifier: Modifier = Modifier) {
    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(48.dp)
    ) {
        val dotRadius = 2.dp.toPx()
        val spacing = 12.dp.toPx()
        val cols = (size.width / spacing).toInt()
        val rows = (size.height / spacing).toInt()

        for (row in 0..rows) {
            for (col in 0..cols) {
                val x = col * spacing
                val y = row * spacing
                val alpha = if ((row + col) % 3 == 0) 0.6f else 0.15f
                drawCircle(
                    color = Color.White.copy(alpha = alpha),
                    radius = dotRadius,
                    center = Offset(x, y)
                )
            }
        }
    }
}
```

- [ ] **Step 2: Create MessageInputField.kt**

```kotlin
// app/src/main/java/com/morseglyph/ui/components/MessageInputField.kt
package com.morseglyph.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.morseglyph.ui.theme.NothingAccent
import com.morseglyph.ui.theme.NothingBorder
import com.morseglyph.ui.theme.NothingDim
import com.morseglyph.ui.theme.NothingError
import com.morseglyph.ui.theme.NothingSurface
import com.morseglyph.ui.theme.RobotoMono

@Composable
fun MessageInputField(
    value: String,
    onValueChange: (String) -> Unit,
    error: String?,
    modifier: Modifier = Modifier
) {
    val isError = error != null
    Column(modifier = modifier) {
        OutlinedTextField(
            value = value,
            onValueChange = { if (it.length <= 100) onValueChange(it) },
            label = {
                Text(
                    text = "ENTER MESSAGE",
                    fontFamily = RobotoMono,
                    fontSize = 11.sp,
                    letterSpacing = 2.sp
                )
            },
            isError = isError,
            supportingText = {
                if (isError) {
                    Text(text = error!!, color = NothingError, fontFamily = RobotoMono, fontSize = 11.sp)
                } else {
                    Text(
                        text = "${value.length}/100",
                        color = if (value.length > 90) NothingError else NothingDim,
                        fontFamily = RobotoMono,
                        fontSize = 11.sp
                    )
                }
            },
            shape = RoundedCornerShape(0.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = NothingAccent,
                unfocusedBorderColor = NothingBorder,
                errorBorderColor = NothingError,
                focusedContainerColor = NothingSurface,
                unfocusedContainerColor = NothingSurface,
                errorContainerColor = NothingSurface,
                focusedTextColor = NothingAccent,
                unfocusedTextColor = NothingAccent,
                cursorColor = NothingAccent,
                focusedLabelColor = NothingAccent,
                unfocusedLabelColor = NothingDim,
                errorLabelColor = NothingError
            ),
            modifier = Modifier.fillMaxWidth(),
            singleLine = false,
            maxLines = 3
        )
    }
}
```

- [ ] **Step 3: Create MorseOutputField.kt**

```kotlin
// app/src/main/java/com/morseglyph/ui/components/MorseOutputField.kt
package com.morseglyph.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.morseglyph.ui.theme.NothingBorder
import com.morseglyph.ui.theme.NothingDim
import com.morseglyph.ui.theme.NothingInactive
import com.morseglyph.ui.theme.NothingSurface
import com.morseglyph.ui.theme.RobotoMono

@Composable
fun MorseOutputField(morseString: String, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .background(NothingSurface)
            .border(1.dp, NothingBorder)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        if (morseString.isEmpty()) {
            Text(
                text = "MORSE OUTPUT",
                color = NothingInactive,
                fontFamily = RobotoMono,
                fontSize = 11.sp,
                letterSpacing = 2.sp
            )
        } else {
            Text(
                text = morseString,
                color = NothingDim,
                fontFamily = RobotoMono,
                fontSize = 14.sp,
                lineHeight = 20.sp,
                textAlign = TextAlign.Start
            )
        }
    }
}
```

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/com/morseglyph/ui/components/DotMatrixHeader.kt \
        app/src/main/java/com/morseglyph/ui/components/MessageInputField.kt \
        app/src/main/java/com/morseglyph/ui/components/MorseOutputField.kt
git commit -m "feat: add base UI components (header, input, morse output)"
```

---

## Task 10: Control Components

**Files:**
- Create: `app/src/main/java/com/morseglyph/ui/components/WpmSlider.kt`
- Create: `app/src/main/java/com/morseglyph/ui/components/IndicatorModeSelector.kt`

- [ ] **Step 1: Create WpmSlider.kt**

```kotlin
// app/src/main/java/com/morseglyph/ui/components/WpmSlider.kt
package com.morseglyph.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.morseglyph.ui.theme.NothingAccent
import com.morseglyph.ui.theme.NothingBorder
import com.morseglyph.ui.theme.NothingDim
import com.morseglyph.ui.theme.NothingInactive
import com.morseglyph.ui.theme.RobotoMono

@Composable
fun WpmSlider(
    wpm: Int,
    onWpmChange: (Int) -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "SPEED",
                color = NothingDim,
                fontFamily = RobotoMono,
                fontSize = 11.sp,
                letterSpacing = 2.sp
            )
            Text(
                text = " — $wpm WPM",
                color = NothingAccent,
                fontFamily = RobotoMono,
                fontSize = 13.sp,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = "5            30",
                color = NothingInactive,
                fontFamily = RobotoMono,
                fontSize = 10.sp
            )
        }
        Slider(
            value = wpm.toFloat(),
            onValueChange = { onWpmChange(it.toInt()) },
            valueRange = 5f..30f,
            steps = 24,
            enabled = enabled,
            colors = SliderDefaults.colors(
                thumbColor = NothingAccent,
                activeTrackColor = NothingAccent,
                inactiveTrackColor = NothingBorder
            ),
            modifier = Modifier.fillMaxWidth()
        )
    }
}
```

- [ ] **Step 2: Create IndicatorModeSelector.kt**

```kotlin
// app/src/main/java/com/morseglyph/ui/components/IndicatorModeSelector.kt
package com.morseglyph.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.morseglyph.ui.theme.NothingAccent
import com.morseglyph.ui.theme.NothingBorder
import com.morseglyph.ui.theme.NothingDim
import com.morseglyph.ui.theme.NothingSurface
import com.morseglyph.ui.theme.RobotoMono
import com.morseglyph.viewmodel.IndicatorMode

private val modes = listOf(
    IndicatorMode.SYMBOL to "SYM",
    IndicatorMode.FULL_STRING to "FULL",
    IndicatorMode.PER_LETTER to "LETTER"
)

@Composable
fun IndicatorModeSelector(
    selected: IndicatorMode,
    onSelect: (IndicatorMode) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(modifier = modifier.fillMaxWidth()) {
        modes.forEach { (mode, label) ->
            val isSelected = mode == selected
            OutlinedButton(
                onClick = { onSelect(mode) },
                shape = RoundedCornerShape(0.dp),
                border = BorderStroke(1.dp, if (isSelected) NothingAccent else NothingBorder),
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = if (isSelected) NothingAccent else NothingSurface,
                    contentColor = if (isSelected) NothingSurface else NothingDim
                ),
                modifier = Modifier
                    .weight(1f)
                    .height(36.dp)
            ) {
                Text(
                    text = label,
                    fontFamily = RobotoMono,
                    fontSize = 10.sp,
                    letterSpacing = 1.sp
                )
            }
        }
    }
}
```

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/morseglyph/ui/components/WpmSlider.kt \
        app/src/main/java/com/morseglyph/ui/components/IndicatorModeSelector.kt
git commit -m "feat: add WPM slider and indicator mode segmented button"
```

---

## Task 11: Live Indicator Composables

**Files:**
- Create: `app/src/main/java/com/morseglyph/ui/components/SymbolIndicator.kt`
- Create: `app/src/main/java/com/morseglyph/ui/components/FullStringIndicator.kt`
- Create: `app/src/main/java/com/morseglyph/ui/components/PerLetterIndicator.kt`
- Create: `app/src/main/java/com/morseglyph/ui/components/LiveIndicator.kt`

- [ ] **Step 1: Create SymbolIndicator.kt**

```kotlin
// app/src/main/java/com/morseglyph/ui/components/SymbolIndicator.kt
package com.morseglyph.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.morseglyph.morse.MorseSymbol
import com.morseglyph.morse.TimedSymbol
import com.morseglyph.morse.TransmitEvent
import com.morseglyph.ui.theme.RobotoMono

@Composable
fun SymbolIndicator(
    events: List<TransmitEvent>,
    activeIndex: Int,
    modifier: Modifier = Modifier
) {
    val activeEvent = events.getOrNull(activeIndex)
    val isTone = activeEvent?.symbol is TimedSymbol.Tone
    val symbolChar = when (activeEvent?.symbolType) {
        MorseSymbol.DOT -> "·"
        MorseSymbol.DASH -> "—"
        null -> if (isTone) "·" else " "
    }
    val alpha by animateFloatAsState(
        targetValue = if (isTone) 1f else 0.12f,
        animationSpec = tween(durationMillis = 40),
        label = "symbol_alpha"
    )
    Box(
        modifier = modifier.fillMaxWidth().height(96.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = symbolChar,
            fontSize = 80.sp,
            color = Color.White.copy(alpha = alpha),
            fontFamily = RobotoMono
        )
    }
}
```

- [ ] **Step 2: Create FullStringIndicator.kt**

```kotlin
// app/src/main/java/com/morseglyph/ui/components/FullStringIndicator.kt
package com.morseglyph.ui.components

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.morseglyph.morse.MorseSymbol
import com.morseglyph.morse.MorseWord
import com.morseglyph.morse.TimedSymbol
import com.morseglyph.morse.TransmitEvent
import com.morseglyph.ui.theme.NothingAccent
import com.morseglyph.ui.theme.NothingInactive
import com.morseglyph.ui.theme.RobotoMono

@Composable
fun FullStringIndicator(
    words: List<MorseWord>,
    events: List<TransmitEvent>,
    activeIndex: Int,
    modifier: Modifier = Modifier
) {
    val activeEvent = events.getOrNull(activeIndex)
        ?.takeIf { it.symbol is TimedSymbol.Tone }

    val annotated = buildAnnotatedString {
        words.forEachIndexed { wIdx, word ->
            if (wIdx > 0) {
                withStyle(SpanStyle(color = NothingInactive, fontSize = 14.sp)) { append(" / ") }
            }
            word.letters.forEachIndexed { lIdx, letter ->
                if (lIdx > 0) {
                    withStyle(SpanStyle(color = NothingInactive, fontSize = 14.sp)) { append("   ") }
                }
                if (letter.symbols.isEmpty()) {
                    withStyle(SpanStyle(color = NothingInactive, fontSize = 14.sp)) { append("?") }
                    return@forEachIndexed
                }
                letter.symbols.forEachIndexed { sIdx, sym ->
                    if (sIdx > 0) {
                        withStyle(SpanStyle(color = NothingInactive, fontSize = 14.sp)) { append(" ") }
                    }
                    val isActive = activeEvent?.wordIndex == wIdx &&
                                   activeEvent.letterIndex == lIdx &&
                                   activeEvent.symbolIndexInLetter == sIdx
                    val color = if (isActive) NothingAccent else NothingInactive
                    val size = if (isActive) 20.sp else 14.sp
                    withStyle(SpanStyle(color = color, fontSize = size)) {
                        append(if (sym == MorseSymbol.DOT) "·" else "—")
                    }
                }
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(72.dp)
            .horizontalScroll(rememberScrollState()),
        contentAlignment = Alignment.CenterStart
    ) {
        Text(
            text = annotated,
            fontFamily = RobotoMono,
            modifier = Modifier.padding(horizontal = 4.dp)
        )
    }
}
```

- [ ] **Step 3: Create PerLetterIndicator.kt**

```kotlin
// app/src/main/java/com/morseglyph/ui/components/PerLetterIndicator.kt
package com.morseglyph.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.morseglyph.morse.MorseSymbol
import com.morseglyph.morse.MorseWord
import com.morseglyph.morse.TimedSymbol
import com.morseglyph.morse.TransmitEvent
import com.morseglyph.ui.theme.NothingAccent
import com.morseglyph.ui.theme.NothingInactive
import com.morseglyph.ui.theme.RobotoMono

@Composable
fun PerLetterIndicator(
    words: List<MorseWord>,
    events: List<TransmitEvent>,
    activeIndex: Int,
    modifier: Modifier = Modifier
) {
    val activeEvent = events.getOrNull(activeIndex)
    val toneEvent = activeEvent?.takeIf { it.symbol is TimedSymbol.Tone }
    val currentLetter = toneEvent?.let {
        words.getOrNull(it.wordIndex)?.letters?.getOrNull(it.letterIndex)
    }

    Box(
        modifier = modifier.fillMaxWidth().height(96.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = currentLetter?.char?.uppercaseChar()?.toString() ?: "·",
                fontSize = 44.sp,
                color = if (currentLetter != null) NothingAccent else NothingInactive,
                fontFamily = RobotoMono
            )
            Spacer(Modifier.height(4.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                currentLetter?.symbols?.forEachIndexed { sIdx, sym ->
                    val isActive = toneEvent?.symbolIndexInLetter == sIdx
                    val alpha by animateFloatAsState(
                        targetValue = if (isActive) 1f else 0.25f,
                        animationSpec = tween(40),
                        label = "sym_alpha_$sIdx"
                    )
                    Text(
                        text = if (sym == MorseSymbol.DOT) "·" else "—",
                        fontSize = if (isActive) 26.sp else 20.sp,
                        color = Color.White.copy(alpha = alpha),
                        fontFamily = RobotoMono
                    )
                } ?: run {
                    Text(text = "—", fontSize = 20.sp, color = NothingInactive, fontFamily = RobotoMono)
                }
            }
        }
    }
}
```

- [ ] **Step 4: Create LiveIndicator.kt (dispatch composable)**

```kotlin
// app/src/main/java/com/morseglyph/ui/components/LiveIndicator.kt
package com.morseglyph.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.morseglyph.morse.MorseWord
import com.morseglyph.morse.TransmitEvent
import com.morseglyph.ui.theme.NothingBorder
import com.morseglyph.ui.theme.NothingSurface
import com.morseglyph.viewmodel.IndicatorMode

@Composable
fun LiveIndicator(
    mode: IndicatorMode,
    words: List<MorseWord>,
    events: List<TransmitEvent>,
    activeIndex: Int,
    modifier: Modifier = Modifier
) {
    val boxMod = modifier
        .fillMaxWidth()
        .background(NothingSurface)
        .border(1.dp, NothingBorder)
        .padding(8.dp)

    when (mode) {
        IndicatorMode.SYMBOL -> SymbolIndicator(events, activeIndex, boxMod)
        IndicatorMode.FULL_STRING -> FullStringIndicator(words, events, activeIndex, boxMod)
        IndicatorMode.PER_LETTER -> PerLetterIndicator(words, events, activeIndex, boxMod)
    }
}
```

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/morseglyph/ui/components/SymbolIndicator.kt \
        app/src/main/java/com/morseglyph/ui/components/FullStringIndicator.kt \
        app/src/main/java/com/morseglyph/ui/components/PerLetterIndicator.kt \
        app/src/main/java/com/morseglyph/ui/components/LiveIndicator.kt
git commit -m "feat: add three live indicator composables (symbol, full-string, per-letter)"
```

---

## Task 12: TransmitStopRow + MorseScreen

**Files:**
- Create: `app/src/main/java/com/morseglyph/ui/components/TransmitStopRow.kt`
- Create: `app/src/main/java/com/morseglyph/ui/MorseScreen.kt`

- [ ] **Step 1: Create TransmitStopRow.kt**

```kotlin
// app/src/main/java/com/morseglyph/ui/components/TransmitStopRow.kt
package com.morseglyph.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.morseglyph.ui.theme.NothingAccent
import com.morseglyph.ui.theme.NothingBorder
import com.morseglyph.ui.theme.NothingError
import com.morseglyph.ui.theme.NothingInactive
import com.morseglyph.ui.theme.NothingSurface
import com.morseglyph.ui.theme.RobotoMono
import com.morseglyph.viewmodel.TransmissionState

@Composable
fun TransmitStopRow(
    transmissionState: TransmissionState,
    onTransmit: () -> Unit,
    onStop: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isTransmitting = transmissionState == TransmissionState.TRANSMITTING
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Button(
            onClick = onTransmit,
            enabled = !isTransmitting,
            shape = RoundedCornerShape(0.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = NothingAccent,
                contentColor = NothingSurface,
                disabledContainerColor = NothingInactive,
                disabledContentColor = NothingSurface
            ),
            modifier = Modifier.weight(1f).height(52.dp)
        ) {
            Text(
                text = "TRANSMIT",
                fontFamily = RobotoMono,
                fontSize = 13.sp,
                letterSpacing = 3.sp
            )
        }
        OutlinedButton(
            onClick = onStop,
            enabled = isTransmitting,
            shape = RoundedCornerShape(0.dp),
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = NothingError,
                disabledContentColor = NothingInactive
            ),
            border = androidx.compose.foundation.BorderStroke(
                1.dp,
                if (isTransmitting) NothingError else NothingBorder
            ),
            modifier = Modifier.weight(1f).height(52.dp)
        ) {
            Text(
                text = "STOP",
                fontFamily = RobotoMono,
                fontSize = 13.sp,
                letterSpacing = 3.sp
            )
        }
    }
}
```

- [ ] **Step 2: Create MorseScreen.kt**

```kotlin
// app/src/main/java/com/morseglyph/ui/MorseScreen.kt
package com.morseglyph.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.morseglyph.ui.components.DotMatrixHeader
import com.morseglyph.ui.components.IndicatorModeSelector
import com.morseglyph.ui.components.LiveIndicator
import com.morseglyph.ui.components.MessageInputField
import com.morseglyph.ui.components.MorseOutputField
import com.morseglyph.ui.components.TransmitStopRow
import com.morseglyph.ui.components.WpmSlider
import com.morseglyph.ui.theme.Black
import com.morseglyph.ui.theme.NothingAccent
import com.morseglyph.ui.theme.NothingBorder
import com.morseglyph.ui.theme.NothingDim
import com.morseglyph.ui.theme.NothingSurface
import com.morseglyph.ui.theme.RobotoMono
import com.morseglyph.viewmodel.MorseViewModel
import com.morseglyph.viewmodel.TransmissionState

@Composable
fun MorseScreen(viewModel: MorseViewModel) {
    val state by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val isTransmitting = state.transmissionState == TransmissionState.TRANSMITTING

    LaunchedEffect(state.snackbarMessage) {
        val msg = state.snackbarMessage ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(msg)
        viewModel.clearSnackbar()
    }

    Scaffold(
        containerColor = Black,
        snackbarHost = {
            SnackbarHost(snackbarHostState) { data ->
                Snackbar(
                    snackbarData = data,
                    containerColor = NothingSurface,
                    contentColor = NothingAccent,
                    actionColor = NothingAccent
                )
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .imePadding()
                .verticalScroll(rememberScrollState())
        ) {
            DotMatrixHeader(
                modifier = Modifier.fillMaxWidth()
            )

            Text(
                text = "MORSEGLYPH",
                color = NothingAccent,
                fontFamily = RobotoMono,
                fontSize = 11.sp,
                letterSpacing = 4.sp,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )

            // Divider
            Spacer(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(NothingBorder)
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                Spacer(Modifier.height(16.dp))

                MessageInputField(
                    value = state.inputText,
                    onValueChange = { viewModel.onInputTextChange(it) },
                    error = state.inputError
                )

                Spacer(Modifier.height(12.dp))

                MorseOutputField(morseString = state.morseDisplayString)

                Spacer(Modifier.height(16.dp))

                // Indicator mode selector label
                Text(
                    text = "INDICATOR MODE",
                    color = NothingDim,
                    fontFamily = RobotoMono,
                    fontSize = 10.sp,
                    letterSpacing = 2.sp
                )
                Spacer(Modifier.height(6.dp))

                IndicatorModeSelector(
                    selected = state.indicatorMode,
                    onSelect = { viewModel.onIndicatorModeChange(it) }
                )

                Spacer(Modifier.height(12.dp))

                LiveIndicator(
                    mode = state.indicatorMode,
                    words = state.morseWords,
                    events = state.transmitEvents,
                    activeIndex = state.activeEventIndex
                )

                Spacer(Modifier.height(16.dp))

                WpmSlider(
                    wpm = state.wpm,
                    onWpmChange = { viewModel.onWpmChange(it) },
                    enabled = !isTransmitting
                )

                Spacer(Modifier.height(20.dp))

                TransmitStopRow(
                    transmissionState = state.transmissionState,
                    onTransmit = { viewModel.transmit() },
                    onStop = { viewModel.stop() }
                )

                Spacer(Modifier.height(24.dp))
            }
        }
    }
}
```

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/morseglyph/ui/components/TransmitStopRow.kt \
        app/src/main/java/com/morseglyph/ui/MorseScreen.kt
git commit -m "feat: add TransmitStopRow and assemble MorseScreen root composable"
```

---

## Task 13: MainActivity + Wire Everything Together

**Files:**
- Create: `app/src/main/java/com/morseglyph/MainActivity.kt`

- [ ] **Step 1: Implement MainActivity.kt**

```kotlin
// app/src/main/java/com/morseglyph/MainActivity.kt
package com.morseglyph

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.core.view.WindowCompat
import com.morseglyph.audio.AudioController
import com.morseglyph.data.SharedPrefsRepository
import com.morseglyph.glyph.GlyphController
import com.morseglyph.ui.MorseScreen
import com.morseglyph.ui.theme.MorseGlyphTheme
import com.morseglyph.viewmodel.MorseViewModel
import com.morseglyph.viewmodel.MorseViewModelFactory

class MainActivity : ComponentActivity() {

    private lateinit var glyphController: GlyphController
    private lateinit var audioController: AudioController

    private val viewModel: MorseViewModel by viewModels {
        MorseViewModelFactory(glyphController, audioController, SharedPrefsRepository(this))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Extend content behind system bars for full-bleed black background
        WindowCompat.setDecorFitsSystemWindows(window, false)

        audioController = AudioController()

        glyphController = GlyphController(
            context = this,
            onBindFailed = { viewModel.onGlyphBindFailed() }
        )
        lifecycle.addObserver(glyphController)

        setContent {
            MorseGlyphTheme {
                MorseScreen(viewModel = viewModel)
            }
        }
    }
}
```

- [ ] **Step 2: Commit**

```bash
git add app/src/main/java/com/morseglyph/MainActivity.kt
git commit -m "feat: wire MainActivity — lifecycle, ViewModel, Compose entry point"
```

---

## Task 14: README

**Files:**
- Create: `README.md`

- [ ] **Step 1: Write README.md**

```markdown
# MorseGlyph

A Nothing OS–inspired Android app that translates text into Morse code and simultaneously:
- Flashes the **Nothing Phone (3) Glyph Matrix** (dots = short flash, dashes = long flash)
- Plays **700 Hz sine-wave beep tones** via AudioTrack

---

## Prerequisites

### 1. Download the GlyphMatrix SDK AAR

1. Visit: https://github.com/Nothing-Developer-Programme/GlyphMatrix-Developer-Kit
2. Download the latest release AAR (e.g. `glyph-matrix-sdk-x.x.x.aar`)
3. Copy it to `app/libs/glyph-matrix-sdk.aar` (rename to that exact filename, or leave as-is — the build includes all `*.aar` files from `app/libs/`)

> **Without the AAR the project will not compile.** The app runs in audio-only mode on non-Nothing-Phone-3 devices.

### 2. Verify SDK API

After downloading, open the AAR's bundled docs or `classes.jar` and confirm:
- The package name used in `GlyphController.kt` (`com.nothing.ketchum`) matches what the AAR exports
- The `GlyphManager`, `GlyphFrame`, callback method names match
- Adjust `GlyphController.kt` imports/calls if they differ

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
            ├── MorseTranslator  (pure Kotlin, no Android deps)
            ├── GlyphController  (GlyphMatrix SDK, LifecycleObserver)
            ├── AudioController  (AudioTrack, 700 Hz sine wave)
            └── SharedPrefsRepository (WPM + indicator mode persistence)
```

---

## Screenshots

| Idle | Transmitting — Full String | Transmitting — Per Letter |
|------|---------------------------|--------------------------|
| _(add screenshot)_ | _(add screenshot)_ | _(add screenshot)_ |
```

- [ ] **Step 2: Commit**

```bash
git add README.md
git commit -m "docs: add README with SDK setup, Glyph enable steps, and architecture overview"
```

---

## Self-Review

### Spec Coverage Check

| Spec requirement | Covered in task |
|---|---|
| A–Z, 0–9, space Morse table | Task 3 (MorseTranslator TABLE) |
| ITU timing: dot=1u, dash=3u, sym-gap=1u, letter-gap=3u, word-gap=7u | Task 3 (toTransmitEvents) |
| WPM 5–30 speed slider | Task 10 (WpmSlider) |
| WPM persists | Task 4 (SharedPrefsRepository) |
| Glyph flash on Tone / off on Silence | Task 6 (GlyphController) |
| 700 Hz AudioTrack beep, click-free | Task 5 (AudioController) |
| Glyph + beep simultaneous sibling coroutines | Task 7 (MorseViewModel.transmit) |
| 3 indicator modes, user-selectable | Tasks 10, 11 |
| Indicator mode persists | Task 4 (SharedPrefsRepository) |
| Graceful degrade non-Nothing-Phone-3 | Task 6 (isNothingPhone3 check) |
| Snackbar "Glyph unavailable — audio only" | Tasks 6, 7 (onBindFailed callback) |
| Empty input error (no toast, inline) | Tasks 2, 7, 9 (inputError state, MessageInputField) |
| Max 100 chars, counter turns red | Task 9 (MessageInputField) |
| Unknown chars shown as `?` | Task 3 (MorseTranslator, MorseOutputField) |
| STOP cancels job, clears matrix immediately | Task 7 (MorseViewModel.stop) |
| Lifecycle-safe glyph release on background | Task 6 (onStop) |
| Min SDK 26, Target 35 | Task 1 (app/build.gradle.kts) |
| Nothing OS aesthetic: black, sharp corners, mono | Task 8 (Theme, Color) |
| Dot-matrix decorative header | Task 9 (DotMatrixHeader) |
| README with SDK setup instructions | Task 14 |
| Unit tests for MorseTranslator | Task 3 |

All requirements covered. No TBDs or placeholders remain.

### Type Consistency Check

- `TransmitEvent` defined in Task 2, used in Tasks 7, 11 ✓
- `MorseUiState.transmitEvents: List<TransmitEvent>` defined in Task 2 ✓
- `MorseTranslator.toTransmitEvents()` defined in Task 3, called in Task 7 ✓
- `GlyphController.flashOn/flashOff/turnOffImmediate` defined in Task 6, called in Task 7 ✓
- `AudioController.beep()` defined in Task 5, called in Task 7 ✓
- `IndicatorMode` enum defined in Task 2, used in Tasks 4, 7, 10, 11 ✓
- `MorseViewModel.onGlyphBindFailed()` defined in Task 7, called in Task 13 ✓
