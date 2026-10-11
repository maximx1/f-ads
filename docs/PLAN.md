# Fuck Ads: plan

*Updated 2026-10-10 after the owner's decisions. Background research (audit of the 2021 prototype, full stack
comparison, verified 2026 platform facts) is in [ASSESSMENT.md](ASSESSMENT.md). Feature work has not started.*

## Decisions

| # | Decision |
|---|---|
| 1 | **Stack:** Kotlin + Jetpack Compose, laid out as a Kotlin Multiplatform (KMP) project so iOS, desktop and web can follow without a rewrite of the rules engine. |
| 2 | **Repo:** public, MIT. |
| 3 | **Not for sale, ever (for now).** No billing, no price, no ads, no tracking, no accounts. Install by sideloading. Play Store publishing is optional and parked. |
| 4 | **Numbered deals: yes.** Every game is a number. Same number, same deal, on every platform. |
| 5 | **Name and branding:** "Fuck Ads", unapologetically, everywhere including the store listing if there ever is one. |
| 6 | **Legacy code:** lives only at git tag `legacy-web-excalibur-2021` (pushed). Not in the working tree. |
| 7 | **Placeholder art:** Unicode suit symbols (♠ ♥ ♦ ♣) and text ranks (A 2-10 J Q K) until the owner draws the real designs. |

## What this means for the earlier plan

- **Dropped:** Play Billing, price, entitlement code, Data safety/privacy-policy polish, licensing. The 12-testers-for-14-days
  rule only applies to publishing on Google Play, so it is parked.
- **New requirement:** getting onto your own phone reliably. See "Sideloading" below.
- **Simplified:** no store-ready art or name needed. The app's only user is you and anyone you hand an APK to.

## Sideloading and Google's developer verification

