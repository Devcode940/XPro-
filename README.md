# XPro

An ad-free X (Twitter) companion client for Android, built with Jetpack Compose, Room, Paging 3,
DataStore, and Firebase (Auth, Firestore, App Check, AI Logic). It ships an “X Pro” experience:
post editing with revision history, undo-send, long-form articles, bookmark folders with optional
cross-device Firestore sync, an integrated media downloader, and a fake-it-till-it’s-real
“Grok” assistant (see [Honesty notes](#honesty-notes)).

> **Status:** actively hardened demo/production hybrid. See [Honesty notes](#honesty-notes) and
> [Production checklist](#production-checklist) before shipping to real users.

---

## Requirements

| Tool | Version | Notes |
|---|---|---|
| JDK | 17+ (CI uses Temurin 21) | Required for the Firestore emulator |
| Android SDK | platform 36, build-tools 36 | `ANDROID_HOME` set, or `local.properties` with `sdk.dir=...` |
| Gradle | **9.3.1** | The repo does **not** commit a `gradle-wrapper.jar` (see below) |
| Node.js | 18+ (20 LTS recommended) | Only for the Firestore rules tests |
| Firebase CLI | (via `npm ci`) | `firebase-tools` is a devDependency |

### Gradle wrapper notice

`gradle/wrapper/gradle-wrapper.properties` pins Gradle 9.3.1, but no wrapper JAR is committed.
Either install Gradle 9.3.1 yourself (`sdk install gradle 9.3.1`, `brew install gradle@9`, …) or
run this once to generate the wrapper scripts + JAR (commit the result if you do):

```bash
gradle wrapper --gradle-version 9.3.1 --distribution-type bin
```

CI uses SDKMAN to install Gradle 9.3.1 for this reason.

## Build

```bash
# Debug APK (signs with the fallback chain: ./debug.keystore → ~/.android/debug.keystore)
./tools/generate-debug-keystore.sh   # or just let Android Studio's debug keystore be used
gradle :app:assembleDebug :media-downloader:assembleDebug

# Unit tests (host-side, Robolectric)
gradle :app:testDebugUnitTest :media-downloader:testDebugUnitTest

# Full release (requires your own upload keystore, see below)
KEYSTORE_PATH=/path/to/my-upload-key.jks STORE_PASSWORD=... KEY_PASSWORD=... gradle :app:assembleRelease
```

### Release signing

Release builds sign with the `release` signing config in `app/build.gradle.kts`, which reads
`KEYSTORE_PATH` (defaults to `${rootDir}/my-upload-key.jks`), `STORE_PASSWORD`, `KEY_PASSWORD`,
key alias `upload`. Release also enables **R8 minification + resource shrinking**
(`app/proguard-rules.pro`); smoke-test a release APK before publishing.

## Firebase setup

The app is designed to **run without any Firebase credentials** (guest mode, everything local).
Cloud features (Google sign-in, bookmark sync, Firebase AI “Grok”) activate when the app is
configured with your Firebase project:

1. Create a Firebase project and an Android app with package name `com.devcode.xpro`.
   Note: the applicationId was lowercased from the legacy `com.Devcode.XPro` — re-register the
   Android app (and its SHA fingerprints) in the Firebase console accordingly, or sign-in/App Check will fail.
2. Download `google-services.json` and place it in `app/` (it is git-ignored). This generates
   `default_web_client_id` for Credential Manager sign-in. Without the file the app **builds and
   runs**, but Google Sign-In reports “not configured” (read via `resources.getIdentifier`, not a
   hard reference).
3. Enable **Authentication → Google** provider. The SHA-1 of your (debug/upload) keystore must be
   registered in the Firebase console for sign-in to succeed.
4. **App Check**: release builds attest with reCAPTCHA using the site key in
   `app/src/main/res/values/firebase_applet_config.xml` (`recaptcha_site_key`, a public site key).
   Debug builds use `DebugAppCheckProviderFactory`; pass a debug token via the
   `FIREBASE_APPCHECK_DEBUG_TOKEN` intent extra (see `AppCheckInit.kt`) — keep it in your local
   `.env` only, it is git-ignored.
5. **Firestore**: the project uses a named database (`firestore_database_id` in
   `firebase_applet_config.xml`). Deploy rules/indexes:
   ```bash
   firebase deploy --only firestore:rules --project <your-project-id>
   ```
   The blueprint for the two collections (`users/{uid}/bookmarks`, `users/{uid}/bookmark_folders`)
   is documented in `firebase-blueprint.json`.

## Testing

| Suite | Command | What it covers |
|---|---|---|
| Host unit tests (Robolectric) | `gradle :app:testDebugUnitTest` | Repository, paging, undo-send, bookmark sync merge/reconcile (`SyncMergeTest`), settings, UI smoke |
| Module unit tests | `gradle :media-downloader:testDebugUnitTest` | URL validation/SSRF guards, download queue persistence, filename dedup |
| Firestore rules tests | `npm ci && npm run test:rules` | Emulator-backed access control for both collections (needs a JDK; runs in CI) |
| Instrumented | `gradle :app:connectedDebugAndroidTest` | Needs a device/emulator |

`npm run test:rules` executes `firebase emulators:exec --only firestore --project demo-xpro`
around `node --test firestore.test.js`. It requires a JDK on `PATH` for the Firestore emulator
(GitHub CI’s `setup-java` provides it). Set `FIRESTORE_EMULATOR_HOST` / `GCP_PROJECT` to target a
different emulator; defaults are `127.0.0.1:8085` (see `firebase.json`).

## CI

`.github/workflows/ci.yml` runs on pushes to `main` and PRs:

- **firestore-rules** — Node 20 + Temurin 21, `npm ci`, `npm run test:rules`.
- **android** — Temurin 21, Android SDK, debug-keystore generation, Gradle 9.3.1 (SDKMAN),
  `assembleDebug` for both modules, then both unit-test suites.

## Architecture

```
app/                     # com.example — main app
  core/                  # navigation, design system, edge-to-edge, scroll behavior
  data/
    local/               # Room: XProDatabase (v4), DAOs, entities, seed data, repository, type converters
    model/               # Domain models
    remote/              # Firestore sync (write-through + reconcile), error sanitization
  feature/               # bookmarks, downloads, media, settings, social, tools
  ui/                    # screens, modals, components, theme
  viewmodel/             # XProViewModel (feed/undo-send), GrokViewModel (AI), ProfileViewModel
media-downloader/        # com.devcode.xpro.mediadownloader — reusable download module
tools/generate-debug-keystore.sh
firebase-blueprint.json  # Firestore document blueprint (reference)
firestore.rules          # Security rules (see firestore.test.js)
```

### Bookmark cloud sync (design)

Room is the source of truth for reads; Firestore is the replication layer for writes:

- **Write-through**: every local bookmark/folder mutation is mirrored to Firestore immediately
  when authenticated (guest mode = device-local only).
- **Pending flags + tombstones**: rows created/modified offline get `pendingSync = 1`; the next
  sync pushes them. Deletes enqueue a row in `pending_remote_deletes`; replayed on next
  authenticated sync so stale remote docs can’t resurrect locally.
- **Reconcile on snapshot**: remote snapshots upsert non-pending local rows and remove synced
  rows that vanished remotely. Rows with `updatedAtMillis = 0` (seeded or pre-v4 data) are never
  reconciled away or auto-pushed as a safety net.
- Conflict policy is deliberately simple (local pending edit wins; otherwise remote wins) — there
  is no operational transform or CRDT. Timestamps are **server timestamps** (`FieldValue.serverTimestamp()`),
  matching the security rules’ `request.time` checks.
- Guest/offline: every write path above degrades to local-only; failures are surfaced in the
  bookmarks banner instead of being silently swallowed.

### Room schema

`exportSchema = true` and schemas are emitted to `app/schemas/` (commit them). v3 → v4 added
`updatedAtMillis`/`pendingSync` to `bookmarks_vault` and `bookmark_folders` plus the
`pending_remote_deletes` tombstone table. v4 rows upgraded from v3 are flagged `pendingSync = 1`
so the first post-upgrade sync safely pushes them. `fallbackToDestructiveMigration` remains as a
last-resort guard, not a strategy — write migrations.

## Backup & privacy

`android:allowBackup` stays enabled but backup rules now exclude the Room database and all
app-private files (`backup_rules.xml`, `data_extraction_rules.xml`) — drafts, DMs and bookmarks
never leave the device via cloud backup. Firestore holds only bookmark/folder data per user,
guarded by `firestore.rules` (auth-scoped, owner-only, field/type/length/enum validated).
`FirestoreErrorHandler` logs sanitized diagnostics only — no uid/email/provider PII.

## Honesty notes

This codebase ships **demo behavior** that is not a bug but also not production reality:

- **Seed data**: demo seeding ("Elena Vance" persona, fake posts/trends/messages) runs only in
  **debug** builds; release installs start with an empty database.
- **“Grok, powered by X”** is actually Firebase AI Logic / Gemini (`GrokViewModel`). It is a UI
  persona; there is no xAI integration and no X API.
- **Ad-free / Premium** is unlocked client-side by toggle; there is no billing.
- **AI Studio artifacts** (`metadata.json`, `firebase-blueprint.json`) describe the app as
  generated in Google AI Studio and are kept for reference.
- **Pause** on downloads cannot truly pause Android’s DownloadManager; "paused" items re-download
  from byte 0 on resume (the UI says so).

## Production checklist

- [x] Demo seeding gated to debug builds (`BuildConfig.DEBUG` in `XProRepository.ensureSeeded`)
- [ ] Rebrand or re-scope the “Grok” assistant; wire a real model name + rate limiting
- [ ] Real `google-services.json` per variant; App Check enforced in production
- [ ] Push release build with `isMinifyEnabled` (already on) and smoke-test R8 output
- [ ] Replace `versionCode = 1`/`1.0`, wire Play delivery, set up keystore custody
- [ ] Firestore: re-register the Android app under the new `com.devcode.xpro` package name + SHA fingerprints
- [ ] Real analytics/crash reporting decision (none bundled today)
- [ ] Firestore: set up `firestore.indexes.json` if query patterns grow
- [ ] Re-verify backup/extraction rules against your privacy policy

## Known limitations / trade-offs

- No offline write queue for anything except bookmark/folder sync (posts, messages etc. are local-only fiction anyway).
- undo-send is in-memory: killing the process during the countdown drops the pending post (it is not yet in Room).
- `Content-Security`-style hardening for the QR scanner/WebView tools is out of scope.
- The v3→v4 Room migration is hand-verified SQL but not yet exercised by an automated Robolectric migration test; `app/schemas/` must carry the exported v3 and v4 JSONs first (generated by `:app:kspDebugKotlin`, commit the output).
- `gradle-wrapper.jar` is intentionally absent; CI installs Gradle 9.3.1 via SDKMAN. Run `gradle wrapper` once and commit the result to restore standard `./gradlew` UX.
- Release `isMinifyEnabled` uses conservative keep rules; if reflection-based crashes appear, extend `proguard-rules.pro` rather than disabling R8.
