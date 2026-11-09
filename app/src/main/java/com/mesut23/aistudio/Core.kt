package com.mesut23.aistudio

import android.app.Application
import android.content.ContentValues
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Base64
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.exifinterface.media.ExifInterface
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.segmentation.subject.SubjectSegmentation
import com.google.mlkit.vision.segmentation.subject.SubjectSegmenterOptions
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.util.UUID

class QuotaException : Exception()

interface AiProvider {
    suspend fun credits(): Int
    suspend fun generate(prompt: String, style: Int): ByteArray
    suspend fun edit(jpeg: ByteArray, instruction: String): ByteArray
    suspend fun reward(): Int
}

/** Provider secrets stay on the server; the Android app only talks to this proxy. */
class ProxyProvider(baseUrl: String, private val deviceId: String) : AiProvider {
    private val base = baseUrl.trim().trimEnd('/')

    private suspend fun request(path: String, payload: JSONObject? = null): JSONObject =
        withContext(Dispatchers.IO) {
            if (base.isBlank() || base.contains("CHANGE-ME")) throw IOException("Backend URL is not configured")
            val connection = URL(base + path).openConnection() as HttpURLConnection
            try {
                connection.requestMethod = if (payload == null) "GET" else "POST"
                connection.connectTimeout = 20_000
                connection.readTimeout = 120_000
                connection.setRequestProperty("Accept", "application/json")
                connection.setRequestProperty("X-Device-Id", deviceId)
                if (payload != null) {
                    connection.doOutput = true
                    connection.setRequestProperty("Content-Type", "application/json; charset=utf-8")
                    connection.outputStream.use { it.write(payload.toString().toByteArray(Charsets.UTF_8)) }
                }
                val status = connection.responseCode
                val stream = if (status in 200..299) connection.inputStream else connection.errorStream
                val response = stream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty()
                if (status == 429) throw QuotaException()
                if (status !in 200..299) throw IOException("Server returned HTTP $status")
                JSONObject(response)
            } finally { connection.disconnect() }
        }

    private fun decodeImage(json: JSONObject): ByteArray {
        val encoded = json.optString("image")
        if (encoded.isBlank()) throw IOException("The server returned no image")
        return try { Base64.decode(encoded, Base64.DEFAULT) }
        catch (_: IllegalArgumentException) { throw IOException("Invalid image response") }
    }

    override suspend fun credits(): Int = request("/credits").getInt("credits")

    override suspend fun generate(prompt: String, style: Int): ByteArray {
        require(prompt.isNotBlank()) { "Prompt cannot be empty" }
        return decodeImage(request("/generate", JSONObject()
            .put("prompt", prompt.trim().take(2000))
            .put("style", style.coerceIn(0, 100))))
    }

    override suspend fun edit(jpeg: ByteArray, instruction: String): ByteArray {
        require(instruction.isNotBlank()) { "Edit instruction cannot be empty" }
        return decodeImage(request("/edit", JSONObject()
            .put("prompt", instruction.trim().take(1500))
            .put("image", Base64.encodeToString(jpeg, Base64.NO_WRAP))))
    }

    // Credit rewards must be verified server-side with AdMob SSV.
    override suspend fun reward(): Int = request("/reward", JSONObject()).getInt("credits")
}

fun mimeOf(file: File): String = if (file.extension.equals("png", true)) "image/png" else "image/jpeg"

val RATIOS = listOf("1:1" to 1f, "9:16" to (9f / 16f), "16:9" to (16f / 9f), "4:3" to (4f / 3f))

class AppVM(app: Application) : AndroidViewModel(app) {
    private val context = app
    private val preferences = app.getSharedPreferences("mesut_ai", 0)
    private val provider: AiProvider = ProxyProvider(BuildConfig.BACKEND_URL, getDeviceId())
    private val creationsDir = File(app.filesDir, "creations").apply { mkdirs() }

    var style by mutableIntStateOf(0)
    var premium by mutableStateOf(false)
        private set
    var favorites by mutableStateOf(preferences.getStringSet("favorites", emptySet())?.toSet() ?: emptySet())
        private set
    var credits by mutableIntStateOf(-1)
        private set
    var busy by mutableStateOf(false)
        private set
    var result by mutableStateOf<File?>(null)
        private set
    var results by mutableStateOf<List<File>>(emptyList())
        private set
    var modelPreparing by mutableStateOf(false)
        private set
    var modelMessage by mutableIntStateOf(0)
        private set
    var error by mutableIntStateOf(0)
        private set
    var files by mutableStateOf(listCreations())
        private set

