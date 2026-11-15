package com.huyhoa.coclio

import android.app.DownloadManager
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import android.widget.Toast
import android.os.Environment
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text as MaterialText
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEmotions
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import coil.compose.AsyncImage
import com.google.android.gms.tasks.Task
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.AggregateSource
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.Query
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import com.huyhoa.coclio.data.VillageSnapshot
import com.huyhoa.coclio.data.ReceivedSharedJsonRecord

private const val COMMUNITY_APK_MIME = "application/vnd.android.package-archive"
private const val COMMUNITY_MAX_EMBEDDED_BYTES = 350 * 1024
private const val COMMUNITY_MAX_INPUT_BYTES = 25 * 1024 * 1024

private data class CommunityMessage(
    val id: String,
    val uid: String,
    val displayName: String,
    val senderIsAdmin: Boolean,
    val photoUrl: String,
    val type: String,
    val text: String,
    val fileName: String,
    val storagePath: String,
    val downloadUrl: String,
    val contentType: String,
    val fileSize: Long,
    val contentData: String = "",
    val createdAt: Long,
    val sharedJsonId: String = ""
)

private data class CommunitySharedJson(
    val id: String,
    val uid: String,
    val displayName: String,
    val playerTag: String,
    val villageName: String,
    val timestamp: Long,
    val rawJson: String,
    val createdAt: Long
)

private data class CommunityRelease(
    val id: String,
    val versionName: String,
    val notes: String,
    val fileName: String,
    val storagePath: String,
    val downloadUrl: String,
    val fileSize: Long,
    val createdAt: Long
)

private data class CommunityArchiveItem(
    val id: String,
    val category: String,
    val displayName: String,
    val text: String,
    val fileName: String,
    val storagePath: String,
    val downloadUrl: String,
    val fileSize: Long,
    val createdAt: Long,
    val archivedAt: Long,
    val versionName: String,
    val notes: String,
    val contentType: String = "",
    val contentData: String = "",
    val sharedJsonId: String = "",
    val type: String = "",
    val sharedJsonRaw: String = "",
    val sharedJsonPlayerTag: String = "",
    val sharedJsonVillageName: String = ""
)

private fun communityArchiveCollection(category: String): String = when (category) {
    "image" -> "community_deleted_images"
    "file" -> "community_deleted_files"
    "apk" -> "app_update_archive"
    else -> "community_deleted_messages"
}

private val COMMUNITY_ARCHIVE_COLLECTIONS = listOf(
    "community_deleted_messages",
    "community_deleted_images",
    "community_deleted_files",
    "app_update_archive"
)

private data class CommunityStorageCounts(
    val activeMessages: Long,
    val activeJsons: Long,
    val activeApks: Long,
    val archivedMessages: Long,
    val archivedImages: Long,
    val archivedFiles: Long,
    val archivedApks: Long
) {
    val activeTotal: Long get() = activeMessages + activeJsons + activeApks
    val archivedTotal: Long get() = archivedMessages + archivedImages + archivedFiles + archivedApks
    val total: Long get() = activeTotal + archivedTotal
}

private suspend fun countCommunityDocuments(collectionName: String): Long {
    val result = FirebaseFirestore.getInstance()
        .collection(collectionName)
        .count()
        .get(AggregateSource.SERVER)
        .awaitCommunityResult()
    return result.count
}

private suspend fun loadCommunityStorageCounts(): CommunityStorageCounts = CommunityStorageCounts(
    activeMessages = countCommunityDocuments("community_messages"),
    activeJsons = countCommunityDocuments("community_shared_jsons"),
    activeApks = countCommunityDocuments("app_updates"),
    archivedMessages = countCommunityDocuments("community_deleted_messages"),
    archivedImages = countCommunityDocuments("community_deleted_images"),
    archivedFiles = countCommunityDocuments("community_deleted_files"),
    archivedApks = countCommunityDocuments("app_update_archive")
)

private suspend fun deleteArchiveDocuments(
    documents: List<com.google.firebase.firestore.DocumentSnapshot>
): Int {
    if (documents.isEmpty()) return 0
    val firestore = FirebaseFirestore.getInstance()
    for (chunk in documents.chunked(200)) {
        val batch = firestore.batch()
        val linkedJsonIds = mutableSetOf<String>()
        for (document in chunk) {
            document.getString("sharedJsonId").orEmpty()
                .takeIf { it.isNotBlank() }
                ?.let { linkedJsonIds.add(it) }
            batch.delete(document.reference)
        }
        linkedJsonIds.forEach { jsonId ->
            batch.delete(firestore.collection("community_shared_jsons").document(jsonId))
        }
        batch.commit().awaitCommunityResult()
    }
    return documents.size
}

private suspend fun deleteExpiredDocuments(
    collectionName: String,
    cutoff: Timestamp,
    deleteLinkedJson: Boolean = false
): Int {
    val firestore = FirebaseFirestore.getInstance()
    var deleted = 0
    while (true) {
        val expired = firestore.collection(collectionName)
            .whereLessThanOrEqualTo("createdAt", cutoff)
            .limit(200)
            .get()
            .awaitCommunityResult()
        if (expired.isEmpty) break
        for (chunk in expired.documents.chunked(200)) {
            val batch = firestore.batch()
            val linkedJsonIds = mutableSetOf<String>()
            for (document in chunk) {
                if (deleteLinkedJson) {
                    document.getString("sharedJsonId").orEmpty()
                        .takeIf { it.isNotBlank() }
                        ?.let { linkedJsonIds.add(it) }
                }
                batch.delete(document.reference)
            }
            linkedJsonIds.forEach { jsonId ->
                batch.delete(firestore.collection("community_shared_jsons").document(jsonId))
            }
            batch.commit().awaitCommunityResult()
            deleted += chunk.size
        }
    }
    return deleted
}

private suspend fun cleanupCommunityArchive(): Int {
    val firestore = FirebaseFirestore.getInstance()
    var removed = 0
    for (collectionName in COMMUNITY_ARCHIVE_COLLECTIONS) {
        while (true) {
            val expired = firestore.collection(collectionName)
                .whereLessThanOrEqualTo("expiresAt", Timestamp.now())
                .limit(200)
                .get()
                .awaitCommunityResult()
            if (expired.isEmpty) break
            removed += deleteArchiveDocuments(expired.documents)
        }
    }
    return removed
}

private suspend fun cleanupExpiredCommunityRecords(retentionDays: Int): Int {
    require(retentionDays in 1..1200)
    val cutoff = Timestamp(
        Date(System.currentTimeMillis() - TimeUnit.DAYS.toMillis(retentionDays.toLong()))
    )
    var removed = 0
    removed += deleteExpiredDocuments("community_messages", cutoff, deleteLinkedJson = true)
    removed += deleteExpiredDocuments("community_shared_jsons", cutoff)
    removed += deleteExpiredDocuments("app_updates", cutoff)
    for (collectionName in COMMUNITY_ARCHIVE_COLLECTIONS) {
        removed += deleteExpiredDocuments(
            collectionName,
            cutoff,
            deleteLinkedJson = collectionName == "community_deleted_messages"
        )
    }
    removed += cleanupCommunityArchive()
    return removed
}

private suspend fun deleteAllCommunityArchives(): Int {
    val firestore = FirebaseFirestore.getInstance()
    var deleted = 0
    for (collectionName in COMMUNITY_ARCHIVE_COLLECTIONS) {
        while (true) {
            val snapshot = firestore.collection(collectionName)
                .limit(200)
                .get()
                .awaitCommunityResult()
            if (snapshot.isEmpty) break
            deleted += deleteArchiveDocuments(snapshot.documents)
        }
    }
    return deleted
}

class CommunityArchiveCleanupWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {
    override suspend fun doWork(): Result {
        val user = FirebaseAuth.getInstance().currentUser ?: return Result.success()
        if (!user.isEmailVerified || !FirebaseAccountAdministration.isOwner(user.email)) {
            return Result.success()
        }
        return try {
            val settings = FirebaseFirestore.getInstance()
                .collection("admin")
                .document("community_archive_settings")
                .get()
                .awaitCommunityResult()
            val autoDeleteEnabled = settings.getBoolean("communityAutoDeleteEnabled")
                ?: settings.getBoolean("attachmentAutoDeleteEnabled")
                ?: false
            val retentionDays = (
                settings.getLong("communityAutoDeleteDays")
                    ?: settings.getLong("attachmentAutoDeleteDays")
                    ?: 30L
                ).toInt()
            if (autoDeleteEnabled && retentionDays in 1..1200) {
                cleanupExpiredCommunityRecords(retentionDays)
            } else {
                cleanupCommunityArchive()
            }
            Result.success()
        } catch (_: Exception) {
            Result.retry()
        }
    }
}

private suspend fun <T> Task<T>.awaitCommunityResult(): T =
    suspendCancellableCoroutine { continuation ->
        addOnCompleteListener { task ->
            if (!continuation.isActive) return@addOnCompleteListener
            if (task.isSuccessful) {
                continuation.resume(task.result)
            } else {
                continuation.resumeWithException(
                    task.exception ?: IllegalStateException("Không thể hoàn tất thao tác cộng đồng. Vui lòng thử lại.")
                )
            }
        }
    }


suspend fun deleteSharedJsonFromCommunity(jsonId: String) {
    require(jsonId.isNotBlank()) { "Không tìm thấy mã JSON." }
    val user = FirebaseAuth.getInstance().currentUser
        ?: throw IllegalStateException("Bạn cần đăng nhập Google trước.")
    val firestore = FirebaseFirestore.getInstance()
    val sharedReference = firestore.collection("community_shared_jsons").document(jsonId)
    val sharedDocument = sharedReference.get().awaitCommunityResult()
    require(sharedDocument.exists() && sharedDocument.getString("uid") == user.uid) {
        "Bạn chỉ có thể xóa JSON do chính mình chia sẻ."
    }
    val relatedMessages = firestore.collection("community_messages")
        .whereEqualTo("sharedJsonId", jsonId)
        .get()
        .awaitCommunityResult()
    val batch = firestore.batch()
    relatedMessages.documents
        .filter { it.getString("uid") == user.uid }
        .forEach { batch.delete(it.reference) }
    batch.delete(sharedReference)
    batch.commit().awaitCommunityResult()
}

