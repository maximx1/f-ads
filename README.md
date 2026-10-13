# Fuck Ads

FreeCell with no ads. Ever. Free, MIT, and not for sale.

The Android FreeCell I like best is crammed with ads and only lets you remove them with a subscription. So this is a
free, ad-free replacement that plays just as well. Android first; iOS, desktop and web maybe later.

## Status

**Playable.** Drag or tap cards, undo, restart, deal at random or by number, and the game is saved as you go. A
clock times each game from the first move to the win, and can be paused. The cards are plain placeholders and there are few extras yet (no
auto-move to foundation, hints, stats or animations beyond cards sliding). What comes next is in [docs/PLAN.md](docs/PLAN.md). The stack is Kotlin + Jetpack Compose, laid out as a Kotlin
Multiplatform project. Every deal is a number: same number, same deal.

## Build

Open the folder in Android Studio and press Run. Nothing else to install: Android Studio brings the JDK and the
Android SDK.

From a terminal, point `JAVA_HOME` at Android Studio's JDK first (macOS path shown):

```bash
export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"
```

| Command | What it does |
|---|---|
| `./gradlew check` | Formatting (Spotless + ktlint), detekt, Android lint, all tests (rules and UI), and the coverage gate on `core` |
| `./gradlew spotlessApply` | Fix formatting |
| `./gradlew :androidApp:installDebug` | Build and install on the connected phone or running emulator |

Layout: `core/` is the pure-Kotlin rules engine (no UI, no Android APIs), `androidApp/` is the Compose app. All
versions live in `gradle/libs.versions.toml`.

The original 2021 web prototype (Excalibur.js + webpack) is not in this tree. It is preserved untouched at the git
tag [`legacy-web-excalibur-2021`](../../tree/legacy-web-excalibur-2021).

## License

[MIT](LICENSE)
