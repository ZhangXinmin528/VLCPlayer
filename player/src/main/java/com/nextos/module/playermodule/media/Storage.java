package com.nextos.module.playermodule.media;


import android.net.Uri;
import android.os.Parcel;
import android.text.TextUtils;


public class Storage extends MediaLibraryItem {

    Uri uri;
    String description;
    public static final MediaWrapper[] EMPTY_COLLECTION = {};
    @Override
//    public MediaWrapper[] getTracks() {
//        return Medialibrary.EMPTY_COLLECTION;
//    }
    public MediaWrapper[] getTracks() {
        return EMPTY_COLLECTION;
    }

    @Override
    public int getTracksCount() {
        return 1;
    }

    @Override
    public int getItemType() {
        return TYPE_STORAGE;
    }

    public Storage(Uri uri){
        this.uri = uri;
        mTitle = uri.getLastPathSegment();
    }

    public String getName() {
        return Uri.decode(mTitle);
    }

    public void setName(String name) {
        mTitle = name;
    }

    @Override
    public void setDescription(String description) {
        this.description = description;
    }

    @Override
    public String getDescription() {
        return description;
    }

    public Uri getUri() {
        return uri;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        super.writeToParcel(parcel, i);
        parcel.writeParcelable(uri, i);
        parcel.writeString(description);
    }

    public static Creator<Storage> CREATOR
            = new Creator<Storage>() {
        @Override
        public Storage createFromParcel(Parcel in) {
            return new Storage(in);
        }

        @Override
        public Storage[] newArray(int size) {
            return new Storage[size];
        }
    };

    private Storage(Parcel in) {
        super(in);
        this.uri = in.readParcelable(Uri.class.getClassLoader());
        this.description = in.readString();
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof Storage && TextUtils.equals(mTitle, ((Storage)obj).getTitle());
    }
}
