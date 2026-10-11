# f-ads: assessment and get-on-track plan

*Prepared 2026-10-10. Repo: `maximx1/f-ads` (public), checked out at `3-card-styling` @ `6b6e2dd` (2021-09-18). Nothing in the repo was modified; all build/test runs were done in a scratch copy.*

---

## Executive summary

**Current state**
- It is an early prototype, about 10% of a game. There are 465 lines of JS: a card/stack data model, a deal into 8 columns, a few adjacency/colour helpers, and a static Excalibur.js (v0.24.5) render of the board. It is **not Pixi**. There is no input, no move rules, no foundation logic, no win detection and no undo. Every card shows the ace-of-spades sprite, the deal is never shuffled, and the free cell/foundation row is drawn half off-screen.
- The tests pass: **78 passing on Node 22.15.0**, 55.7% statement coverage (100% on the pure-logic modules, 0% on rendering). The build is broken on modern Node (`ERR_OSSL_EVP_UNSUPPORTED` from webpack 5.37). It works with `NODE_OPTIONS=--openssl-legacy-provider`. `build:prod` points to a config file that doesn't exist. `npm audit` reports 70 vulnerabilities (7 critical, 42 high), all in the dev toolchain. CI targets Node 15 and has no retained runs.
- The logic is *mostly* separate from rendering: no domain file imports Excalibur. But the domain objects hold back-references to renderer actors (`card.actors`, `freeCell.emptyCardSlotActor`, `Foundation.getActiveActors()`), so the separation isn't clean.
- **The ideas and test cases are the valuable part, not the code.** None of the code is hard to rewrite. The SVG art is a start: 4 aces, a card back and 4 suit icons.

**Recommendation.** Rebuild as a **native Android app in Kotlin + Jetpack Compose**, laid out as a **Kotlin Multiplatform project from day one**. The rules engine goes in a pure-Kotlin `:core` module. That gives the best Android result, with first-party Play Billing and no plugin lag behind Google's 2-year billing deprecation cycle. It also leaves a stable route to iOS through Compose Multiplatform. Desktop and web can come later from the same code (desktop is stable, web is Beta). Flutter is a close second, and the better pick if iOS or web quality soon matters as much as Android. Capacitor is the fallback if you strongly want to stay in TypeScript.

**First 5 actions**
1. Answer the blocking open questions (§7, especially Q1–Q4: Kotlin comfort, repo visibility and licence, paid-upfront vs. free+unlock, MS deal compatibility).
2. Register the Google Play developer account now ($25, identity verification). Start lining up **15–20 closed-test testers**: new personal accounts need 12 opted-in testers for 14 continuous days before production access. This is the longest wall-clock item.
3. Repo hygiene: fast-forward `develop` to `3-card-styling`, tag `legacy-web-excalibur-2021`, make `main` the single default branch, and close or retitle stale issues (§5.1).
4. Bootstrap the Gradle KMP skeleton (`:core` + `:androidApp`) with CI running `./gradlew check` on every PR (§5.3).
5. Port the domain model and existing test cases into `:core` with an immutable state + move design. Add Microsoft-compatible numbered deals (§5.4).

Roughly **12–17 working sessions** (about 3 h each) to reach "ready for feature work". On top of that comes a **≥14-day wall-clock wait** for closed testing, which can overlap with feature work.

---

## 1. Current-state audit

### 1.1 What exists

