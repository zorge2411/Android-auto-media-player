package com.pscholer.autoplayer.data.local

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import androidx.documentfile.provider.DocumentFile
import com.pscholer.autoplayer.data.models.MediaItem
import com.pscholer.autoplayer.util.PreferencesManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LocalMediaRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val prefs: PreferencesManager
) {
    companion object {
        private const val TAG = "LocalMediaRepo"

        private val SUPPORTED_MIME = setOf(
            "video/mp4", "video/x-matroska", "video/webm",
            "video/avi", "video/x-msvideo", "video/quicktime",
            "video/3gpp", "video/x-flv", "video/x-ms-wmv"
        )
    }

    /**
     * Scan device video library using MediaStore.
     *
     * Covers: /Movies, /DCIM, /Downloads and any other folder Android
     * has indexed. On API 29+ uses the scoped VOLUME_EXTERNAL collection.
     * Requires READ_MEDIA_VIDEO (API 33+) or READ_EXTERNAL_STORAGE (pre-33).
     */
    suspend fun scanMediaStore(): List<MediaItem> = withContext(Dispatchers.IO) {
        val results = mutableListOf<MediaItem>()

        val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
        } else {
            @Suppress("DEPRECATION")
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI
        }

        val projection = arrayOf(
            MediaStore.Video.Media._ID,
            MediaStore.Video.Media.DISPLAY_NAME,
            MediaStore.Video.Media.DURATION,
            MediaStore.Video.Media.MIME_TYPE
        )

        context.contentResolver.query(
            collection,
            projection,
            null, null,
            "${MediaStore.Video.Media.DISPLAY_NAME} ASC"
        )?.use { cursor ->
            val idCol   = cursor.getColumnIndexOrThrow(MediaStore.Video.Media._ID)
            val nameCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DISPLAY_NAME)
            val durCol  = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DURATION)
            val mimeCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.MIME_TYPE)

            while (cursor.moveToNext()) {
                val mime = cursor.getString(mimeCol) ?: ""
                if (!mime.startsWith("video/")) continue

                val id  = cursor.getLong(idCol)
                val uri = ContentUris.withAppendedId(collection, id)

                results += MediaItem(
                    id         = id.toString(),
                    title      = cursor.getString(nameCol),
                    streamUrl  = uri.toString(),
                    durationMs = cursor.getLong(durCol),
                    mimeType   = mime
                )
            }
        }

        Log.d(TAG, "MediaStore: found ${results.size} videos")
        results
    }

    /**
     * Get list of user-added SAF folders as folder items.
     */
    fun getSafFolders(): List<MediaItem> {
        return prefs.safUris.map { uri ->
            val rootDir = DocumentFile.fromTreeUri(context, uri)
            MediaItem(
                id = uri.toString(),
                title = rootDir?.name ?: "Folder",
                isFolder = true,
                subtitle = "Local folder"
            )
        }
    }

    /**
     * Scan a Storage Access Framework (SAF) tree URI.
     *
     * Used for:
     *  • External USB drives connected to the phone (OTG)
     *  • Custom folders the user explicitly grants access to
     *  • Directories outside MediaStore's scan scope
     *
     * The user grants access via Intent(Intent.ACTION_OPEN_DOCUMENT_TREE)
     * and the returned URI is persisted with takePersistableUriPermission().
     * Pass that URI here.
     */
    suspend fun scanSafDirectory(treeUri: Uri): List<MediaItem> = withContext(Dispatchers.IO) {
        val results = mutableListOf<MediaItem>()
        val rootDir = DocumentFile.fromTreeUri(context, treeUri)
        if (rootDir == null) {
            Log.w(TAG, "SAF: could not open tree URI $treeUri")
            return@withContext results
        }
        walkDocumentTree(rootDir, results)
        Log.d(TAG, "SAF scan of $treeUri: found ${results.size} videos")
        results
    }

    private fun walkDocumentTree(dir: DocumentFile, sink: MutableList<MediaItem>) {
        dir.listFiles().forEach { file ->
            when {
                file.isDirectory -> walkDocumentTree(file, sink)
                file.type in SUPPORTED_MIME -> sink += MediaItem(
                    id        = file.uri.toString(),
                    title     = file.name ?: "Unknown",
                    streamUrl = file.uri.toString(),
                    mimeType  = file.type
                )
            }
        }
    }

    suspend fun getItem(uriString: String, title: String): MediaItem? = withContext(Dispatchers.IO) {
        try {
            val uri = Uri.parse(uriString)
            val mime = context.contentResolver.getType(uri) ?: "video/*"
            MediaItem(
                id = uriString,
                title = title,
                streamUrl = uriString,
                mimeType = mime
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to reconstruct local MediaItem from $uriString", e)
            null
        }
    }
}
