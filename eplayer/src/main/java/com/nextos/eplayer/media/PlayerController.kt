package com.nextos.eplayer.media

import android.content.Context
import android.net.Uri
import android.os.Environment
import android.util.Log
import com.nextos.eplayer.media.media.MediaWrapper
import com.nextos.eplayer.media.utils.FileUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.withContext
import org.videolan.libvlc.FactoryManager
import org.videolan.libvlc.MediaPlayer
import org.videolan.libvlc.interfaces.IMediaFactory
import java.io.File

/**
 * Created by zhangxinmin on 2023/6/29.
 */

class PlayerController(val context: Context) : MediaPlayer.EventListener, CoroutineScope {

    private val settings by lazy(LazyThreadSafetyMode.NONE) { Settings.getInstance(context) }
    override val coroutineContext = Dispatchers.Main.immediate + SupervisorJob()
    private val mediaFactory = FactoryManager.getFactory(IMediaFactory.factoryId) as IMediaFactory

    var mediaplayer = newMediaPlayer()
        private set
    private var onPlayStateChangedListener: OnPlayStateChangedListener? = null

    private fun newMediaPlayer(): MediaPlayer {
        return MediaPlayer(VLCInstance.getInstance(context)).apply {
            setAudioDigitalOutputEnabled(VLCOptions.isAudioDigitalOutputEnabled(settings))
            VLCOptions.getAout(settings)?.let { setAudioOutput(it) }
        }
    }

    fun setOnPlayStateChangedListener(listener: OnPlayStateChangedListener) {
        onPlayStateChangedListener = listener
    }

    suspend fun playSound(filePath: String): Boolean {
        if (!isExternalStorageWritable()) return false

        val srcFile = File(filePath)
        if (srcFile.exists()) {
            val srcUri = Uri.parse(srcFile.absolutePath)
            val mw = MLServiceLocator.getAbstractMediaWrapper(srcUri)
            if (mw.type != MediaWrapper.TYPE_VIDEO || mw.hasFlag(
                    MediaWrapper.MEDIA_FORCE_AUDIO
                )
            ) {
                var mwUri: Uri? = withContext(Dispatchers.IO) {
                    FileUtils.getUri(mw.uri)
                } ?: return false

                val media = mediaFactory.getFromUri(VLCInstance.getInstance(context), mwUri)
                if (!mediaplayer.isReleased) {
                    withContext(Dispatchers.IO) {
                        mediaplayer.media = media.apply {
                            parse()
                        }
                    }
                    mediaplayer.setEventListener(this@PlayerController)
                    mediaplayer.play()
                    return true
                }
            }
        }
        return false
    }

    override fun onEvent(event: MediaPlayer.Event?) {
        Log.d("zxm==", "PlayerController....onEvent()..type:${event?.type}")
        when (event?.type) {
            MediaPlayer.Event.EndReached -> {
                onPlayStateChangedListener?.onPlayCompleted()
            }
        }
    }

    private fun isExternalStorageWritable(): Boolean {
        val state = Environment.getExternalStorageState()
        return Environment.MEDIA_MOUNTED == state
    }
}