/*
 * *************************************************************************
 *  Util.java
 * **************************************************************************
 *  Copyright © 2015 VLC authors and VideoLAN
 *  Author: Geoffrey Métais
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation; either version 2 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, write to the Free Software
 *  Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston MA 02110-1301, USA.
 *  ***************************************************************************
 */

package com.nextos.module.playermodule.helper

import android.annotation.SuppressLint
import android.annotation.TargetApi
import android.app.Activity
import android.app.PendingIntent
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.RenderEffect
import android.graphics.Shader
import android.graphics.drawable.BitmapDrawable
import android.media.MediaRouter
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.renderscript.*
import android.text.TextUtils
import android.view.*
import android.view.animation.*
import android.view.inputmethod.InputMethodManager
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.IdRes
import androidx.annotation.RequiresApi
import androidx.annotation.StringRes
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.appcompat.view.ActionMode
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import androidx.core.content.getSystemService
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat
import androidx.core.net.toUri
import androidx.core.os.bundleOf
import androidx.core.view.MenuItemCompat
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.snackbar.Snackbar
import com.nextos.module.playermodule.R
import kotlinx.coroutines.*
import org.videolan.libvlc.util.AndroidUtil
import kotlin.math.min

object UiTools {
    var currentNightMode: Int = 0
    private const val TAG = "VLC/UiTools"
    private var DEFAULT_COVER_VIDEO_DRAWABLE: BitmapDrawable? = null
    private var DEFAULT_COVER_AUDIO_DRAWABLE: BitmapDrawable? = null
    private var DEFAULT_COVER_AUDIO_AUTO_DRAWABLE: BitmapDrawable? = null
    private var DEFAULT_COVER_ALBUM_DRAWABLE: BitmapDrawable? = null
    private var DEFAULT_COVER_ARTIST_DRAWABLE: BitmapDrawable? = null
    private var DEFAULT_COVER_MOVIE_DRAWABLE: BitmapDrawable? = null
    private var DEFAULT_COVER_TVSHOW_DRAWABLE: BitmapDrawable? = null
    private var DEFAULT_COVER_FOLDER_DRAWABLE: BitmapDrawable? = null

    private var DEFAULT_COVER_VIDEO_DRAWABLE_BIG: BitmapDrawable? = null
    private var DEFAULT_COVER_AUDIO_DRAWABLE_BIG: BitmapDrawable? = null
    private var DEFAULT_COVER_ALBUM_DRAWABLE_BIG: BitmapDrawable? = null
    private var DEFAULT_COVER_ARTIST_DRAWABLE_BIG: BitmapDrawable? = null
    private var DEFAULT_COVER_MOVIE_DRAWABLE_BIG: BitmapDrawable? = null
    private var DEFAULT_COVER_TVSHOW_DRAWABLE_BIG: BitmapDrawable? = null
    private var DEFAULT_COVER_FOLDER_DRAWABLE_BIG: BitmapDrawable? = null

    private val sHandler = Handler(Looper.getMainLooper())
    private const val DELETE_DURATION = 3000

    fun getDefaultVideoDrawable(context: Context): BitmapDrawable {
        if (DEFAULT_COVER_VIDEO_DRAWABLE == null) {
            DEFAULT_COVER_VIDEO_DRAWABLE = BitmapDrawable(
                context.resources, getBitmapFromDrawable(context, R.drawable.ic_no_thumbnail_1610)
            )
        }
        return DEFAULT_COVER_VIDEO_DRAWABLE!!
    }

    fun getDefaultAudioDrawable(context: Context): BitmapDrawable {
        if (DEFAULT_COVER_AUDIO_DRAWABLE == null) {
            DEFAULT_COVER_AUDIO_DRAWABLE = BitmapDrawable(
                context.resources, getBitmapFromDrawable(context, R.drawable.ic_no_song)
            )
        }
        return DEFAULT_COVER_AUDIO_DRAWABLE!!
    }

    fun getDefaultAudioAutoDrawable(context: Context): BitmapDrawable {
        if (DEFAULT_COVER_AUDIO_AUTO_DRAWABLE == null) {
            DEFAULT_COVER_AUDIO_AUTO_DRAWABLE = BitmapDrawable(
                context.resources, getBitmapFromDrawable(context, R.drawable.ic_auto_nothumb)
            )
        }
        return DEFAULT_COVER_AUDIO_AUTO_DRAWABLE!!
    }

    fun getDefaultFolderDrawable(context: Context): BitmapDrawable {
        if (DEFAULT_COVER_FOLDER_DRAWABLE == null) {
            DEFAULT_COVER_FOLDER_DRAWABLE = BitmapDrawable(
                context.resources, getBitmapFromDrawable(context, R.drawable.ic_menu_folder)
            )
        }
        return DEFAULT_COVER_FOLDER_DRAWABLE!!
    }

