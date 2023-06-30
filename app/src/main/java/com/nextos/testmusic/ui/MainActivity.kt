package com.nextos.testmusic.ui

import android.Manifest
import android.content.ContentResolver
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Environment
import android.os.Environment.DIRECTORY_DOWNLOADS
import android.os.Environment.DIRECTORY_MUSIC
import android.util.Log
import android.view.View
import android.view.View.OnClickListener
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.lifecycle.lifecycleScope
import com.nextos.core.BaseActivity
import com.nextos.core.BaseApp.Companion.context
import com.nextos.module.playermodule.media.MLServiceLocator
import com.nextos.module.playermodule.media.MediaUtils
import com.nextos.module.playermodule.media.MediaWrapper
import com.nextos.module.playermodule.resources.VLCInstance
import com.nextos.module.playermodule.resources.VLCOptions
import com.nextos.module.playermodule.service.PlaybackService
import com.nextos.module.playermodule.tools.Settings
import com.nextos.module.playermodule.tools.toast
import com.nextos.testmusic.R
import com.nextos.testmusic.databinding.ActivityMainBinding
import com.zxm.utils.core.dialog.DialogUtil
import com.zxm.utils.core.file.FileUtils
import com.zxm.utils.core.permission.PermissionChecker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.videolan.libvlc.FactoryManager
import org.videolan.libvlc.MediaPlayer
import org.videolan.libvlc.MediaPlayer.Event
import org.videolan.libvlc.interfaces.IMediaFactory
import java.io.File
import java.util.logging.Logger

open class MainActivity : BaseActivity(), OnClickListener, PlaybackService.Callback {

    private lateinit var mainBinding: ActivityMainBinding
    private var service: PlaybackService? = null
    private lateinit var startedScope: CoroutineScope
    private val mediaFactory = FactoryManager.getFactory(IMediaFactory.factoryId) as IMediaFactory
    private val settings by lazy(LazyThreadSafetyMode.NONE) {
        Settings.getInstance(
            applicationContext
        )
    }

    override fun setContentLayout(): Any {
        mainBinding = ActivityMainBinding.inflate(layoutInflater)
        return mainBinding.root
    }


    override fun initParamsAndValues() {

    }

    override fun initViews() {
        mainBinding.btnPermission.setOnClickListener(this)
        mainBinding.btnPlayMusic.setOnClickListener(this)
        mainBinding.btnDirectPlay.setOnClickListener(this)

    }

    override fun onStart() {
        super.onStart()
        startedScope = MainScope()
        PlaybackService.start(this)
        PlaybackService.serviceFlow.onEach { onServiceChanged(it) }.launchIn(startedScope)
    }

    private fun setOption() {

    }

    open fun onServiceChanged(service: PlaybackService?) {
        if (service != null) {
            this.service = service
            service.addCallback(this)
        }
    }

