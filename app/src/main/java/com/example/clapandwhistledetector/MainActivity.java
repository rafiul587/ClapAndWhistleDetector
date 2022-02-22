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

package com.example.clapandwhistledetector;

import android.app.ActivityManager;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.os.Build;
import android.os.Bundle;
import android.app.Activity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.example.clapandwhistledetector.databinding.ActivityMainBinding;

public class MainActivity extends Activity implements OnClickListener {

    ActivityMainBinding binding;

    public static final Boolean ON = true;
    public static final Boolean OFF = false;
    public static final String FLASH = "flash";
    public static final String VIBRATION = "vibration";
    public static final String SOUND = "sound";
    PreferenceUtil prefUtil;
    private MyService service = null;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        prefUtil = new PreferenceUtil(this);
        binding.powerSwitch.setOnClickListener(this);
        if (isMyServiceRunning()) {
            binding.powerSwitch.setColorFilter(Color.RED, PorterDuff.Mode.MULTIPLY);
            binding.detectionStatus.setText(R.string.detecting);
        } else {
            binding.powerSwitch.setColorFilter(null);
            binding.detectionStatus.setText(R.string.melody_tunes);
        }
        binding.goToSettings.setOnClickListener(this);
        addSwitchesListener();
    }

    private void addSwitchesListener() {
        binding.vibrationSwitch.setOnCheckedChangeListener((vs, i) -> {
            prefUtil.save(VIBRATION, i);
        });
        binding.flashLightSwitch.setOnCheckedChangeListener((fs, i) -> {
            prefUtil.save(FLASH, i);
        });
        binding.soundSwitch.setOnCheckedChangeListener((ss, i) -> {
            prefUtil.save(SOUND, i);
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        initializeSettings();
    }

    private void initializeSettings() {
        binding.vibrationSwitch.setChecked(prefUtil.read(VIBRATION, OFF));
        binding.flashLightSwitch.setChecked(prefUtil.read(FLASH, OFF));
        binding.soundSwitch.setChecked(prefUtil.read(SOUND, OFF));
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

    private void checkAndStartService() {
        Intent i = new Intent(MainActivity.this, MyService.class);
        if (service == null && !isMyServiceRunning()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                MainActivity.this.startForegroundService(i);
                binding.powerSwitch.setColorFilter(Color.RED, PorterDuff.Mode.MULTIPLY);
                binding.detectionStatus.setText(R.string.detecting);
                binding.vibrationSwitch.setEnabled(false);
                binding.flashLightSwitch.setEnabled(false);
                binding.soundSwitch.setEnabled(false);
            }
        } else {
            stopService(i);
            binding.powerSwitch.setColorFilter(null);
            binding.detectionStatus.setText(R.string.melody_tunes);
            binding.vibrationSwitch.setEnabled(true);
            binding.flashLightSwitch.setEnabled(true);
            binding.soundSwitch.setEnabled(true);
        }
    }

    @Override
    public void onClick(View view) {
        if (view == binding.powerSwitch) {
            if (hasPermission()) {
                checkAndStartService();
            } else {
                ActivityCompat.requestPermissions(this, new String[]{"android.permission.RECORD_AUDIO"}, 123);
            }
        } else if (view == binding.goToSettings) {
            if (isMyServiceRunning()) {
                Toast.makeText(this, "You can't change configurations when detection is running.", Toast.LENGTH_SHORT).show();
            } else startActivity(new Intent(MainActivity.this, SettingsActivity.class));
        }
    }

    @Override
    public void onBackPressed() {
        if (isMyServiceRunning()) {
            showDialog("The App will keep detecting in the background.", "Ok");
        } else {
            showDialog("Do you want to exit the app?", "Yes");
        }
    }

    private void showDialog(String message, String positive) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        //Setting message manually and performing action on button click
        builder
                .setMessage(message)
                .setCancelable(false)
                .setPositiveButton(positive, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        finish();
                    }
                })
                .setNegativeButton("No", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        //  Action for 'NO' Button
                        dialog.cancel();
                    }
                });
        builder.create().show();
    }
}
