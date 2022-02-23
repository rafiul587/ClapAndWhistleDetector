package com.example.clapandwhistledetector;

import android.net.Uri;

public class FileModel {
    private String name;
    private Uri uri;
    private boolean isActive = false;

    FileModel(String name, Uri uri, Boolean isActive){
        this.name = name;
        this.uri = uri;
        this.isActive = isActive;
    }

    public boolean isActive() {
        return isActive;
    }

    public void setActive(boolean active) {
        isActive = active;
    }

    public String getName() {
        return name;
    }

    public Uri getUri() {
        return uri;
    }
}
