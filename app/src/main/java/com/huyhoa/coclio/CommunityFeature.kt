package com.huyhoa.coclio

import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.os.Environment
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material3.Switch
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
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.UUID
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import com.huyhoa.coclio.data.VillageSnapshot

private const val COMMUNITY_APK_MIME = "application/vnd.android.package-archive"
private const val COMMUNITY_MAX_FILE_BYTES = 15L * 1024L * 1024L
private const val COMMUNITY_MAX_APK_BYTES = 150L * 1024L * 1024L

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
    val notes: String
)

private fun communityArchiveCollection(category: String): String = when (category) {
    "image" -> "community_deleted_images"
    "file" -> "community_deleted_files"
    "apk" -> "app_update_archive"
    else -> "community_deleted_messages"
}

private fun communityArchiveExpiry(archivedAt: Long, value: Int, unit: String): Timestamp {
    val calendar = Calendar.getInstance().apply {
        timeInMillis = archivedAt
        add(if (unit == "months") Calendar.MONTH else Calendar.DAY_OF_YEAR, value)
    }
    return Timestamp(calendar.time)
}

private suspend fun cleanupCommunityArchive(): Int {
    val firestore = FirebaseFirestore.getInstance()
    val collections = listOf(
        "community_deleted_messages",
        "community_deleted_images",
        "community_deleted_files",
        "app_update_archive"
    )
    var removed = 0
    for (collectionName in collections) {
        while (true) {
            val expired = firestore.collection(collectionName)
                .whereLessThanOrEqualTo("expiresAt", Timestamp.now())
                .limit(100)
                .get()
                .awaitCommunityResult()
            if (expired.isEmpty) break
            for (document in expired.documents) {
                document.reference.delete().awaitCommunityResult()
                removed++
            }
        }
    }
    return removed
}

class CommunityArchiveCleanupWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {
    override suspend fun doWork(): Result {
        val user = FirebaseAuth.getInstance().currentUser ?: return Result.success()
        if (!user.isEmailVerified || user.email?.equals("phunghuyhoahaikba@gmail.com", ignoreCase = true) != true) {
            return Result.success()
        }
        return try {
            cleanupCommunityArchive()
            Result.success()
        } catch (_: Exception) {
            Result.retry()
        }
    }
}

