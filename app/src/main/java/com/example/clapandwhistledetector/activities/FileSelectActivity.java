package com.example.clapandwhistledetector.activities;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.example.clapandwhistledetector.fragments.FileSelectFragment;
import com.example.clapandwhistledetector.R;

public class FileSelectActivity extends AppCompatActivity {
    public final static String SELECTED_FILE_URI = "selected_file_uri";
    public final static String SELECTED_FILE_POSITION = "selected_file_position";
    public static final String SELECTED_FILE_NAME = "selected_file_name";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_select_file);
        if (savedInstanceState == null) {
            getSupportFragmentManager()
                    .beginTransaction()
                    .setReorderingAllowed(true)
                    .replace(R.id.fragment_container_view, new FileSelectFragment())
                    .commit();
        }
    }
}