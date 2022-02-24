package com.example.clapandwhistledetector.activities;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;

import com.example.clapandwhistledetector.MyService;
import com.example.clapandwhistledetector.R;

public class SplashActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if(MyService.vibrator != null) MyService.vibrator.cancel();
        MyService.keepPlayingSound = false;
        MyService.keepFlashOn = false;
        MyService.threadGroups.interrupt();
        MyService.keepVibrate = false;
    }
}