package com.nextos.module.playermodule.media

import android.app.ProgressDialog
import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import android.provider.OpenableColumns
import com.nextos.module.playermodule.R
import com.nextos.module.playermodule.resources.AppContextProvider
import com.nextos.module.playermodule.resources.VLCOptions
import com.nextos.module.playermodule.tools.AppScope
import com.nextos.module.playermodule.tools.Tools
import com.nextos.module.playermodule.util.FileUtils
import com.nextos.module.playermodule.util.TextUtils
import com.nextos.module.playermodule.util.generateResolutionClass
import com.nextos.module.playermodule.util.isSchemeStreaming
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.actor
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import org.videolan.libvlc.util.AndroidUtil
import com.nextos.module.playermodule.service.PlaybackService
import com.nextos.module.playermodule.tools.markBidi
import java.io.File
import java.util.*

private const val TAG = "VLC/MediaUtils"


object MediaUtils {


    fun loadlastPlaylist(context: Context?, type: Int) {
        if (context == null) return
        SuspendDialogCallback(context) { service -> service.loadLastPlaylist(type) }
    }



    fun openMedia(context: Context?, media: MediaWrapper?) {
        if (media == null || context == null) return
        SuspendDialogCallback(context) { service -> service.load(media) }
    }


    fun openMediaNoUi(uri: Uri) = openMediaNoUi(AppContextProvider.appContext, MLServiceLocator.getAbstractMediaWrapper(uri))

    fun openMediaNoUi(context: Context?, media: MediaWrapper?) {
        if (media == null || context == null) return
        object : BaseCallBack(context) {
            override fun onServiceReady(service: PlaybackService) {
                service.load(media)
            }
        }
    }

    fun playTracks(context: Context, item: MediaLibraryItem, position: Int, shuffle:Boolean = false) = context.scope.launch {
        openList(context, withContext(Dispatchers.IO) { item.tracks }.toList(), position, shuffle)
    }

    @JvmOverloads
    fun openList(context: Context?, list: List<MediaWrapper>, position: Int, shuffle: Boolean = false) {
        if (list.isEmpty() || context == null) return
        SuspendDialogCallback(context) { service ->
            service.load(list, position)
            if (shuffle && !service.isShuffling) service.shuffle()
        }
    }


    fun openUri(context: Context?, uri: Uri?) {
        if (uri == null || context == null) return
        SuspendDialogCallback(context) { service ->
            service.loadUri(uri)
        }
    }

    fun openStream(context: Context?, uri: String?) {
        if (uri == null || context == null) return
        SuspendDialogCallback(context) { service ->
            service.loadLocation(uri)
        }
    }

    fun getMediaArtist(ctx: Context, media: MediaWrapper?): String = when {
        media == null -> getMediaString(ctx, R.string.unknown_artist)
        media.type == MediaWrapper.TYPE_VIDEO -> ""
        media.artist != null -> media.artist
        media.nowPlaying != null -> media.title
        isSchemeStreaming(media.uri.scheme) -> ""
        else -> getMediaString(ctx, R.string.unknown_artist)
    }

    fun getMediaReferenceArtist(ctx: Context, media: MediaWrapper?) = getMediaArtist(ctx, media)

    fun getMediaAlbumArtist(ctx: Context, media: MediaWrapper?) = media?.albumArtist
            ?: getMediaString(ctx, R.string.unknown_artist)

    fun getMediaAlbum(ctx: Context, media: MediaWrapper?): String = when {
        media == null -> getMediaString(ctx, R.string.unknown_album)
        media.album != null -> media.album
        media.nowPlaying != null -> ""
        isSchemeStreaming(media.uri.scheme) -> ""
        else -> getMediaString(ctx, R.string.unknown_album)
    }

    fun getMediaGenre(ctx: Context, media: MediaWrapper?) = media?.genre
            ?: getMediaString(ctx, R.string.unknown_genre)