suspend fun shareVillageJsonToCommunity(snapshot: VillageSnapshot): String {
    val signedInUser = FirebaseAuth.getInstance().currentUser
        ?: throw IllegalStateException("Bạn cần đăng nhập Google để chia sẻ JSON.")
    val rawJson = snapshot.rawJson
    val jsonSize = rawJson.toByteArray(Charsets.UTF_8).size
    require(rawJson.isNotBlank() && jsonSize in 1..700_000) {
        "JSON trống hoặc quá lớn. Dung lượng tối đa là 700 KB."
    }
    val firestore = FirebaseFirestore.getInstance()
    val senderIsAdmin = FirebaseAccountAdministration.isOwner(signedInUser.email)
    val displayName = if (senderIsAdmin) "AILIO" else signedInUser.displayName ?: signedInUser.email ?: "Thành viên"
    val sharedJson = firestore.collection("community_shared_jsons").add(
        mapOf(
            "uid" to signedInUser.uid,
            "displayName" to displayName.take(80),
            "playerTag" to snapshot.tag,
            "villageName" to snapshot.villageName.orEmpty().take(100),
            "timestamp" to snapshot.timestamp,
            "rawJson" to rawJson,
            "createdAt" to FieldValue.serverTimestamp()
        )
    ).awaitCommunityResult()
    firestore.collection("community_messages").add(
        communityMessagePayload(
            uid = signedInUser.uid,
            displayName = displayName,
            senderIsAdmin = senderIsAdmin,
            photoUrl = signedInUser.photoUrl?.toString().orEmpty(),
            type = "json",
            text = "Đã chia sẻ JSON ${snapshot.villageName?.takeIf { it.isNotBlank() } ?: snapshot.tag} • ${snapshot.tag}",
            fileName = "${snapshot.tag.removePrefix("#")}.json",
            storagePath = "",
            downloadUrl = "",
            contentType = "application/json",
            fileSize = jsonSize.toLong(),
            sharedJsonId = sharedJson.id
        )
    ).awaitCommunityResult()
    return "Đã chia sẻ JSON vào Cộng đồng."
}

private fun communityFileName(context: Context, uri: Uri): String {
    var result: String? = null
    context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use {
        if (it.moveToFirst()) result = it.getString(0)
    }
    return result?.takeIf { it.isNotBlank() } ?: "tep_dinh_kem"
}

private fun communityFileSize(context: Context, uri: Uri): Long {
    var result = -1L
    context.contentResolver.query(uri, arrayOf(OpenableColumns.SIZE), null, null, null)?.use {
        if (it.moveToFirst() && !it.isNull(0)) result = it.getLong(0)
    }
    if (result < 0L) {
        result = runCatching {
            context.contentResolver.openAssetFileDescriptor(uri, "r")?.use { it.length } ?: -1L
        }.getOrDefault(-1L)
    }
    return result
}

private fun communityMimeType(context: Context, uri: Uri, fileName: String): String {
    val extension = fileName.substringAfterLast('.', "").lowercase(Locale.ROOT)
    if (extension == "apk") return COMMUNITY_APK_MIME
    val supplied = context.contentResolver.getType(uri)?.lowercase(Locale.ROOT).orEmpty()
    if (supplied.startsWith("image/")) return supplied
    return when {
        supplied in setOf(
            "application/pdf",
            "text/plain",
            "application/zip",
            "application/msword",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            "application/vnd.ms-powerpoint",
            "application/vnd.openxmlformats-officedocument.presentationml.presentation",
            "application/vnd.ms-excel",
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
        ) -> supplied
        extension == "pdf" -> "application/pdf"
        extension in setOf("txt", "log", "csv") -> "text/plain"
        extension == "zip" -> "application/zip"
        extension == "doc" -> "application/msword"
        extension == "docx" -> "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
        extension == "ppt" -> "application/vnd.ms-powerpoint"
        extension == "pptx" -> "application/vnd.openxmlformats-officedocument.presentationml.presentation"
        extension == "xls" -> "application/vnd.ms-excel"
        extension == "xlsx" -> "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
        else -> supplied
    }
}

private data class CommunityAttachmentUpload(
    val fileName: String,
    val contentType: String,
    val bytes: ByteArray,
    val encoded: String
)

private suspend fun prepareCommunityAttachment(
    context: Context,
    uri: Uri,
    isImage: Boolean
): CommunityAttachmentUpload = withContext(Dispatchers.IO) {
    val originalName = communityFileName(context, uri)
    val originalBytes = context.contentResolver.openInputStream(uri)?.use { input ->
        val output = ByteArrayOutputStream()
        val buffer = ByteArray(8192)
        var total = 0
        while (true) {
            val count = input.read(buffer)
            if (count < 0) break
            total += count
            require(total <= COMMUNITY_MAX_INPUT_BYTES) {
                "Tệp gốc vượt quá giới hạn xử lý 25 MB."
            }
            output.write(buffer, 0, count)
        }
        output.toByteArray()
    } ?: throw IllegalStateException("Không thể đọc tệp đã chọn.")
    require(originalBytes.isNotEmpty()) { "Tệp đang trống." }

    val finalBytes: ByteArray
    val finalName: String
    val finalType: String
    if (isImage) {
        finalBytes = compressCommunityImage(originalBytes)
        finalName = originalName.substringBeforeLast('.', originalName).ifBlank { "anh" } + ".jpg"
        finalType = "image/jpeg"
    } else {
        val detectedType = communityMimeType(context, uri, originalName)
        require(detectedType in setOf(
            "application/pdf",
            "text/plain",
            "application/zip",
            "application/msword",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            "application/vnd.ms-powerpoint",
            "application/vnd.openxmlformats-officedocument.presentationml.presentation",
            "application/vnd.ms-excel",
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
            COMMUNITY_APK_MIME
        )) { "Định dạng tệp này chưa được hỗ trợ." }
        require(originalBytes.size <= COMMUNITY_MAX_EMBEDDED_BYTES) {
            "Tệp quá lớn. Trên gói miễn phí, mỗi tệp tối đa 350 KB."
        }
        finalBytes = originalBytes
        finalName = originalName
        finalType = detectedType
    }

    require(finalBytes.size in 1..COMMUNITY_MAX_EMBEDDED_BYTES) {
        "Không thể nén tệp xuống giới hạn 350 KB. Hãy chọn tệp nhỏ hơn."
    }
    CommunityAttachmentUpload(
        fileName = finalName.take(180),
        contentType = finalType,
        bytes = finalBytes,
        encoded = Base64.encodeToString(finalBytes, Base64.NO_WRAP)
    )
}

private fun compressCommunityImage(source: ByteArray): ByteArray {
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeByteArray(source, 0, source.size, bounds)
    require(bounds.outWidth > 0 && bounds.outHeight > 0) { "Ảnh không hợp lệ." }
    var sampleSize = 1
    while (maxOf(bounds.outWidth, bounds.outHeight) / sampleSize > 1600) sampleSize *= 2
    var bitmap = BitmapFactory.decodeByteArray(
        source,
        0,
        source.size,
        BitmapFactory.Options().apply { inSampleSize = sampleSize }
    ) ?: throw IllegalArgumentException("Không thể đọc ảnh này.")
    try {
        val largestSide = maxOf(bitmap.width, bitmap.height)
        if (largestSide > 1280) {
            val scale = 1280f / largestSide.toFloat()
            val resized = Bitmap.createScaledBitmap(
                bitmap,
                (bitmap.width * scale).toInt().coerceAtLeast(1),
                (bitmap.height * scale).toInt().coerceAtLeast(1),
                true
            )
            if (resized !== bitmap) bitmap.recycle()
            bitmap = resized
        }
        val output = ByteArrayOutputStream()
        var quality = 85
        repeat(12) {
            output.reset()
            bitmap.compress(Bitmap.CompressFormat.JPEG, quality, output)
            if (output.size() <= COMMUNITY_MAX_EMBEDDED_BYTES) return output.toByteArray()
            if (quality > 50) {
                quality -= 10
            } else {
                val resized = Bitmap.createScaledBitmap(
                    bitmap,
                    (bitmap.width * 0.8f).toInt().coerceAtLeast(1),
                    (bitmap.height * 0.8f).toInt().coerceAtLeast(1),
                    true
                )
                if (resized === bitmap) return@repeat
                bitmap.recycle()
                bitmap = resized
                quality = 75
            }
        }
        throw IllegalArgumentException("Ảnh quá lớn sau khi nén. Hãy chọn ảnh nhỏ hơn.")
    } finally {
        if (!bitmap.isRecycled) bitmap.recycle()
    }
}

private suspend fun sendCommunityAttachment(
    context: Context,
    uri: Uri,
    isImage: Boolean
): String {
    val signedInUser = FirebaseAuth.getInstance().currentUser
        ?: throw IllegalStateException("Bạn cần đăng nhập Google để gửi tệp.")
    if (!isImage && !FirebaseAccountAdministration.isOwner(signedInUser.email)) {
        throw IllegalStateException("Chỉ tài khoản quản lý mới được gửi tệp.")
    }
    val attachment = prepareCommunityAttachment(context, uri, isImage)
    FirebaseFirestore.getInstance().collection("community_messages").add(
        communityMessagePayload(
            uid = signedInUser.uid,
            displayName = signedInUser.displayName ?: signedInUser.email ?: "Thành viên",
            senderIsAdmin = FirebaseAccountAdministration.isOwner(signedInUser.email),
            photoUrl = signedInUser.photoUrl?.toString().orEmpty(),
            type = if (isImage) "image" else "file",
            text = "",
            fileName = attachment.fileName,
            storagePath = "",
            downloadUrl = "",
            contentType = attachment.contentType,
            fileSize = attachment.bytes.size.toLong(),
            contentData = attachment.encoded
        )
    ).awaitCommunityResult()
    return if (isImage) "Đã gửi ảnh." else "Đã gửi tệp."
}

private fun communityTime(timestamp: Long): String =
    if (timestamp <= 0L) "Vừa gửi" else
        SimpleDateFormat("dd/MM · HH:mm", Locale("vi", "VN")).format(Date(timestamp))

