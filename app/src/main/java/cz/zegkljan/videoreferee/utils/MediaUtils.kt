/*
 * Copyright 2020 The Android Open Source Project
 * Modifications copyright 2021 Jan Žegklitz
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package cz.zegkljan.videoreferee.utils

import android.annotation.SuppressLint
import android.content.ContentValues
import android.content.Context
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.ParcelFileDescriptor
import android.provider.MediaStore
import android.util.Log
import androidx.core.net.toUri
import java.io.File
import java.io.FileDescriptor
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

private const val TAG = "Files"
private val SDF = SimpleDateFormat("yyyy_MM_dd_HH_mm_ss_SSS", Locale.US)

/** Creates a [File] in the app-specific directory */
fun createDummyFile(context: Context): File {
    return File(context.filesDir, "dummyfile")
}

abstract class MediaItem {
    abstract fun getUriString(): String
    abstract fun getWriteFileDescriptor(context: Context): FileDescriptor
    abstract fun closeFileDescriptor()
    open fun finalize(context: Context) = Unit
}

private class MediaStoreItem(val uri: Uri) : MediaItem() {
    var fd: ParcelFileDescriptor? = null

    override fun getUriString(): String {
        return uri.toString()
    }

    override fun getWriteFileDescriptor(context: Context): FileDescriptor {
        fd = context.contentResolver.openFileDescriptor(uri, "w")
        return fd!!.fileDescriptor
    }

    override fun closeFileDescriptor() {
        if (fd == null) {
            return
        }
        fd!!.close()
    }

    @SuppressLint("InlinedApi")
    override fun finalize(context: Context) {
        val resolver = context.contentResolver
        resolver.update(uri, ContentValues().apply {
            put(MediaStore.Video.Media.IS_PENDING, 0)
        }, null, null)
    }

    override fun toString(): String {
        return "MediaStoreItem($uri)"
    }
}

private class FileItem(val file: File) : MediaItem() {
    var fis: FileOutputStream? = null

    override fun getUriString(): String {
        return file.toUri().toString()
    }

    override fun getWriteFileDescriptor(context: Context): FileDescriptor {
        fis = FileOutputStream(file)
        return fis!!.fd
    }

    override fun closeFileDescriptor() {
        if (fis == null) {
            return
        }
        fis!!.close()
    }

    override fun toString(): String {
        return "FileItem($file)"
    }

    override fun finalize(context: Context) {
        MediaScannerConnection.scanFile(context, arrayOf(file.absolutePath), arrayOfNulls(1), null)
    }
}

/** Creates a media [Uri] named with the current date and time */
fun prepareMediaItem(context: Context, extension: String): MediaItem {
    // Log.d(TAG, "createFile")

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        val resolver = context.contentResolver
        val videoCollection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        } else {
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI
        }
        val videoDetails = ContentValues().apply {
            put(MediaStore.Video.Media.DISPLAY_NAME, "VID_${SDF.format(Date())}.$extension")
            put(MediaStore.MediaColumns.RELATIVE_PATH, "DCIM/VideoReferee/")
            put(MediaStore.Video.Media.IS_PENDING, 1)
        }
        val videoUri = resolver.insert(videoCollection, videoDetails)
        Log.d(TAG, videoUri.toString())

        return MediaStoreItem(videoUri!!)
    } else {
        val externalFilesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM)
        val videoRefereeDir = File(externalFilesDir, "VideoReferee")
        videoRefereeDir.mkdirs()
        val file = File(videoRefereeDir, "VID_${SDF.format(Date())}.$extension")
        return FileItem(file)
    }
}