    /**
     * check permission
     */
    private fun checkSinglePermission() {
        if (!PermissionChecker.checkPersmission(
                mContext, Manifest.permission.WRITE_EXTERNAL_STORAGE
            )
        ) {
            PermissionChecker.requestPermissions(
                this, arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE), REQUEST_EXTERNAL
            )
        }
    }

    override fun onClick(v: View?) {
        when (v?.id) {
            R.id.btn_permission -> {
                checkSinglePermission()
            }

            R.id.btn_play_music -> {
                if (!isExternalStorageWritable()) {
                    Toast.makeText(this, "权限未授予", Toast.LENGTH_SHORT).show()
                    return
                }
//                val dir = Environment.getExternalStoragePublicDirectory(DIRECTORY_DOWNLOADS)
//                val songPath = File(dir, "f1_bass_41_43.ac3").absolutePath
//                if (!FileUtils.isFileExists(songPath)) {
//                    Toast.makeText(this, "歌曲不存在", Toast.LENGTH_SHORT).show()
//                    return
//                }
//                val songUri = Uri.parse(ContentResolver.SCHEME_ANDROID_RESOURCE + "://" + packageName + "/raw/f1_bass_41_43.ac3")
//                val songUri = Uri.parse("file:///android_asset//f1_bass_41_43.ac3")

                //共享目录
                val dir = Environment.getExternalStoragePublicDirectory(DIRECTORY_MUSIC)
                val songPath = File(dir, "f1_bass_41_43.ac3").absolutePath
//                val songPath = File(dir, "父亲写的散文诗-许飞.flac").absolutePath
                if (!FileUtils.isFileExists(songPath)) {
                    Toast.makeText(this, "歌曲不存在", Toast.LENGTH_SHORT).show()
                    return
                }

                //私有目录
//                val dir = cacheDir
//                val songPath = File(dir, "f1_bass_41_43.ac3").absolutePath
//                if (!FileUtils.isFileExists(songPath)) {
//                    Toast.makeText(this, "歌曲不存在", Toast.LENGTH_SHORT).show()
//                    return
//                }
                val songUri = Uri.parse(songPath)
                MediaUtils.openMediaNoUi(songUri)

            }

            R.id.btn_direct_play -> {
                if (!isExternalStorageWritable()) {
                    Toast.makeText(this, "权限未授予", Toast.LENGTH_SHORT).show()
                    return
                }
                //共享目录
                val dir = Environment.getExternalStoragePublicDirectory(DIRECTORY_MUSIC)
                val songPath = File(dir, "f1_bass_41_43.ac3").absolutePath
//                val songPath = File(dir, "父亲写的散文诗-许飞.flac").absolutePath
                if (!FileUtils.isFileExists(songPath)) {
                    Toast.makeText(this, "歌曲不存在", Toast.LENGTH_SHORT).show()
                    return
                }
                val songUri = Uri.parse(songPath)
                lifecycleScope.launch {
                    directPlay(songUri)
                }
            }
        }
    }

    private fun newMediaPlayer(): MediaPlayer {
        return MediaPlayer(VLCInstance.getInstance(applicationContext)).apply {
            setAudioDigitalOutputEnabled(VLCOptions.isAudioDigitalOutputEnabled(settings))
            VLCOptions.getAout(settings)?.let { setAudioOutput(it) }
//            setRenderer(PlaybackService.renderer.value)
//            this.vlcVout.addCallback(this)
        }
    }

    private suspend fun directPlay(uri: Uri) {
        var mediaplayer = newMediaPlayer()
        if (service == null) {
            Log.d("zxm==", "directPlay()..service未初始化")
            Toast.makeText(this, "service未初始化", Toast.LENGTH_SHORT).show()
            return
        }
        val mw = MLServiceLocator.getAbstractMediaWrapper(uri)
//        if (mw.uri.scheme == "content") withContext(Dispatchers.IO) {
//            MediaUtils.retrieveMediaTitle(
//                mw
//            )
//        }

        if (mw.type != MediaWrapper.TYPE_VIDEO || mw.hasFlag(
                MediaWrapper.MEDIA_FORCE_AUDIO
            )
        ) {
            var mwUri: Uri? = withContext(Dispatchers.IO) {
                com.nextos.module.playermodule.util.FileUtils.getUri(mw.uri)
            } ?: return

            val media = mediaFactory.getFromUri(VLCInstance.getInstance(applicationContext), mwUri)
            if (!mediaplayer.isReleased) {
                withContext(Dispatchers.IO) {
                    mediaplayer.media = media.apply {
                        parse()
                    }
                }
                if (!mediaplayer.hasMedia()) {
                    Toast.makeText(this, "没有资源", Toast.LENGTH_SHORT).show()
                    return
                }
                mediaplayer.play()
            }
        }

    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<String>, grantResults: IntArray
    ) {
        if (grantResults != null) {
            val size = grantResults.size
            for (i in 0 until size) {
                val grantResult = grantResults[i]
                if (grantResult != PackageManager.PERMISSION_GRANTED) {
                    val showRequest = ActivityCompat.shouldShowRequestPermissionRationale(
                        this, permissions[i]
                    )
                    if (showRequest) {
                        DialogUtil.showForceDialog(
                            mContext!!, PermissionChecker.matchRequestPermissionRationale(
                                mContext, permissions[i]
                            )
                        ) { dialog, which -> }
                    }
                } else {
                    //do something
                }

            }
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    override fun update() {

    }

    override fun onMediaEvent(event: org.videolan.libvlc.interfaces.IMedia.Event) {

    }

    override fun onMediaPlayerEvent(event: org.videolan.libvlc.MediaPlayer.Event) {
        if (event.type == Event.EndReached) {
            Toast.makeText(this, "播放完毕", Toast.LENGTH_SHORT).show()
            Log.d("zxm==", "MainActivity..onMediaPlayerEvent..event#type:${event.type}")
        }
    }
}

private const val REQUEST_EXTERNAL = 1001

private fun isExternalStorageWritable(): Boolean {
    val state = Environment.getExternalStorageState()
    return Environment.MEDIA_MOUNTED == state
}
