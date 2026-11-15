package com.huyhoa.coclio

import android.content.Context
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import org.json.JSONArray
import org.json.JSONObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

internal object CloudflareRemoteContentClient {
    private const val ENDPOINT =
        "https://coc-lio-remote-content.coc-lio-data.workers.dev/v1/content"
    private const val CACHE_FILE = "coc-lio-remote-content.json"
    private const val PREFS = "coc_lio_cloudflare_content"
    private const val MAX_BYTES = 8 * 1024 * 1024

    suspend fun load(context: Context): RemoteContentState? = withContext(Dispatchers.IO) {
        val appContext = context.applicationContext
        val cacheFile = File(appContext.filesDir, CACHE_FILE)
        val cachedRaw = runCatching {
            cacheFile.takeIf { it.isFile && it.length() in 1..MAX_BYTES.toLong() }
                ?.readText(Charsets.UTF_8)
        }.getOrNull()
        val cachedState = cachedRaw?.let(::parseState)
        val preferences = appContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        var connection: HttpURLConnection? = null

        try {
            connection = (URL(ENDPOINT).openConnection() as HttpURLConnection).apply {
                connectTimeout = 8000
                readTimeout = 12000
                requestMethod = "GET"
                doInput = true
                useCaches = false
                instanceFollowRedirects = false
                setRequestProperty("Accept", "application/json")
                setRequestProperty("User-Agent", "COC-LIO Android")
                if (cachedState != null) {
                    preferences.getString("etag", null)
                        ?.takeIf { it.isNotBlank() }
                        ?.let { setRequestProperty("If-None-Match", it) }
                }
            }

            val status = connection.responseCode
            if (status == HttpURLConnection.HTTP_NOT_MODIFIED) {
                return@withContext cachedState
            }
            if (status !in 200..299 || connection.contentLengthLong > MAX_BYTES) {
                return@withContext cachedState
            }

            val raw = connection.inputStream.use(::readLimited).toString(Charsets.UTF_8)
            val remoteState = parseState(raw) ?: return@withContext cachedState
            if (cachedState != null && remoteState.version <= cachedState.version) {
                return@withContext cachedState
            }

            saveAtomically(cacheFile, raw)
            connection.getHeaderField("ETag")
                ?.takeIf { it.isNotBlank() && it.length <= 200 }
                ?.let { preferences.edit().putString("etag", it).apply() }

            remoteState
        } catch (_: Exception) {
            cachedState
        } finally {
            connection?.disconnect()
        }
    }

    private fun parseState(raw: String): RemoteContentState? = runCatching {
        val root = JSONObject(raw)
        require(root.optInt("version", 0) >= 1)
        require(root.opt("catalog") is JSONArray)
        require(root.optJSONArray("catalog")?.length() ?: 0 <= 500)
        parseRemoteContent(root.toSafeMap())
    }.getOrNull()

    private fun saveAtomically(target: File, value: String) {
        val temporary = File(target.parentFile, target.name + ".tmp")
        FileOutputStream(temporary).use { output ->
            output.write(value.toByteArray(Charsets.UTF_8))
            output.fd.sync()
        }
        if (!temporary.renameTo(target)) {
            temporary.copyTo(target, overwrite = true)
            temporary.delete()
        }
    }

    private fun readLimited(input: InputStream): ByteArray {
        val output = ByteArrayOutputStream()
        val buffer = ByteArray(8192)
        while (true) {
            val count = input.read(buffer)
            if (count < 0) break
            if (count == 0) continue
            require(output.size() + count <= MAX_BYTES)
            output.write(buffer, 0, count)
        }
        return output.toByteArray()
    }

    private fun JSONObject.toSafeMap(): Map<String, Any?> {
        val result = linkedMapOf<String, Any?>()
        val iterator = keys()
        while (iterator.hasNext()) {
            val key = iterator.next()
            result[key] = toSafeValue(opt(key))
        }
        return result
    }

    private fun toSafeValue(value: Any?): Any? = when (value) {
        null, JSONObject.NULL -> null
        is JSONObject -> value.toSafeMap()
        is JSONArray -> (0 until value.length()).map { toSafeValue(value.opt(it)) }
        else -> value
    }
}