| Area | Files | State |
|---|---|---|
| Data structures | `app/core/Structures.js` (`Stack`: push/pop/take/peek/peekMany/peekAll/clone) | Solid, 100% covered. Silently drops null/undefined items. |
| Card model | `app/core/entities/card/Card.js` | `Card(number 1–13, suit 1–4)`. Suits: SPADES=1, HEARTS=2, CLUBS=3, DIAMONDS=4, so colour = parity. Also holds `actors` (render back-reference). |
| Rules | `app/core/rules/CardPlacementValidator.js` | Generic helpers: adjacent rank (with optional K–A wrap), alternating/same colour, `isCardPlaceableOnOtherCard`. 100% covered. |
| FreeCell rules | `app/freecell/rules/FreecellCardPlacementValidator.js` | **Empty class, and broken**: it `extends CardPlacementValidator` without importing it, so it throws `ReferenceError` if anything ever imports it. Nothing does. |
| Entities | `Foundation.js` (pile only; add/remove are TODO), `FreeCell.js` (place/pick up) | `Foundation.getActiveActors()` returns *cards*, not actors, when the pile is non-empty (latent bug, marked TODO). |
| Dealing | `DealerBase.js` (fresh "kissing king" deck, Fisher–Yates shuffle with `Math.random`), `FreecellDealer.js` (round-robin into 8 columns) | Works. Not seedable, so no numbered deals, no replay, and shuffle tests can't be deterministic. Not Microsoft-deal compatible. |
| Layout | `FreecellChoreographer.js` | Unit-based responsive layout math (49 units wide, card 5×6 units, 1-unit gutter, max 700 px). It mutates actor positions directly. Positions assume a top-left anchor, but Excalibur actors are centre-anchored, which is why the top row is clipped. Untested. |
| Rendering | `Game.js`, `FreecellGame.js`, `CardActor.js`, `EmptyCardSlotActor.js`, `ResourceManager.js`, `assets/CardTextures.js` | Static board only. `startNewGame()` never calls `shuffleCards()`. Every card uses `resources.aceOfSpades`. Resources reach actors by mutating properties (`choreographer.resources = …`, `actor.resources = …`). |
| Assets | `app/core/assets/*.svg` (4 aces + back), `design/card assets/*.svg` (card base + 4 suit icons) | Inkscape SVGs, 2in×3in viewBox, no gradients or filters. **The aces use `<text>` elements**, which Android VectorDrawable import drops, so ranks need a font-based approach. |
| Misc | `index.js` | Actually an HTML file (`<title>Fuck Ads</title>`), unused because HtmlWebpackPlugin generates `index.html` with the title "Excalibur Webpack Sample". **The repo is public**, so this title is public too. |

**Verified by running it.** I built the copy with the legacy-OpenSSL flag, served `dist/` and clicked "Play game" in a browser. The result was 8 columns of unshuffled cards, all showing the ace of spades, plus a row of orange placeholder rectangles half off the top of the screen. There is no interaction.

### 1.2 Build, test and dependency health (scratch copy, Node v22.15.0, npm 10.9.2)

| Check | Result |
|---|---|
| `npm ci` | OK (lockfile v2, 879 packages). Deprecation warnings: chokidar 2, fsevents 1, urix, resolve-url. |
| `npm test` (nyc + mocha + @babel/register) | **78 passing**, 20 ms. Coverage: Stmts 55.73%, Branch 96.55%, Funcs 48.61%, Lines 53.22%. |
| `npm run build:dev` | **Fails**: `ERR_OSSL_EVP_UNSUPPORTED` (webpack 5.37.1 uses md4, which Node ≥17 blocks). **Succeeds** with `NODE_OPTIONS=--openssl-legacy-provider`: 892 KiB of JS (827 KiB of it Excalibur). |
| `npm run dev` | Works with the same flag (webpack-dev-server 3, port 3000). |
| `npm run build:prod` | **Fails**: `webpack.production.js` doesn't exist. |
| Undeclared or phantom deps | `webpack-merge` is used but not declared; it only resolves because webpack-cli pulls it in. `ts-loader` is referenced in the config but not installed (harmless, since there are no `.ts` files). `devServer.contentBase` is a WDS3-only option. |
| `npm audit` | **70 vulnerabilities: 7 critical, 42 high, 15 moderate, 6 low.** Criticals: `webpack` (direct), `@babel/traverse`, `eventsource`, `minimist`, `proxy-addr`, `url-parse`, `websocket-driver`. All are dev/build-time, nothing ships to users, but the toolchain is past saving. |
| `npm outdated` (current → latest) | excalibur 0.24.5 → 0.32.0 (many breaking API changes), mocha 8 → 12, nyc 15 → 18, chai 4 → 6, webpack-cli 4 → 7, webpack-dev-server 3 → 6, webpack 5.37 → 5.111, Babel 7.14 → 7.29 (runtime 8.x available). |
| CI (`.github/workflows/node.js.yml`) | Node 15.x (EOL), `actions/checkout@v2` and `setup-node@v2` (deprecated runtimes), runs only on `develop`. `gh run list` returns no retained runs, so I couldn't confirm whether it would still pass. |

### 1.3 Git and project state

