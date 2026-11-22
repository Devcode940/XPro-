const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_123";
const BOB_UID = "bob_456";

const [emulatorHost, emulatorPortStr] = (
  process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085"
).split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

before(async () => {
  const rules = fs.readFileSync("./firestore.rules", "utf8");
  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      rules,
      host: emulatorHost,
      port: emulatorPort,
    },
  });
});

after(async () => {
  if (testEnv) {
    await testEnv.cleanup();
  }
});

beforeEach(async () => {
  if (testEnv) {
    await testEnv.clearFirestore();
  }
});

test("Unauthenticated users cannot read or write bookmarks or folders", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  const now = new Date(Date.now() - 2000);

  await assertFails(
    unauthDb.collection("users").doc(ALICE_UID).collection("bookmarks").get()
  );
  await assertFails(
    unauthDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("bookmarks")
      .doc("bm_1")
      .set({
        userId: ALICE_UID,
        title: "Saved post",
        content: "Unauthorized write",
        authorName: "Alice",
        authorHandle: "@alice",
        itemType: "POST",
        createdAt: now,
        updatedAt: now,
      })
  );
  await assertFails(
    unauthDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("bookmark_folders")
      .get()
  );
});

test("Authenticated owner can create, list, partial-update, and delete bookmarks", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const created = new Date(Date.now() - 10000);
  const bmRef = aliceDb
    .collection("users")
    .doc(ALICE_UID)
    .collection("bookmarks")
    .doc("bm_post_1");

  await assertSucceeds(
    bmRef.set({
      userId: ALICE_UID,
      title: "Jetpack Compose Architecture",
      content: "Deep dive on state hoisting and Material 3 drawers.",
      authorName: "XTRA Dev",
      authorHandle: "@xtradev",
      itemType: "LINK",
      linkUrl: "https://developer.android.com/compose",
      folderId: "folder_eng",
      createdAt: created,
      updatedAt: created,
    })
  );

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("bookmarks").get()
  );

  await assertSucceeds(
    bmRef.update({
      folderId: "folder_design",
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Reject immutable createdAt mutation
  await assertFails(
    bmRef.update({
      createdAt: new Date(Date.now() - 500),
      updatedAt: new Date(Date.now() - 500),
    })
  );

  // Reject Ghost field & @DocumentId 'id' inside document body
  await assertFails(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("bookmarks")
      .doc("bm_ghost")
      .set({
        id: "bm_ghost",
        userId: ALICE_UID,
        title: "Ghost",
        content: "Should fail",
        authorName: "Alice",
        authorHandle: "@alice",
        itemType: "POST",
        createdAt: created,
        updatedAt: created,
      })
  );

  await assertSucceeds(bmRef.delete());
});

test("Cross-user isolation blocks Bob from reading or modifying Alice bookmarks and folders", async () => {
  const created = new Date(Date.now() - 5000);
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    await ctx
      .firestore()
      .collection("users")
      .doc(ALICE_UID)
      .collection("bookmarks")
      .doc("bm_alice")
      .set({
        userId: ALICE_UID,
        title: "Private Bookmark",
        content: "Alice private saved link",
        authorName: "Alice",
        authorHandle: "@alice",
        itemType: "LINK",
        createdAt: created,
        updatedAt: created,
      });
    await ctx
      .firestore()
      .collection("users")
      .doc(ALICE_UID)
      .collection("bookmark_folders")
      .doc("folder_alice")
      .set({
        userId: ALICE_UID,
        name: "Research",
        description: "Private folder",
        colorHex: 0xFF1D9BF0,
        iconKey: "code",
        createdAt: created,
        updatedAt: created,
      });
  });

  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  await assertFails(
    bobDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("bookmarks")
      .doc("bm_alice")
      .get()
  );
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).collection("bookmarks").get()
  );
  await assertFails(
    bobDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("bookmarks")
      .doc("bm_alice")
      .delete()
  );
  await assertFails(
    bobDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("bookmark_folders")
      .doc("folder_alice")
      .get()
  );
});