    private fun getDeviceId(): String {
        preferences.getString("device_id", null)?.takeIf { it.isNotBlank() }?.let { return it }
        return UUID.randomUUID().toString().replace("-", "").also {
            preferences.edit().putString("device_id", it).apply()
        }
    }

    private fun listCreations(): List<File> =
        creationsDir.listFiles()?.filter { it.isFile && (it.extension.equals("jpg", true) || it.extension.equals("png", true)) }
            ?.sortedByDescending { it.lastModified() } ?: emptyList()

    fun refresh() {
        viewModelScope.launch {
            try { credits = provider.credits(); error = 0 }
            catch (_: Exception) { if (credits < 0) error = R.string.error_network }
        }
    }

    fun clear() { result = null; results = emptyList(); error = 0 }

    fun generate(prompt: String, selectedStyle: Int, ratio: Float, count: Int = 1) {
        if (!busy && prompt.isNotBlank()) runImageJob(ratio, count = count.coerceIn(1, 4)) {
            provider.generate(prompt, selectedStyle)
        }
    }

    fun edit(uri: Uri, instruction: String) {
        if (!busy && instruction.isNotBlank()) runImageJob { provider.edit(loadJpeg(uri), instruction) }
    }

    fun reward() {
        viewModelScope.launch {
            try { credits = provider.reward(); error = 0 }
            catch (_: QuotaException) { error = R.string.reward_max }
            catch (_: Exception) { error = R.string.error_network }
        }
    }

