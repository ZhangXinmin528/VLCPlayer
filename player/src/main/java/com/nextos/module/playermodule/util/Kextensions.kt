package com.nextos.module.playermodule.util

import android.annotation.TargetApi
import android.app.Activity
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.res.Resources
import android.net.Uri
import android.os.Build
import android.text.Spannable
import android.text.SpannableString
import android.text.style.DynamicDrawableSpan
import android.text.style.ImageSpan
import android.util.DisplayMetrics
import android.view.View
import android.view.ViewGroup.MarginLayoutParams
import android.widget.TextView
import androidx.annotation.WorkerThread
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.content.FileProvider
import androidx.core.net.toUri
import androidx.core.text.PrecomputedTextCompat
import androidx.core.text.toSpannable
import androidx.core.widget.TextViewCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.*
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.snackbar.Snackbar
import com.nextos.module.playermodule.R
import com.nextos.module.playermodule.media.MLServiceLocator
import com.nextos.module.playermodule.media.MediaLibraryItem
import com.nextos.module.playermodule.media.MediaWrapper
import com.nextos.module.playermodule.media.MediaWrapper.TYPE_VIDEO
import com.nextos.module.playermodule.tools.AppScope
import com.nextos.module.playermodule.tools.isStarted
import com.nextos.module.playermodule.tools.retrieveParent
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import org.videolan.libvlc.Media
import org.videolan.libvlc.interfaces.IMedia
import org.videolan.libvlc.util.AndroidUtil
import java.io.File
import java.lang.ref.WeakReference
import java.net.URI
import java.net.URISyntaxException
import java.security.SecureRandom
import java.util.*
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

fun String.validateLocation(): Boolean {
    var location = this
    /* Check if the MRL contains a scheme */
    if (!location.matches("\\w+://.+".toRegex())) location = "file://$location"
    if (location.lowercase(Locale.ENGLISH).startsWith("file://")) {
        /* Ensure the file exists */
        val f: File
        try {
            f = File(URI(location))
        } catch (e: URISyntaxException) {
            return false
        } catch (e: IllegalArgumentException) {
            return false
        }
        if (!f.isFile) return false
    }
    return true
}

inline fun <reified T : ViewModel> Fragment.getModelWithActivity() = ViewModelProvider(requireActivity()).get(T::class.java)
inline fun <reified T : ViewModel> Fragment.getModel() = ViewModelProvider(this).get(T::class.java)
inline fun <reified T : ViewModel> FragmentActivity.getModel() = ViewModelProvider(this).get(T::class.java)



private fun setTextAsync(view: TextView, text: CharSequence, params: PrecomputedTextCompat.Params) {
    val ref = WeakReference(view)
    AppScope.launch(Dispatchers.Default) {
        val pText = PrecomputedTextCompat.create(text, params)
        val result = pText.toSpannable()
        withContext(Dispatchers.Main) {
            ref.get()?.let { textView ->
                textView.text = result
            }
        }
    }
}

const val folderReplacementMarker = "§*§"
const val fileReplacementMarker = "*§*"