*Checked against Google's own pages on 2026-10-10.
Sources: [overview](https://developer.android.com/developer-verification),
[FAQ](https://developer.android.com/developer-verification/guides/faq).*

- Google is requiring developer verification for apps installed on certified Android devices. Enforcement began
  **2026-09-30 in Brazil, Indonesia, Singapore and Thailand**, with global expansion in **2027**. Exact US dates are not
  announced.
- **Installing from Android Studio / `adb` does not need verification.** The FAQ states apps installed using ADB won't
  require it. That covers all development builds on your own phone, today and after 2027.
- **Handing an APK to friends** (tap a file, no adb) is what verification affects. The free route is a
  **Limited Distribution account**: no fee, no government ID, only an email, up to **20 registered devices**. (The $25
  Full Distribution account is only needed for unlimited installs or Play.)
- The "advanced flow" lets power users install unverified apps anyway (developer mode, confirm, restart, one-day wait,
  then allow for 7 days or indefinitely). It's the fallback if you skip registration.
- **Practical answer for now:** install tomorrow's Android Studio, build, and run on your phone over adb. Nothing to
  register. Create a Limited Distribution account only when you want to give the APK to other people (2027 at the latest
  for the US).
- Not confirmed from Google's pages: how devices are identified for the 20-device list, and how a verified app is tied
  to its signing key. Read the guides when it's time.

## Numbered deals (design)

- A **deal number** is a non-negative integer. `Deal.fromNumber(n)` always yields the same 52-card layout, forever, on
  every platform (Android, iOS, desktop, JVM tests).
- Use our own simple, fully specified PRNG, e.g. SplitMix64 or xorshift, seeded with the deal number, driving a
  Fisher-Yates shuffle of a fixed canonical deck order. Implement with explicit integer math (no `kotlin.random.Random`,
  whose algorithm isn't guaranteed stable across versions or platforms).
- "New game" picks a random number; the UI always shows it, with "enter a number" and "replay this deal".
- Fixture tests lock the layouts for deal numbers 1, 2 and a few big ones, so any future change to the algorithm is a
  deliberate, visible break.
- **Not** Microsoft-compatible, by choice. If you ever want classic "#11982" compatibility, it is a second deal
  generator behind a setting, because the algorithm is public. Cheap to add later, so no decision needed now.
- The number space can be anything: 0..2^31 or larger. Pick a display range later (e.g. 1 to 1,000,000).

## Placeholder card art

Cards are drawn in code: a rounded rectangle, a text rank, and a suit glyph (♠ ♥ ♦ ♣, red for hearts and diamonds,
dark for spades and clubs), plus a plain patterned back. The renderer takes a `CardStyle` so your real designs drop
in later without touching the rules or layout. The old SVG suit icons are in the legacy tag if you ever want them.
Use a bundled font so glyphs look the same on every device.

## Target architecture

```
f-ads/
├─ settings.gradle.kts
├─ gradle/libs.versions.toml     one place for all versions
├─ core/                         KMP library, pure Kotlin, no UI or platform code
│   ├─ model/                    Suit, Rank, Card
│   ├─ state/                    GameState (immutable: 8 columns, 4 free cells, 4 foundations), serializable
│   ├─ rules/                    canMove, legalMoves, max movable run
│   ├─ engine/                   apply(state, move) -> state, isWon
│   ├─ deal/                     Deal.fromNumber(n)
│   └─ commonTest/
├─ androidApp/                   Compose UI, ViewModel (state + undo history), persistence
├─ docs/
└─ .github/workflows/ci.yml
```

Rules of the design: the UI only renders `GameState` and sends `Move`s; `core` never knows about the UI; undo is a
list of previous states; a game is fully described by (deal number + move list), which gives save/resume, replay and
future hints for free.

## Phases

Effort unit: one session is about 3 hours.

### Phase 0: You (no code)
- Install Android Studio (it bundles the JDK). Enable developer mode and USB debugging on your phone.
- Nothing else is required: no Play account, no fee, no identity check, to run on your own phone.

### Phase 1: Repo reset (done on branch `cleanup/repo-reset`, awaiting your merge)
- Legacy code preserved at tag `legacy-web-excalibur-2021` and pushed. Working tree contains only README, LICENSE,
  docs, `.gitignore`, `.editorconfig`.
- Still to do on GitHub when you're ready: merge this branch to `main`, make `main` the default branch, delete
  `develop`/`3-card-styling` if you like (the tag keeps the code), and close the 12 stale 2021 issues (most are
  Excalibur-specific; #24-#26 are superseded by this plan).

### Phase 2: Kotlin skeleton and CI (2-3 sessions)
1. Create the KMP project: modules `:core` (targets Android + JVM) and `:androidApp`. Leave iOS out for now (it needs
   Xcode and slows CI) but keep `core` free of Android and JVM-only APIs so adding it later is mechanical.
2. `compileSdk` and `targetSdk` 36. minSdk 26 (your Pixel 9 Pro is the main test device).
3. Spotless + ktlint, detekt, Kover coverage, all in `./gradlew check`.
4. GitHub Actions CI: `./gradlew check :androidApp:assembleDebug` on every PR and on `main`.
5. A blank Compose screen running on your phone and a tablet/foldable emulator (Android 16 removes orientation locks on
   large screens, so landscape and tablets must work from the start).

### Phase 3: Rules engine in `:core` (4-6 sessions)
Immutable `GameState`; `Move` types (column to column with count, to free cell, to foundation, free cell to column or
foundation); `canMove`, `apply`, `isWon`; max movable run = (free cells + 1) x 2^(empty columns, not counting a
destination that is empty); `Deal.fromNumber`; `kotlinx.serialization` round-trip. Tests: truth tables for colour and
rank adjacency (ported ideas from the old validator tests), deal fixtures, property tests (random legal playouts never
lose or duplicate a card), and a scripted full win. Exit: 90%+ coverage of `core`.

### Phase 4: Playable spike (3-4 sessions)
Responsive board in Compose with placeholder art; drag a card or valid run to a legal target with snap-back if illegal;
tap-to-move as an alternative; save/restore on process death; deal number shown with "new game / enter number". Test on
your real phone.

### Phase 5: Ready for features
Green CI on `main`, a rules engine you can trust, a playable build on your phone, short decision notes in `docs/`.
Features (undo UI, auto-move to foundation, hints, stats, the real card designs, animations, win celebration, settings,
iOS target) begin here.

**Effort before features: about 9-13 sessions.** No external waits, since the Play closed-test requirement no longer
applies.

### Parked, not planned
Google Play listing (needs the $25 account plus 12 testers for 14 days); iOS (Xcode, free on-device install with
limits, or $99/year for the App Store/TestFlight); desktop and web targets; Microsoft-compatible deals.

## Suggested GitHub milestones

| Milestone | Issues |
|---|---|
| **M1 Repo reset** | Merge `cleanup/repo-reset`, `main` as default, close stale issues |
| **M2 Skeleton and CI** | KMP project; lint/format/coverage; CI; blank screen on phone and tablet emulator |
| **M3 Core engine** | Card model; GameState + serialization; Deal.fromNumber with fixtures; moves; run-size rule; win detection; property tests; scripted win |
| **M4 Playable spike** | Responsive layout; placeholder card drawing; drag and drop; tap-to-move; save/restore; deal-number UI |
| **Later** | Features list above |

## Remaining open questions

1. ~~Phone~~ Answered: Pixel 9 Pro (shipped with Android 15, can run 16). It is the primary test device, so no
   need to support old phones: minSdk 26 is a safe floor, higher is fine. It is a large-screen foldable-class
   display, so also test landscape and a tablet emulator.
2. Applicationid: e.g. `com.maximx1.fads`. Permanent once published, so worth a quick decision, though it doesn't
   matter while sideloading.
3. Rules scope: standard 4 free cells only, for now? (Recommended: yes.)
4. Gameplay preferences for the feature phase: unlimited undo? auto-move to foundation (always / safe only / off)?
5. When you design the suits, do you want them as vector files (SVG) or something else? The renderer is built to accept
   either.
