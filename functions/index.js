const admin = require("firebase-admin");
const functionsV1 = require("firebase-functions/v1");
const { onCall, HttpsError } = require("firebase-functions/v2/https");
const { onDocumentCreated } = require("firebase-functions/v2/firestore");

admin.initializeApp();

const auth = admin.auth();
const db = admin.firestore();
const messaging = admin.messaging();
const REGION = "asia-southeast1";
const OWNER_EMAIL = "phunghuyhoahaikba@gmail.com";

function requireOwner(request) {
  const email = String(request.auth?.token?.email || "").trim().toLowerCase();
  const verified = request.auth?.token?.email_verified === true;

  if (!request.auth || !verified || email !== OWNER_EMAIL) {
    throw new HttpsError("permission-denied", "Chỉ tài khoản quản lý COC-LIO mới được thực hiện thao tác này.");
  }
}

exports.adminSyncSharedAccounts = onCall({ region: REGION }, async (request) => {
  requireOwner(request);

  const profiles = await db.collection("users")
    .where("dataSharingConsent", "==", true)
    .get();

  let removed = 0;
  let batch = db.batch();
  let pending = 0;

  for (const profile of profiles.docs) {
    try {
      await auth.getUser(profile.id);
    } catch (error) {
      if (error.code === "auth/user-not-found") {
        batch.delete(profile.ref);
        removed += 1;
        pending += 1;
      } else {
        throw error;
      }
    }

    if (pending >= 400) {
      await batch.commit();
      batch = db.batch();
      pending = 0;
    }
  }

  if (pending > 0) {
    await batch.commit();
  }

  return { removed };
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

  if (!profile.exists || profile.get("dataSharingConsent") !== true) {
    throw new HttpsError("not-found", "Không tìm thấy hồ sơ đã đồng ý chia sẻ của tài khoản này.");
  }

  const user = await auth.getUser(uid);
  if (String(user.email || "").trim().toLowerCase() === OWNER_EMAIL) {
    throw new HttpsError("failed-precondition", "Không thể xóa tài khoản chủ sở hữu COC-LIO.");
  }

  await auth.deleteUser(uid);
  await profileRef.delete();

  return { success: true, uid, email: user.email || "" };
});

exports.cleanupDeletedAuthUser = functionsV1.auth.user().onDelete(async (user) => {
  await db.collection("users").doc(user.uid).delete();
});

exports.notifyCommunityMessage = onDocumentCreated(
  { document: "community_messages/{messageId}", region: REGION },
  async (event) => {
    const snapshot = event.data;
    if (!snapshot) return;

    const data = snapshot.data() || {};
    const sender = String(data.displayName || "Thành viên").slice(0, 80);
    const text = String(data.text || "").trim();
    const type = String(data.type || "text");

    let content = text;
    if (!content && type === "image") content = "Đã chia sẻ một hình ảnh.";
    if (!content && type === "file") content = "Đã chia sẻ một tệp.";
    if (!content) return;

    await messaging.send({
      topic: "coc_lio_community",
      data: {
        type: "community_message",
        messageId: event.params.messageId,
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