    fun getDefaultAlbumDrawable(context: Context): BitmapDrawable {
        if (DEFAULT_COVER_ALBUM_DRAWABLE == null) {
            DEFAULT_COVER_ALBUM_DRAWABLE = BitmapDrawable(
                context.resources, getBitmapFromDrawable(context, R.drawable.ic_no_album)
            )
        }
        return DEFAULT_COVER_ALBUM_DRAWABLE!!
    }

    fun getDefaultArtistDrawable(context: Context): BitmapDrawable {
        if (DEFAULT_COVER_ARTIST_DRAWABLE == null) {
            DEFAULT_COVER_ARTIST_DRAWABLE = BitmapDrawable(
                context.resources, getBitmapFromDrawable(context, R.drawable.ic_no_artist)
            )
        }
        return DEFAULT_COVER_ARTIST_DRAWABLE!!
    }

    fun getDefaultMovieDrawable(context: Context): BitmapDrawable {
        if (DEFAULT_COVER_MOVIE_DRAWABLE == null) {
            DEFAULT_COVER_MOVIE_DRAWABLE = BitmapDrawable(
                context.resources, getBitmapFromDrawable(context, R.drawable.ic_browser_movie)
            )
        }
        return DEFAULT_COVER_MOVIE_DRAWABLE!!
    }

    fun getDefaultTvshowDrawable(context: Context): BitmapDrawable {
        if (DEFAULT_COVER_TVSHOW_DRAWABLE == null) {
            DEFAULT_COVER_TVSHOW_DRAWABLE = BitmapDrawable(
                context.resources, getBitmapFromDrawable(context, R.drawable.ic_browser_tvshow)
            )
        }
        return DEFAULT_COVER_TVSHOW_DRAWABLE!!
    }

    fun getDefaultVideoDrawableBig(context: Context): BitmapDrawable {
        if (DEFAULT_COVER_VIDEO_DRAWABLE_BIG == null) {
            DEFAULT_COVER_VIDEO_DRAWABLE_BIG = BitmapDrawable(
                context.resources,
                getBitmapFromDrawable(context, R.drawable.ic_browser_video_big_normal)
            )
        }
        return DEFAULT_COVER_VIDEO_DRAWABLE_BIG!!
    }

    fun getDefaultAudioDrawableBig(context: Context): BitmapDrawable {
        if (DEFAULT_COVER_AUDIO_DRAWABLE_BIG == null) {
            DEFAULT_COVER_AUDIO_DRAWABLE_BIG = BitmapDrawable(
                context.resources, getBitmapFromDrawable(context, R.drawable.ic_song_big)
            )
        }
        return DEFAULT_COVER_AUDIO_DRAWABLE_BIG!!
    }

    fun getDefaultAlbumDrawableBig(context: Context): BitmapDrawable {
        if (DEFAULT_COVER_ALBUM_DRAWABLE_BIG == null) {
            DEFAULT_COVER_ALBUM_DRAWABLE_BIG = BitmapDrawable(
                context.resources, getBitmapFromDrawable(context, R.drawable.ic_album_big)
            )
        }
        return DEFAULT_COVER_ALBUM_DRAWABLE_BIG!!
    }

    fun getDefaultArtistDrawableBig(context: Context): BitmapDrawable {
        if (DEFAULT_COVER_ARTIST_DRAWABLE_BIG == null) {
            DEFAULT_COVER_ARTIST_DRAWABLE_BIG = BitmapDrawable(
                context.resources, getBitmapFromDrawable(context, R.drawable.ic_artist_big)
            )
        }
        return DEFAULT_COVER_ARTIST_DRAWABLE_BIG!!
    }

    fun getDefaultMovieDrawableBig(context: Context): BitmapDrawable {
        if (DEFAULT_COVER_MOVIE_DRAWABLE_BIG == null) {
            DEFAULT_COVER_MOVIE_DRAWABLE_BIG = BitmapDrawable(
                context.resources, getBitmapFromDrawable(context, R.drawable.ic_browser_movie_big)
            )
        }
        return DEFAULT_COVER_MOVIE_DRAWABLE_BIG!!
    }

    fun getDefaultTvshowDrawableBig(context: Context): BitmapDrawable {
        if (DEFAULT_COVER_TVSHOW_DRAWABLE_BIG == null) {
            DEFAULT_COVER_TVSHOW_DRAWABLE_BIG = BitmapDrawable(
                context.resources, getBitmapFromDrawable(context, R.drawable.ic_browser_tvshow_big)
            )
        }
        return DEFAULT_COVER_TVSHOW_DRAWABLE_BIG!!
    }

    fun getDefaultFolderDrawableBig(context: Context): BitmapDrawable {
        if (DEFAULT_COVER_FOLDER_DRAWABLE_BIG == null) {
            DEFAULT_COVER_FOLDER_DRAWABLE_BIG = BitmapDrawable(
                context.resources, getBitmapFromDrawable(context, R.drawable.ic_menu_folder_big)
            )
        }
        return DEFAULT_COVER_FOLDER_DRAWABLE_BIG!!
    }

