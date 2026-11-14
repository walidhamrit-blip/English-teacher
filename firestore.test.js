const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

const nowTs = () => new Date(Date.now() - 1000);

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

test("Unauthenticated user cannot read or write user progress or vocab", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).get());
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).collection("vocab").get());
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).collection("tutor_turns").get());
});

test("Authenticated user can create and update own progress, cannot spoof uid or inject ghost field", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const aliceRef = aliceDb.collection("users").doc(ALICE_UID);

  await assertSucceeds(
    aliceRef.set({
      userId: ALICE_UID,
      displayName: "Alice",
      xp: 0,
      completedLessonIds: [],
      createdAt: nowTs(),
      updatedAt: nowTs(),
    })
  );

  await assertSucceeds(
    aliceRef.update({
      xp: 50,
      completedLessonIds: ["greetings"],
      lastLessonId: "greetings",
      dailyGoal: 10,
      currentStreak: 3,
      longestStreak: 5,
      lastActiveDate: "2026-11-12",
      dailyStudyMinutesGoal: 20,
      todayStudySeconds: 420,
      lastStudyDate: "2026-11-13",
      reminderEnabled: true,
      reminderHour: 20,
      reminderMinute: 30,
      lastDailyQuizDate: "2026-11-13",
      lastDailyQuizScore: 4,
      updatedAt: nowTs(),
    })
  );

  // Out-of-range daily quiz score must fail
  await assertFails(
    aliceRef.update({
      lastDailyQuizScore: 6,
      updatedAt: nowTs(),
    })
  );

  // Out-of-range dailyGoal must fail
  await assertFails(
    aliceRef.update({
      dailyGoal: 0,
      updatedAt: nowTs(),
    })
  );

  // Out-of-range dailyStudyMinutesGoal or negative todayStudySeconds must fail
  await assertFails(
    aliceRef.update({
      dailyStudyMinutesGoal: 0,
      updatedAt: nowTs(),
    })
  );
  await assertFails(
    aliceRef.update({
      dailyStudyMinutesGoal: 181,
      updatedAt: nowTs(),
    })
  );
  await assertFails(
    aliceRef.update({
      todayStudySeconds: -5,
      updatedAt: nowTs(),
    })
  );

  // Out-of-range reminderHour or reminderMinute must fail
  await assertFails(
    aliceRef.update({
      reminderHour: 24,
      updatedAt: nowTs(),
    })
  );
  await assertFails(
    aliceRef.update({
      reminderMinute: 60,
      updatedAt: nowTs(),
    })
  );

  // Negative streak must fail
  await assertFails(
    aliceRef.update({
      currentStreak: -1,
      updatedAt: nowTs(),
    })
  );

  // Shadow update with ghost field must fail
  await assertFails(
    aliceRef.update({
      xp: 100,
      isAdmin: true,
      updatedAt: nowTs(),
    })
  );

  // Cross-user access must fail
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  await assertFails(bobDb.collection("users").doc(ALICE_UID).get());
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).update({ xp: 999, updatedAt: nowTs() })
  );
});

test("Authenticated user can CRUD own vocab and list, cross-user fails", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const wordRef = aliceDb.collection("users").doc(ALICE_UID).collection("vocab").doc("word_1");

  await assertSucceeds(
    wordRef.set({
      userId: ALICE_UID,
      english: "Thank you",
      arabic: "شكراً",
      phoneticArabic: "ثانك يو",
      phoneticEnglish: "/ˈθæŋk juː/ • THANK yoo",
      exampleEnglish: "Thank you very much!",
      category: "Greetings",
      mastered: false,
      usageCount: 3,
      createdAt: nowTs(),
      updatedAt: nowTs(),
    })
  );

  await assertSucceeds(aliceDb.collection("users").doc(ALICE_UID).collection("vocab").get());

  await assertSucceeds(
    wordRef.update({
      mastered: true,
      usageCount: 5,
      phoneticEnglish: "/ˈθæŋk juː/",
      updatedAt: nowTs(),
    })
  );

  await assertFails(
    wordRef.update({
      usageCount: 0,
      updatedAt: nowTs(),
    })
  );

  await assertFails(
    wordRef.update({
      phoneticEnglish: "a".repeat(161),
      updatedAt: nowTs(),
    })
  );

  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("vocab").get());
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("vocab").doc("word_1").delete());

  await assertSucceeds(wordRef.delete());
});

test("Tutor turns enforce valid mode and append-only immutability", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const turnRef = aliceDb.collection("users").doc(ALICE_UID).collection("tutor_turns").doc("turn_1");

  await assertSucceeds(
    turnRef.set({
      userId: ALICE_UID,
      mode: "vocal",
      userInput: "Hello teacher",
      englishReply: "Hello! Great pronunciation.",
      arabicTranslation: "مرحباً! نطق رائع.",
      feedbackArabic: "نطقك واضح جداً.",
      phoneticHint: "هيلو",
      createdAt: nowTs(),
    })
  );

  // Invalid mode rejected
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("tutor_turns").doc("turn_2").set({
      userId: ALICE_UID,
      mode: "video",
      userInput: "Hi",
      englishReply: "Hi",
      arabicTranslation: "أهلاً",
      feedbackArabic: "",
      phoneticHint: "",
      createdAt: nowTs(),
    })
  );

  // Update rejected (append-only)
  await assertFails(turnRef.update({ userInput: "Edited" }));
});