private fun communitySafeFileName(value: String): String =
    value.replace(Regex("[^A-Za-z0-9._-]"), "_").take(90).ifBlank { "tep_tai_xuong" }

private fun downloadCommunityFile(context: Context, url: String, fileName: String) {
    val request = DownloadManager.Request(Uri.parse(url))
        .setTitle(fileName)
        .setMimeType(if (fileName.endsWith(".apk", ignoreCase = true)) COMMUNITY_APK_MIME else "application/octet-stream")
        .setDescription("Đang tải từ COC-LIO")
        .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
        .setDestinationInExternalPublicDir(
            Environment.DIRECTORY_DOWNLOADS,
            communitySafeFileName(fileName)
        )
    val manager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
    manager.enqueue(request)
}

@Composable
fun CommunityScreen(
    isGoogleSignedIn: Boolean,
    availableJsons: List<VillageSnapshot>,
    receivedJsons: List<ReceivedSharedJsonRecord>,
    onDeleteReceivedJson: (String) -> Unit,
    initialSection: Int = 0,
    onInitialSectionConsumed: () -> Unit = {},
    onImportJson: (String, String, String, String, String, Long, String) -> Unit,
    onOpenSettings: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val user = FirebaseAuth.getInstance().currentUser?.takeIf { isGoogleSignedIn }
    val isOwner = FirebaseAccountAdministration.isOwner(user?.email)
    var selectedSection by remember { mutableStateOf(0) }
    var sharedJsonTab by remember { mutableStateOf("sent") }
    LaunchedEffect(initialSection) {
        if (initialSection == 3) {
            selectedSection = 3
            onInitialSectionConsumed()
        }
    }
    var messages by remember { mutableStateOf<List<CommunityMessage>>(emptyList()) }
    var sharedJsons by remember { mutableStateOf<List<CommunitySharedJson>>(emptyList()) }
    var releases by remember { mutableStateOf<List<CommunityRelease>>(emptyList()) }
    var loadingMessages by remember { mutableStateOf(true) }
    var loadingSharedJsons by remember { mutableStateOf(true) }
    var loadingReleases by remember { mutableStateOf(true) }
    var messageError by remember { mutableStateOf<String?>(null) }
    var releaseError by remember { mutableStateOf<String?>(null) }
    var statusMessage by remember { mutableStateOf<String?>(null) }
    var draft by remember { mutableStateOf("") }
    var showEmojiPicker by remember { mutableStateOf(false) }
    var showJsonPicker by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    val jsonOptions = remember(availableJsons) {
        availableJsons.distinctBy { it.tag to it.timestamp }.sortedByDescending { it.timestamp }
    }
    var deleteMessage by remember { mutableStateOf<CommunityMessage?>(null) }
    var deleteRelease by remember { mutableStateOf<CommunityRelease?>(null) }
    var deleteArchiveItem by remember { mutableStateOf<CommunityArchiveItem?>(null) }
    var deleteSharedJson by remember { mutableStateOf<CommunitySharedJson?>(null) }
    var deleteReceivedJson by remember { mutableStateOf<ReceivedSharedJsonRecord?>(null) }
    var archiveItems by remember { mutableStateOf<List<CommunityArchiveItem>>(emptyList()) }
    var loadingArchive by remember { mutableStateOf(false) }
    var archiveError by remember { mutableStateOf<String?>(null) }
    var archiveCategory by remember { mutableStateOf("text") }
    var communityAutoDeleteEnabled by remember { mutableStateOf(false) }
    var communityAutoDeleteDays by remember { mutableStateOf("30") }
    var communityStorageCounts by remember { mutableStateOf<CommunityStorageCounts?>(null) }
    var communityStorageCountError by remember { mutableStateOf<String?>(null) }
    var communityCountRefreshKey by remember { mutableStateOf(0) }
    var showDeleteAllArchiveConfirmation by remember { mutableStateOf(false) }
    val messageListState = rememberLazyListState()

    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            scope.launch {
                busy = true
                statusMessage = null
                try {
                    statusMessage = sendCommunityAttachment(context, uri, true)
                } catch (error: Exception) {
                    statusMessage = error.message ?: "Không thể gửi ảnh."
                } finally {
                    busy = false
                }
            }
        }
    }

    val filePicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            scope.launch {
                busy = true
                statusMessage = null
                try {
                    statusMessage = sendCommunityAttachment(context, uri, false)
                } catch (error: Exception) {
                    statusMessage = error.message ?: "Không thể gửi tệp."
                } finally {
                    busy = false
                }
            }
        }
    }

    fun receiveSharedJson(jsonId: String) {
        if (jsonId.isBlank()) {
            statusMessage = "Không tìm thấy mã JSON được chia sẻ."
            return
        }
        scope.launch {
            busy = true
            statusMessage = null
            try {
                val document = FirebaseFirestore.getInstance()
                    .collection("community_shared_jsons")
                    .document(jsonId)
                    .get()
                    .awaitCommunityResult()
                val rawJson = document.getString("rawJson").orEmpty()
                require(rawJson.isNotBlank()) { "JSON chia sẻ không còn dữ liệu." }
                require(rawJson.toByteArray(Charsets.UTF_8).size <= 700_000) { "JSON chia sẻ vượt quá giới hạn cho phép." }
                onImportJson(
                    rawJson,
                    document.getString("uid").orEmpty(),
                    document.getString("displayName").orEmpty(),
                    document.getString("playerTag").orEmpty(),
                    document.getString("villageName").orEmpty(),
                    document.getLong("timestamp") ?: 0L,
                    jsonId
                )
                statusMessage = "Đã nhận JSON. Bản nhận được đã được lưu riêng trong Kho JSON; bản nhập vẫn có trong lịch sử."
            } catch (error: Exception) {
                statusMessage = error.message ?: "Không thể nhận JSON được chia sẻ."
            } finally {
                busy = false
            }
        }
    }

    DisposableEffect(user?.uid, selectedSection, archiveCategory, isOwner) {
        if (user == null) {
            messages = emptyList()
            sharedJsons = emptyList()
            releases = emptyList()
            archiveItems = emptyList()
            loadingMessages = false
            loadingSharedJsons = false
            loadingReleases = false
            loadingArchive = false
            onDispose { }
        } else {
            val firestore = FirebaseFirestore.getInstance()
            val messageRegistration = firestore.collection("community_messages")
                .orderBy("createdAt", Query.Direction.ASCENDING)
                .limitToLast(50)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        messageError = error.message ?: "Không thể tải tin nhắn."
                        loadingMessages = false
                    } else {
                        messages = snapshot?.documents.orEmpty().mapNotNull { document ->
                            val uid = document.getString("uid") ?: return@mapNotNull null
                            val senderIsAdmin = document.getBoolean("senderIsAdmin") ?: false
                            CommunityMessage(
                                id = document.id,
                                uid = uid,
                                displayName = if (senderIsAdmin) "AILIO" else document.getString("displayName").orEmpty().ifBlank { "Thành viên" },
                                senderIsAdmin = senderIsAdmin,
                                photoUrl = if (senderIsAdmin) "" else document.getString("photoUrl").orEmpty(),
                                type = document.getString("type").orEmpty(),
                                text = document.getString("text").orEmpty(),
                                fileName = document.getString("fileName").orEmpty(),
                                storagePath = document.getString("storagePath").orEmpty(),
                                downloadUrl = document.getString("downloadUrl").orEmpty(),
                                contentType = document.getString("contentType").orEmpty(),
                                fileSize = document.getLong("fileSize") ?: 0L,
                                contentData = document.getString("contentData").orEmpty(),
                                createdAt = document.getTimestamp("createdAt")?.toDate()?.time ?: 0L,
                                sharedJsonId = document.getString("sharedJsonId").orEmpty()
                            )
                        }
                        messageError = null
                        loadingMessages = false
                    }
                }
            val sharedJsonRegistration = firestore.collection("community_shared_jsons")
                .whereEqualTo("uid", user.uid)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        statusMessage = error.message ?: "Không thể tải JSON cộng đồng."
                        loadingSharedJsons = false
                    } else {
                        sharedJsons = snapshot?.documents.orEmpty().mapNotNull { document ->
                            val rawJson = document.getString("rawJson") ?: return@mapNotNull null
                            CommunitySharedJson(
                                id = document.id,
                                uid = document.getString("uid").orEmpty(),
                                displayName = document.getString("displayName").orEmpty().ifBlank { "Thành viên" },
                                playerTag = document.getString("playerTag").orEmpty(),
                                villageName = document.getString("villageName").orEmpty(),
                                timestamp = document.getLong("timestamp") ?: 0L,
                                rawJson = rawJson,
                                createdAt = document.getTimestamp("createdAt")?.toDate()?.time ?: 0L
                            )
                        }.sortedByDescending { it.createdAt }
                        loadingSharedJsons = false
                    }
                }
            val releaseRegistration = firestore.collection("app_updates")
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .limit(30)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        releaseError = error.message ?: "Không thể tải danh sách cập nhật."
                        loadingReleases = false
                    } else {
                        releases = snapshot?.documents.orEmpty().mapNotNull { document ->
                            val name = document.getString("versionName") ?: return@mapNotNull null
                            CommunityRelease(
                                id = document.id,
                                versionName = name,
                                notes = document.getString("notes").orEmpty(),
                                fileName = document.getString("fileName").orEmpty(),
                                storagePath = document.getString("storagePath").orEmpty(),
                                downloadUrl = document.getString("downloadUrl").orEmpty(),
                                fileSize = document.getLong("fileSize") ?: 0L,
                                createdAt = document.getTimestamp("createdAt")?.toDate()?.time ?: 0L
                            )
                        }
                        releaseError = null
                        loadingReleases = false
                    }
                }
            val archiveRegistrations = mutableListOf<com.google.firebase.firestore.ListenerRegistration>()
            if (isOwner && selectedSection == 2) {
                loadingArchive = true
                archiveError = null
                fun mapArchiveDocuments(
                    documents: List<com.google.firebase.firestore.DocumentSnapshot>,
                    category: String
                ): List<CommunityArchiveItem> = documents.map { document ->
                    CommunityArchiveItem(
                        id = document.id,
                        category = category,
                        displayName = document.getString("displayName").orEmpty(),
                        text = document.getString("text").orEmpty(),
                        fileName = document.getString("fileName").orEmpty(),
                        storagePath = document.getString("storagePath").orEmpty(),
                        downloadUrl = document.getString("downloadUrl").orEmpty(),
                        fileSize = document.getLong("fileSize") ?: 0L,
                        createdAt = document.getTimestamp("createdAt")?.toDate()?.time ?: 0L,
                        archivedAt = document.getTimestamp("archivedAt")?.toDate()?.time ?: 0L,
                        versionName = document.getString("versionName").orEmpty(),
                        notes = document.getString("notes").orEmpty(),
                        contentType = document.getString("contentType").orEmpty(),
                        contentData = document.getString("contentData").orEmpty(),
                        sharedJsonId = document.getString("sharedJsonId").orEmpty(),
                        type = document.getString("type").orEmpty(),
                        sharedJsonRaw = (document.get("sharedJsonData") as? Map<*, *>)?.get("rawJson") as? String ?: "",
                        sharedJsonPlayerTag = (document.get("sharedJsonData") as? Map<*, *>)?.get("playerTag") as? String ?: "",
                        sharedJsonVillageName = (document.get("sharedJsonData") as? Map<*, *>)?.get("villageName") as? String ?: ""
                    )
                }

                if (archiveCategory == "file") {
                    var filesLoaded = false
                    var apksLoaded = false
                    var archivedFiles = emptyList<CommunityArchiveItem>()
                    var archivedApks = emptyList<CommunityArchiveItem>()

                    archiveRegistrations += firestore.collection("community_deleted_files")
                        .orderBy("archivedAt", Query.Direction.DESCENDING)
                        .addSnapshotListener { snapshot, error ->
                            if (error != null) {
                                archiveError = error.message ?: "Không thể tải kho tệp đã gỡ."
                                loadingArchive = false
                            } else {
                                archivedFiles = mapArchiveDocuments(snapshot?.documents.orEmpty(), "file")
                                archiveItems = (archivedFiles + archivedApks).sortedByDescending { it.archivedAt }
                                filesLoaded = true
                                if (filesLoaded && apksLoaded) loadingArchive = false
                            }
                        }

                    archiveRegistrations += firestore.collection("app_update_archive")
                        .orderBy("archivedAt", Query.Direction.DESCENDING)
                        .addSnapshotListener { snapshot, error ->
                            if (error != null) {
                                archiveError = error.message ?: "Không thể tải kho APK đã gỡ."
                                loadingArchive = false
                            } else {
                                archivedApks = mapArchiveDocuments(snapshot?.documents.orEmpty(), "apk")
                                archiveItems = (archivedFiles + archivedApks).sortedByDescending { it.archivedAt }
                                apksLoaded = true
                                if (filesLoaded && apksLoaded) loadingArchive = false
                            }
                        }
                } else {
                    archiveRegistrations += firestore.collection(communityArchiveCollection(archiveCategory))
                        .orderBy("archivedAt", Query.Direction.DESCENDING)
                        .addSnapshotListener { snapshot, error ->
                            if (error != null) {
                                archiveError = error.message ?: "Không thể tải kho đã gỡ."
                                loadingArchive = false
                            } else {
                                archiveItems = mapArchiveDocuments(snapshot?.documents.orEmpty(), archiveCategory)
                                archiveError = null
                                loadingArchive = false
                            }
                        }
                }
            } else {
                archiveItems = emptyList()
            }
            onDispose {
                messageRegistration.remove()
                sharedJsonRegistration.remove()
                releaseRegistration.remove()
                archiveRegistrations.forEach { it.remove() }
            }
        }
    }

    LaunchedEffect(user?.uid, isOwner) {
        if (user != null && isOwner) {
            try {
                val settings = FirebaseFirestore.getInstance()
                    .collection("admin")
                    .document("community_archive_settings")
                    .get()
                    .awaitCommunityResult()
                communityAutoDeleteEnabled = settings.getBoolean("communityAutoDeleteEnabled")
                    ?: settings.getBoolean("attachmentAutoDeleteEnabled")
                    ?: false
                communityAutoDeleteDays = (
                    settings.getLong("communityAutoDeleteDays")
                        ?: settings.getLong("attachmentAutoDeleteDays")
                        ?: 30L
                    ).toString()
                val request = PeriodicWorkRequestBuilder<CommunityArchiveCleanupWorker>(1, TimeUnit.DAYS).build()
                WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                    "community_archive_cleanup",
                    ExistingPeriodicWorkPolicy.KEEP,
                    request
                )
                val retentionDays = communityAutoDeleteDays.toIntOrNull()
                val removed = if (communityAutoDeleteEnabled && retentionDays != null && retentionDays in 1..1200) {
                    cleanupExpiredCommunityRecords(retentionDays)
                } else {
                    cleanupCommunityArchive()
                }
                communityCountRefreshKey += 1
                if (removed > 0) statusMessage = "Đã tự xóa $removed bản ghi cộng đồng/kho đã gỡ hết hạn."
            } catch (error: Exception) {
                statusMessage = error.message ?: "Không thể kiểm tra thời hạn lưu trữ."
            }
        }
    }

    LaunchedEffect(user?.uid, selectedSection, communityCountRefreshKey) {
        if (user != null && isOwner && selectedSection == 2) {
            communityStorageCountError = null
            try {
                communityStorageCounts = loadCommunityStorageCounts()
            } catch (error: Exception) {
                communityStorageCounts = null
                communityStorageCountError = error.message ?: "Không thể đọc số lượng bản ghi trên Firestore."
            }
        }
    }

    LaunchedEffect(messages.lastOrNull()?.id) {
        if (messages.isNotEmpty()) messageListState.animateScrollToItem(messages.lastIndex)
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(9.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text("CỘNG ĐỒNG COC-LIO", color = MaterialTheme.colorScheme.primary, fontSize = 15.sp, fontWeight = FontWeight.Black)
            Text("Trò chuyện, chia sẻ hình ảnh và cập nhật COC-LIO cùng mọi thành viên.", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp)
        }
        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = selectedSection == 0,
                onClick = { selectedSection = 0; statusMessage = null },
                label = { Text("TRÒ CHUYỆN", fontSize = 10.sp) }
            )
            FilterChip(
                selected = selectedSection == 1,
                onClick = { selectedSection = 1; statusMessage = null },
                label = { Text("CẬP NHẬT APK", fontSize = 10.sp) }
            )
            FilterChip(
                selected = selectedSection == 3,
                onClick = { selectedSection = 3; statusMessage = null },
                label = { Text("CHIA SẺ", fontSize = 10.sp) }
            )
            if (isOwner) {
                FilterChip(
                    selected = selectedSection == 2,
                    onClick = { selectedSection = 2; statusMessage = null },
                    label = { Text("KHO ĐÃ GỠ", fontSize = 10.sp) }
                )
            }
        }
        statusMessage?.let {
            Card(
                modifier = Modifier.fillMaxWidth().ledRgbBorder(cornerRadiusDp = 12f),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
            ) {
                Text(it, modifier = Modifier.padding(10.dp), color = MaterialTheme.colorScheme.onSecondaryContainer, fontSize = 10.sp)
            }
        }
        if (user == null) {
            Card(
                modifier = Modifier.fillMaxWidth().ledRgbBorder(cornerRadiusDp = 12f),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("ĐĂNG NHẬP ĐỂ THAM GIA", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Black)
                    Text("Đăng nhập Google để trò chuyện và chia sẻ cùng cộng đồng.", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp, lineHeight = 16.sp)
                    Button(onClick = onOpenSettings) { Text("ĐĂNG NHẬP") }
                }
            }
        } else if (selectedSection == 0) {
            if (loadingMessages) {
                Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else {
                messageError?.let {
                    Text(it, color = MaterialTheme.colorScheme.error, fontSize = 10.sp)
                }
                LazyColumn(
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    state = messageListState,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (messages.isEmpty()) {
                        item {
                            Text("Chưa có tin nhắn. Hãy gửi lời chào đầu tiên!", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
                        }
                    }
                    items(messages, key = { it.id }) { message ->
                        CommunityMessageCard(
                            message = message,
                            currentUid = user.uid,
                            isOwner = isOwner,
                            onDownload = { url, name -> downloadCommunityFile(context, url, name) },
                            onUseJson = { jsonId -> receiveSharedJson(jsonId) },
                            onDelete = { deleteMessage = message }
                        )
                    }
                }
            }
            Card(
                modifier = Modifier.fillMaxWidth().ledRgbBorder(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(
                    modifier = Modifier.padding(9.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        OutlinedButton(onClick = { imagePicker.launch("image/*") }, enabled = !busy) {
                            Text("🖼 ẢNH", fontSize = 10.sp)
                        }
                        OutlinedButton(
                            onClick = {
                                if (jsonOptions.isEmpty()) {
                                    statusMessage = "Kho JSON chưa có dữ liệu. Hãy nhập JSON trước khi chia sẻ."
                                } else {
                                    showJsonPicker = true
                                }
                            },
                            enabled = !busy
                        ) {
                            Text("📦 GỬI JSON", fontSize = 10.sp)
                        }
                        if (isOwner) {
                            OutlinedButton(onClick = { filePicker.launch(arrayOf("*/*")) }, enabled = !busy) {
                                Text("📎 TỆP", fontSize = 10.sp)
                            }
                        }
                    }
                    if (showEmojiPicker) {
                        val chatEmojis = listOf(
                            "😀", "😃", "😄", "😁", "😆", "😅", "🤣", "😂", "🙂", "🙃",
                            "😉", "😊", "😇", "🥰", "😍", "🤩", "😘", "😗", "😚", "😋",
                            "😛", "😜", "🤪", "😎", "🤓", "🧐", "🤗", "🤭", "🤫", "🤔",
                            "🫡", "🤐", "🤨", "😐", "😑", "😶", "🙄", "😏", "😣", "😥",
                            "😮", "😯", "😪", "😫", "🥱", "😴", "🤤", "😵", "🤯", "🥳",
                            "😤", "😭", "😢", "😱", "😡", "🤬", "😈", "👻", "💀", "💩",
                            "🤖", "👋", "🤚", "✋", "🖖", "👌", "🤌", "🤏", "✌️", "🤞",
                            "🫶", "🤟", "🤘", "🤙", "👈", "👉", "👆", "👇", "☝️", "👍",
                            "👎", "✊", "👊", "👏", "🙌", "👐", "🤝", "🙏", "💪", "❤️",
                            "🧡", "💛", "💚", "💙", "💜", "🖤", "🤍", "🤎", "💔", "💕",
                            "💞", "💖", "💗", "💓", "💘", "💝", "💟", "🔥", "✨", "⭐",
                            "🌟", "💫", "⚡", "💥", "🎉", "🎊", "🏆", "🥇", "🎮", "🎯",
                            "🚀", "💎", "🛡️", "⚔️", "🏰", "🐉", "👑", "💯", "✅", "❌",
                            "⭕", "❗", "❓", "💬", "📌", "🎁", "🍀", "🌈", "☀️"
                        )
                        Column(
                            modifier = Modifier.heightIn(max = 170.dp).verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            chatEmojis.chunked(10).forEach { emojiRow ->
                                Row(
                                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                                ) {
                                    emojiRow.forEach { emoji ->
                                        TextButton(
                                            onClick = {
                                                if (draft.length + emoji.length <= 2000) draft += emoji
                                                showEmojiPicker = false
                                            }
                                        ) {
                                            Text(emoji, fontSize = 21.sp)
                                        }
                                    }
                                }
                            }
                        }
                    }
                    Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        IconButton(
                            onClick = { showEmojiPicker = !showEmojiPicker },
                            enabled = !busy
                        ) {
                            Icon(
                                imageVector = Icons.Filled.EmojiEmotions,
                                contentDescription = if (showEmojiPicker) "Đóng bảng emoji" else "Mở bảng emoji",
                                tint = if (showEmojiPicker) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        OutlinedTextField(
                            value = draft,
                            onValueChange = { if (it.length <= 2000) draft = it },
                            modifier = Modifier.weight(1f),
                            placeholder = { Text("Viết tin nhắn…", fontSize = 11.sp) },
                            maxLines = 4,
                            textStyle = MaterialTheme.typography.bodyMedium
                        )
                        Button(
                            onClick = {
                                val signedInUser = FirebaseAuth.getInstance().currentUser
                                val text = draft.trim()
                                if (signedInUser != null && text.isNotEmpty()) {
                                    scope.launch {
                                        busy = true
                                        statusMessage = null
                                        try {
                                            FirebaseFirestore.getInstance().collection("community_messages").add(
                                                communityMessagePayload(
                                                    uid = signedInUser.uid,
                                                    displayName = signedInUser.displayName ?: signedInUser.email ?: "Thành viên",
                                                    senderIsAdmin = FirebaseAccountAdministration.isOwner(signedInUser.email),
                                                    photoUrl = signedInUser.photoUrl?.toString().orEmpty(),
                                                    type = "text",
                                                    text = text,
                                                    fileName = "",
                                                    storagePath = "",
                                                    downloadUrl = "",
                                                    contentType = "",
                                                    fileSize = 0L
                                                )
                                            ).awaitCommunityResult()
                                            draft = ""
                                        } catch (error: Exception) {
                                            statusMessage = error.message ?: "Không gửi được tin nhắn."
                                        } finally {
                                            busy = false
                                        }
                                    }
                                }
                            },
                            enabled = !busy && draft.trim().isNotEmpty(),
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 13.dp, vertical = 12.dp)
                        ) {
                            if (busy) CircularProgressIndicator(Modifier.size(15.dp), strokeWidth = 2.dp)
                            else Text("GỬI", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    if (busy) {
                        Text("Đang gửi…", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 9.sp)
                    }
                }
            }
        } else if (selectedSection == 2 && isOwner) {
            Column(
                modifier = Modifier.fillMaxWidth().weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Card(
                    modifier = Modifier.fillMaxWidth().ledRgbBorder(cornerRadiusDp = 12f),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                        Text("TỰ ĐỘNG XÓA TOÀN BỘ CỘNG ĐỒNG", color = MaterialTheme.colorScheme.primary, fontSize = 11.sp, fontWeight = FontWeight.Black)
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            FilterChip(
                                selected = !communityAutoDeleteEnabled,
                                onClick = { communityAutoDeleteEnabled = false },
                                label = { Text("TẮT", fontSize = 9.sp) }
                            )
                            FilterChip(
                                selected = communityAutoDeleteEnabled,
                                onClick = { communityAutoDeleteEnabled = true },
                                label = { Text("BẬT", fontSize = 9.sp) }
                            )
                        }
                        OutlinedTextField(
                            value = communityAutoDeleteDays,
                            onValueChange = { communityAutoDeleteDays = it.filter(Char::isDigit).take(4) },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            label = { Text("Số ngày giữ nội dung") }
                        )
                        Text(
                            if (communityAutoDeleteEnabled) {
                                "Tin nhắn, JSON, ảnh, tệp, APK và bản đã gỡ sẽ bị xóa khỏi Firestore khi đủ số ngày tính từ lúc đăng. Ứng dụng quản lý sẽ tự dọn định kỳ; thời điểm xóa có thể trễ khi máy ngoại tuyến hoặc Android hạn chế chạy nền."
                            } else {
                                "Đang tắt tự động xóa. Nội dung trong Cộng đồng và Kho đã gỡ sẽ không bị xóa theo thời hạn mới."
                            },
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 9.sp
                        )
                        Button(
                            onClick = {
                                val days = communityAutoDeleteDays.toIntOrNull()
                                if (communityAutoDeleteEnabled && (days == null || days !in 1..1200)) {
                                    statusMessage = "Nhập số ngày tự động xóa từ 1 đến 1200."
                                } else {
                                    scope.launch {
                                        busy = true
                                        try {
                                            FirebaseFirestore.getInstance()
                                                .collection("admin")
                                                .document("community_archive_settings")
                                                .set(
                                                    mapOf(
                                                        "communityAutoDeleteEnabled" to communityAutoDeleteEnabled,
                                                        "communityAutoDeleteDays" to (days ?: 30),
                                                        "attachmentAutoDeleteEnabled" to communityAutoDeleteEnabled,
                                                        "attachmentAutoDeleteDays" to (days ?: 30),
                                                        "updatedAt" to FieldValue.serverTimestamp()
                                                    ),
                                                    SetOptions.merge()
                                                )
                                                .awaitCommunityResult()
                                            val removed = if (communityAutoDeleteEnabled && days != null) {
                                                cleanupExpiredCommunityRecords(days)
                                            } else {
                                                cleanupCommunityArchive()
                                            }
                                            communityCountRefreshKey += 1
                                            statusMessage = if (communityAutoDeleteEnabled) {
                                                "Đã lưu. Tự động xóa toàn bộ nội dung sau $days ngày; đã dọn $removed bản ghi hết hạn."
                                            } else {
                                                "Đã lưu. Tự động xóa toàn bộ cộng đồng đã tắt; đã dọn $removed mục lưu trữ đến hạn cũ."
                                            }
                                        } catch (error: Exception) {
                                            statusMessage = error.message ?: "Không thể lưu thiết lập tự động xóa."
                                        } finally {
                                            busy = false
                                        }
                                    }
                                }
                            },
                            enabled = !busy,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("LƯU THIẾT LẬP")
                        }
                    }
                }
                Card(
                    modifier = Modifier.fillMaxWidth().ledRgbBorder(cornerRadiusDp = 12f),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                        Text("BẢN GHI CÒN LƯU TRONG FIREBASE", color = MaterialTheme.colorScheme.primary, fontSize = 11.sp, fontWeight = FontWeight.Black)
                        if (communityStorageCounts != null) {
                            val counts = communityStorageCounts!!
                            Text("TỔNG CỘNG: ${counts.total} BẢN GHI", color = MaterialTheme.colorScheme.onSurface, fontSize = 13.sp, fontWeight = FontWeight.Black)
                            Text("Cộng đồng: ${counts.activeTotal}  •  Kho đã gỡ: ${counts.archivedTotal}", color = MaterialTheme.colorScheme.onSurface, fontSize = 10.sp)
                            Text(
                                "Cộng đồng: ${counts.activeMessages} mục tin nhắn/ảnh/tệp, ${counts.activeJsons} JSON riêng, ${counts.activeApks} APK",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 9.sp
                            )
                            Text(
                                "Kho đã gỡ: ${counts.archivedMessages} tin nhắn, ${counts.archivedImages} ảnh, ${counts.archivedFiles + counts.archivedApks} tệp và APK",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 9.sp
                            )
                        } else if (communityStorageCountError != null) {
                            Text(communityStorageCountError!!, color = MaterialTheme.colorScheme.error, fontSize = 9.sp)
                        } else {
                            Text("Đang kiểm tra dữ liệu trên máy chủ…", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 9.sp)
                        }
                        OutlinedButton(
                            onClick = { communityCountRefreshKey += 1 },
                            enabled = !busy,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("LÀM MỚI SỐ LIỆU")
                        }
                        Text(
                            "Bộ đếm lấy tổng số tài liệu thật từ các mục Cộng đồng và Kho đã gỡ trên Firestore; không tính hồ sơ, cài đặt và dữ liệu khác của ứng dụng.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 8.sp
                        )
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    listOf(
                        "text" to "TIN NHẮN",
                        "image" to "HÌNH ẢNH",
                        "file" to "TỆP"
                    ).forEach { (category, label) ->
                        FilterChip(
                            selected = archiveCategory == category,
                            onClick = { archiveCategory = category },
                            label = { Text(label, fontSize = 9.sp) }
                        )
                    }
                }
                Button(
                    onClick = { showDeleteAllArchiveConfirmation = true },
                    enabled = !busy && (communityStorageCounts?.archivedTotal ?: 0L) > 0L,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("XÓA VĨNH VIỄN TẤT CẢ")
                }
                if (loadingArchive) {
                    Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                } else {
                    archiveError?.let {
                        Text(it, color = MaterialTheme.colorScheme.error, fontSize = 10.sp)
                    }
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth().weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (archiveItems.isEmpty()) {
                            item {
                                Text("Chưa có dữ liệu trong mục này.", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
                            }
                        }
                        items(archiveItems, key = { it.id }) { item ->
                            CommunityArchiveCard(
                                item = item,
                                onDownload = { url, name -> downloadCommunityFile(context, url, name) },
                                onDelete = { deleteArchiveItem = item }
                            )
                        }
                    }
                }
            }
        } else if (selectedSection == 3) {
            Column(
                modifier = Modifier.fillMaxWidth().weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    "CHIA SẺ JSON",
                    color = MaterialTheme.colorScheme.primary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Black
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(7.dp)
                ) {
                    FilterChip(
                        selected = sharedJsonTab == "sent",
                        onClick = { sharedJsonTab = "sent" },
                        label = { Text("ĐÃ GỬI", fontSize = 10.sp) }
                    )
                    FilterChip(
                        selected = sharedJsonTab == "received",
                        onClick = { sharedJsonTab = "received" },
                        label = { Text("ĐÃ NHẬN", fontSize = 10.sp) }
                    )
                }
                if (sharedJsonTab == "sent") {
                    if (loadingSharedJsons) {
                        Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator()
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxWidth().weight(1f),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            if (sharedJsons.isEmpty()) {
                                item {
                                    Text(
                                        "Bạn chưa chia sẻ JSON nào.",
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontSize = 11.sp
                                    )
                                }
                            }
                            items(sharedJsons, key = { it.id }) { sharedJson ->
                                CommunitySharedJsonCard(
                                    item = sharedJson,
                                    enabled = !busy,
                                    onDelete = { deleteSharedJson = sharedJson }
                                )
                            }
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth().weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (receivedJsons.isEmpty()) {
                            item {
                                Text(
                                    "Bạn chưa nhận JSON nào từ người khác.",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 11.sp
                                )
                            }
                        }
                        items(receivedJsons, key = { it.id }) { received ->
                            CommunityReceivedJsonCard(
                                item = received,
                                enabled = !busy,
                                onUse = {
                                    onImportJson(
                                        received.rawJson,
                                        received.senderUid,
                                        received.senderName,
                                        received.tag,
                                        received.villageName,
                                        received.timestamp,
                                        received.id
                                    )
                                },
                                onDelete = { deleteReceivedJson = received }
                            )
                        }
                    }
                }
            }
        } else {
            if (isOwner) {
                Card(
                    modifier = Modifier.fillMaxWidth().ledRgbBorder(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    shape = RoundedCornerShape(18.dp)
                ) {
                    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("ĐĂNG BẢN CẬP NHẬT APK", color = MaterialTheme.colorScheme.primary, fontSize = 11.sp, fontWeight = FontWeight.Black)
                        Text(
                            "Tạm dừng trên gói miễn phí hiện tại. Tải lên và phân phối APK chưa được hỗ trợ.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 10.sp
                        )
                    }
                }
            }
            if (loadingReleases) {
                Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else {
                releaseError?.let { Text(it, color = MaterialTheme.colorScheme.error, fontSize = 10.sp) }
                LazyColumn(
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (releases.isEmpty()) {
                        item {
                            Text("Chưa có bản cập nhật nào được đăng.", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
                        }
                    }
                    items(releases, key = { it.id }) { release ->
                        CommunityReleaseCard(
                            release = release,
                            isOwner = isOwner,
                            onDownload = { url, name -> downloadCommunityFile(context, url, name) },
                            onDelete = { deleteRelease = release }
                        )
                    }
                }
            }
        }
    }

    if (showJsonPicker) {
        AlertDialog(
            onDismissRequest = { if (!busy) showJsonPicker = false },
            title = { Text("CHỌN JSON ĐỂ GỬI") },
            text = {
                LazyColumn(
                    modifier = Modifier.heightIn(max = 360.dp),
                    verticalArrangement = Arrangement.spacedBy(7.dp)
                ) {
                    items(jsonOptions, key = { "${it.tag}_${it.timestamp}" }) { snapshot ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(9.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                    Text(
                                        snapshot.villageName?.takeIf { it.isNotBlank() } ?: "Làng ${snapshot.tag}",
                                        color = MaterialTheme.colorScheme.onSurface,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        "Mã người chơi: ${snapshot.tag} · ${communityTime(snapshot.timestamp * 1000L)}",
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontSize = 9.sp
                                    )
                                }
                                TextButton(
                                    onClick = {
                                        showJsonPicker = false
                                        scope.launch {
                                            busy = true
                                            statusMessage = null
                                            try {
                                                statusMessage = shareVillageJsonToCommunity(snapshot)
                                            } catch (error: Exception) {
                                                statusMessage = error.message ?: "Không thể chia sẻ JSON."
                                            } finally {
                                                busy = false
                                            }
                                        }
                                    },
                                    enabled = !busy
                                ) {
                                    Text("GỬI", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showJsonPicker = false }, enabled = !busy) {
                    Text("ĐÓNG")
                }
            }
        )
    }

    deleteReceivedJson?.let { item ->
        AlertDialog(
            onDismissRequest = { if (!busy) deleteReceivedJson = null },
            title = { Text("Xóa JSON đã nhận?") },
            text = {
                Text("Chỉ xóa bản JSON trong danh sách ĐÃ NHẬN trên thiết bị này. Bản trong Lịch sử và JSON gốc của người chia sẻ vẫn được giữ.")
            },
            confirmButton = {
                TextButton(
                    enabled = !busy,
                    onClick = {
                        deleteReceivedJson = null
                        onDeleteReceivedJson(item.id)
                    }
                ) { Text("XÓA", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { deleteReceivedJson = null }, enabled = !busy) { Text("HỦY") }
            }
        )
    }

    deleteSharedJson?.let { item ->
        AlertDialog(
            onDismissRequest = { if (!busy) deleteSharedJson = null },
            title = { Text("Xóa JSON đã gửi?") },
            text = {
                Text("JSON này sẽ bị xóa khỏi danh sách chia sẻ và bài đăng JSON tương ứng trong Cộng đồng. Bản JSON trong Lịch sử và Đang lưu trên thiết bị vẫn được giữ.")
            },
            confirmButton = {
                TextButton(
                    enabled = !busy,
                    onClick = {
                        deleteSharedJson = null
                        scope.launch {
                            busy = true
                            try {
                                deleteSharedJsonFromCommunity(item.id)
                                statusMessage = "Đã xóa JSON đã gửi khỏi Cộng đồng."
                            } catch (error: Exception) {
                                statusMessage = error.message ?: "Không thể xóa JSON đã gửi."
                            } finally {
                                busy = false
                            }
                        }
                    }
                ) { Text("XÓA JSON", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { deleteSharedJson = null }, enabled = !busy) { Text("HỦY") }
            }
        )
    }

    deleteMessage?.let { message ->
        AlertDialog(
            onDismissRequest = { deleteMessage = null },
            title = { Text("Gỡ tin nhắn vào kho?") },
            text = { Text("Bản ghi tin nhắn/JSON sẽ được chuyển vào Kho đã gỡ. Tệp đính kèm cũ hiện có thể chưa truy cập được.") },
            confirmButton = {
                TextButton(onClick = {
                    deleteMessage = null
                    scope.launch {
                        try {
                            val firestore = FirebaseFirestore.getInstance()
                            val archiveCollection = communityArchiveCollection(message.type)
                            val archiveData = mutableMapOf<String, Any>(
                                "originalId" to message.id,
                                "uid" to message.uid,
                                "displayName" to message.displayName,
                                "photoUrl" to message.photoUrl,
                                "type" to message.type,
                                "text" to message.text,
                                "fileName" to message.fileName,
                                "storagePath" to message.storagePath,
                                "downloadUrl" to message.downloadUrl,
                                "contentType" to message.contentType,
                                "fileSize" to message.fileSize,
                                "contentData" to message.contentData,
                                "sharedJsonId" to message.sharedJsonId,
                                "createdAt" to Timestamp(Date(message.createdAt)),
                                "archivedBy" to FirebaseAuth.getInstance().currentUser?.uid.orEmpty(),
                                "archivedAt" to FieldValue.serverTimestamp()
                            )
                            val sharedJsonReference = message.sharedJsonId.takeIf { it.isNotBlank() }
                                ?.let { firestore.collection("community_shared_jsons").document(it) }
                            if (sharedJsonReference != null) {
                                val sharedJsonDocument = sharedJsonReference.get().awaitCommunityResult()
                                if (sharedJsonDocument.exists()) {
                                    archiveData["sharedJsonData"] = sharedJsonDocument.data.orEmpty()
                                }
                            }
                            val batch = firestore.batch()
                            batch.set(firestore.collection(archiveCollection).document(message.id), archiveData)
                            batch.delete(firestore.collection("community_messages").document(message.id))
                            if (sharedJsonReference != null) batch.delete(sharedJsonReference)
                            batch.commit().awaitCommunityResult()
                            statusMessage = "Đã gỡ và lưu vào Kho đã gỡ."
                        } catch (error: Exception) {
                            statusMessage = error.message ?: "Không thể chuyển tin nhắn vào kho."
                        }
                    }
                }) { Text("GỠ VÀ LƯU") }
            },
            dismissButton = { TextButton(onClick = { deleteMessage = null }) { Text("HỦY") } }
        )
    }

    deleteRelease?.let { release ->
        AlertDialog(
            onDismissRequest = { deleteRelease = null },
            title = { Text("Gỡ bản cập nhật vào kho?") },
            text = { Text("Bản ghi ${release.versionName} sẽ được chuyển vào kho đã gỡ. Tệp APK cũ vẫn được giữ nhưng hiện không thể truy cập.") },
            confirmButton = {
                TextButton(onClick = {
                    deleteRelease = null
                    scope.launch {
                        try {
                            val firestore = FirebaseFirestore.getInstance()
                            val archiveData = mutableMapOf<String, Any>(
                                "originalId" to release.id,
                                "versionName" to release.versionName,
                                "notes" to release.notes,
                                "fileName" to release.fileName,
                                "storagePath" to release.storagePath,
                                "downloadUrl" to release.downloadUrl,
                                "fileSize" to release.fileSize,
                                "createdAt" to Timestamp(Date(release.createdAt)),
                                "archivedBy" to FirebaseAuth.getInstance().currentUser?.uid.orEmpty(),
                                "archivedAt" to FieldValue.serverTimestamp()
                            )
                            val batch = firestore.batch()
                            batch.set(firestore.collection("app_update_archive").document(release.id), archiveData)
                            batch.delete(firestore.collection("app_updates").document(release.id))
                            batch.commit().awaitCommunityResult()
                            statusMessage = "Đã gỡ và lưu APK cập nhật."
                        } catch (error: Exception) {
                            statusMessage = error.message ?: "Không thể chuyển APK vào kho."
                        }
                    }
                }) { Text("GỠ VÀ LƯU") }
            },
            dismissButton = { TextButton(onClick = { deleteRelease = null }) { Text("HỦY") } }
        )
    }

    if (showDeleteAllArchiveConfirmation) {
        AlertDialog(
            onDismissRequest = { if (!busy) showDeleteAllArchiveConfirmation = false },
            title = { Text("Xóa vĩnh viễn toàn bộ Kho đã gỡ?") },
            text = { Text("Tất cả tin nhắn, ảnh, tệp và APK trong Kho đã gỡ sẽ bị xóa khỏi Firestore. Nội dung đang còn trong Cộng đồng sẽ không bị ảnh hưởng. Thao tác này không thể hoàn tác.") },
            confirmButton = {
                TextButton(
                    enabled = !busy,
                    onClick = {
                        showDeleteAllArchiveConfirmation = false
                        scope.launch {
                            busy = true
                            try {
                                val deleted = deleteAllCommunityArchives()
                                communityCountRefreshKey += 1
                                statusMessage = "Đã xóa vĩnh viễn $deleted mục trong Kho đã gỡ."
                            } catch (error: Exception) {
                                statusMessage = error.message ?: "Không thể xóa toàn bộ Kho đã gỡ."
                            } finally {
                                busy = false
                            }
                        }
                    }
                ) { Text("XÓA TẤT CẢ", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteAllArchiveConfirmation = false }) { Text("HỦY") }
            }
        )
    }

    deleteArchiveItem?.let { item ->
        AlertDialog(
            onDismissRequest = { deleteArchiveItem = null },
            title = { Text("Xóa vĩnh viễn khỏi kho?") },
            text = { Text("Bản ghi và dữ liệu đính kèm/JSON được lưu bên trong bản ghi này sẽ bị xóa vĩnh viễn khỏi Firestore.") },
            confirmButton = {
                TextButton(onClick = {
                    deleteArchiveItem = null
                    scope.launch {
                        try {
                            val firestore = FirebaseFirestore.getInstance()
                            val batch = firestore.batch()
                            if (item.sharedJsonId.isNotBlank()) {
                                batch.delete(
                                    firestore.collection("community_shared_jsons").document(item.sharedJsonId)
                                )
                            }
                            batch.delete(
                                firestore.collection(communityArchiveCollection(item.category)).document(item.id)
                            )
                            batch.commit().awaitCommunityResult()
                            communityCountRefreshKey += 1
                            statusMessage = "Đã xóa vĩnh viễn bản ghi và dữ liệu JSON liên quan khỏi Firebase."
                        } catch (error: Exception) {
                            statusMessage = error.message ?: "Không thể xóa vĩnh viễn."
                        }
                    }
                }) { Text("XÓA VĨNH VIỄN") }
            },
            dismissButton = { TextButton(onClick = { deleteArchiveItem = null }) { Text("HỦY") } }
        )
    }
}

private fun communityMessagePayload(
    uid: String,
    displayName: String,
    senderIsAdmin: Boolean,
    photoUrl: String,
    type: String,
    text: String,
    fileName: String,
    storagePath: String,
    downloadUrl: String,
    contentType: String,
    fileSize: Long,
    contentData: String = "",
    sharedJsonId: String = ""
): Map<String, Any> = mapOf(
    "uid" to uid,
    "displayName" to (if (senderIsAdmin) "AILIO" else displayName).take(80),
    "senderIsAdmin" to senderIsAdmin,
    "photoUrl" to (if (senderIsAdmin) "" else photoUrl).take(1000),
    "type" to type,
    "text" to text.take(2000),
    "fileName" to fileName.take(180),
    "storagePath" to storagePath,
    "downloadUrl" to downloadUrl,
    "contentType" to contentType,
    "fileSize" to fileSize,
    "contentData" to contentData,
    "sharedJsonId" to sharedJsonId,
    "createdAt" to FieldValue.serverTimestamp()
)

@Composable
private fun CommunitySaveAttachmentButton(
    fileName: String,
    contentType: String,
    contentData: String,
    label: String
) {
    val context = LocalContext.current
    val saveLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument(contentType.ifBlank { "application/octet-stream" })
    ) { destination ->
        if (destination != null) {
            val result = runCatching {
                val bytes = Base64.decode(contentData, Base64.DEFAULT)
                require(bytes.isNotEmpty()) { "Tệp trống." }
                val output = context.contentResolver.openOutputStream(destination)
                    ?: throw IllegalStateException("Không thể mở vị trí lưu.")
                output.use { it.write(bytes) }
            }
            val message = if (result.isSuccess) {
                AppLanguageSettings.translate(context, "Đã lưu tệp.")
            } else {
                AppLanguageSettings.translate(
                    context,
                    result.exceptionOrNull()?.message ?: "Không thể lưu tệp."
                )
            }
            Toast.makeText(context, message, Toast.LENGTH_LONG).show()
        }
    }
    TextButton(
        onClick = { saveLauncher.launch(communitySafeFileName(fileName)) },
        enabled = contentData.isNotBlank()
    ) {
        Text(label, fontSize = 9.sp)
    }
}

@Composable
private fun CommunitySaveTextButton(
    fileName: String,
    textContent: String,
    mimeType: String,
    label: String
) {
    val context = LocalContext.current
    val saveLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument(mimeType)
    ) { destination ->
        if (destination != null) {
            val result = runCatching {
                require(textContent.isNotBlank()) { "Nội dung trống." }
                val output = context.contentResolver.openOutputStream(destination)
                    ?: throw IllegalStateException("Không thể mở vị trí lưu.")
                output.use { it.write(textContent.toByteArray(Charsets.UTF_8)) }
            }
            val message = if (result.isSuccess) {
                AppLanguageSettings.translate(context, "Đã lưu tệp.")
            } else {
                AppLanguageSettings.translate(
                    context,
                    result.exceptionOrNull()?.message ?: "Không thể lưu tệp."
                )
            }
            Toast.makeText(context, message, Toast.LENGTH_LONG).show()
        }
    }
    TextButton(
        onClick = { saveLauncher.launch(communitySafeFileName(fileName)) },
        enabled = textContent.isNotBlank()
    ) {
        Text(label, fontSize = 9.sp)
    }
}

@Composable
private fun CommunityMessageCard(
    message: CommunityMessage,
    currentUid: String,
    isOwner: Boolean,
    onDownload: (String, String) -> Unit,
    onUseJson: (String) -> Unit,
    onDelete: () -> Unit
) {
    val mine = message.uid == currentUid
    val previewBitmap = remember(message.contentData, message.type) {
        if (message.type == "image" && message.contentData.isNotBlank()) {
            runCatching {
                val bytes = Base64.decode(message.contentData, Base64.DEFAULT)
                BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap()
            }.getOrNull()
        } else null
    }
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (mine) Arrangement.End else Arrangement.Start
    ) {
        Card(
            modifier = Modifier.widthIn(max = 330.dp).ledRgbBorder(cornerRadiusDp = 17f, strokeWidthDp = 2.0f),
            shape = RoundedCornerShape(17.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (mine) MaterialTheme.colorScheme.primaryContainer
                else MaterialTheme.colorScheme.surfaceVariant
            ),
            border = null
        ) {
            Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                    if (message.senderIsAdmin) {
                        Box(
                            Modifier.size(30.dp).clip(CircleShape).background(Color(0xFF26354D)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("ADMIN", color = Color(0xFFFFD166), fontSize = 5.5.sp, fontWeight = FontWeight.Black, maxLines = 1)
                        }
                    } else if (message.photoUrl.isNotBlank()) {
                        AsyncImage(
                            model = message.photoUrl,
                            contentDescription = AppLanguageSettings.translate(LocalContext.current, "Ảnh đại diện"),
                            modifier = Modifier.size(26.dp).clip(CircleShape),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Box(
                            Modifier.size(26.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surface),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(message.displayName.take(1).uppercase(), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    Text(
                        if (message.senderIsAdmin) "AILIO" else if (mine) "Bạn" else message.displayName,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(communityTime(message.createdAt), color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 8.sp)
                }
                if (message.text.isNotBlank()) {
                    Text(message.text, color = MaterialTheme.colorScheme.onSurface, fontSize = 12.sp)
                }
                if (message.type == "image") {
                    if (previewBitmap != null) {
                        Image(
                            bitmap = previewBitmap,
                            contentDescription = message.fileName,
                            modifier = Modifier.fillMaxWidth()
                                .heightIn(max = 230.dp)
                                .clip(RoundedCornerShape(10.dp)),
                            contentScale = ContentScale.Fit
                        )
                    } else {
                        Text("Không thể hiển thị ảnh này.", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 9.sp)
                    }
                    CommunitySaveAttachmentButton(
                        fileName = message.fileName.ifBlank { "anh.jpg" },
                        contentType = message.contentType.ifBlank { "image/jpeg" },
                        contentData = message.contentData,
                        label = "LƯU ẢNH"
                    )
                } else if (message.type == "json") {
                    Row(
                        modifier = Modifier.fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.55f))
                            .padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(
                                message.fileName.ifBlank { "JSON COC-LIO" },
                                color = MaterialTheme.colorScheme.onSurface,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                formatCommunitySize(message.fileSize),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 8.sp
                            )
                        }
                        TextButton(
                            onClick = { onUseJson(message.sharedJsonId) },
                            enabled = message.sharedJsonId.isNotBlank()
                        ) {
                            Text("NHẬN JSON", fontSize = 9.sp)
                        }
                    }
                } else if (message.type == "file") {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(
                                message.fileName,
                                color = MaterialTheme.colorScheme.onSurface,
                                fontSize = 10.sp,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                formatCommunitySize(message.fileSize),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 8.sp
                            )
                        }
                        if (message.contentData.isNotBlank()) {
                            CommunitySaveAttachmentButton(
                                fileName = message.fileName,
                                contentType = message.contentType,
                                contentData = message.contentData,
                                label = "TẢI TỆP"
                            )
                        } else if (message.downloadUrl.isNotBlank()) {
                            TextButton(onClick = { onDownload(message.downloadUrl, message.fileName) }) {
                                Text("TẢI TỆP", fontSize = 9.sp)
                            }
                        }
                    }
                }
                if (isOwner) {
                    TextButton(onClick = onDelete, modifier = Modifier.align(Alignment.End)) {
                        Text("GỠ TIN", fontSize = 8.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun CommunitySharedJsonCard(
    item: CommunitySharedJson,
    enabled: Boolean,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth().ledRgbBorder(cornerRadiusDp = 17f),
        shape = RoundedCornerShape(17.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(Modifier.padding(11.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                item.villageName.ifBlank { "JSON làng ${item.playerTag}" },
                color = MaterialTheme.colorScheme.primary,
                fontSize = 12.sp,
                fontWeight = FontWeight.Black,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                "Mã người chơi: ${item.playerTag}",
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                "Đã gửi: ${communityTime(item.createdAt)}",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 9.sp
            )
            Text(
                "Dung lượng JSON: ${formatCommunitySize(item.rawJson.toByteArray(Charsets.UTF_8).size.toLong())}",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 9.sp
            )
            TextButton(
                onClick = onDelete,
                enabled = enabled,
                modifier = Modifier.align(Alignment.End)
            ) {
                Text("XÓA JSON ĐÃ GỬI", color = MaterialTheme.colorScheme.error, fontSize = 9.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun CommunityReceivedJsonCard(
    item: ReceivedSharedJsonRecord,
    enabled: Boolean,
    onUse: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth().ledRgbBorder(cornerRadiusDp = 17f),
        shape = RoundedCornerShape(17.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(Modifier.padding(11.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                item.villageName.ifBlank { "JSON làng ${item.tag}" },
                color = MaterialTheme.colorScheme.primary,
                fontSize = 12.sp,
                fontWeight = FontWeight.Black,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                "Mã người chơi: ${item.tag}",
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                "Chia sẻ bởi: ${item.senderName}",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 9.sp
            )
            Text(
                "Ngày dữ liệu: ${formatTimestamp(item.timestamp)} • Nhận: ${formatTimestamp(item.receivedAt)}",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 9.sp
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = onUse, enabled = enabled) {
                    Text("NHẬP LẠI", fontSize = 9.sp, fontWeight = FontWeight.Bold)
                }
                TextButton(onClick = onDelete, enabled = enabled) {
                    Text("XÓA", color = MaterialTheme.colorScheme.error, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun CommunityArchiveCard(
    item: CommunityArchiveItem,
    onDownload: (String, String) -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth().ledRgbBorder(cornerRadiusDp = 17f),
        shape = RoundedCornerShape(17.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(Modifier.padding(11.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            val title = when {
                item.category == "image" -> "HÌNH ẢNH ĐÃ GỠ"
                item.category == "file" -> "TỆP ĐÃ GỠ"
                item.category == "apk" -> "APK CẬP NHẬT ĐÃ GỠ"
                item.type == "json" -> "JSON CHIA SẺ ĐÃ GỠ"
                else -> "TIN NHẮN ĐÃ GỠ"
            }
            Text(title, color = MaterialTheme.colorScheme.primary, fontSize = 10.sp, fontWeight = FontWeight.Black)

            if (item.category == "apk") {
                Text(
                    "COC-LIO " + item.versionName,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
                if (item.notes.isNotBlank()) {
                    Text(item.notes, color = MaterialTheme.colorScheme.onSurface, fontSize = 10.sp)
                }
            } else {
                if (item.displayName.isNotBlank()) {
                    Text(
                        item.displayName,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                if (item.type == "json") {
                    val villageTitle = item.sharedJsonVillageName.ifBlank { "JSON làng" }
                    Text(
                        villageTitle,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                    if (item.sharedJsonPlayerTag.isNotBlank()) {
                        Text(
                            "Mã người chơi: " + item.sharedJsonPlayerTag,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 9.sp
                        )
                    }
                    Text(
                        if (item.sharedJsonRaw.isNotBlank()) {
                            "JSON: " + formatCommunitySize(item.sharedJsonRaw.toByteArray(Charsets.UTF_8).size.toLong())
                        } else {
                            "Bản lưu cũ chưa có dữ liệu JSON gốc."
                        },
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 9.sp
                    )
                    if (item.text.isNotBlank()) {
                        Text(item.text, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 9.sp)
                    }
                } else if (item.text.isNotBlank()) {
                    Text(item.text, color = MaterialTheme.colorScheme.onSurface, fontSize = 11.sp)
                }
            }

            if (item.category == "image") {
                val archivedImage = remember(item.contentData) {
                    if (item.contentData.isBlank()) null else runCatching {
                        val bytes = Base64.decode(item.contentData, Base64.DEFAULT)
                        BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap()
                    }.getOrNull()
                }
                if (archivedImage != null) {
                    Image(
                        bitmap = archivedImage,
                        contentDescription = item.fileName,
                        modifier = Modifier.fillMaxWidth().heightIn(max = 220.dp).clip(RoundedCornerShape(10.dp)),
                        contentScale = ContentScale.Fit
                    )
                } else {
                    Text(
                        "Ảnh không còn dữ liệu xem trước.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 9.sp
                    )
                }
            }

            if (item.fileName.isNotBlank()) {
                Text(
                    item.fileName + " · " + formatCommunitySize(item.fileSize),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 9.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Text(
                "Đã gửi: " + communityTime(item.createdAt) + "  •  Đã gỡ: " + communityTime(item.archivedAt),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 8.sp
            )

            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(5.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                when {
                    item.category == "image" && item.contentData.isNotBlank() -> {
                        CommunitySaveAttachmentButton(
                            fileName = item.fileName.ifBlank { "anh.jpg" },
                            contentType = item.contentType.ifBlank { "image/jpeg" },
                            contentData = item.contentData,
                            label = "LƯU ẢNH"
                        )
                    }
                    item.category == "file" && item.contentData.isNotBlank() -> {
                        CommunitySaveAttachmentButton(
                            fileName = item.fileName.ifBlank { "tep_tai_xuong" },
                            contentType = item.contentType.ifBlank { "application/octet-stream" },
                            contentData = item.contentData,
                            label = "LƯU TỆP"
                        )
                    }
                    item.category == "text" && item.type == "json" -> {
                        if (item.sharedJsonRaw.isNotBlank()) {
                            CommunitySaveTextButton(
                                fileName = item.fileName.ifBlank { "chia_se.json" },
                                textContent = item.sharedJsonRaw,
                                mimeType = "application/json",
                                label = "LƯU JSON"
                            )
                        } else {
                            Text(
                                "JSON gốc không có trong bản lưu cũ.",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 8.sp
                            )
                        }
                    }
                    item.category == "text" && item.text.isNotBlank() -> {
                        val savedMessage = buildString {
                            append(item.text)
                            append("\n\n")
                            append("Người gửi: ")
                            append(item.displayName)
                            append("\n")
                            append("Ngày gửi: ")
                            append(communityTime(item.createdAt))
                        }
                        CommunitySaveTextButton(
                            fileName = item.fileName.takeIf { it.endsWith(".txt", true) }
                                ?: "tin_nhan_${item.id.take(8)}.txt",
                            textContent = savedMessage,
                            mimeType = "text/plain",
                            label = "LƯU TIN NHẮN"
                        )
                    }
                    item.downloadUrl.isNotBlank() -> {
                        TextButton(onClick = {
                            onDownload(
                                item.downloadUrl,
                                item.fileName.ifBlank {
                                    if (item.category == "apk") "COC-LIO.apk" else "tep_tai_xuong"
                                }
                            )
                        }) {
                            Text(
                                when {
                                    item.category == "apk" -> "LƯU APK"
                                    item.category == "image" -> "LƯU ẢNH"
                                    else -> "LƯU TỆP"
                                },
                                fontSize = 9.sp
                            )
                        }
                    }
                }

                TextButton(onClick = onDelete) {
                    Text("XÓA VĨNH VIỄN", color = MaterialTheme.colorScheme.error, fontSize = 9.sp)
                }
            }

            if (item.category == "apk" && item.downloadUrl.isBlank()) {
                Text(
                    "Bản lưu hiện chỉ có thông tin phiên bản, chưa có đường dẫn tải APK.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 8.sp
                )
            }
            if (item.category == "text" && item.type == "json" && item.sharedJsonRaw.isBlank()) {
                Text(
                    "Không thể tải JSON gốc từ bản lưu cũ vì tài liệu JSON riêng không còn trong kho.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 8.sp
                )
            }
        }
    }
}

@Composable
private fun CommunityReleaseCard(
    release: CommunityRelease,
    isOwner: Boolean,
    onDownload: (String, String) -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth().ledRgbBorder(cornerRadiusDp = 17f),
        shape = RoundedCornerShape(17.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("COC-LIO ${release.versionName}", color = MaterialTheme.colorScheme.primary, fontSize = 13.sp, fontWeight = FontWeight.Black, modifier = Modifier.weight(1f))
                Text("APK", color = MaterialTheme.colorScheme.tertiary, fontSize = 9.sp, fontWeight = FontWeight.Bold)
            }
            if (release.notes.isNotBlank()) {
                Text(release.notes, color = MaterialTheme.colorScheme.onSurface, fontSize = 11.sp)
            }
            Text(
                "${release.fileName} · ${formatCommunitySize(release.fileSize)} · ${communityTime(release.createdAt)}",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 9.sp
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "Tải APK hiện đang tạm dừng trên gói miễn phí.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 9.sp
                )
                Spacer(Modifier.width(6.dp))
                if (isOwner) {
                    TextButton(onClick = onDelete) { Text("GỠ VÀO KHO", fontSize = 9.sp) }
                }
            }
        }
    }
}

private fun formatCommunitySize(bytes: Long): String = when {
    bytes <= 0L -> "Không rõ dung lượng"
    bytes < 1024L -> "${bytes} B"
    bytes < 1024L * 1024L -> String.format(Locale.US, "%.1f KB", bytes / 1024.0)
    else -> String.format(Locale.US, "%.1f MB", bytes / (1024.0 * 1024.0))
}
