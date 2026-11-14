const admin = require("firebase-admin");
const { getStorage } = require("firebase-admin/storage");
const functionsV1 = require("firebase-functions/v1");
const { onCall, HttpsError } = require("firebase-functions/v2/https");
const { onDocumentCreated } = require("firebase-functions/v2/firestore");

admin.initializeApp();

const auth = admin.auth();
const db = admin.firestore();
const messaging = admin.messaging();
const REGION = "asia-southeast1";
const OWNER_EMAIL = "phunghuyhoahaikba@gmail.com";

async function deleteDocumentsByUid(collectionName, uid) {
  let deleted = 0;

  while (true) {
    const snapshot = await db.collection(collectionName)
      .where("uid", "==", uid)
      .limit(400)
      .get();

    if (snapshot.empty) break;

    const batch = db.batch();
    for (const document of snapshot.docs) {
      batch.delete(document.ref);
    }
    await batch.commit();
    deleted += snapshot.size;
  }

  return deleted;
}

async function deleteUserOwnedData(uid) {
  const bucket = getStorage().bucket();
  const [files] = await bucket.getFiles({
    prefix: `community/${uid}/`
  });

  for (let index = 0; index < files.length; index += 20) {
    await Promise.all(
      files.slice(index, index + 20).map(file =>
        file.delete({ ignoreNotFound: true })
      )
    );
  }

  const ownedCollections = [
    "community_messages",
    "community_shared_jsons",
    "community_deleted_messages",
    "community_deleted_images",
    "community_deleted_files"
  ];

  let deletedDocuments = 0;
  for (const collectionName of ownedCollections) {
    deletedDocuments += await deleteDocumentsByUid(collectionName, uid);
  }

  await db.collection("licenses").doc(uid).delete();
  await db.collection("users").doc(uid).delete();

  return { deletedDocuments };
}

function requireOwner(request) {
  const email = String(request.auth?.token?.email || "").trim().toLowerCase();
  const verified = request.auth?.token?.email_verified === true;

  if (!request.auth || !verified || email !== OWNER_EMAIL) {
    throw new HttpsError("permission-denied", "Chỉ tài khoản quản lý COC-LIO mới được thực hiện thao tác này.");
  }
}

exports.adminSyncSharedAccounts = onCall({ region: REGION }, async (request) => {
  requireOwner(request);

  const profiles = await db.collection("users").get();
  let removed = 0;

  for (const profile of profiles.docs) {
    try {
      await auth.getUser(profile.id);
    } catch (error) {
      if (error.code === "auth/user-not-found") {
        await deleteUserOwnedData(profile.id);
        removed += 1;
      } else {
        throw error;
      }
    }
  }

  return { removed };
});

exports.recordAccountActivity = onCall({ region: REGION }, async (request) => {
  if (!request.auth) {
    throw new HttpsError("unauthenticated", "Bạn cần đăng nhập COC-LIO.");
  }

  const uid = request.auth.uid;
  const profileRef = db.collection("users").doc(uid);
  const profile = await profileRef.get();

  if (
    !profile.exists ||
    profile.get("dataSharingConsent") !== true ||
    Number(profile.get("activityDataConsentVersion") || 0) !== 6
  ) {
    return { recorded: false, reason: "consent-required" };
  }

  const data = request.data || {};
  const event = String(data.event || "");
  if (!["open", "heartbeat", "background"].includes(event)) {
    throw new HttpsError("invalid-argument", "Loại hoạt động không hợp lệ.");
  }

  const safeString = (value, maxLength) =>
    typeof value === "string" ? value.trim().slice(0, maxLength) : "";

  const safeInteger = (value, maxValue) => {
    const number = Number(value);
    return Number.isFinite(number)
      ? Math.max(0, Math.min(maxValue, Math.floor(number)))
      : 0;
  };

  const update = {
    lastActivityEvent: event,
    activityUpdatedAt: admin.firestore.FieldValue.serverTimestamp()
  };

  const deviceManufacturer = safeString(data.deviceManufacturer, 60);
  const deviceModel = safeString(data.deviceModel, 100);
  const androidVersion = safeString(data.androidVersion, 40);
  const appVersionName = safeString(data.appVersionName, 40);

  if (deviceManufacturer) update.deviceManufacturer = deviceManufacturer;
  if (deviceModel) update.deviceModel = deviceModel;
  if (androidVersion) update.androidVersion = androidVersion;
  if (appVersionName) update.appVersionName = appVersionName;
  update.sdkInt = safeInteger(data.sdkInt, 1000);
  update.appVersionCode = safeInteger(data.appVersionCode, 2147483647);

  if (event === "open") {
    if (!profile.get("firstOpenedAt")) {
      update.firstOpenedAt = admin.firestore.FieldValue.serverTimestamp();
    }
    update.lastOpenedAt = admin.firestore.FieldValue.serverTimestamp();
    update.lastActiveAt = admin.firestore.FieldValue.serverTimestamp();
    update.sessionStartedAt = admin.firestore.FieldValue.serverTimestamp();
    update.appOpenCount = admin.firestore.FieldValue.increment(1);
  } else if (event === "heartbeat") {
    update.lastActiveAt = admin.firestore.FieldValue.serverTimestamp();
  } else {
    const duration = safeInteger(data.sessionDurationSeconds, 86400);
    update.lastBackgroundAt = admin.firestore.FieldValue.serverTimestamp();
    update.lastSessionDurationSeconds = duration;
    update.totalForegroundSeconds = admin.firestore.FieldValue.increment(duration);
  }

  await profileRef.set(update, { merge: true });
  return { recorded: true };
});

