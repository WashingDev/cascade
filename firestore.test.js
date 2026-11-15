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

async function seedProject(uid, projectId, status = "active") {
  await testEnv.withSecurityRulesDisabled(async (context) => {
    await context.firestore().collection("users").doc(uid).collection("projects").doc(projectId).set({
      userId: uid,
      title: "Launch Portfolio",
      description: "Redesign personal site",
      category: "Creative",
      accentColor: "cobalt",
      status,
      createdAt: new Date(),
      updatedAt: new Date(),
    });
  });
}

async function seedMilestone(uid, milestoneId, projectId) {
  await testEnv.withSecurityRulesDisabled(async (context) => {
    await context.firestore().collection("users").doc(uid).collection("milestones").doc(milestoneId).set({
      userId: uid,
      projectId,
      title: "Beta Deployed",
      description: "Deploy staging build",
      targetDateMs: Date.now() + 86400000,
      isReached: false,
      createdAt: new Date(),
      updatedAt: new Date(),
    });
  });
}

test("Unauthenticated user cannot list or write projects, milestones, or tasks", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).collection("projects").get());
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).collection("milestones").get());
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).collection("tasks").get());
});

test("Cross-user isolation: Bob cannot read or create under Alice's subcollections", async () => {
  await seedProject(ALICE_UID, "proj_1");
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("projects").doc("proj_1").get());
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("projects").get());
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).collection("projects").doc("proj_2").set({
      userId: ALICE_UID,
      title: "Hijack",
      description: "",
      category: "Work",
      accentColor: "cobalt",
      status: "active",
      createdAt: new Date(),
      updatedAt: new Date(),
    })
  );
});

test("Alice can create, list, and update her own project", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const ref = aliceDb.collection("users").doc(ALICE_UID).collection("projects").doc("proj_1");
  await assertSucceeds(
    ref.set({
      userId: ALICE_UID,
      title: "Home Studio",
      description: "Build acoustic panels",
      category: "Home",
      accentColor: "terracotta",
      status: "active",
      createdAt: new Date(),
      updatedAt: new Date(),
    })
  );
  await assertSucceeds(aliceDb.collection("users").doc(ALICE_UID).collection("projects").get());
  await assertSucceeds(ref.update({ status: "completed", updatedAt: new Date() }));
});

test("Shadow update with ghost field on project is rejected", async () => {
  await seedProject(ALICE_UID, "proj_1");
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("projects").doc("proj_1").update({
      title: "Updated",
      ghostField: true,
      updatedAt: new Date(),
    })
  );
});

test("Archived project is terminal and blocks further updates", async () => {
  await seedProject(ALICE_UID, "proj_arch", "archived");
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("projects").doc("proj_arch").update({
      status: "active",
      updatedAt: new Date(),
    })
  );
});

test("Task creation requires existing parent project and optional existing milestone", async () => {
  await seedProject(ALICE_UID, "proj_1");
  await seedMilestone(ALICE_UID, "ms_1", "proj_1");
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();

  // Fails with non-existent project
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("tasks").doc("task_bad").set({
      userId: ALICE_UID,
      projectId: "missing_proj",
      title: "Orphan task",
      notes: "",
      isCompleted: false,
      priority: "medium",
      createdAt: new Date(),
      updatedAt: new Date(),
    })
  );

  // Fails with non-existent milestone
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("tasks").doc("task_bad_ms").set({
      userId: ALICE_UID,
      projectId: "proj_1",
      milestoneId: "missing_ms",
      title: "Bad milestone task",
      notes: "",
      isCompleted: false,
      priority: "high",
      createdAt: new Date(),
      updatedAt: new Date(),
    })
  );

  // Succeeds with valid project and milestone
  const taskRef = aliceDb.collection("users").doc(ALICE_UID).collection("tasks").doc("task_ok");
  await assertSucceeds(
    taskRef.set({
      userId: ALICE_UID,
      projectId: "proj_1",
      milestoneId: "ms_1",
      title: "Write copy",
      notes: "Draft hero section",
      isCompleted: false,
      priority: "high",
      dueDateMs: Date.now() + 3600000,
      createdAt: new Date(),
      updatedAt: new Date(),
    })
  );

  // Toggle completion succeeds
  await assertSucceeds(
    taskRef.update({
      isCompleted: true,
      completedAtMs: Date.now(),
      updatedAt: new Date(),
    })
  );
});

test("Milestone creation requires existing parent project and allows toggle", async () => {
  await seedProject(ALICE_UID, "proj_1");
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();

  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("milestones").doc("ms_bad").set({
      userId: ALICE_UID,
      projectId: "no_such_proj",
      title: "Orphan milestone",
      description: "",
      targetDateMs: Date.now() + 86400000,
      isReached: false,
      createdAt: new Date(),
      updatedAt: new Date(),
    })
  );

  const msRef = aliceDb.collection("users").doc(ALICE_UID).collection("milestones").doc("ms_ok");
  await assertSucceeds(
    msRef.set({
      userId: ALICE_UID,
      projectId: "proj_1",
      title: "First Release",
      description: "Ship v1",
      targetDateMs: Date.now() + 86400000,
      isReached: false,
      createdAt: new Date(),
      updatedAt: new Date(),
    })
  );

  await assertSucceeds(
    msRef.update({
      isReached: true,
      reachedAtMs: Date.now(),
      updatedAt: new Date(),
    })
  );
});
