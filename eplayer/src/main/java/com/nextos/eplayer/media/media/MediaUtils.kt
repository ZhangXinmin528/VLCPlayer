package com.nextos.eplayer.media.media

import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import android.provider.OpenableColumns
import com.nextos.eplayer.media.AppContextProvider
import com.nextos.eplayer.media.VLCOptions
import com.nextos.eplayer.media.utils.FileUtils
import kotlinx.coroutines.*
import org.videolan.libvlc.util.AndroidUtil
import java.io.File

private const val TAG = "VLC/MediaUtils"


object MediaUtils {



    fun getMediaTitle(mediaWrapper: MediaWrapper) = mediaWrapper.title
            ?: FileUtils.getFileNameFromPath(mediaWrapper.location)

    fun getContentMediaUri(data: Uri) = try {
        AppContextProvider.appContext.contentResolver.query(data,
                arrayOf(MediaStore.Video.Media.DATA), null, null, null)?.use {
            val columnIndex = it.getColumnIndexOrThrow(MediaStore.Video.Media.DATA)
            if (it.moveToFirst()) AndroidUtil.PathToUri(it.getString(columnIndex)) ?: data else data
        }
    } catch (e: SecurityException) {
        data
    } catch (e: IllegalArgumentException) {
        data
    } catch (e: NullPointerException) {
        data
    }



    fun retrieveMediaTitle(mw: MediaWrapper): Unit? {
        return try {
            AppContextProvider.appContext.contentResolver.query(mw.uri, null, null, null, null)?.use {
                val nameIndex = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (nameIndex > -1 && it.count > 0) {
                    it.moveToFirst()
                    if (!it.isNull(nameIndex)) mw.title = it.getString(nameIndex)
                }
            }
        } catch (ignored: UnsupportedOperationException) {
        } catch (ignored: IllegalArgumentException) {
        } catch (ignored: NullPointerException) {
        } catch (ignored: IllegalStateException) {
        } catch (ignored: SecurityException) {}
    }



    suspend fun useAsSoundFont(context: Context, uri:Uri) {
        withContext(Dispatchers.IO) {
            FileUtils.copyFile(File(uri.path), VLCOptions.getSoundFontFile(context))
        }
    }
}