exports.adminDeleteUser = onCall({ region: REGION }, async (request) => {
  requireOwner(request);

  const uid = String(request.data?.uid || "").trim();
  if (!uid) {
    throw new HttpsError("invalid-argument", "Thiếu Firebase UID.");
  }

  if (uid === request.auth.uid) {
    throw new HttpsError("failed-precondition", "Không thể tự xóa tài khoản quản lý đang sử dụng.");
  }

  const profileRef = db.collection("users").doc(uid);
  const profile = await profileRef.get();

  if (!profile.exists) {
    throw new HttpsError("not-found", "Không tìm thấy hồ sơ tài khoản này.");
  }

  const user = await auth.getUser(uid);
  if (String(user.email || "").trim().toLowerCase() === OWNER_EMAIL) {
    throw new HttpsError("failed-precondition", "Không thể xóa tài khoản chủ sở hữu COC-LIO.");
  }

  await auth.deleteUser(uid);
  await deleteUserOwnedData(uid);

  return { success: true, uid, email: user.email || "" };
});

exports.cleanupDeletedAuthUser = functionsV1.auth.user().onDelete(async (user) => {
  await deleteUserOwnedData(user.uid);
});

exports.notifyCommunityMessage = onDocumentCreated(
  { document: "community_messages/{messageId}", region: REGION },
  async (event) => {
    const snapshot = event.data;
    if (!snapshot) return;

    const data = snapshot.data() || {};
    const senderIsAdmin = data.senderIsAdmin === true;
    const sender = senderIsAdmin ? "AILIO" : String(data.displayName || "Thành viên").slice(0, 80);
    const text = String(data.text || "").trim();
    const type = String(data.type || "text");

    let content = text;
    if (!content && type === "image") content = "Đã gửi một hình ảnh.";
    if (!content && type === "file") content = "Đã gửi một tệp.";
    if (!content) return;

    await messaging.send({
      topic: "coc_lio_community",
      data: {
        type: "community_message",
        messageId: event.params.messageId,
        senderUid: String(data.uid || ""),
        senderName: sender,
        senderIsAdmin: String(senderIsAdmin),
        senderPhotoUrl: senderIsAdmin ? "" : String(data.photoUrl || "").slice(0, 900),
        messageText: content.slice(0, 1000),
        title: "Cộng đồng COC-LIO",
        body: (sender + ": " + content).slice(0, 450)
      },
      android: { priority: "high" }
    });
  }
);

exports.notifyAppUpdate = onDocumentCreated(
  { document: "app_updates/{updateId}", region: REGION },
  async (event) => {
    const snapshot = event.data;
    if (!snapshot) return;

    const data = snapshot.data() || {};
    if (data.notifyUsers !== true) return;

    const version = String(data.versionName || "").slice(0, 40);
    const notes = String(data.notes || "").trim();
    const body = ("COC-LIO " + version + (notes ? ": " + notes : " đã có APK mới.")).slice(0, 450);

    await messaging.send({
      topic: "coc_lio_updates",
      data: {
        type: "app_update",
        version: version,
        title: "COC-LIO có bản cập nhật mới",
        body: body
      },
      android: { priority: "high" }
    });
  }
);
