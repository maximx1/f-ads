# Fuck Ads

FreeCell with no ads. Ever. Free, MIT, and not for sale.

The Android FreeCell I like best is crammed with ads and only lets you remove them with a subscription. So this is a
free, ad-free replacement that plays just as well. Android first; iOS, desktop and web maybe later.

## Status

**Playable.** Drag or tap cards, take cards back off the foundations, undo, restart, deal at random or by number, and
the game is saved as you go. A clock times each game from the first move to the win and can be paused, a counter keeps
your moves, and your best times are kept. A hint points out a move worth trying, and cards that cannot move right now
are dimmed unless you switch that off. There are five themes to pick from in the pause dialog: flat, Windows Vista,
cyberpunk, brushed metal and a smoky back room. It runs on Android and, from the same code, as a desktop app for macOS,
Windows and Linux. The cards are plain placeholders and there are few extras yet (no auto-move to foundation, stats or
animations beyond cards sliding). What comes next is in [docs/PLAN.md](docs/PLAN.md). The stack is Kotlin + Compose
Multiplatform, laid out as a Kotlin Multiplatform project. Every deal is a number: same number, same deal.

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
| `./gradlew :desktopApp:run` | Run the desktop app |
| `./gradlew :desktopApp:packageDistributionForCurrentOS` | Build an installer for the machine you are on: a `.dmg` on macOS, an `.msi` on Windows, a `.deb` on Linux. It lands in `desktopApp/build/compose/binaries/main/` |

Layout: `core/` is the pure-Kotlin rules engine (no UI, no platform APIs). `ui/` is the whole game screen in Compose
Multiplatform, shared by both apps. `androidApp/` and `desktopApp/` are thin shells round it: each starts the game and
says where its files are kept. All versions live in `gradle/libs.versions.toml`.

Building an installer needs `jpackage`, which a full JDK has but the one inside Android Studio does not. Point
`JAVA_HOME` at a full JDK 21 or newer for that one command. A macOS installer can only be built on a Mac, a Windows one
on Windows, and a Linux one on Linux. The desktop app keeps its saved game in the usual place for each system, for
example `~/Library/Application Support/Fuck Ads` on macOS.

The original 2021 web prototype (Excalibur.js + webpack) is not in this tree. It is preserved untouched at the git
tag [`legacy-web-excalibur-2021`](../../tree/legacy-web-excalibur-2021).

## License

[MIT](LICENSE)
