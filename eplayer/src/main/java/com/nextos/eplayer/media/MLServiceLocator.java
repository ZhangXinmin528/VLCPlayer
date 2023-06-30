package com.nextos.eplayer.media;

import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Parcel;

import com.nextos.eplayer.media.media.MediaWrapper;
import com.nextos.eplayer.media.media.MediaWrapperImpl;

import org.videolan.libvlc.interfaces.IMedia;


public class MLServiceLocator {

    private static LocatorMode sMode = LocatorMode.VLC_ANDROID;

    public static void setLocatorMode(LocatorMode mode) {
        MLServiceLocator.sMode = mode;
    }

    public static LocatorMode getLocatorMode() {
        return MLServiceLocator.sMode;
    }

    public static String EXTRA_TEST_STUBS = "extra_test_stubs";

    public enum LocatorMode {
        VLC_ANDROID,
        TESTS,
    }

    // MediaWrapper
    public static MediaWrapper getAbstractMediaWrapper(long id, String mrl, long time, float position, long length,
                                                       int type, String title, String filename,
                                                       String artist, String genre, String album,
                                                       String albumArtist, int width, int height,
                                                       String artworkURL, int audio, int spu,
                                                       int trackNumber, int discNumber, long lastModified,
                                                       long seen, boolean isThumbnailGenerated, int releaseDate, boolean isPresent) {
        return new MediaWrapperImpl(id, mrl, time, position, length, type, title,
                filename, artist, genre, album, albumArtist, width, height, artworkURL,
                audio, spu, trackNumber, discNumber, lastModified, seen, isThumbnailGenerated, releaseDate, isPresent);
    }

    public static MediaWrapper getAbstractMediaWrapper(Uri uri, long time, float position, long length, int type,
                                                       Bitmap picture, String title, String artist,
                                                       String genre, String album, String albumArtist,
                                                       int width, int height, String artworkURL,
                                                       int audio, int spu, int trackNumber,
                                                       int discNumber, long lastModified, long seen) {
        return new MediaWrapperImpl(uri, time, position, length, type, picture, title, artist, genre,
                album, albumArtist, width, height, artworkURL, audio, spu, trackNumber,
                discNumber, lastModified, seen);
    }

    public static MediaWrapper getAbstractMediaWrapper(Uri uri) {
        return new MediaWrapperImpl(uri);
    }

    public static MediaWrapper getAbstractMediaWrapper(IMedia media) {
        return new MediaWrapperImpl(media);
    }

    public static MediaWrapper getAbstractMediaWrapper(Parcel in) {
        return new MediaWrapperImpl(in);
    }
}
