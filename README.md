# f-ads

FreeCell with no ads. Ever.

The Android FreeCell I like best is full of ads and only lets you remove them with a subscription. So this is a
free, ad-free replacement that plays just as well. Not for sale. Android first; iOS, desktop and web maybe later.

## Status

**Planning.** No playable game yet.

- `legacy-web/` is the 2021 web prototype (Excalibur.js + webpack). It's kept for reference and is not being developed.
  It is also preserved at the git tag `legacy-web-excalibur-2021`.
- `design/` is the source art (card base and suit icons).
- [`docs/PLAN.md`](docs/PLAN.md) is the assessment and plan for restarting the project. The current recommendation
  is Kotlin + Jetpack Compose, structured as Kotlin Multiplatform so iOS can follow without a rewrite.

## Running the legacy prototype

Needs the legacy OpenSSL flag on modern Node:

```bash
cd legacy-web
npm ci
npm test
NODE_OPTIONS=--openssl-legacy-provider npm run dev
```
