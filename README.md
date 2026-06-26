# Loopline — One Line. Infinite Focus.

A minimal, native **Android** puzzle game built from the design in `loopline.png`.
Connect every dot with a single continuous line — the line may never cross
itself and every dot must be used exactly once. Solve fast to earn 3 stars.

> Genre: Hyper-casual puzzle · Platform: Android (8.0+ / API 26) · 100% native Kotlin

## Features

- **One-finger gameplay** — drag to draw the line, drag back to undo.
- **Hundreds of levels** — deterministic, procedurally generated and always solvable
  (each level is derived from a real non-crossing solution), with smooth difficulty ramp.
- **Daily challenge** — a fresh deterministic puzzle every calendar day.
- **Stars, coins & progression** — level unlocks, best-time tracking, hint system.
- **Hints** — spend coins to reveal (and correct) the next step.
- **Settings** — sound, vibration and 3 line themes (Amber / Pink / Teal).
- **Polished feel** — glowing line rendering, dot pop animations, synthesized
  musical feedback tones (no bundled audio assets) and haptics.
- **No internet permission, no ads, no tracking.**

## Project layout

```
app/src/main/java/com/loopline/game/
  game/   Board.kt, BoardModel, LevelGenerator (self-avoiding-walk), GameView (custom canvas view)
  ui/     MainActivity, GameActivity, LevelSelectActivity, SettingsActivity
  util/   Prefs, Haptics, SoundManager, LineThemes
app/src/main/res/   themes, drawables (vector icons + backgrounds), layouts, adaptive launcher icon
.github/workflows/android.yml   builds + publishes the APK
```

## Getting the APK

This repository's CI builds an installable APK automatically.

1. Open the **Actions** tab → **Build Loopline APK** → the latest run.
2. Download the `loopline-apks` artifact, **or** grab `loopline-debug.apk`
   from the auto-created **Release** (`build-<n>`).
3. On your phone enable *Install unknown apps* for your browser/files app and
   open the APK to install. Requires Android 8.0 (Oreo) or newer.

`loopline-debug.apk` is signed with the standard Android debug key, so it
installs directly on any device — no developer account needed.

## Building locally

Requires the Android SDK (platform 35, build-tools 35). With `ANDROID_HOME` set:

```bash
./gradlew assembleDebug      # -> app/build/outputs/apk/debug/app-debug.apk
```

> Note: CI is used to produce the APK because the development sandbox's network
> policy blocks Google's Maven/SDK servers (`dl.google.com`). GitHub-hosted
> runners have the Android SDK and full network access, so the build runs there.

## How to play

1. Tap **PLAY**.
2. Drag your finger from any dot to trace one continuous line.
3. Pass through every dot exactly once; the line cannot cross itself.
4. Drag back over the previous dot to undo, or use the on-screen controls.
5. Complete the shape to win — finish quickly for 3 stars!
