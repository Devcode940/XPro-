# Room schema exports

This directory is where Room writes the JSON snapshots of the database schema
(`room.schemaLocation` in `app/build.gradle.kts`, `exportSchema = true` on `XProDatabase`).

**These files must be committed**, because `androidx.room.testing.MigrationTestHelper` loads
`<version>.json` from here to build a database at an older version and then run the real
migrations against it.

Right now the directory contains only this note: the JSONs have not been generated since
`XProDatabase` moved to version 5. Generate and commit them before writing a migration test:

```bash
gradle :app:kspDebugKotlin
git add app/schemas/*.json
```

Cold-start history (see `XProDatabase` for the SQL):

| Version | Change |
|---|---|
| 3 | pre-sync baseline |
| 4 | `updatedAtMillis` + `pendingSync` on `bookmarks_vault` / `bookmark_folders`; `pending_remote_deletes` table |
| 5 | nullable `ownerUid` on the two tables above and on `pending_remote_deletes`; resets `pendingSync` on never-synced rows |

Once `3.json`, `4.json` and `5.json` are present, a migration test looks like:

```kotlin
@get:Rule
val helper = MigrationTestHelper(
    InstrumentationRegistry.getInstrumentation(),
    XProDatabase::class.java
)

@Test
fun migrate3To5_keepsBookmarksAndFlagsThemForSync() {
    helper.createDatabase(TEST_DB, 3).apply {
        execSQL("INSERT INTO bookmarks_vault (...) VALUES (...)")
        close()
    }
    val db = helper.runMigrationsAndValidate(TEST_DB, 5, true, MIGRATION_3_4, MIGRATION_4_5)
    // assert ownerUid IS NULL, pendingSync = 1 for synced rows, 0 for never-synced rows
}
```

Do not hand-write these JSONs: a snapshot that does not match what the released app actually
created makes the migration test pass against a schema no user has.
