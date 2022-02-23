package com.example.clapandwhistledetector;

import android.database.Cursor;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.PreferenceFragmentCompat;

public class FileSelectActivity extends AppCompatActivity {
    final static String SELECTED_FILE_URI = "selected_file_uri";
    final static String SELECTED_FILE_POSITION = "selected_file_position";
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.settings_activity);
        if (savedInstanceState == null) {
            getSupportFragmentManager()
                    .beginTransaction()
                    .setReorderingAllowed(true)
                    .replace(R.id.fragment_container_view, new FileSelectFragment())
                    .commit();
        }
    }
}