    private fun getSnackAnchorView(activity: Activity, overAudioPlayer: Boolean = false) =
        activity.findViewById<View>(android.R.id.content)


    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    fun CoroutineScope.snackerConfirm(
        activity: Activity, message: String, action: suspend () -> Unit
    ) {
        val view = getSnackAnchorView(activity) ?: return
        val snack = Snackbar.make(view, message, Snackbar.LENGTH_LONG)
            .setAction(R.string.ok) { launch { action.invoke() } }
        if (AndroidUtil.isLolliPopOrLater) snack.view.elevation =
            view.resources.getDimensionPixelSize(R.dimen.audio_player_elevation).toFloat()
        snack.show()
    }


    /**
     * Print an on-screen message to alert the user, with undo action
     */
    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    fun snackerWithCancel(
        activity: Activity,
        message: String,
        overAudioPlayer: Boolean = false,
        action: () -> Unit,
        cancelAction: () -> Unit
    ) {
        val view = getSnackAnchorView(activity, overAudioPlayer) ?: return
        @SuppressLint("WrongConstant") val snack =
            Snackbar.make(view, message, DELETE_DURATION).setAction(R.string.cancel) {
                    sHandler.removeCallbacks(action)
                    cancelAction.invoke()
                }
        if (AndroidUtil.isLolliPopOrLater) snack.view.elevation =
            view.resources.getDimensionPixelSize(R.dimen.audio_player_elevation).toFloat()
        snack.show()
        sHandler.postDelayed(action, DELETE_DURATION.toLong())
    }

    fun snackerMessageInfinite(activity: Activity, message: String): Snackbar? {
        val view = getSnackAnchorView(activity) ?: return null
        return Snackbar.make(view, message, Snackbar.LENGTH_INDEFINITE)
    }


    /**
     * Get a resource id from an attribute id.
     *
     * @param context
     * @param attrId
     * @return the resource id
     */
    fun getResourceFromAttribute(context: Context, attrId: Int): Int {
        val a = context.theme.obtainStyledAttributes(intArrayOf(attrId))
        val resId = a.getResourceId(0, 0)
        a.recycle()
        return resId
    }

    /**
     * Get a color id from an attribute id.
     *
     * @param context
     * @param attrId
     * @return the color id
     */
    fun getColorFromAttribute(context: Context, attrId: Int): Int {
        return ContextCompat.getColor(context, getResourceFromAttribute(context, attrId))
    }

    fun setViewOnClickListener(v: View?, ocl: View.OnClickListener?) {
        v?.setOnClickListener(ocl)
    }



//    private fun manageDonationVisibility(activity: FragmentActivity, donationsButton:View) {
//        if (VLCBilling.getInstance(activity.application).status == BillingStatus.FAILURE ||  VLCBilling.getInstance(activity.application).skuDetails.isEmpty()) donationsButton.setGone() else donationsButton.setVisible()
//    }

    fun setKeyboardVisibility(v: View?, show: Boolean) {
        if (v == null) return
        val inputMethodManager =
            v.context.applicationContext.getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager
        sHandler.post {
            if (show) inputMethodManager.showSoftInput(v, InputMethodManager.SHOW_IMPLICIT)
            else inputMethodManager.hideSoftInputFromWindow(v.windowToken, 0)
        }
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
    fun setRotationAnimation(activity: Activity) {
        if (!AndroidUtil.isJellyBeanMR2OrLater) return
        val win = activity.window
        val winParams = win.attributes
        winParams.rotationAnimation =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) WindowManager.LayoutParams.ROTATION_ANIMATION_SEAMLESS else WindowManager.LayoutParams.ROTATION_ANIMATION_JUMPCUT
        win.attributes = winParams
    }




    fun hasSecondaryDisplay(context: Context): Boolean {
        val mediaRouter = context.getSystemService<MediaRouter>()!!
        val route = mediaRouter.getSelectedRoute(MediaRouter.ROUTE_TYPE_LIVE_VIDEO)
        val presentationDisplay = route?.presentationDisplay
        return presentationDisplay != null
    }

    /**
     * Invalidate the default bitmaps that are different in light and dark modes
     */
    fun invalidateBitmaps() {
        DEFAULT_COVER_VIDEO_DRAWABLE = null
    }

    fun TextView.addFavoritesIcon() {
        val drawable = ContextCompat.getDrawable(context, R.drawable.ic_emoji_favorite)
        setCompoundDrawablesWithIntrinsicBounds(null, null, drawable, null)
    }

    fun TextView.removeDrawables() {
        setCompoundDrawablesWithIntrinsicBounds(null, null, null, null)
    }

}



