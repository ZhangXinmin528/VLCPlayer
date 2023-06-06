package com.nextos.module.playermodule.helper

import android.Manifest
import android.annotation.TargetApi
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment.isExternalStorageManager
import android.os.FileUtils
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.annotation.RequiresApi
import androidx.core.content.ContentProviderCompat.requireContext
import androidx.core.content.ContextCompat.startActivity
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import com.nextos.module.playermodule.resources.EXTRA_FIRST_RUN
import com.nextos.module.playermodule.resources.EXTRA_UPGRADE
import com.nextos.module.playermodule.resources.SCHEME_PACKAGE
import com.nextos.module.playermodule.tools.INITIAL_PERMISSION_ASKED
import com.nextos.module.playermodule.tools.LiveEvent
import com.nextos.module.playermodule.tools.Settings
import com.nextos.module.playermodule.tools.isCallable
import com.nextos.module.playermodule.util.AndroidDevices
import com.nextos.module.playermodule.util.Permissions
import com.nextos.module.playermodule.util.Permissions.canReadStorage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.videolan.libvlc.util.AndroidUtil

private const val WRITE_ACCESS = "write"
private const val WITH_DIALOG = "with_dialog"
private const val ONLY_MEDIA = "only_media"