    fun getMediaSubtitle(media: MediaWrapper): String {
        val prefix = when {
            media.length <= 0L -> null
            media.type == MediaWrapper.TYPE_VIDEO -> Tools.millisToText(media.length)
            else -> Tools.millisToString(media.length)
        }
        val suffix = when {
            media.type == MediaWrapper.TYPE_VIDEO -> generateResolutionClass(media.width, media.height)
            media.length > 0L -> media.artist
            isSchemeStreaming(media.uri.scheme) -> media.uri.toString()
            else -> media.artist
        }
        return TextUtils.separatedString(prefix, suffix)
    }

    fun getDisplaySubtitle(ctx: Context, media: MediaWrapper, mediaPosition: Int, mediaSize: Int): String {
        val album = getMediaAlbum(ctx, media)
        val artist = getMediaArtist(ctx, media)
        val isAlbumUnknown = album == getMediaString(ctx, R.string.unknown_album)
        val isArtistUnknown = artist == getMediaString(ctx, R.string.unknown_artist)
        val prefix = if (mediaSize > 1) "${mediaPosition + 1} / $mediaSize" else null
        val suffix = if (!isArtistUnknown && !isAlbumUnknown) TextUtils.separatedString('-', artist.markBidi(), album.markBidi()) else null
        return TextUtils.separatedString(prefix, suffix)
    }

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

    private fun getMediaString(ctx: Context?, id: Int): String {
        return ctx?.resources?.getString(id)
                ?: when (id) {
                    R.string.unknown_artist -> "Unknown Artist"
                    R.string.unknown_album -> "Unknown Album"
                    R.string.unknown_genre -> "Unknown Genre"
                    else -> ""
                }
    }

    @Suppress("LeakingThis")
    private abstract class BaseCallBack(context: Context) {

        init {
            AppScope.launch {
                onServiceReady(PlaybackService.serviceFlow.filterNotNull().first())
            }
            PlaybackService.start(context)
        }

        abstract fun onServiceReady(service: PlaybackService)
    }

    class SuspendDialogCallback(context: Context, private val task: suspend (service: PlaybackService) -> Unit) {
        private lateinit var dialog: ProgressDialog
        var job: Job = Job()
        val scope = context.scope
        @OptIn(ObsoleteCoroutinesApi::class)
        val actor = scope.actor<Action>(capacity = Channel.UNLIMITED) {
            for (action in channel) when (action) {
                Connect -> {
                    val service = PlaybackService.instance
                    if (service != null) channel.trySend(Task(service, task))
                    else {
                        PlaybackService.start(context)
                        PlaybackService.serviceFlow.filterNotNull().first().let {
                            channel.trySend(Task(it, task))
                        }
                    }
                }
                Disconnect -> dismiss()
                is Task -> {
                    action.task.invoke(action.service)
                    job.cancel()
                    dismiss()
                }
            }
        }

        init {
            job = scope.launch {
                delay(300)
                dialog = ProgressDialog.show(
                        context,
                        "${context.applicationContext.getString(R.string.loading)}…",
                        context.applicationContext.getString(R.string.please_wait), true)
                dialog.setCancelable(true)
                dialog.setOnCancelListener { actor.trySend(Disconnect) }
            }
            actor.trySend(Connect)
        }

        private fun dismiss() {
            try {
                if (this::dialog.isInitialized && dialog.isShowing) dialog.dismiss()
            } catch (ignored: IllegalArgumentException) {}
        }
    }

    fun retrieveMediaTitle(mw: MediaWrapper) = try {
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

    fun deletePlaylist(playlist: Playlist) = AppScope.launch(Dispatchers.IO) { playlist.delete() }



    suspend fun useAsSoundFont(context: Context, uri:Uri) {
        withContext(Dispatchers.IO) {
            FileUtils.copyFile(File(uri.path), VLCOptions.getSoundFontFile(context))
        }
    }
}


private val Context.scope: CoroutineScope
    get() = (this as? CoroutineScope) ?: AppScope

open class Action
private object Connect : Action()
private object Disconnect : Action()
private class Task(val service: PlaybackService, val task: suspend (service: PlaybackService) -> Unit) : Action()