fun CharSequence.getDescriptionSpan(context: Context):SpannableString {
    val string = SpannableString(this)
    if (this.contains(folderReplacementMarker)) {
        string.setSpan(ImageSpan(context, R.drawable.ic_emoji_folder, DynamicDrawableSpan.ALIGN_BASELINE), this.indexOf(
            folderReplacementMarker
        ), this.indexOf(folderReplacementMarker)+3, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
    }
    if (this.contains(fileReplacementMarker)) {
        string.setSpan(ImageSpan(context, R.drawable.ic_emoji_file, DynamicDrawableSpan.ALIGN_BASELINE), this.indexOf(
            fileReplacementMarker
        ), this.indexOf(fileReplacementMarker)+3, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
    }
    return string
}

/**
 * Get the folder number from the formatted string
 *
 * @return the folder number
 */
fun CharSequence?.getFolderNumber():Int {
    if (isNullOrBlank()) return 0
    if (!contains(folderReplacementMarker)) return 0
    val cutString = replace(Regex("[^0-9 ]"), "")
    return cutString.trim().split(" ")[0].toInt()
}

/**
 * Get the file number from the formatted string
 *
 * @return the file number
 */
fun CharSequence?.getFilesNumber():Int {
    if (isNullOrBlank()) return 0
    if (!contains(fileReplacementMarker)) return 0
    val cutString = replace(Regex("[^0-9 ]"), "").trim().split(" ")
    return cutString[cutString.size -1].toInt()
}

const val presentReplacementMarker = "§*§"
const val missingReplacementMarker = "*§*"

fun CharSequence.getPresenceDescriptionSpan(context: Context):SpannableString {
    val string = SpannableString(this)
    if (this.contains(presentReplacementMarker)) {
        string.setSpan(ImageSpan(context, R.drawable.ic_emoji_media_present, DynamicDrawableSpan.ALIGN_CENTER), this.indexOf(
            folderReplacementMarker
        ), this.indexOf(folderReplacementMarker)+3, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
    }
    if (this.contains(missingReplacementMarker)) {
        string.setSpan(ImageSpan(context, R.drawable.ic_emoji_media_absent, DynamicDrawableSpan.ALIGN_CENTER), this.indexOf(
            fileReplacementMarker
        ), this.indexOf(fileReplacementMarker)+3, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
    }
    return string
}

fun Int.toPixel(): Int {
    val metrics = Resources.getSystem().displayMetrics
    val px = toFloat() * (metrics.densityDpi / 160f)
    return px.roundToInt()
}

fun Activity.getScreenWidth() : Int {
    val dm = DisplayMetrics().also { windowManager.defaultDisplay.getMetrics(it) }
    return dm.widthPixels
}

fun Activity.getScreenHeight(): Int {
    val dm = DisplayMetrics().also { windowManager.defaultDisplay.getMetrics(it) }
    return dm.heightPixels
}

/**
 * Detect if the device has a notch.
 * @return true if the device has a notch
 * @throws NullPointerException if the window is not attached yet
 */
fun Activity.hasNotch() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.P && window.decorView.rootWindowInsets.displayCutout != null

@TargetApi(Build.VERSION_CODES.O)
fun Context.getPendingIntent(iPlay: Intent): PendingIntent {
    return if (AndroidUtil.isOOrLater) PendingIntent.getForegroundService(applicationContext, 0, iPlay, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    else PendingIntent.getService(applicationContext, 0, iPlay, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
}

/**
 * Register an [RecyclerView.AdapterDataObserver] for the adapter.
 *
 * [listener] is called each time a change occurs in the adapter
 *
 * return the registered [RecyclerView.AdapterDataObserver]
 *
 * /!\ Make sure to unregister [RecyclerView.AdapterDataObserver]
 */
fun RecyclerView.Adapter<*>.onAnyChange(listener: ()->Unit): RecyclerView.AdapterDataObserver {
    val dataObserver = object : RecyclerView.AdapterDataObserver() {
        override fun onChanged() {
            super.onChanged()
            listener.invoke()
        }

        override fun onItemRangeChanged(positionStart: Int, itemCount: Int) {
            super.onItemRangeChanged(positionStart, itemCount)
            listener.invoke()
        }

        override fun onItemRangeChanged(positionStart: Int, itemCount: Int, payload: Any?) {
            super.onItemRangeChanged(positionStart, itemCount, payload)
            listener.invoke()
        }

        override fun onItemRangeInserted(positionStart: Int, itemCount: Int) {
            super.onItemRangeInserted(positionStart, itemCount)
            listener.invoke()
        }

        override fun onItemRangeMoved(fromPosition: Int, toPosition: Int, itemCount: Int) {
            super.onItemRangeMoved(fromPosition, toPosition, itemCount)
            listener.invoke()
        }

        override fun onItemRangeRemoved(positionStart: Int, itemCount: Int) {
            super.onItemRangeRemoved(positionStart, itemCount)
            listener.invoke()
        }
    }
    registerAdapterDataObserver(dataObserver)
    return dataObserver
}

/**
 * Generate a string containing the commercial denomination of the video resolution
 *
 * @param width the video width
 * @param height the video height
 * @return the commercial resolution (SD, HD, 4K, ...)
 */
fun generateResolutionClass(width: Int, height: Int): String? = if (width <= 0 || height <= 0) {
    null
} else {
    val realHeight = min(height, width)
    val realWidth = max(height, width)
    when {
        realHeight >= 4320 || realWidth >= 4320.0 * (16.0 / 9.0) -> "8K"
        realHeight >= 2160 || realWidth >= 2160.0 * (16.0 / 9.0) -> "4K"
        realHeight >= 1440 || realWidth >= 1440.0 * (16.0 / 9.0) -> "1440p"
        realHeight >= 1080 || realWidth >= 1080.0 * (16.0 / 9.0) -> "1080p"
        realHeight >= 720 || realWidth >= 720.0 * (16.0 / 9.0) -> "720p"
        else -> "SD"
    }
}

val View.scope : CoroutineScope
    get() = when(val ctx = context) {
        is CoroutineScope -> ctx
        is LifecycleOwner -> ctx.lifecycleScope
        else -> AppScope
    }

fun <T> Flow<T>.launchWhenStarted(scope: LifecycleCoroutineScope): Job = scope.launchWhenStarted {
    collect() // tail-call
}

/**
 * Sanitize a string by adding enough "0" at the start
 * to make a "natural" alphanumeric comparison (1, 2, 10, 11, 20) instead of a strict one (1, 10, 11, 21, 20)
 *
 * @param nbOfDigits the number of digits to reach
 * @return a string having exactly [nbOfDigits] digits at the start
 */
fun String?.sanitizeStringForAlphaCompare(nbOfDigits: Int): String? {
    if (this == null) return null
    if (first().isDigit()) return buildString {
        var numberOfPrependingZeros =0
        for (c in this@sanitizeStringForAlphaCompare) {
            if (c.isDigit() && c.digitToInt() == 0) numberOfPrependingZeros++ else break
        }
        for (i in 0 until (nbOfDigits - numberOfPrependingZeros - (getStartingNumber()?.numberOfDigits() ?: 0))) {
            append("0")
        }
        append(this@sanitizeStringForAlphaCompare)
    }
    return this
}

/**
 * Calculate the number of digits of an Int
 *
 * @return the number of digits of this Int
 */
fun Int.numberOfDigits(): Int = when (this) {
    in -9..9 -> 1
    else -> 1 + (this / 10).numberOfDigits()
}

/**
 * Get the number described at the start of this String if any
 *
 * @return the starting number of this String, null if no number found
 */
fun String.getStartingNumber(): Int? {
    return try {
        buildString {
            for (c in this@getStartingNumber)
                //we exclude starting "0" to prevent bad sorts
                if (c.isDigit()) {
                    if (!(this.isEmpty() && c.digitToInt() == 0)) append(c)
                } else break
        }.toInt()
    } catch (e: NumberFormatException) {
        null
    }
}

/**
 * Determine the max number of digits iat the start of
 * this lit items' filename
 *
 * @return a max number of digits
 */
fun List<MediaLibraryItem>.determineMaxNbOfDigits(): Int {
    var numberOfPrepending = 0
    forEach {
        numberOfPrepending = max((it as? MediaWrapper)?.fileName?.getStartingNumber()?.numberOfDigits()
                ?: 0, numberOfPrepending)
    }
    return numberOfPrepending
}


/**
 * Finds the [ViewPager2] current fragment
 * @param fragmentManager: The used [FragmentManager]
 *
 * @return the current fragment if found
 */
fun ViewPager2.findCurrentFragment(fragmentManager: FragmentManager): Fragment? {
    return fragmentManager.findFragmentByTag("f$currentItem")
}

/**
 * Finds the [ViewPager2] fragment at a specified position
 * @param fragmentManager: The used [FragmentManager]
 * @param position: The position to look at
 *
 * @return the fragment if found
 */
fun ViewPager2.findFragmentAt(fragmentManager: FragmentManager, position: Int): Fragment? {
    return fragmentManager.findFragmentByTag("f$position")
}