- **`main`** (`4e39932`) is a strict ancestor of `develop`: 25 commits behind, fast-forwardable. It hasn't been used since PRs started.
- **`develop`** (`996e86c`, 2021-09-11) is the GitHub default branch. All PRs (#12–#31) merged here.
- **`3-card-styling`** (`6b6e2dd`, 2021-09-18) is 4 commits ahead of `develop` and fast-forwardable. It has no open PR and is pushed to origin. It contains the SVG assets and texture loading.
- Issues: 12 open, all from 2021. #24 desktop, #25 android, #26 ipad, #21 README, #17 "pit testing?" and #2/#3/#4/#6/#8/#9/#10/#11/#18 are Excalibur-specific UI tasks.
- Licence: MIT, in a public repo. This matters if the app will be sold (see Q2).

### 1.4 Code quality and architecture

**Good, worth keeping as ideas:**
- Small, readable classes with real unit tests and coverage discipline. The issue-per-branch workflow with `[#n]` commit prefixes is good habit and worth keeping.
- Domain modules have no engine imports, so the separation instinct was right.
- Unit-based responsive layout (`FreecellChoreographer`) is the right approach for phones and tablets.
- The validator truth tables (colour/rank adjacency) and the exact expected deal layout are good test fixtures to port.
- The suit-icon SVGs.

**Weak, don't carry forward:**
- Mutable entities with back-references to renderer objects (`card.actors`, `emptyCardSlotActor`). That two-way coupling makes undo, save/restore, solvers and cross-platform reuse painful. The new design should use **immutable state + moves**, and the UI derives everything from state.
- Inheritance-heavy design (`DealerBase` → `FreecellDealer`, `CardPlacementValidator` → empty subclass) built for "maybe other solitaires later". YAGNI: build FreeCell first.
- Unseeded randomness, magic numbers for suits, properties injected after construction, and an Excalibur API that has since been rewritten (0.24 → 0.32).
- Webpack + Babel + mocha + nyc for something this small. Even a web restart would use Vite + Vitest + TypeScript.

**Verdict: rewrite, don't modernize.** Modernizing the JS toolchain and porting Excalibur 0.24 → 0.32 would cost more than rewriting 465 lines. Keep the test cases, the layout approach, the suit art and the git history (via a tag).

---

## 2. Facts checked for 2026 (with sources)

| Topic | Finding | Confidence |
|---|---|---|
| Target API level | From **2026-08-31**, new apps and updates must target **Android 16 (API 36)**. Extensions are available to 2026-11-01. Existing apps must target API 35 to stay visible to new users on newer devices. API-36 apps on ≥600 dp screens can no longer lock orientation or aspect ratio, and the edge-to-edge opt-out is gone. ([Android Developers: target SDK](https://developer.android.com/google/play/requirements/target-sdk), [Play Console Help](https://support.google.com/googleplay/android-developer/answer/11926878)) | High (official page, updated 2026-10-01). Relevant: **the layout must work in landscape and on tablets/foldables.** |
| Play Billing Library | Latest is **9.1.0 (2026-06-18)**; 9.0.0 came out 2026-05-19. New apps and updates must use **PBL ≥ 8** from 2026-08-31 (extension to 2026-11-01). Each version has a ~2-year deprecation cycle. PBL 8 renamed in-app items to "one-time products" with multiple purchase options. Unacknowledged purchases are auto-refunded after 3 days. ([Release notes](https://developer.android.com/google/play/billing/release-notes), [Deprecation FAQ](https://developer.android.com/google/play/billing/deprecation-faq)) | High (official). The 3-day refund rule comes from a secondary source. |
| New personal account testing | Personal accounts created after 2023-11-13 must run a **closed test with ≥12 testers opted in for the last 14 continuous days** before they can apply for production access. ([Play Console Help 14151465](https://support.google.com/googleplay/android-developer/answer/14151465)) | High for the rule. Third-party claims (the clock resets if testers drop below 12, a 2026 "testers must actually use the app" check, organization accounts exempt) are **unverified**. ([example](https://www.testerscommunity.com/blog/google-play-closed-testing-requirements-2026)) |
| Developer fee and identity | **$25 one-time** registration. Android developer verification (identity required to install on certified devices, including sideloading) was scheduled to start enforcing 2026-09-30 in BR/ID/SG/TH, then expand globally in 2027. Play Console developers verify through Play Console. ([Android developer verification](https://developer.android.com/developer-verification/guides/early-access), [Help](https://support.google.com/android-developer-console/answer/16561738)) | Fee: high. Whether enforcement actually started on 2026-09-30: **unverified**. |
| Play service fee | US/UK/EEA changed 2026-06-30 to **10% service + 5% billing fee** on the first $1M for new installs. Other markets keep the 15% tier until the rollout. ([Play Console Help 112622](https://support.google.com/googleplay/android-developer/answer/112622)) | Medium-high. In practice it's still about 15%, and you must enroll in the 15% tier. |
| Apple (for later) | Developer Program **$99/year**. Small Business Program is 15%. | Medium (secondary sources only). |
| Compose Multiplatform | iOS **stable since 1.8.0 (May 2025)**; **1.12.0** released Aug 2026; web (Wasm) **Beta** since 1.9.0; desktop (JVM) stable. ([JetBrains 1.8.0](https://blog.jetbrains.com/kotlin/2025/05/compose-multiplatform-1-8-0-released-compose-multiplatform-for-ios-is-stable-and-production-ready/), [1.12.0](https://blog.jetbrains.com/kotlin/2026/08/compose-multiplatform-1-12-0/), [1.9.0 web Beta](https://blog.jetbrains.com/kotlin/2025/09/compose-multiplatform-1-9-0-compose-for-web-beta/)) | High. |
| Flutter / Flame | Flutter 3.44 confirmed (May 2026); 3.47/3.50 scheduled for Aug/Nov 2026. Flame ~1.38. Official `in_app_purchase` ~3.2.x. ([Flutter archive](https://docs.flutter.dev/install/archive)) | Medium: I couldn't confirm the exact current versions. |
| Capacitor | Capacitor 8 (Dec 2025): minSdk 23, targetSdk 36, edge-to-edge. IAP plugins: RevenueCat Capacitor SDK (managed backend), Capawesome Purchases (PBL 8, **paid/Insiders**), Capgo native-purchases (README says PBL 7.x, so stale). RevenueCat's Cordova SDK is deprecated and stuck at PBL 7. ([Capawesome](https://capawesome.io/plugins/purchases), [RevenueCat Capacitor](https://www.revenuecat.com/docs/getting-started/installation/capacitor)) | Medium. This shows the plugin-lag risk for wrapped stacks. |
| React Native / Expo | Expo SDK 56 (May 2026, RN 0.85). `react-native-iap` repo **archived 2026-08-04** in favour of `expo-iap`. ([Expo SDK 56](https://expo.dev/sdk/56)) | Medium-high. |
| Godot | Official `GodotGooglePlayBilling` supports Godot 4.2+, but I **couldn't confirm** which PBL version it uses now. A community plugin (AndroidIAPP) claims Godot 4.6 + PBL 8.3. ([godot-google-play-billing](https://github.com/godot-sdk-integrations/godot-google-play-billing)) | Low-medium. |

Not verified at all: exact current Kotlin/AGP/Gradle versions (use whatever Android Studio's new-project wizard generates), Unity's current licensing terms, and whether a paid-upfront app needs any extra licensing integration in 2026. Historically it doesn't; Play handles it, with Play Integrity optional.

---

## 3. Technology decision

### 3.1 What the requirements imply
- **Android first** means native feel: predictive back, edge-to-edge, tablets/foldables/landscape (now required by API 36), fast cold start, small download, and reliable resume after process death.
- **2D card game with drag and drop**: 52 cards plus about 16 slots, with tween animations (deal, auto-move, win cascade). Any of the candidates can handle this. It's not a high-performance rendering problem. A UI toolkit with good gesture and animation primitives is enough; a game engine is optional.
- **One-time purchase**: either a paid-upfront app (no billing code at all) or a free app with one non-consumable unlock (PBL ≥ 8, kept current every ~2 years).
- **Offline, saved games and stats**: local persistence (DataStore/SQLite). No backend needed. Android Auto Backup covers device moves.
- **Solo developer**: minimize the number of languages, tools and third-party plugins on the critical path.

### 3.2 Decision matrix (1–5, higher is better; weights reflect "Android first")

| Criterion (weight) | (a) Capacitor + TS/Vite + Pixi/DOM | (b) Kotlin + Compose (KMP) | (c) Flutter (+Flame) | (d) Godot 4 | (e) React Native / Expo | (f) Unity |
|---|---|---|---|---|---|---|
| Android quality / native integration (25%) | 3 | **5** | 4 | 3 | 3.5 | 3 |
| Play Billing robustness, no plugin lag (15%) | 3 | **5** (first-party) | 4 (official plugin) | 2.5 | 3.5 | 4 |
| iOS-later path (15%) | 4 | 4 (CMP stable; StoreKit via interop/RevenueCat KMP) | **5** | 3.5 | **5** | 4.5 |
| Solo-dev productivity (15%) | **4** (if TS-fluent) | 3.5 (learning curve if new to Kotlin) | 4 | 3 (menus/settings UI awkward) | 3.5 | 2 |
| Reuse of existing code (5%) | **4** | 1 | 1 | 1 | 2 | 1 |
| Drag/animation smoothness (10%) | 3.5 (WebView on low-end devices) | 4.5 | 4.5 | 4.5 | 3.5 (needs Reanimated/Gesture Handler/Skia) | **5** |
| Long-term maintenance / churn (10%) | 3 (WebView + native shell + plugins) | **4.5** | 4 | 3 | 2.5 | 2 |
| Web/desktop later (5%) | **5** | 3 (desktop stable, web Beta) | 4 | 4 | 3 | 4 |
| **Weighted total** | 3.50 | **4.23** | 4.05 | 3.10 | 3.53 | 3.28 |

**Notes on each option**
- **(a) Capacitor.** Its only real advantage is code reuse, and there's very little worth reusing. You would still need a full TS/Vite rewrite. IAP depends on a third-party plugin keeping up with Google: the Cordova SDK is already stuck on PBL 7, one Capacitor plugin is paid and one is stale. Android feel (back handling, insets, WebView updates, cold start) takes extra effort. Choose it only if you want to stay in TypeScript and care about web nearly as much as Android.
- **(b) Kotlin + Compose (KMP).** The platform's own toolkit and first-party billing, with the smallest APK and best Android integration. `:core` in commonMain is plain Kotlin, tested on the JVM in milliseconds. For iOS later, Compose Multiplatform has been stable since May 2025, so most UI can be shared. Payments on iOS need a StoreKit bridge or RevenueCat's KMP SDK. Desktop (JVM) works; web is Beta. The cost is that everything gets rewritten, plus a Kotlin/Gradle learning curve if you're coming from JS.
- **(c) Flutter.** Very close second and the best "all platforms equally" option, with mature iOS and web. Plain Flutter widgets (`Draggable`, implicit animations) or a `CustomPainter` are enough; Flame is optional. Dart lock-in, larger APK, and one layer between you and Android APIs. **Pick Flutter instead if iOS is likely within about 6 months of the Android launch, or if web matters.**
- **(d) Godot.** Great at sprites and tweens, weaker for the menu/settings/stats screens most of this app is made of. Billing goes through plugins with unclear PBL currency. Not recommended.
- **(e) React Native / Expo.** JS-familiar, but the gesture/animation stack (Reanimated + Gesture Handler or Skia) adds complexity. The IAP ecosystem just churned (react-native-iap archived Aug 2026). The JS reuse argument is weak for the same reason as Capacitor.
- **(f) Unity: dismissed.** Large binaries, slow iteration, a heavy editor and license/pricing churn, all for a game that needs no physics or 3D. It's overkill.

### 3.3 Recommendation: Kotlin + Jetpack Compose, structured as Kotlin Multiplatform

- **Android (now):** `:androidApp` uses Compose with `compileSdk`/`targetSdk` 36 and minSdk 24–26. Persistence via DataStore (settings and the current game as serialized JSON) and optionally Room/SQLite for stats history. Billing via Play Billing Library 9.x (`billing-ktx`) only if you choose free + unlock.
- **iOS (later):** add an `iosApp` target. Move the Compose UI into a shared `composeApp` module using Compose Multiplatform (the API matches Jetpack Compose, so most UI ports directly). Add a StoreKit 2 wrapper or RevenueCat KMP for purchases. Budget roughly 1–2 weeks of sessions plus the $99/yr Apple account.
- **Desktop (optional):** Compose Desktop target from the same shared UI, packaged as DMG/MSI/DEB. Low cost once the UI is shared.
- **Web (optional, lowest priority):** Compose for Web (Wasm, Beta). Alternatively, compile `:core` to JS and put a small TS/Canvas UI on top.
- **Honest tradeoffs:**
  - If you aren't comfortable with Kotlin, the first 2–3 sessions will feel slow: Gradle, Android Studio, emulator.
  - Compose drag-and-drop for stacked cards is hand-rolled (`pointerInput` + `Animatable` offsets), not a ready-made widget.
  - iOS later is "mostly shared" rather than free.
  - If the answer to Q1 is "I don't want to learn Kotlin", switch to **Flutter**. The plan below applies almost unchanged: swap Gradle for pub, kotlin.test for `flutter test`, and so on.

---

## 4. Target architecture (light, enough to structure the cleanup)

```
f-ads/                         (rename repo later per Q5)
├─ settings.gradle.kts
├─ gradle/libs.versions.toml   version catalog, single source of versions
├─ core/                       KMP library: commonMain only, zero platform deps
│   ├─ model/                  Rank, Suit(color), Card (value class), Pile types
│   ├─ state/                  GameState (immutable: tableau[8], freeCells[4], foundations[4]), serializable
│   ├─ rules/                  canMove(state, move), legalMoves(state), maxMovableRun(state, target)
│   ├─ engine/                 apply(state, move) -> GameState; isWon; (later) safe auto-move
│   ├─ deal/                   Deal(seed): MS-compatible LCG deal + "random" = random seed
│   └─ commonTest/
├─ androidApp/                 Compose UI, ViewModel holding GameState + history, persistence, (billing)
├─ design/                     source SVGs (kept)
└─ .github/workflows/ci.yml
```

Key rules for this design:
- `:core` has no UI types and no callbacks into the UI. The UI renders `GameState` and sends `Move`s.
- Undo is just a list of previous states, or an inverse-move log.
- Every game is a seed. That gives numbered deals, "replay this deal", deterministic tests, and later a solver or hint feature, all for free.

---

## 5. Cleanup and get-on-track plan (ordered)

Effort unit: **session ≈ 3 focused hours**.

### Phase 0: Decisions (0.5 session)
- Answer Q1–Q4 in §7. Q1 (Kotlin vs. Flutter) and Q2 (repo visibility/licence) block Phase 1.
- Register the Play Console developer account and complete identity verification. Start recruiting 15–20 testers (friends, family, coworkers with Android phones; a Google Group makes opt-in easy). The 14-day clock can't start until a closed-test build exists (Phase 5), but recruiting takes time.

### Phase 1: Branch and repo hygiene (1 session)
1. **Preserve the legacy code.** Fast-forward `develop` to `3-card-styling` (or open and merge a PR for #3 to keep the issue trail). Tag the tip `legacy-web-excalibur-2021` and push the tag.
2. **Collapse branches to one trunk.** Fast-forward `main` to that tip. Set **`main` as the GitHub default branch**. Delete `develop` and `3-card-styling` locally and on origin (the tag keeps everything). From then on, use trunk-based development: short-lived `issue-N-slug` branches with a PR into `main`. Keep CI required on `main` with branch protection, even solo.
3. **Remove the legacy JS from the tree** in a single commit ("Remove 2021 Excalibur prototype; see tag legacy-web-excalibur-2021"). Move the SVGs you want into `design/legacy-assets/`. Delete `index.js`, the webpack/babel/mocha/nyc configs, `package*.json`, `app/` and `test/`. Keep the old test files as **reference for porting** by copying them to `docs/legacy-test-cases/`, or just rely on the tag. The tag alone is simpler.
4. **Housekeeping:**
   - Add a README with a one-paragraph vision, a build section and a status section (closes #21).
   - Update `.gitignore` for Gradle/Android Studio (`.gradle/`, `build/`, `local.properties`, `.idea/` mostly, `*.keystore`/`*.jks`).
   - Add `.editorconfig`.
   - Decide the licence and visibility per Q2. If the repo stays public, **never commit signing keys, `google-services.json` or Play API credentials**. Use GitHub Actions secrets.
5. **Triage issues.**
   - Close the Excalibur-specific ones (#2, #4, #6, #8, #9, #10, #11, #18) as "superseded by the Kotlin rewrite", linking the new milestone.
   - Close #24/#25/#26 (desktop/android/ipad) and replace them with milestone-level issues.
   - Answer #17: Pitest works on the JVM (Gradle `info.solidsoft.pitest`), but it isn't a priority. Close or relabel it as "later".
   - Create the new milestones and issues from §6.

### Phase 2: Toolchain bootstrap (2–3 sessions)
1. Install the current stable **Android Studio** (it bundles a JDK; the project needs JDK 17+) and create a KMP project with the Android Studio KMP wizard or kmp.jetbrains.com. Targets: Android and JVM. Leave iOS targets out for now: they need macOS/Xcode in CI and slow down builds, and are trivial to add later.
2. Modules: `:core` (KMP library) and `:androidApp` (Compose). Use a version catalog. Kotlin 2.x with the Compose compiler plugin. `compileSdk = targetSdk = 36`. minSdk 24 or 26 (Q8).
3. Code quality: **ktlint via Spotless** (formatting) and **detekt** (lint), both in `./gradlew check`. **Kover** for coverage on `:core`.
4. **CI** (`.github/workflows/ci.yml`): on push to `main` and every PR. Steps: `actions/checkout@v4+`, `actions/setup-java` (Temurin 17/21), `gradle/actions/setup-gradle`, then `./gradlew check :androidApp:assembleDebug`. Upload the Kover HTML report and debug APK as artifacts. Delete the old Node workflow.
5. "Hello table" Compose screen running on a **real, preferably low-end, Android phone** and on a tablet/foldable emulator. API 36 removes orientation locks on large screens, so check the layout from day one.

### Phase 3: Port `:core` with tests (4–6 sessions)
Port the *behaviour*, not the classes:

| Legacy | New `:core` | Tests to carry over / add |
|---|---|---|
| `Card`, `CardSuit` (numeric, parity colour) | `enum Suit(color)`, `enum Rank`, `Card` value class (0–51 index) | Validity, colour mapping (port Card.spec) |
| `Stack` | Kotlin `List` (immutable). No custom stack class needed. | Drop Structures.spec. It tested a container Kotlin already provides. |
| `CardPlacementValidator` | `rules/TableauRules.canStack(top, moving)` (descending, alternating colour) and `FoundationRules.canPlace(foundation, card)` (same suit, ascending, ace on empty) | Port the alternating-colour truth tables. **Drop K–A wrap and "same colour" variants** (not FreeCell). |
| `FreeCell`, `Foundation`, `Field` | `GameState(tableau: List<List<Card>>, freeCells: List<Card?>, foundations: Map<Suit, Rank?> or List<List<Card>>)` | Place/pick-up semantics as move tests |
| `DealerBase` / `FreecellDealer` | `deal/Deal.fromSeed(n)`: Microsoft-compatible deal (the well-known LCG `state = state*214013 + 2531011 mod 2^31; rand = state >> 16` over deck order AC, AD, AH, AS, 2C…, dealing round-robin). Random game = random seed in 1..N. | Golden fixtures for known deals (e.g. #1 and #617 layouts, both widely published); "52 unique cards" invariant; same seed gives same layout. Optionally keep the old unshuffled "kissing king" layout test as a sanity fixture. |
| *(missing)* | `Move` sealed type (Tableau→Tableau with count, →FreeCell, →Foundation, FreeCell→…), `canMove`, `apply`, `isWon`, max movable run = (freeFreeCells + 1) × 2^(emptyColumns, excluding the target if empty) | Unit tests per rule; **property tests** (kotest-property or a simple seeded loop): random legal-move playouts never lose or duplicate cards, and `apply` never produces an illegal state |
| *(missing)* | `kotlinx.serialization` for `GameState` + seed + move history | Round-trip test, which is the basis for save/resume |

Exit criteria: `:core` has 90%+ line coverage. A JVM test can "play" a known deal to a win from a scripted move list.

### Phase 4: Rendering spike (3–4 sessions; deliberately throwaway-tolerant)
- Draw a state-driven board in Compose with the responsive layout. Reuse the old idea: unit grid, card aspect ratio, a gutter, and column overlap that compresses as columns grow. Support portrait, landscape and tablet.
- **Card faces:** generate them procedurally rather than drawing 52 SVGs: a rounded rect, rank text in a bundled font, and suit pip paths from `design/card assets/*_icon.svg`, imported as `ImageVector`/VectorDrawable. Note that `<text>` in the old ace SVGs won't survive VectorDrawable import, which is another reason to go procedural. Card back from `cardback.svg`.
- Prototype **one** interaction end-to-end: drag a card or valid run from a column, drop it on a legal target, and animate a snap-back when illegal. Also tap-to-move as an accessibility fallback. Measure on the low-end phone. This de-risks the main UX question before you invest in features.
- Survive process death: save on `onStop` and restore the game.

### Phase 5: Release plumbing (1–2 sessions + ≥14 days wall clock, overlapping feature work)
- Choose the app name and `applicationId` (it's permanent; see Q5). Generate the upload keystore and enroll in **Play App Signing**. Store the keystore outside the repo, backed up.
- Add a CI job that builds a signed **AAB** on tags. Optionally upload to the **internal testing** track via Gradle Play Publisher or fastlane `supply` using a Play service account secret.
- Write a privacy policy page (needed even with no data collection; a GitHub Pages page is fine). Fill in the Data safety form ("no data collected" if true). Content rating questionnaire. Store listing drafts.
- Push the skeleton game to **closed testing** as soon as it is minimally playable, and start the **12 testers × 14 days** clock. Leave it running while you build features.
- Monetization plumbing (only if free + unlock is chosen, Q3): one non-consumable one-time product. Wrap PBL 9 behind a small `Entitlements` interface so the iOS StoreKit version can slot in later. Acknowledge purchases, and test with license testers. If you choose **paid upfront**, skip billing entirely.

### Phase 6: Ready for feature work
At this point you have:
- a green CI on `main`
- `:core` with full FreeCell rules, numbered deals and serialization, well tested
- a playable but plain Android build on a closed-test track
- documented decisions (`docs/decisions/` ADRs, one paragraph each: stack choice, deal algorithm, monetization)

Features (undo UI, auto-move, hints/solver, stats, themes, animations polish, win celebration, settings) start here.

**Effort total before features:** about 12–17 sessions, plus the 14-day closed-test wait running in parallel.

---

## 6. Suggested GitHub milestones and issues

| Milestone | Issues (one PR each, roughly) | Est. |
|---|---|---|
| **M0 · Decisions & accounts** | Record answers to open questions (ADR-001 stack, ADR-002 monetization, ADR-003 deal numbering) · Register Play developer account + identity verification · Recruit closed-test testers (target 15–20) | 0.5 |
| **M1 · Repo reset** | FF develop ← 3-card-styling and tag `legacy-web-excalibur-2021` · Make `main` default, delete `develop`/`3-card-styling`, add branch protection · Remove legacy JS tree; move SVGs to `design/` · README (closes #21) + .gitignore + .editorconfig · Licence/visibility decision applied · Triage and close stale issues | 1 |
| **M2 · Kotlin skeleton & CI** | KMP project (`:core`, `:androidApp`), version catalog · Spotless/ktlint + detekt + Kover · GitHub Actions CI (check + assembleDebug + artifacts) · Hello-table screen on device + tablet emulator | 2–3 |
| **M3 · Core rules engine** | Card/Suit/Rank model · GameState + serialization · MS-compatible seeded deal with golden fixtures · Move types + `canMove`/`apply` (tableau, free cell, foundation) · Supermove capacity rule · Win detection · Property tests (card conservation, legal-state invariant) · Scripted win test | 4–6 |
| **M4 · Board rendering spike** | Responsive layout (portrait/landscape/tablet) · Procedural card faces + back from SVG icons · Drag-and-drop one card/run with legal-drop + snap-back · Tap-to-move fallback · Save/restore on process death | 3–4 |
| **M5 · Release plumbing** | applicationId + name + icon placeholder · Upload key + Play App Signing · Signed AAB CI job (+ optional internal-track upload) · Privacy policy page + Data safety + content rating · First closed-test release (starts the 14-day clock) · (If free+unlock) Entitlements interface + PBL 9 one-time product | 1–2 + 14 days |
| *M6+ · Features* | *Out of scope here: undo/redo UI, auto-move to foundation, hints, stats, deal picker, themes, animations, settings, iOS target* | – |

Labels: `core`, `android`, `ci`, `release`, `decision`, `legacy-cleanup`, `later`.

---

## 7. Open questions for the owner

1. **Language comfort:** are you happy to learn or work in Kotlin + Android Studio? If not, should we switch the plan to Flutter/Dart? (This decides the stack; see §3.3.)
2. **Repo visibility and licence:** the repo is public and MIT-licensed. Anyone could legally build and give away a paid app built from it. Keep it public with a different licence (or keep only `:core` open), or make it private? Also, the public HTML title is profane. Fine to keep "f-ads" as a codename but not as the store name?
3. **Monetization model:** paid upfront (simplest, no billing code, but far fewer installs and no trial), or free with a one-time "unlock/support" purchase? If the latter, what's free (all deals with a nag-free tip jar, N deals per day, or a feature subset)? Target price (e.g. $1.99–$4.99)?
4. **Deal compatibility:** should "Game #N" match the Microsoft FreeCell numbering (deals 1–32000, plus the extended range)? Recommended: yes. It costs almost nothing and players expect it.
5. **App name and applicationId** (e.g. `com.<you>.freecell`). The ID can never change after publishing. Is a trademark-safe name needed? Avoid "Microsoft".
6. **iOS timing:** is iOS a "maybe in a year" or a "within ~6 months"? The second makes Flutter more attractive.
7. **Stats and persistence scope:** local only (games played/won, streaks, best times) or also cloud sync/Play Games achievements? Recommend local-only at first.
8. **Minimum Android version:** how old a phone must it support? Recommend minSdk 26 (Android 8), or 24 if needed.
9. **Rules variants:** standard 4 free cells only, or also 1–3-cell / "Baker's Game" variants later? This affects only `:core` parameterization; keep light.
10. **Gameplay assists** (feature phase, just record a preference): unlimited undo? Auto-move to foundation (always / safe-only / off)? Tap-to-move vs. drag only? Hints?
11. **Testers:** do you have 12+ people with Android phones who will opt in for 14 days? If not, plan for it early.
12. **Art direction:** keep developing your own Inkscape card style, or use an open-licensed card set (with licence check) to ship sooner?

---

## 8. Things I could not verify
- Whether CI on `develop` still passes on GitHub. There are no retained runs, and it uses Node 15 with v2 actions.
- Exact current versions of Kotlin/AGP/Gradle, Flutter (3.44 vs. 3.47), Flame, and the Flutter/Godot/Capacitor IAP plugins' Play Billing versions.
- Whether Android developer verification enforcement actually started on 2026-09-30, and whether Google now checks tester *activity* during closed testing (third-party claim only).
- Apple fee details (secondary sources only) and Unity's current licensing.
- I validated the Excalibur build visually in a desktop browser only, not on a phone.