test("Validation rejects invalid enum types, future timestamps, and invalid folder IDs", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const created = new Date(Date.now() - 5000);

  // Invalid itemType
  await assertFails(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("bookmarks")
      .doc("bm_bad_enum")
      .set({
        userId: ALICE_UID,
        title: "Bad Enum",
        content: "Testing enum guard",
        authorName: "Alice",
        authorHandle: "@alice",
        itemType: "AUDIO",
        createdAt: created,
        updatedAt: created,
      })
  );

  // Tampered future timestamp
  await assertFails(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("bookmarks")
      .doc("bm_future")
      .set({
        userId: ALICE_UID,
        title: "Future",
        content: "Future timestamp check",
        authorName: "Alice",
        authorHandle: "@alice",
        itemType: "POST",
        createdAt: new Date(Date.now() + 60000),
        updatedAt: new Date(Date.now() + 60000),
      })
  );

  // Valid bookmark folder creation & update
  const folderRef = aliceDb
    .collection("users")
    .doc(ALICE_UID)
    .collection("bookmark_folders")
    .doc("folder_1");
  await assertSucceeds(
    folderRef.set({
      userId: ALICE_UID,
      name: "Engineering",
      description: "Systems notes",
      colorHex: 0xFF1D9BF0,
      iconKey: "code",
      createdAt: created,
      updatedAt: created,
    })
  );
  await assertSucceeds(
    folderRef.update({
      name: "Systems Engineering",
      updatedAt: new Date(Date.now() - 1000),
    })
  );
});

test("Validation rejects oversized fields, missing required keys and invalid folder ids", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const created = new Date(Date.now() - 5000);
  const baseBookmark = {
    userId: ALICE_UID,
    title: "Valid title",
    content: "Valid content",
    authorName: "Alice",
    authorHandle: "@alice",
    itemType: "POST",
    createdAt: created,
    updatedAt: created,
  };
  const bookmarks = aliceDb.collection("users").doc(ALICE_UID).collection("bookmarks");

  // Baseline: the shape these negative cases mutate is accepted.
  await assertSucceeds(bookmarks.doc("bm_valid").set(baseBookmark));

  // title > 300 characters
  await assertFails(
    bookmarks.doc("bm_long_title").set({ ...baseBookmark, title: "x".repeat(301) })
  );

  // content must be non-empty and <= 5000 characters
  await assertFails(
    bookmarks.doc("bm_empty_content").set({ ...baseBookmark, content: "" })
  );
  await assertFails(
    bookmarks.doc("bm_long_content").set({ ...baseBookmark, content: "x".repeat(5001) })
  );

  // required key omitted
  const missingKey = { ...baseBookmark };
  delete missingKey.authorHandle;
  await assertFails(bookmarks.doc("bm_missing_key").set(missingKey));

  // optional mediaUrl is length-checked
  await assertFails(
    bookmarks.doc("bm_long_media").set({
      ...baseBookmark,
      mediaUrl: "https://cdn.example.com/" + "a".repeat(1000),
    })
  );

  // folderId must match ^[a-zA-Z0-9_-]+$
  await assertFails(
    bookmarks.doc("bm_bad_folder").set({ ...baseBookmark, folderId: "folder/../escape" })
  );

  // update may not touch userId or authorName
  await assertFails(
    bookmarks.doc("bm_valid").update({ userId: BOB_UID, updatedAt: new Date(Date.now() - 1000) })
  );
  await assertFails(
    bookmarks.doc("bm_valid").update({ authorName: "Mallory", updatedAt: new Date(Date.now() - 1000) })
  );

  // folder name length and required keys
  const folders = aliceDb.collection("users").doc(ALICE_UID).collection("bookmark_folders");
  const baseFolder = {
    userId: ALICE_UID,
    name: "Research",
    description: "Notes",
    colorHex: 0xFF1D9BF0,
    iconKey: "code",
    createdAt: created,
    updatedAt: created,
  };
  await assertSucceeds(folders.doc("folder_valid").set(baseFolder));
  await assertFails(folders.doc("folder_long_name").set({ ...baseFolder, name: "x".repeat(61) }));
  const folderMissingDescription = { ...baseFolder };
  delete folderMissingDescription.description;
  await assertFails(folders.doc("folder_missing").set(folderMissingDescription));

  // folder updates may not rewrite createdAt
  await assertFails(
    folders.doc("folder_valid").update({
      createdAt: new Date(Date.now() - 1000),
      updatedAt: new Date(Date.now() - 1000),
    })
  );
});