private suspend fun updateCommunityArchiveRetention(keepForever: Boolean, value: Int, unit: String) {
    val firestore = FirebaseFirestore.getInstance()
    val collections = listOf(
        "community_deleted_messages",
        "community_deleted_images",
        "community_deleted_files",
        "app_update_archive"
    )
    for (collectionName in collections) {
        val snapshot = firestore.collection(collectionName).get().awaitCommunityResult()
        for (chunk in snapshot.documents.chunked(400)) {
            val batch = firestore.batch()
            for (document in chunk) {
                val expiry: Any = if (keepForever) {
                    FieldValue.delete()
                } else {
                    val archivedAt = document.getTimestamp("archivedAt")?.toDate()?.time
                        ?: System.currentTimeMillis()
                    communityArchiveExpiry(archivedAt, value, unit)
                }
                batch.update(document.reference, "expiresAt", expiry)
            }
            if (chunk.isNotEmpty()) batch.commit().awaitCommunityResult()
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


suspend fun shareVillageJsonToCommunity(snapshot: VillageSnapshot): String {
    val signedInUser = FirebaseAuth.getInstance().currentUser
        ?: throw IllegalStateException("Bạn cần đăng nhập Google để chia sẻ JSON.")
    val rawJson = snapshot.rawJson
    val jsonSize = rawJson.toByteArray(Charsets.UTF_8).size
    require(rawJson.isNotBlank() && jsonSize in 1..700_000) {
        "JSON trống hoặc quá lớn. Dung lượng tối đa là 700 KB."
    }
    val firestore = FirebaseFirestore.getInstance()
    val displayName = signedInUser.displayName ?: signedInUser.email ?: "Thành viên"
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
            senderIsAdmin = FirebaseAccountAdministration.isOwner(signedInUser.email),
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
    initialSection: Int = 0,
    onInitialSectionConsumed: () -> Unit = {},
    onImportJson: (String, String, String, String, String, Long, String) -> Unit,
    onOpenSettings: () -> Unit
) {
    val context = LocalContext.current
    var autoTranslationEnabled by remember {
        mutableStateOf(CommunityAutoTranslationPreferences.isEnabled(context))
    }
    val scope = rememberCoroutineScope()
    val user = FirebaseAuth.getInstance().currentUser?.takeIf { isGoogleSignedIn }
    val isOwner = FirebaseAccountAdministration.isOwner(user?.email)
    var selectedSection by remember { mutableStateOf(0) }
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
    var archiveItems by remember { mutableStateOf<List<CommunityArchiveItem>>(emptyList()) }
    var loadingArchive by remember { mutableStateOf(false) }
    var archiveError by remember { mutableStateOf<String?>(null) }
    var archiveCategory by remember { mutableStateOf("text") }
    var archiveKeepForever by remember { mutableStateOf(true) }
    var archiveRetentionValue by remember { mutableStateOf("30") }
    var archiveRetentionUnit by remember { mutableStateOf("days") }
    val messageListState = rememberLazyListState()

    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            statusMessage = "Hiện tại, gửi ảnh đang tạm dừng trên gói miễn phí. Bạn vẫn có thể gửi tin nhắn văn bản và chia sẻ JSON."
        }
    }

    val filePicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            statusMessage = "Hiện tại, gửi tệp đính kèm đang tạm dừng trên gói miễn phí."
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
                .limitToLast(100)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        messageError = error.message ?: "Không thể tải tin nhắn."
                        loadingMessages = false
                    } else {
                        messages = snapshot?.documents.orEmpty().mapNotNull { document ->
                            val uid = document.getString("uid") ?: return@mapNotNull null
                            CommunityMessage(
                                id = document.id,
                                uid = uid,
                                displayName = document.getString("displayName").orEmpty().ifBlank { "Thành viên" },
                                senderIsAdmin = document.getBoolean("senderIsAdmin") ?: false,
                                photoUrl = document.getString("photoUrl").orEmpty(),
                                type = document.getString("type").orEmpty(),
                                text = document.getString("text").orEmpty(),
                                fileName = document.getString("fileName").orEmpty(),
                                storagePath = document.getString("storagePath").orEmpty(),
                                downloadUrl = document.getString("downloadUrl").orEmpty(),
                                contentType = document.getString("contentType").orEmpty(),
                                fileSize = document.getLong("fileSize") ?: 0L,
                                createdAt = document.getTimestamp("createdAt")?.toDate()?.time ?: 0L,
                                sharedJsonId = document.getString("sharedJsonId").orEmpty()
                            )
                        }
                        messageError = null
                        loadingMessages = false
                    }
                }
            val sharedJsonRegistration = firestore.collection("community_shared_jsons")
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .limit(100)
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
                        }
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
            val archiveRegistration = if (isOwner && selectedSection == 2) {
                loadingArchive = true
                firestore.collection(communityArchiveCollection(archiveCategory))
                    .orderBy("archivedAt", Query.Direction.DESCENDING)
                    .addSnapshotListener { snapshot, error ->
                        if (error != null) {
                            archiveError = error.message ?: "Không thể tải kho đã gỡ."
                            loadingArchive = false
                        } else {
                            archiveItems = snapshot?.documents.orEmpty().map { document ->
                                CommunityArchiveItem(
                                    id = document.id,
                                    category = archiveCategory,
                                    displayName = document.getString("displayName").orEmpty(),
                                    text = document.getString("text").orEmpty(),
                                    fileName = document.getString("fileName").orEmpty(),
                                    storagePath = document.getString("storagePath").orEmpty(),
                                    downloadUrl = document.getString("downloadUrl").orEmpty(),
                                    fileSize = document.getLong("fileSize") ?: 0L,
                                    createdAt = document.getTimestamp("createdAt")?.toDate()?.time ?: 0L,
                                    archivedAt = document.getTimestamp("archivedAt")?.toDate()?.time ?: 0L,
                                    versionName = document.getString("versionName").orEmpty(),
                                    notes = document.getString("notes").orEmpty()
                                )
                            }
                            archiveError = null
                            loadingArchive = false
                        }
                    }
            } else {
                archiveItems = emptyList()
                null
            }
            onDispose {
                messageRegistration.remove()
                sharedJsonRegistration.remove()
                releaseRegistration.remove()
                archiveRegistration?.remove()
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
                archiveKeepForever = settings.getBoolean("keepForever") ?: true
                archiveRetentionValue = (settings.getLong("retentionValue") ?: 30L).toString()
                archiveRetentionUnit = settings.getString("retentionUnit") ?: "days"
                val request = PeriodicWorkRequestBuilder<CommunityArchiveCleanupWorker>(1, TimeUnit.DAYS).build()
                WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                    "community_archive_cleanup",
                    ExistingPeriodicWorkPolicy.KEEP,
                    request
                )
                val removed = cleanupCommunityArchive()
                if (removed > 0) statusMessage = "Đã tự dọn $removed mục lưu trữ hết hạn."
            } catch (error: Exception) {
                statusMessage = error.message ?: "Không thể kiểm tra thời hạn lưu trữ."
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
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Tự động dịch", modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.onSurface, fontSize = 10.sp)
                Switch(
                    checked = autoTranslationEnabled,
                    onCheckedChange = {
                        autoTranslationEnabled = it
                        CommunityAutoTranslationPreferences.setEnabled(context, it)
                    }
                )
            }
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
                        Text("THỜI HẠN LƯU KHO", color = MaterialTheme.colorScheme.primary, fontSize = 11.sp, fontWeight = FontWeight.Black)
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            FilterChip(
                                selected = archiveKeepForever,
                                onClick = { archiveKeepForever = true },
                                label = { Text("GIỮ MÃI", fontSize = 9.sp) }
                            )
                            FilterChip(
                                selected = !archiveKeepForever,
                                onClick = { archiveKeepForever = false },
                                label = { Text("TỰ ĐỘNG XÓA", fontSize = 9.sp) }
                            )
                        }
                        if (!archiveKeepForever) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                                OutlinedTextField(
                                    value = archiveRetentionValue,
                                    onValueChange = { archiveRetentionValue = it.filter(Char::isDigit).take(4) },
                                    modifier = Modifier.weight(1f),
                                    singleLine = true,
                                    label = { Text("Số lượng") }
                                )
                                FilterChip(
                                    selected = archiveRetentionUnit == "days",
                                    onClick = { archiveRetentionUnit = "days" },
                                    label = { Text("NGÀY", fontSize = 9.sp) }
                                )
                                FilterChip(
                                    selected = archiveRetentionUnit == "months",
                                    onClick = { archiveRetentionUnit = "months" },
                                    label = { Text("THÁNG", fontSize = 9.sp) }
                                )
                            }
                        }
                        Text(
                            if (archiveKeepForever) "Dữ liệu đã gỡ được giữ lại cho đến khi quản lý xóa vĩnh viễn."
                            else "Áp dụng thời hạn cho các bản ghi trong kho. Tệp đính kèm cũ hiện chưa thể truy cập hoặc dọn dẹp trên gói miễn phí.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 9.sp
                        )
                        Button(
                            onClick = {
                                val value = archiveRetentionValue.toIntOrNull()
                                if (!archiveKeepForever && (value == null || value !in 1..1200)) {
                                    statusMessage = "Nhập thời hạn từ 1 đến 1200 ngày hoặc tháng."
                                } else {
                                    scope.launch {
                                        busy = true
                                        try {
                                            FirebaseFirestore.getInstance()
                                                .collection("admin")
                                                .document("community_archive_settings")
                                                .set(
                                                    mapOf(
                                                        "keepForever" to archiveKeepForever,
                                                        "retentionValue" to (value ?: 0),
                                                        "retentionUnit" to archiveRetentionUnit,
                                                        "updatedAt" to FieldValue.serverTimestamp()
                                                    )
                                                )
                                                .awaitCommunityResult()
                                            updateCommunityArchiveRetention(
                                                archiveKeepForever,
                                                value ?: 0,
                                                archiveRetentionUnit
                                            )
                                            val removed = cleanupCommunityArchive()
                                            statusMessage = if (removed > 0) {
                                                "Đã lưu thời hạn và dọn $removed mục hết hạn."
                                            } else {
                                                "Đã lưu chế độ lưu trữ."
                                            }
                                        } catch (error: Exception) {
                                            statusMessage = error.message ?: "Không thể lưu thời hạn lưu trữ."
                                        } finally {
                                            busy = false
                                        }
                                    }
                                }
                            },
                            enabled = !busy,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("LƯU THIẾT LẬP LƯU TRỮ")
                        }
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    listOf(
                        "text" to "TIN NHẮN",
                        "image" to "HÌNH ẢNH",
                        "file" to "TỆP",
                        "apk" to "APK CẬP NHẬT"
                    ).forEach { (category, label) ->
                        FilterChip(
                            selected = archiveCategory == category,
                            onClick = { archiveCategory = category },
                            label = { Text(label, fontSize = 9.sp) }
                        )
                    }
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
                    "JSON DO CỘNG ĐỒNG CHIA SẺ",
                    color = MaterialTheme.colorScheme.primary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Black
                )
                Text(
                    "Tất cả thành viên đã đăng nhập đều có thể nhận JSON và nhập vào tài khoản của mình.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 9.sp
                )
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
                                    "Chưa có JSON nào được chia sẻ.",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 11.sp
                                )
                            }
                        }
                        items(sharedJsons, key = { it.id }) { sharedJson ->
                            CommunitySharedJsonCard(
                                item = sharedJson,
                                enabled = !busy,
                                onReceive = { receiveSharedJson(sharedJson.id) }
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
                                        "Player Tag: ${snapshot.tag} · ${communityTime(snapshot.timestamp * 1000L)}",
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
                                "createdAt" to Timestamp(Date(message.createdAt)),
                                "archivedBy" to FirebaseAuth.getInstance().currentUser?.uid.orEmpty(),
                                "archivedAt" to FieldValue.serverTimestamp()
                            )
                            if (!archiveKeepForever) {
                                val retention = archiveRetentionValue.toIntOrNull() ?: 30
                                archiveData["expiresAt"] = communityArchiveExpiry(
                                    System.currentTimeMillis(),
                                    retention,
                                    archiveRetentionUnit
                                )
                            }
                            val batch = firestore.batch()
                            batch.set(firestore.collection(archiveCollection).document(message.id), archiveData)
                            batch.delete(firestore.collection("community_messages").document(message.id))
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
                            if (!archiveKeepForever) {
                                val retention = archiveRetentionValue.toIntOrNull() ?: 30
                                archiveData["expiresAt"] = communityArchiveExpiry(
                                    System.currentTimeMillis(),
                                    retention,
                                    archiveRetentionUnit
                                )
                            }
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

    deleteArchiveItem?.let { item ->
        AlertDialog(
            onDismissRequest = { deleteArchiveItem = null },
            title = { Text("Xóa vĩnh viễn khỏi kho?") },
            text = { Text("Bản ghi dữ liệu này sẽ bị xóa vĩnh viễn. Tệp đính kèm cũ không bị xóa và hiện vẫn không thể truy cập.") },
            confirmButton = {
                TextButton(onClick = {
                    deleteArchiveItem = null
                    scope.launch {
                        try {
                            FirebaseFirestore.getInstance()
                                .collection(communityArchiveCollection(item.category))
                                .document(item.id)
                                .delete()
                                .awaitCommunityResult()
                            statusMessage = "Đã xóa vĩnh viễn khỏi kho."
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
    sharedJsonId: String = ""
): Map<String, Any> = mapOf(
    "uid" to uid,
    "displayName" to displayName.take(80),
    "senderIsAdmin" to senderIsAdmin,
    "photoUrl" to photoUrl.take(1000),
    "type" to type,
    "text" to text.take(2000),
    "fileName" to fileName.take(180),
    "storagePath" to storagePath,
    "downloadUrl" to downloadUrl,
    "contentType" to contentType,
    "fileSize" to fileSize,
    "sharedJsonId" to sharedJsonId,
    "createdAt" to FieldValue.serverTimestamp()
)

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
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (mine) Arrangement.End else Arrangement.Start
    ) {
        Card(
            modifier = Modifier.widthIn(max = 330.dp).ledRgbBorder(cornerRadiusDp = 17f, strokeWidthDp = 2.0f),
            shape = RoundedCornerShape(17.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (message.senderIsAdmin) Color(0xFF3A2A12)
                else if (mine) MaterialTheme.colorScheme.primaryContainer
                else MaterialTheme.colorScheme.surfaceVariant
            ),
            border = if (message.senderIsAdmin) BorderStroke(1.8.dp, Color(0xFFFFD166)) else null
        ) {
            Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                    if (message.photoUrl.isNotBlank()) {
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
                        if (mine) "Bạn" else message.displayName,
                        color = if (message.senderIsAdmin) Color(0xFFFFD166) else MaterialTheme.colorScheme.onSurface,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (message.senderIsAdmin) {
                        Text(
                            "ADMIN",
                            color = Color(0xFF261A00),
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Black,
                            modifier = Modifier
                                .background(Color(0xFFFFD166), RoundedCornerShape(6.dp))
                                .padding(horizontal = 6.dp, vertical = 3.dp)
                        )
                    }
                    Text(communityTime(message.createdAt), color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 8.sp)
                }
                if (message.text.isNotBlank()) {
                    if (message.type == "text") {
                        CommunityAutoTranslatedText(
                            sourceText = message.text,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 12.sp,
                            enabled = autoTranslationEnabled
                        )
                    } else {
                        Text(message.text, color = MaterialTheme.colorScheme.onSurface, fontSize = 12.sp)
                    }
                }
                if (message.type == "image") {
                    Text("Ảnh đính kèm hiện chưa thể xem trên gói miễn phí.", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 9.sp)
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
                    Text(
                        "${message.fileName} · ${formatCommunitySize(message.fileSize)} · Tải tệp hiện tạm ngừng trên gói miễn phí.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 9.sp
                    )
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
    onReceive: () -> Unit
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
                "Player Tag: ${item.playerTag}",
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                "Chia sẻ bởi: ${item.displayName} • ${communityTime(item.createdAt)}",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 9.sp
            )
            Text(
                "Dữ liệu JSON: ${formatCommunitySize(item.rawJson.toByteArray(Charsets.UTF_8).size.toLong())}",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 9.sp
            )
            Button(
                onClick = onReceive,
                enabled = enabled,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("NHẬN JSON VÀ SỬ DỤNG", fontSize = 10.sp, fontWeight = FontWeight.Bold)
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
            val title = when (item.category) {
                "image" -> "HÌNH ẢNH ĐÃ GỠ"
                "file" -> "TỆP ĐÃ GỠ"
                "apk" -> "APK CẬP NHẬT ĐÃ GỠ"
                else -> "TIN NHẮN ĐÃ GỠ"
            }
            Text(title, color = MaterialTheme.colorScheme.primary, fontSize = 10.sp, fontWeight = FontWeight.Black)
            if (item.category == "apk") {
                Text("COC-LIO " + item.versionName, color = MaterialTheme.colorScheme.onSurface, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                if (item.notes.isNotBlank()) {
                    CommunityAutoTranslatedText(
                        sourceText = item.notes,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 10.sp,
                        enabled = autoTranslationEnabled
                    )
                }
            } else {
                if (item.displayName.isNotBlank()) {
                    Text(item.displayName, color = MaterialTheme.colorScheme.onSurface, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
                if (item.text.isNotBlank()) {
                    if (item.category == "text") {
                        CommunityAutoTranslatedText(
                            sourceText = item.text,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 11.sp,
                            enabled = autoTranslationEnabled
                        )
                    } else {
                        Text(item.text, color = MaterialTheme.colorScheme.onSurface, fontSize = 11.sp)
                    }
                }
            }
            if (item.category == "image") {
                Text("Ảnh lưu trữ hiện chưa thể xem trên gói miễn phí.", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 9.sp)
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
            Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                if (item.downloadUrl.isNotBlank()) {
                    Text("Tệp đính kèm hiện chưa thể tải xuống", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 8.sp)
                }
                TextButton(onClick = onDelete) {
                    Text("XÓA VĨNH VIỄN", color = MaterialTheme.colorScheme.error, fontSize = 9.sp)
                }
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
                CommunityAutoTranslatedText(
                    sourceText = release.notes,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 11.sp,
                    enabled = autoTranslationEnabled
                )
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
