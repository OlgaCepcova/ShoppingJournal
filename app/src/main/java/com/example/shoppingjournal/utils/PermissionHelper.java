package com.example.shoppingjournal;

import android.Manifest;
import android.os.Build;

import java.util.ArrayList;
import java.util.List;

public class PermissionHelper {

    public static String[] forCameraAndSaveToGallery() {
        List<String> perms = new ArrayList<>();
        perms.add(Manifest.permission.CAMERA);

        // API 28 un zemāk vajag WRITE_EXTERNAL_STORAGE
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
            perms.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);
        }
        return perms.toArray(new String[0]);
    }
}


