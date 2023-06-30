/*
 *****************************************************************************
 * MediaWrapperImpl.java
 *****************************************************************************
 * Copyright © 2011-2019 VLC authors and VideoLAN
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation; either version 2.1 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston MA 02110-1301, USA.
 *****************************************************************************/

package com.nextos.eplayer.media.media;


import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Parcel;

import org.videolan.libvlc.interfaces.IMedia;

import java.util.Locale;

@SuppressWarnings("JniMissingFunction")
public class MediaWrapperImpl extends MediaWrapper {
    public final static String TAG = "VLC/MediaWrapperImpl";

    @Override
    public void rename(String name) {

    }

    @Override
    public long getMetaLong(int metaDataType) {
        return 0;
    }

    @Override
    public String getMetaString(int metaDataType) {
        return null;
    }

    @Override
    public boolean setLongMeta(int metaDataType, long metaDataValue) {
        return false;
    }

    @Override
    public boolean setStringMeta(int metaDataType, String metaDataValue) {
        return false;
    }

    @Override
    public void setThumbnail(String mrl) {

    }

    @Override
    public boolean setPlayCount(long playCount) {
        return false;
    }

    @Override
    public long getPlayCount() {
        return 0;
    }

    @Override
    public void removeThumbnail() {

    }

    @Override
    public void requestThumbnail(int width, float position) {

    }

    @Override
    public void requestBanner(int width, float position) {

    }

    @Override
    public boolean removeFromHistory() {
        return false;
    }

    @Override
    public boolean removeBookmark(long time) {
        return false;
    }

    @Override
    public boolean removeAllBookmarks() {
        return false;
    }

    @Override
    public boolean markAsPlayed() {
        return false;
    }

    public MediaWrapperImpl(long id, String mrl, long time, float position, long length, int type, String title,
                            String filename, String artist, String genre, String album, String albumArtist,
                            int width, int height, String artworkURL, int audio, int spu, int trackNumber,
                            int discNumber, long lastModified, long seen, boolean isThumbnailGenerated, int releaseDate, boolean isPresent) {
        super(id, mrl, time, position, length, type, title, filename, artist,
                genre, album, albumArtist, width, height, artworkURL,
                audio, spu, trackNumber, discNumber, lastModified,
                seen, isThumbnailGenerated, releaseDate, isPresent);
    }

    public MediaWrapperImpl(Uri uri, long time, float position, long length, int type,
                            Bitmap picture, String title, String artist, String genre, String album, String albumArtist,
                            int width, int height, String artworkURL, int audio, int spu, int trackNumber, int discNumber, long lastModified, long seen) {
        super(uri, time, position, length, type, picture, title, artist,
                genre, album, albumArtist, width, height, artworkURL,
                audio, spu, trackNumber, discNumber, lastModified, seen);
    }

    public MediaWrapperImpl(Uri uri) { super(uri); }
    public MediaWrapperImpl(IMedia media) { super(media); }
    public MediaWrapperImpl(Parcel in) { super(in); }

    public void setArtist(String artist) {
        mArtist = artist;
    }

    public String getReferenceArtist() {
        return mAlbumArtist == null ? mArtist : mAlbumArtist;
    }

    public String getArtist() {
        return mArtist;
    }

    public Boolean isArtistUnknown() {
        return mArtist == null;
    }

    public String getGenre() {
        if (mGenre == null)
            return null;
        else if (mGenre.length() > 1)/* Make genres case insensitive via normalisation */
            return Character.toUpperCase(mGenre.charAt(0)) + mGenre.substring(1).toLowerCase(Locale.getDefault());
        else
            return mGenre;
    }

    public String getCopyright() {
        return mCopyright;
    }

    public String getAlbum() {
        return mAlbum;
    }

    public String getAlbumArtist() {
        return mAlbumArtist;
    }

    public Boolean isAlbumUnknown() {
        return mAlbum == null;
    }

    public int getTrackNumber() {
        return mTrackNumber;
    }

    public int getDiscNumber() {
        return mDiscNumber;
    }

    public String getRating() {
        return mRating;
    }

    public String getDate() {
        return mDate;
    }

    public String getSettings() {
        return mSettings;
    }

    public String getNowPlaying() {
        return mNowPlaying;
    }

    public String getPublisher() {
        return mPublisher;
    }

    public String getEncodedBy() {
        return mEncodedBy;
    }

    public String getTrackID() {
        return mTrackID;
    }

    public String getArtworkURL() {
        return mArtworkURL;
    }

    public boolean isThumbnailGenerated() {
        return mThumbnailGenerated;
    }

    @Override
    public String getArtworkMrl() {
        return mArtworkURL;
    }

    public void setArtworkURL(String url) {
        mArtworkURL = url;
    }

    public long getLastModified() {
        return mLastModified;
    }

    public void setLastModified(long mLastModified) {
        this.mLastModified = mLastModified;
    }

    public long getSeen() {
        return mSeen;
    }

    public void setSeen(long seen) {
        mSeen = seen;
    }

    public void addFlags(int flags) {
        mFlags |= flags;
    }

    public void setFlags(int flags) {
        mFlags = flags;
    }

    public int getFlags() {
        return mFlags;
    }

    public boolean hasFlag(int flag) {
        return (mFlags & flag) != 0;
    }

    public void removeFlags(int flags) {
        mFlags &= ~flags;
    }

}
