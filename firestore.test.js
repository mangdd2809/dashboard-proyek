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

const [emulatorHost, emulatorPortStr] = (process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085").split(":");
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

test("Unauthenticated user: cannot read projects or rab items", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("projects").get());
  await assertFails(unauthDb.collection("rab_items").get());
});

test("Authenticated user: cannot read another user's project", async () => {
  await testEnv.withSecurityRulesDisabled(async (context) => {
    await context.firestore().collection("projects").doc("bob_proj").set({
      name: "Bob Project",
      status: "Aktif",
      userId: BOB_UID,
    });
  });

  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertFails(aliceDb.collection("projects").doc("bob_proj").get());
});

test("Authenticated user: can create and read their own project", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertSucceeds(
    aliceDb.collection("projects").doc("alice_proj").set({
      name: "Alice Construction Proj",
      status: "Aktif",
      userId: ALICE_UID,
      budgetRAB: 500000000,
      progressPercent: 15.5
    })
  );

  await assertSucceeds(
    aliceDb.collection("projects").where("userId", "==", ALICE_UID).get()
  );
});

test("Authenticated user: can create and read their own rab items", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertSucceeds(
    aliceDb.collection("rab_items").doc("alice_rab_1").set({
      userId: ALICE_UID,
      projectId: "alice_proj",
      floorLevel: "LANTAI 1",
      workDescription: "Pek. Pondasi Footplate",
      volume: 12.5,
      unitPrice: 3500000,
      totalPrice: 43750000
    })
  );

  await assertSucceeds(
    aliceDb.collection("rab_items").where("userId", "==", ALICE_UID).get()
  );
});
