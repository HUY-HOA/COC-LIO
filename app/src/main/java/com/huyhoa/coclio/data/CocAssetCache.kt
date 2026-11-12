package com.huyhoa.coclio.data

import android.content.Context
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import java.util.concurrent.TimeUnit

object CocAssetCache {
    private const val DIRECTORY = "coc-lio-assets"

    private fun targetFile(context: Context, url: String): File {
        val digest = MessageDigest.getInstance("SHA-256")
            .digest(url.toByteArray(Charsets.UTF_8))
            .joinToString("") { byte -> (byte.toInt() and 0xff).toString(16).padStart(2, '0') }
        return File(File(context.filesDir, DIRECTORY), "$digest.png")
    }

    fun cachedFile(context: Context, url: String): File? =
        targetFile(context, url).takeIf { it.isFile && it.length() > 0L }

    suspend fun prefetch(context: Context, urls: List<String>): Int = withContext(Dispatchers.IO) {
        val uniqueUrls = urls.distinct()
        val missingUrls = uniqueUrls.filter { cachedFile(context, it) == null }
        val semaphore = Semaphore(4)

        coroutineScope {
            missingUrls.map { url ->
                async {
                    semaphore.withPermit {
                        if (download(context, url)) 0 else 1
                    }
                }
            }.awaitAll().sum()
        }
    }

    private fun download(context: Context, url: String): Boolean {
        val target = targetFile(context, url)
        if (target.isFile && target.length() > 0L) return true

        target.parentFile?.mkdirs()
        val temporary = File(target.parentFile, target.name + ".part-" + Thread.currentThread().id)
        var connection: HttpURLConnection? = null

        return try {
            connection = (URL(url).openConnection() as HttpURLConnection).apply {
                connectTimeout = 15000
                readTimeout = 25000
                requestMethod = "GET"
                instanceFollowRedirects = true
                setRequestProperty("User-Agent", "COC-LIO Android")
            }

            if (connection.responseCode !in 200..299) return false

            connection.inputStream.use { input ->
                temporary.outputStream().buffered().use { output ->
                    input.copyTo(output)
                }
            }

            if (!temporary.isFile || temporary.length() == 0L) return false

            if (target.isFile && target.length() > 0L) {
                temporary.delete()
            } else if (!temporary.renameTo(target)) {
                temporary.copyTo(target, overwrite = true)
                temporary.delete()
            }

            target.isFile && target.length() > 0L
        } catch (_: Exception) {
            false
        } finally {
            temporary.delete()
            connection?.disconnect()
        }
    }
}


class CocAssetPrefetchWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {
    override suspend fun doWork(): Result {
        val profile = inputData.getString("profile") ?: "local"
        val urls = VillageStore.loadAll(applicationContext, profile)
            .asSequence()
            .flatMap { it.items.asSequence() }
            .mapNotNull(CocAssets::imageUrl)
            .distinct()
            .toList()

        val failedCount = CocAssetCache.prefetch(applicationContext, urls)
        return if (failedCount > 0) Result.retry() else Result.success()
    }

    companion object {
        fun enqueue(context: Context, profile: String, assetSignature: Int) {
            val request = OneTimeWorkRequestBuilder<CocAssetPrefetchWorker>()
                .setInputData(Data.Builder().putString("profile", profile).build())
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .build()
                )
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
                .build()

            val uniqueName = "coc_lio_assets_" +
                profile.hashCode().toUInt().toString(16) + "_" +
                assetSignature.toUInt().toString(16)

            WorkManager.getInstance(context)
                .enqueueUniqueWork(uniqueName, ExistingWorkPolicy.KEEP, request)
        }
    }
}
