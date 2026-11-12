package com.huyhoa.coclio

import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.os.Environment
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
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
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageMetadata
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

private const val COMMUNITY_APK_MIME = "application/vnd.android.package-archive"
private const val COMMUNITY_MAX_FILE_BYTES = 15L * 1024L * 1024L
private const val COMMUNITY_MAX_APK_BYTES = 150L * 1024L * 1024L

private data class CommunityMessage(
    val id: String,
    val uid: String,
    val displayName: String,
    val photoUrl: String,
    val type: String,
    val text: String,
    val fileName: String,
    val storagePath: String,
    val downloadUrl: String,
    val contentType: String,
    val fileSize: Long,
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
                val path = document.getString("storagePath").orEmpty()
                if (path.isNotBlank()) {
                    FirebaseStorage.getInstance().reference.child(path).delete().awaitCommunityResult()
                }
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
                    task.exception ?: IllegalStateException("Firebase không trả về kết quả.")
                )
            }
        }
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
fun CommunityScreen(isGoogleSignedIn: Boolean, onOpenSettings: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val user = FirebaseAuth.getInstance().currentUser?.takeIf { isGoogleSignedIn }
    val isOwner = FirebaseAccountAdministration.isOwner(user?.email)
    var selectedSection by remember { mutableStateOf(0) }
    var messages by remember { mutableStateOf<List<CommunityMessage>>(emptyList()) }
    var releases by remember { mutableStateOf<List<CommunityRelease>>(emptyList()) }
    var loadingMessages by remember { mutableStateOf(true) }
    var loadingReleases by remember { mutableStateOf(true) }
    var messageError by remember { mutableStateOf<String?>(null) }
    var releaseError by remember { mutableStateOf<String?>(null) }
    var statusMessage by remember { mutableStateOf<String?>(null) }
    var draft by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var releaseVersion by remember { mutableStateOf("") }
    var releaseNotes by remember { mutableStateOf("") }
    var selectedApkName by remember { mutableStateOf<String?>(null) }
    var selectedApkUri by remember { mutableStateOf<Uri?>(null) }
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
            val signedInUser = FirebaseAuth.getInstance().currentUser
            if (signedInUser == null) {
                statusMessage = "Bạn cần đăng nhập Google để gửi ảnh."
            } else {
                scope.launch {
                    busy = true
                    statusMessage = null
                    var uploadedPath: String? = null
                    try {
                        val fileName = communityFileName(context, uri)
                        val size = communityFileSize(context, uri)
                        val mime = communityMimeType(context, uri, fileName)
                        require(mime.startsWith("image/")) { "Tệp đã chọn không phải hình ảnh." }
                        require(size in 1..COMMUNITY_MAX_FILE_BYTES) { "Ảnh phải có dung lượng không quá 15 MB." }
                        val path = "community/${signedInUser.uid}/${UUID.randomUUID()}_${communitySafeFileName(fileName)}"
                        uploadedPath = path
                        val reference = FirebaseStorage.getInstance().reference.child(path)
                        val metadata = StorageMetadata.Builder().setContentType(mime).build()
                        reference.putFile(uri, metadata).awaitCommunityResult()
                        val downloadUrl = reference.downloadUrl.awaitCommunityResult().toString()
                        FirebaseFirestore.getInstance().collection("community_messages").add(
                            communityMessagePayload(
                                uid = signedInUser.uid,
                                displayName = signedInUser.displayName ?: signedInUser.email ?: "Thành viên",
                                photoUrl = signedInUser.photoUrl?.toString().orEmpty(),
                                type = "image",
                                text = draft.trim(),
                                fileName = fileName,
                                storagePath = path,
                                downloadUrl = downloadUrl,
                                contentType = mime,
                                fileSize = size
                            )
                        ).awaitCommunityResult()
                        draft = ""
                        statusMessage = "Đã gửi ảnh."
                    } catch (error: Exception) {
                        uploadedPath?.let { path ->
                            runCatching { FirebaseStorage.getInstance().reference.child(path).delete().awaitCommunityResult() }
                        }
                        statusMessage = error.message?.takeIf { it.isNotBlank() }
                            ?: "Không thể gửi ảnh. Hãy kiểm tra Cloud Storage và quyền truy cập."
                    } finally {
                        busy = false
                    }
                }
            }
        }
    }

    val filePicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            val signedInUser = FirebaseAuth.getInstance().currentUser
            if (signedInUser == null) {
                statusMessage = "Bạn cần đăng nhập Google để gửi tệp."
            } else {
                scope.launch {
                    busy = true
                    statusMessage = null
                    var uploadedPath: String? = null
                    try {
                        val fileName = communityFileName(context, uri)
                        val size = communityFileSize(context, uri)
                        val mime = communityMimeType(context, uri, fileName)
                        require(size in 1..COMMUNITY_MAX_FILE_BYTES) { "Tệp đính kèm phải có dung lượng không quá 15 MB." }
                        require(
                            mime.startsWith("image/") || mime in setOf(
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
                            )
                        ) { "Định dạng tệp này chưa được hỗ trợ trong cộng đồng." }
                        require(mime != COMMUNITY_APK_MIME || isOwner) { "Chỉ tài khoản quản lý mới được gửi tệp APK." }
                        val path = "community/${signedInUser.uid}/${UUID.randomUUID()}_${communitySafeFileName(fileName)}"
                        uploadedPath = path
                        val reference = FirebaseStorage.getInstance().reference.child(path)
                        val metadata = StorageMetadata.Builder().setContentType(mime).build()
                        reference.putFile(uri, metadata).awaitCommunityResult()
                        val downloadUrl = reference.downloadUrl.awaitCommunityResult().toString()
                        FirebaseFirestore.getInstance().collection("community_messages").add(
                            communityMessagePayload(
                                uid = signedInUser.uid,
                                displayName = signedInUser.displayName ?: signedInUser.email ?: "Thành viên",
                                photoUrl = signedInUser.photoUrl?.toString().orEmpty(),
                                type = "file",
                                text = draft.trim(),
                                fileName = fileName,
                                storagePath = path,
                                downloadUrl = downloadUrl,
                                contentType = mime,
                                fileSize = size
                            )
                        ).awaitCommunityResult()
                        draft = ""
                        statusMessage = "Đã gửi tệp."
                    } catch (error: Exception) {
                        uploadedPath?.let { path ->
                            runCatching { FirebaseStorage.getInstance().reference.child(path).delete().awaitCommunityResult() }
                        }
                        statusMessage = error.message?.takeIf { it.isNotBlank() }
                            ?: "Không thể gửi tệp. Hãy kiểm tra Cloud Storage và quyền truy cập."
                    } finally {
                        busy = false
                    }
                }
            }
        }
    }

    val apkPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            selectedApkUri = uri
            selectedApkName = communityFileName(context, uri)
            statusMessage = null
        }
    }

    DisposableEffect(user?.uid, selectedSection, archiveCategory, isOwner) {
        if (user == null) {
            messages = emptyList()
            releases = emptyList()
            archiveItems = emptyList()
            loadingMessages = false
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
                                photoUrl = document.getString("photoUrl").orEmpty(),
                                type = document.getString("type").orEmpty(),
                                text = document.getString("text").orEmpty(),
                                fileName = document.getString("fileName").orEmpty(),
                                storagePath = document.getString("storagePath").orEmpty(),
                                downloadUrl = document.getString("downloadUrl").orEmpty(),
                                contentType = document.getString("contentType").orEmpty(),
                                fileSize = document.getLong("fileSize") ?: 0L,
                                createdAt = document.getTimestamp("createdAt")?.toDate()?.time ?: 0L
                            )
                        }
                        messageError = null
                        loadingMessages = false
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
                    .limit(100)
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
            Text("Trò chuyện, chia sẻ tệp và nhận bản cập nhật chính thức.", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
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
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)) {
                Text(it, modifier = Modifier.padding(10.dp), color = MaterialTheme.colorScheme.onSecondaryContainer, fontSize = 10.sp)
            }
        }
        if (user == null) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("ĐĂNG NHẬP ĐỂ THAM GIA", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Black)
                    Text("Bạn cần đăng nhập Google trong mục Cài đặt để xem tin nhắn, nhắn tin, gửi tệp và tải bản cập nhật.", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp, lineHeight = 16.sp)
                    Button(onClick = onOpenSettings) { Text("MỞ CÀI ĐẶT") }
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
                            onDelete = { deleteMessage = message }
                        )
                    }
                }
            }
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(
                    modifier = Modifier.padding(9.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        listOf("😂", "🤣", "😅", "🥳", "❤️", "🔥", "👍", "😎", "🎉", "😭", "😁", "💪").forEach { emoji ->
                            TextButton(onClick = { draft += emoji }) {
                                Text(emoji, fontSize = 19.sp)
                            }
                        }
                    }
                    Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
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
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = { imagePicker.launch("image/*") }, enabled = !busy) {
                            Text("ẢNH", fontSize = 9.sp)
                        }
                        OutlinedButton(onClick = { filePicker.launch(arrayOf("*/*")) }, enabled = !busy) {
                            Text("TỆP", fontSize = 9.sp)
                        }
                        if (busy) Text("Đang gửi…", modifier = Modifier.align(Alignment.CenterVertically), color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 9.sp)
                    }
                }
            }
        } else if (selectedSection == 2 && isOwner) {
            Column(
                modifier = Modifier.fillMaxWidth().weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
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
                            else "Áp dụng thời hạn này cho toàn bộ dữ liệu trong kho. Hệ thống kiểm tra và dọn mục hết hạn khi tài khoản quản lý mở Cộng đồng.",
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
        } else {
            if (isOwner) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    shape = RoundedCornerShape(18.dp)
                ) {
                    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("ĐĂNG BẢN CẬP NHẬT MỚI", color = MaterialTheme.colorScheme.primary, fontSize = 11.sp, fontWeight = FontWeight.Black)
                        OutlinedTextField(
                            value = releaseVersion,
                            onValueChange = { releaseVersion = it.take(40) },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            label = { Text("Phiên bản, ví dụ 1.0.2") }
                        )
                        OutlinedTextField(
                            value = releaseNotes,
                            onValueChange = { releaseNotes = it.take(3000) },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("Nội dung cập nhật") },
                            minLines = 2,
                            maxLines = 4
                        )
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(onClick = { apkPicker.launch(arrayOf(COMMUNITY_APK_MIME)) }, enabled = !busy) {
                                Text("CHỌN FILE APK", fontSize = 9.sp)
                            }
                            Text(
                                selectedApkName ?: "Chưa chọn APK",
                                modifier = Modifier.weight(1f),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 9.sp,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Button(
                            onClick = {
                                val signedInUser = FirebaseAuth.getInstance().currentUser
                                val uri = selectedApkUri
                                if (signedInUser == null || !isOwner) {
                                    statusMessage = "Không có quyền đăng bản cập nhật."
                                } else if (releaseVersion.isBlank() || releaseNotes.isBlank() || uri == null) {
                                    statusMessage = "Hãy nhập phiên bản, nội dung cập nhật và chọn file APK."
                                } else {
                                    scope.launch {
                                        busy = true
                                        statusMessage = null
                                        var uploadedPath: String? = null
                                        try {
                                            val fileName = communityFileName(context, uri)
                                            val size = communityFileSize(context, uri)
                                            require(fileName.lowercase(Locale.ROOT).endsWith(".apk")) { "Chỉ chấp nhận file có đuôi .apk." }
                                            require(size in 1..COMMUNITY_MAX_APK_BYTES) { "APK phải có dung lượng không quá 150 MB." }
                                            val path = "updates/${UUID.randomUUID()}_${communitySafeFileName(fileName)}"
                                            uploadedPath = path
                                            val reference = FirebaseStorage.getInstance().reference.child(path)
                                            val metadata = StorageMetadata.Builder().setContentType(COMMUNITY_APK_MIME).build()
                                            reference.putFile(uri, metadata).awaitCommunityResult()
                                            val downloadUrl = reference.downloadUrl.awaitCommunityResult().toString()
                                            FirebaseFirestore.getInstance().collection("app_updates").add(
                                                mapOf(
                                                    "versionName" to releaseVersion.trim(),
                                                    "notes" to releaseNotes.trim(),
                                                    "fileName" to fileName,
                                                    "storagePath" to path,
                                                    "downloadUrl" to downloadUrl,
                                                    "fileSize" to size,
                                                    "createdBy" to signedInUser.uid,
                                                    "createdAt" to FieldValue.serverTimestamp()
                                                )
                                            ).awaitCommunityResult()
                                            releaseVersion = ""
                                            releaseNotes = ""
                                            selectedApkUri = null
                                            selectedApkName = null
                                            statusMessage = "Đã đăng bản cập nhật. Thành viên có thể tải APK trong mục này."
                                        } catch (error: Exception) {
                                            uploadedPath?.let { path ->
                                                runCatching { FirebaseStorage.getInstance().reference.child(path).delete().awaitCommunityResult() }
                                            }
                                            statusMessage = error.message ?: "Không thể đăng APK. Hãy kiểm tra Cloud Storage và quyền quản lý."
                                        } finally {
                                            busy = false
                                        }
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = !busy && isOwner
                        ) {
                            if (busy) CircularProgressIndicator(Modifier.size(15.dp), strokeWidth = 2.dp)
                            else Text("ĐĂNG BẢN CẬP NHẬT", fontWeight = FontWeight.Bold)
                        }
                        Text("Chỉ tài khoản quản lý mới được đăng APK. Người dùng cần tự xác nhận cài đặt trên Android.", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 9.sp)
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

    deleteMessage?.let { message ->
        AlertDialog(
            onDismissRequest = { deleteMessage = null },
            title = { Text("Gỡ tin nhắn vào kho?") },
            text = { Text("Tin nhắn và tệp đính kèm sẽ được chuyển vào Kho đã gỡ, không bị xóa khỏi bộ nhớ.") },
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
            text = { Text("Bản ${release.versionName} sẽ được chuyển vào mục APK cập nhật đã gỡ, tệp APK vẫn được giữ.") },
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
            text = { Text("Dữ liệu này và tệp đính kèm sẽ bị xóa hoàn toàn, không thể khôi phục.") },
            confirmButton = {
                TextButton(onClick = {
                    deleteArchiveItem = null
                    scope.launch {
                        try {
                            if (item.storagePath.isNotBlank()) {
                                FirebaseStorage.getInstance().reference.child(item.storagePath).delete().awaitCommunityResult()
                            }
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
    photoUrl: String,
    type: String,
    text: String,
    fileName: String,
    storagePath: String,
    downloadUrl: String,
    contentType: String,
    fileSize: Long
): Map<String, Any> = mapOf(
    "uid" to uid,
    "displayName" to displayName.take(80),
    "photoUrl" to photoUrl.take(1000),
    "type" to type,
    "text" to text.take(2000),
    "fileName" to fileName.take(180),
    "storagePath" to storagePath,
    "downloadUrl" to downloadUrl,
    "contentType" to contentType,
    "fileSize" to fileSize,
    "createdAt" to FieldValue.serverTimestamp()
)

@Composable
private fun CommunityMessageCard(
    message: CommunityMessage,
    currentUid: String,
    isOwner: Boolean,
    onDownload: (String, String) -> Unit,
    onDelete: () -> Unit
) {
    val mine = message.uid == currentUid
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (mine) Arrangement.End else Arrangement.Start
    ) {
        Card(
            modifier = Modifier.widthIn(max = 330.dp),
            shape = RoundedCornerShape(17.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (mine) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                    if (message.photoUrl.isNotBlank()) {
                        AsyncImage(
                            model = message.photoUrl,
                            contentDescription = "Ảnh đại diện",
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
                if (message.type == "image" && message.downloadUrl.isNotBlank()) {
                    AsyncImage(
                        model = message.downloadUrl,
                        contentDescription = "Ảnh được chia sẻ trong cộng đồng",
                        modifier = Modifier.fillMaxWidth().heightIn(max = 230.dp).clip(RoundedCornerShape(12.dp)),
                        contentScale = ContentScale.Crop
                    )
                } else if (message.type == "file" && message.downloadUrl.isNotBlank()) {
                    Row(
                        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(MaterialTheme.colorScheme.surface.copy(alpha = 0.55f)).padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(message.fileName, color = MaterialTheme.colorScheme.onSurface, fontSize = 10.sp, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                            Text(formatCommunitySize(message.fileSize), color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 8.sp)
                        }
                        TextButton(onClick = { onDownload(message.downloadUrl, message.fileName) }) { Text("TẢI", fontSize = 9.sp) }
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
private fun CommunityArchiveCard(
    item: CommunityArchiveItem,
    onDownload: (String, String) -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
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
                if (item.notes.isNotBlank()) Text(item.notes, color = MaterialTheme.colorScheme.onSurface, fontSize = 10.sp)
            } else {
                if (item.displayName.isNotBlank()) {
                    Text(item.displayName, color = MaterialTheme.colorScheme.onSurface, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
                if (item.text.isNotBlank()) {
                    Text(item.text, color = MaterialTheme.colorScheme.onSurface, fontSize = 11.sp)
                }
            }
            if (item.category == "image" && item.downloadUrl.isNotBlank()) {
                AsyncImage(
                    model = item.downloadUrl,
                    contentDescription = "Hình ảnh đã gỡ",
                    modifier = Modifier.fillMaxWidth().heightIn(max = 230.dp).clip(RoundedCornerShape(12.dp)),
                    contentScale = ContentScale.Crop
                )
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
                    TextButton(onClick = { onDownload(item.downloadUrl, item.fileName.ifBlank { "tep_tai_xuong" }) }) {
                        Text("TẢI LẠI", fontSize = 9.sp)
                    }
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
        modifier = Modifier.fillMaxWidth(),
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
                Button(
                    onClick = { onDownload(release.downloadUrl, release.fileName) },
                    enabled = release.downloadUrl.isNotBlank()
                ) {
                    Text("TẢI BẢN CẬP NHẬT", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.width(6.dp))
                if (isOwner) {
                    TextButton(onClick = onDelete) { Text("XÓA", fontSize = 9.sp) }
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
