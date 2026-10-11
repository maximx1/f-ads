# Fuck Ads

FreeCell with no ads. Ever. Free, MIT, and not for sale.

The Android FreeCell I like best is crammed with ads and only lets you remove them with a subscription. So this is a
free, ad-free replacement that plays just as well. Android first; iOS, desktop and web maybe later.

## Status

**Rules engine done, no playable game yet.** `core` knows the whole game (numbered deals, legal moves, winning,
saved games) and is fully tested, but the app is still a blank screen. The playable board is next: see
[docs/PLAN.md](docs/PLAN.md). The stack is Kotlin + Jetpack Compose, laid out as a Kotlin
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
| `./gradlew check` | Formatting (Spotless + ktlint), detekt, Android lint, tests, and the coverage gate on `core` |
| `./gradlew spotlessApply` | Fix formatting |
| `./gradlew :androidApp:installDebug` | Build and install on the connected phone or running emulator |

Layout: `core/` is the pure-Kotlin rules engine (no UI, no Android APIs), `androidApp/` is the Compose app. All
versions live in `gradle/libs.versions.toml`.

The original 2021 web prototype (Excalibur.js + webpack) is not in this tree. It is preserved untouched at the git
tag [`legacy-web-excalibur-2021`](../../tree/legacy-web-excalibur-2021).

## License

[MIT](LICENSE)