    private fun runImageJob(
        ratio: Float? = null,
        ext: String = "jpg",
        count: Int = 1,
        operation: suspend () -> ByteArray
    ) {
        viewModelScope.launch {
            busy = true
            error = 0
            results = emptyList()
            result = null
            try {
                repeat(count.coerceIn(1, 4)) {
                    val bytes = operation()
                    if (bytes.isEmpty()) throw IOException("Empty image")
                    val file = withContext(Dispatchers.IO) {
                        val output = File(creationsDir, "mesut_" + System.currentTimeMillis() + "_" + UUID.randomUUID().toString().take(6) + "." + ext)
                        output.writeBytes(if (ratio == null) bytes else cropToRatio(bytes, ratio))
                        output
                    }
                    result = file
                    results = results + file
                    files = listCreations()
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: QuotaException) {
                error = if (results.isEmpty()) R.string.quota_reached else R.string.quota_partial
            } catch (_: IOException) {
                error = R.string.error_network
            } catch (_: Exception) {
                error = R.string.error_generic
            } finally {
                busy = false
                refresh()
            }
        }
    }

    fun removeBackground(uri: Uri) {
        if (busy) return
        runImageJob(ext = "png") {
            withContext(Dispatchers.IO) {
                val bitmap = loadBitmap(uri, 1536)
                val segmenter = SubjectSegmentation.getClient(
                    SubjectSegmenterOptions.Builder().enableForegroundBitmap().build()
                )
                try {
                    val foreground = segmenter.process(InputImage.fromBitmap(bitmap, 0)).await().foregroundBitmap
                        ?: throw IOException("No foreground subject was detected")
                    ByteArrayOutputStream().use { output ->
                        if (!foreground.compress(Bitmap.CompressFormat.PNG, 100, output)) throw IOException("Could not encode PNG")
                        output.toByteArray()
                    }
                } finally {
                    segmenter.close()
                    bitmap.recycle()
                }
            }
        }
    }

    fun enlarge(uri: Uri) {
        if (busy) return
        runImageJob {
            withContext(Dispatchers.IO) {
                val source = loadBitmap(uri, 1536)
                val scaled = Bitmap.createScaledBitmap(source, source.width * 2, source.height * 2, true)
                try {
                    ByteArrayOutputStream().use { output ->
                        if (!scaled.compress(Bitmap.CompressFormat.JPEG, 94, output)) throw IOException("Could not encode image")
                        output.toByteArray()
                    }
                } finally {
                    if (scaled !== source) scaled.recycle()
                    source.recycle()
                }
            }
        }
    }

    fun prepareBackgroundModel() {
        if (modelPreparing) return
        viewModelScope.launch {
            modelPreparing = true
            modelMessage = 0
            try {
                withContext(Dispatchers.IO) {
                    val segmenter = SubjectSegmentation.getClient(
                        SubjectSegmenterOptions.Builder().enableForegroundBitmap().build()
                    )
                    val sample = Bitmap.createBitmap(128, 128, Bitmap.Config.ARGB_8888)
                    try { segmenter.process(InputImage.fromBitmap(sample, 0)).await() }
                    finally { sample.recycle(); segmenter.close() }
                }
                modelMessage = R.string.model_ready
            } catch (_: Exception) {
                modelMessage = R.string.model_download_pending
            } finally { modelPreparing = false }
        }
    }

    private fun cropToRatio(bytes: ByteArray, ratio: Float): ByteArray {
        val source = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
            ?: throw IOException("Unable to decode generated image")
        val sourceRatio = source.width.toFloat() / source.height
        val width: Int
        val height: Int
        if (sourceRatio > ratio) {
            height = source.height
            width = (height * ratio).toInt().coerceAtLeast(1)
        } else {
            width = source.width
            height = (width / ratio).toInt().coerceAtLeast(1)
        }
        val cropped = Bitmap.createBitmap(source, (source.width - width) / 2, (source.height - height) / 2, width, height)
        return try {
            ByteArrayOutputStream().use { output ->
                if (!cropped.compress(Bitmap.CompressFormat.JPEG, 94, output)) throw IOException("Unable to encode image")
                output.toByteArray()
            }
        } finally {
            if (cropped !== source) cropped.recycle()
            source.recycle()
        }
    }

    private suspend fun loadJpeg(uri: Uri): ByteArray = withContext(Dispatchers.IO) {
        val bitmap = loadBitmap(uri, 1600)
        ByteArrayOutputStream().use { output ->
            try {
                if (!bitmap.compress(Bitmap.CompressFormat.JPEG, 90, output)) throw IOException("Cannot encode selected image")
                output.toByteArray()
            } finally { bitmap.recycle() }
        }
    }

    private fun loadBitmap(uri: Uri, maxDimension: Int): Bitmap {
        val resolver = context.contentResolver
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
            ?: throw IOException("Cannot open selected image")
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) throw IOException("Invalid image")
        var sample = 1
        while (maxOf(bounds.outWidth, bounds.outHeight) / sample > maxDimension) sample *= 2
        val decoded = resolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { inSampleSize = sample })
        } ?: throw IOException("Cannot decode selected image")
        val orientation = runCatching {
            resolver.openInputStream(uri)?.use { ExifInterface(it).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL) }
                ?: ExifInterface.ORIENTATION_NORMAL
        }.getOrDefault(ExifInterface.ORIENTATION_NORMAL)
        val matrix = Matrix()
        when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
            ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
            ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
            ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.postScale(-1f, 1f)
            ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.postScale(1f, -1f)
            ExifInterface.ORIENTATION_TRANSPOSE -> { matrix.postScale(-1f, 1f); matrix.postRotate(90f) }
            ExifInterface.ORIENTATION_TRANSVERSE -> { matrix.postScale(-1f, 1f); matrix.postRotate(270f) }
        }
        if (matrix.isIdentity) return decoded
        return Bitmap.createBitmap(decoded, 0, 0, decoded.width, decoded.height, matrix, true).also {
            if (it !== decoded) decoded.recycle()
        }
    }

    fun saveToGallery(file: File): Boolean = runCatching {
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, "MesutAI_" + file.name)
            put(MediaStore.Images.Media.MIME_TYPE, mimeOf(file))
            if (Build.VERSION.SDK_INT >= 29) {
                put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/Mesut AI Studio")
                put(MediaStore.Images.Media.IS_PENDING, 1)
            }
        }
        val uri = context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
            ?: error("Unable to create media item")
        context.contentResolver.openOutputStream(uri)?.use { output ->
            file.inputStream().use { it.copyTo(output) }
        } ?: error("Unable to write media item")
        if (Build.VERSION.SDK_INT >= 29) {
            context.contentResolver.update(uri, ContentValues().apply {
                put(MediaStore.Images.Media.IS_PENDING, 0)
            }, null, null)
        }
    }.isSuccess

    private fun updateFavorites(value: Set<String>) {
        favorites = value
        preferences.edit().putStringSet("favorites", value).apply()
    }

    fun toggleFav(file: File) {
        updateFavorites(if (file.name in favorites) favorites - file.name else favorites + file.name)
    }

    fun delete(file: File) {
        file.delete()
        updateFavorites(favorites - file.name)
        if (result == file) result = null
        results = results - file
        files = listCreations()
    }

    fun deleteAll() {
        files.forEach { it.delete() }
        updateFavorites(emptySet())
        result = null
        results = emptyList()
        files = listCreations()
    }
}
