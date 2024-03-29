/*
 * Copyright (C) 2012 Jacquet Wong
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * musicg api in Google Code: http://code.google.com/p/musicg/
 * Android Application in Google Play: https://play.google.com/store/apps/details?id=com.whistleapp
 *
 */

package com.example.clapandwhistledetector.activities;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.splashscreen.SplashScreen;

import com.example.clapandwhistledetector.MyService;
import com.example.clapandwhistledetector.R;
import com.example.clapandwhistledetector.databinding.ActivityMainBinding;
import com.example.clapandwhistledetector.util.PreferenceUtil;

public class MainActivity extends Activity implements OnClickListener {

    ActivityMainBinding binding;

    final Boolean ON = true;
    final Boolean OFF = false;
    final String WHISTLE = "whistle";
    final String CLAP = "clap";
    final String FLASH = "flash";
    final String VIBRATION = "vibration";
    final String SOUND = "sound";
    PreferenceUtil prefUtil;
    private MyService service = null;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        SplashScreen.installSplashScreen(this);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        prefUtil = new PreferenceUtil(this);
        binding.powerSwitch.setOnClickListener(this);
        binding.goToSettings.setOnClickListener(this);
        initializeSettings();
        addSwitchesListener();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!Settings.canDrawOverlays(this)) {
                Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:" + getPackageName()));
                startActivityForResult(intent, 321);
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (isMyServiceRunning()) {
            binding.powerSwitch.setColorFilter(Color.RED, PorterDuff.Mode.MULTIPLY);
            binding.detectionStatus.setText(R.string.detecting);
        } else {
            binding.powerSwitch.setColorFilter(null);
            binding.detectionStatus.setText(R.string.melody_tunes);
        }
    }

    private void addSwitchesListener() {
        binding.whistleSwitch.setOnCheckedChangeListener((vs, i) -> {
            prefUtil.save(WHISTLE, i);
            if (i) {
                prefUtil.save(VIBRATION, ON);
                prefUtil.save(FLASH, ON);
                prefUtil.save(SOUND, ON);
            }
            if (isMyServiceRunning()) {
                if (i || prefUtil.read(CLAP, OFF)) {
                    MyService.isRestartNeeded = true;
                }
                stopService(new Intent(MainActivity.this, MyService.class));

            }
        });
        binding.clapSwitch.setOnCheckedChangeListener((fs, i) -> {
            prefUtil.save(CLAP, i);
            if (isMyServiceRunning()) {
                if (i || prefUtil.read(WHISTLE, OFF)) {
                    MyService.isRestartNeeded = true;
                }
                stopService(new Intent(MainActivity.this, MyService.class));
            }
        });
    }

    private void initializeSettings() {
        binding.whistleSwitch.setChecked(prefUtil.read(WHISTLE, OFF));
        binding.clapSwitch.setChecked(prefUtil.read(CLAP, OFF));
    }

    public boolean hasPermission() {
        return ContextCompat.checkSelfPermission(this, "android.permission.RECORD_AUDIO") == 0;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == 123) {
            if (permissions.length > 0 && grantResults[0] == 0) {
                checkAndStartService();
            }
        }
    }

    private boolean isMyServiceRunning() {
        for (ActivityManager.RunningServiceInfo runningServiceInfo : ((ActivityManager) getSystemService(ACTIVITY_SERVICE)).getRunningServices(Integer.MAX_VALUE)) {
            if (MyService.class.getName().equals(runningServiceInfo.service.getClassName())) {
                return true;
            }
        }
        return false;
    }

    private void startService(){
        Intent i = new Intent(MainActivity.this, MyService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(i);
        } else {
            startService(i);
        }
        binding.powerSwitch.setColorFilter(Color.RED, PorterDuff.Mode.MULTIPLY);
        binding.detectionStatus.setText(R.string.detecting);
    }

    private void stopService(){
        Intent i = new Intent(MainActivity.this, MyService.class);
        stopService(i);
        binding.powerSwitch.setColorFilter(null);
        binding.detectionStatus.setText(R.string.melody_tunes);
    }

    private void checkAndStartService() {
        if (service == null && !isMyServiceRunning()) {
            if (prefUtil.read(WHISTLE, OFF) || prefUtil.read(CLAP, OFF)) {
                startService();
            }else {
                Toast.makeText(this, R.string.enabele_whistle_or_clap, Toast.LENGTH_SHORT).show();
            }
        } else {
            stopService();
        }

    }

    @Override
    public void onClick(View view) {
        if (view.getId() == R.id.powerSwitch) {
            if (hasPermission()) {
                checkAndStartService();
            } else {
                ActivityCompat.requestPermissions(this, new String[]{"android.permission.RECORD_AUDIO"}, 123);
            }
        } else if (view.getId() == R.id.goToSettings) {
            if (isMyServiceRunning()) {
                Toast.makeText(this, R.string.setting_change_detection_running_message, Toast.LENGTH_SHORT).show();
            } else startActivity(new Intent(MainActivity.this, SettingsActivity.class));
        }
    }

    @Override
    public void onBackPressed() {
        if (isMyServiceRunning()) {
            showDialog(getString(R.string.exit_app_service_running_message), "Ok");
        } else {
            showDialog(getString(R.string.exit_app_message), "Yes");
        }
    }

    private void showDialog(String message, String positive) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        //Setting message manually and performing action on button click
        builder
                .setMessage(message)
                .setPositiveButton(positive, (dialog, id) -> finish())
                .setNegativeButton("No", (dialog, id) -> {
                    dialog.cancel();
                });
        builder.create().show();
    }
}
