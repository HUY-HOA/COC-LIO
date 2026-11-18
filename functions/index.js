const admin = require("firebase-admin");
const { getStorage } = require("firebase-admin/storage");
const functionsV1 = require("firebase-functions/v1");
const { onCall, HttpsError } = require("firebase-functions/v2/https");

admin.initializeApp();

const auth = admin.auth();
const db = admin.firestore();
const REGION = "asia-southeast1";
const OWNER_UID = "T0PXzlCfddNZsWZzva2v1NCrWOf2";

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
  const verified = request.auth?.token?.email_verified === true;

  if (!request.auth || !verified || request.auth.uid !== OWNER_UID) {
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

// Account activity and device metadata collection are disabled.
exports.recordAccountActivity = onCall({ region: REGION }, async () => ({
  recorded: false,
  reason: "disabled"
}));
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
  if (uid === OWNER_UID) {
    throw new HttpsError("failed-precondition", "Không thể xóa tài khoản chủ sở hữu COC-LIO.");
  }

  await auth.deleteUser(uid);
  await deleteUserOwnedData(uid);

  return { success: true, uid, email: user.email || "" };
});

exports.cleanupDeletedAuthUser = functionsV1.auth.user().onDelete(async (user) => {
  await deleteUserOwnedData(user.uid);
});

