# Bookmark sync — design notes

> Status: implemented in Room v5 (`XProDatabase.MIGRATION_4_5`) and
> `FirestoreBookmarkSyncRepository` / `BookmarksViewModel`. This file explains *why* the
> mechanism looks like this; the user-facing summary lives in the README.

## Data model

| Table | Sync fields | Notes |
|---|---|---|
| `bookmarks_vault` | `ownerUid`, `pendingSync`, `updatedAtMillis` | `ownerUid = NULL` means "device-local, not bound to an account yet". |
| `bookmark_folders` | same | |
| `pending_remote_deletes` | `ownerUid` | Tombstones: `(id, kind)` + the account they belong to. |

`updatedAtMillis = 0` is the "never synced" marker used by seeded/demo rows; the reconciler
never deletes such rows and the claim step never adopts them.

## Ownership

1. `XProRepository.setActiveOwner(uid)` is called whenever the signed-in account changes
   (`XProViewModel.setCloudSyncUid`, `BookmarksViewModel`). Every row created after that is
   stamped with that uid.
2. On the next push for `uid`, `claimUnownedRows(uid)` adopts rows that are unowned **and**
   either pending or already timestamped (`pendingSync = 1 OR updatedAtMillis > 0`). That is how
   a user's pre-sign-in bookmarks reach their cloud. Demo rows (`updatedAtMillis = 0`,
   `pendingSync = 0`) are never adopted.
3. Every push, mark-synced, reconcile and tombstone replay is `WHERE ownerUid = :uid`. A row
   owned by account A is invisible to account B, so signing in as B cannot upload or delete A's
   data. A's rows simply stay on the device until A signs in again.
4. Immediate write-through deletes are gated by `isRemoteCopyOwnedBy(rowOwner, uid)` — a delete
   performed while signed in as B on a leftover row from A must not delete B's document that
   happens to share the id.

## Push (`BookmarksViewModel.pushPendingSync`)

```
claimUnownedRows(uid)
replay(getPendingRemoteDeletes(uid))       # delete remote docs, clear tombstone on success
for each pending folder  -> upsertFolder -> markFoldersSynced(uid, [id])
for each pending bookmark-> upsertBookmark -> markBookmarksSynced(uid, [id])
```

A failed write leaves `pendingSync = 1`. There is no background worker: retries happen on the
next account change, the next app start, or when the user taps the sync action
(`retrySync()` bumps `syncRetryTick`, which re-runs the push).

## Reconcile (`BookmarksViewModel` snapshot listener)

1. `activeUid.flatMapLatest { uid -> combine(observeRemoteBookmarks(uid), observeRemoteFolders(uid)) }`
   keeps the uid next to the payload, so a snapshot can never be applied to the wrong account.
2. A snapshot is applied **only** when `FirestoreSyncResult.Success.isAuthoritative` is true,
   i.e. it came from the server (`!metadata.isFromCache`) rather than the local Firebase cache.
   The empty placeholder produced when Firebase is unavailable is not authoritative either. This
   matters because reconcile is destructive: an empty authoritative snapshot deletes every
   synced row for that account.
3. Per document: skip if a tombstone exists (delete in flight), skip if the local row is pending
   (local edit wins), skip if the local row belongs to another account; otherwise upsert with
   `pendingSync = false` and the remote timestamp.
4. Stale rows are removed by diffing local synced ids against remote ids and deleting in chunks
   (`SQL_IN_CHUNK_SIZE = 400`) instead of one `NOT IN (...)` list, which would exceed SQLite's
   bound-parameter limit on large libraries.

## Tombstones

- Created for deletes of rows that may exist remotely (`ownerUid` set, or pending, or already
  timestamped). Never created for never-synced rows — there is nothing remote to delete, and a
  phantom tombstone could delete an unrelated document that shares a generated id.
- Replayed only for their own account, then cleared on success.
- Deleting a row whose owner is another account leaves the tombstone for that owner's next sync.

## Conflict policy

Deliberately simple: a local pending edit wins; otherwise the remote document wins. No OT/CRDT.
Timestamps are Firestore server timestamps (`FieldValue.serverTimestamp()`), which the security
rules validate against `request.time`.

## Invariants worth preserving

1. A row is pushed, marked synced, or deleted remotely only under the uid stored in
   `ownerUid` (or after being adopted by `claimUnownedRows`).
2. Reconcile runs only on authoritative server snapshots.
3. `updatedAtMillis = 0 && pendingSync = 0` ⇒ the row is local-only forever unless edited.
4. Tombstones are scoped to one account and cleared only after a successful remote delete.

## Tests that pin this behaviour

`app/src/test/java/com/example/SyncMergeTest.kt` covers remote deletes propagating, pending rows
winning, seeded rows surviving, cross-account isolation (push + reconcile), guest deletes being
claimed by the next sign-in, no tombstone for never-synced rows, and the ownership matrix.
`firestore.test.js` covers the server-side rules the client depends on.

## Not done yet

- `app/schemas/*.json` are not committed, so `MigrationTestHelper` cannot run; see
  `app/schemas/README.md`.
- No per-account UI to browse or delete another account's local copy.
- No WorkManager retry — pushes only run while the app is alive.
