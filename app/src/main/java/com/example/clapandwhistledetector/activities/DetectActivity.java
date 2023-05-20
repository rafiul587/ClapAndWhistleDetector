package com.example.clapandwhistledetector.activities;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.Window;
import android.view.WindowManager;

import androidx.appcompat.app.AppCompatActivity;

import com.example.clapandwhistledetector.MyService;
import com.example.clapandwhistledetector.databinding.ActivityDetectBinding;

public class DetectActivity extends AppCompatActivity {

    ActivityDetectBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityDetectBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        binding.whistle.setOnClickListener(view -> {
            if (MyService.isRestartNeeded) {
                stopService(new Intent(this, MyService.class));
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        MyService.isRestartNeeded = true;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (MyService.isRestartNeeded) {
            stopService(new Intent(this, MyService.class));
        }
    }
}