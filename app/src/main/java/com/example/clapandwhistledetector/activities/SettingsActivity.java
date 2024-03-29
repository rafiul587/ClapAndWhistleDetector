package com.example.clapandwhistledetector.activities;

import android.content.Intent;
import android.media.AudioManager;
import android.os.Build;
import android.os.Bundle;
import android.view.KeyEvent;
import android.widget.SeekBar;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.clapandwhistledetector.databinding.ActivitySettingsBinding;
import com.example.clapandwhistledetector.util.PreferenceUtil;

public class SettingsActivity extends AppCompatActivity implements SeekBar.OnSeekBarChangeListener {

    public final Boolean ON = true;
    public final Boolean OFF = false;
    public final String FLASH = "flash";
    public final String VIBRATION = "vibration";
    public final String SOUND = "sound";

    ActivitySettingsBinding binding;
    PreferenceUtil prefUtil;
    AudioManager audioManager;
    int maxV, curV;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivitySettingsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        prefUtil = new PreferenceUtil(this);
        audioManager = (AudioManager) getSystemService(AUDIO_SERVICE);
        maxV = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
        curV = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
        binding.volumeSeekBar.setMax(maxV);
        binding.volumeSeekBar.setProgress(curV);
        binding.volumeSeekBar.setOnSeekBarChangeListener(this);

        initializeSettings();
        checkSettings();
        addSwitchesListener();

        binding.selectTonesLayout.setOnClickListener(view -> {
            startActivity(new Intent(SettingsActivity.this, FileSelectActivity.class));
        });
    }

    private void addSwitchesListener() {
        binding.vibrationSwitch.setOnCheckedChangeListener((vs, i) -> {
            prefUtil.save(VIBRATION, i);
        });
        binding.flashLightSwitch.setOnCheckedChangeListener((fs, i) -> {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                prefUtil.save(FLASH, i);
            }else {
                if(binding.flashLightSwitch.isChecked()) {
                    Toast.makeText(this, "FlashLight only available for Marshmallow+ device", Toast.LENGTH_SHORT).show();
                    binding.flashLightSwitch.setChecked(false);
                    binding.flashLightSwitch.setClickable(false);
                }
            }
        });
        binding.soundSwitch.setOnCheckedChangeListener((ss, i) -> {
            prefUtil.save(SOUND, i);
        });
    }

    private void initializeSettings() {
        binding.vibrationSwitch.setChecked(prefUtil.read(VIBRATION, OFF));
        binding.flashLightSwitch.setChecked(prefUtil.read(FLASH, OFF));
        binding.soundSwitch.setChecked(prefUtil.read(SOUND, OFF));
    }

    private void checkSettings() {
        if (binding.vibrationSwitch.isChecked()) {
            prefUtil.save(VIBRATION, ON);
        } else {
            prefUtil.save(VIBRATION, OFF);
        }
        if (binding.flashLightSwitch.isChecked()) {
            prefUtil.save(FLASH, ON);
        } else {
            prefUtil.save(FLASH, OFF);
        }
        if (binding.soundSwitch.isChecked()) {
            prefUtil.save(SOUND, ON);
        } else {
            prefUtil.save(SOUND, OFF);
        }
    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean b) {
        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, progress, 0);
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {

    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {

    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN) {
            binding.volumeSeekBar.setProgress(binding.volumeSeekBar.getProgress() - 1);
            return true;
        } else if (keyCode == KeyEvent.KEYCODE_VOLUME_UP) {
            binding.volumeSeekBar.setProgress(binding.volumeSeekBar.getProgress() + 1);
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }
}