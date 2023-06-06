/*****************************************************************************
 * browserutils.kt
 *****************************************************************************
 * Copyright © 2018 VLC authors and VideoLAN
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston MA 02110-1301, USA.
 *****************************************************************************/

package com.nextos.module.playermodule.util

import android.net.Uri

fun isSchemeStreaming(scheme: String?): Boolean = when {
    scheme.isNullOrEmpty() -> false
    isSchemeHttpOrHttps(scheme) -> true
    scheme.startsWith("mms") -> true
    scheme.startsWith("rtsp") -> true
    else -> false
}

fun isSchemeHttpOrHttps(scheme: String?): Boolean = scheme?.startsWith("http") == true

fun isSchemeSupported(scheme: String?) = when(scheme) {
    "file", "smb", "ssh", "nfs", "ftp", "ftps", "content" -> true
    else -> false
}
fun String?.isSchemeNetwork() = when(this) {
    "smb", "ssh", "nfs", "ftp", "ftps", "upnp" -> true
    else -> false
}

fun String?.isSchemeFile() = when(this) {
    "file", null -> true
    else -> false
}

fun Uri.isOTG() = this.path?.startsWith("/mnt") == true
fun Uri.isSD() = this.path != null && this.path?.startsWith("/storage") == true && this.path?.startsWith(AndroidDevices.EXTERNAL_PUBLIC_DIRECTORY) == false
fun String?.isSchemeSMB() = this == "smb"
fun String?.isSchemeFD() = this == "fd"

fun String?.isSchemeDistant() = !this.isSchemeFile()

