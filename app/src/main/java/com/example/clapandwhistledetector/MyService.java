package com.example.clapandwhistledetector;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraManager;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Vibrator;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.camera.core.Camera;
import androidx.camera.core.CameraControl;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.LifecycleService;

import com.google.common.util.concurrent.ListenableFuture;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

public class MyService extends LifecycleService implements OnSignalsDetectedListener, MediaPlayer.OnCompletionListener {

    private static final String CHANNEL_DEFAULT_IMPORTANCE = "Low";
    private static final int ONGOING_NOTIFICATION_ID = 2;
    private DetectorThread detectorThread;
    private RecorderThread recorderThread;
    private DetectClapClap detectClapClap;
    ThreadGroup threadGroups = new ThreadGroup("Thread Groups");
    String cameraId;
    MediaPlayer mediaPlayer;
    Vibrator vibrator;
    CameraManager cameraManager;
    CountDownTimer mTimer;
    Notification notification;
    PreferenceUtil prefUtil;
    final Boolean OFF = false;
    final String FLASH = "flash";
    final String VIBRATION = "vibration";
    final String SOUND = "sound";

    static volatile Boolean keepVibrate = false;
    static volatile Boolean keepFlashOn = false;
    ;
    private boolean deviceHasCameraFlash;
    private boolean isFlashOn;
    private boolean isFlashEnable, isVibrationEnable, isSoundEnable;

    public MyService() {
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        super.onBind(intent);
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);
        Toast.makeText(this, "Detection Started", Toast.LENGTH_SHORT).show();
        prefUtil = new PreferenceUtil(this);
        cameraManager = (CameraManager) getSystemService(CAMERA_SERVICE);
        try {
            cameraId = cameraManager.getCameraIdList()[0];
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
        vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);
        checkSettings();
        initializeDetector();
        buildNotification();
        startForeground(ONGOING_NOTIFICATION_ID, notification);
        return START_STICKY;
    }

    private void checkSettings() {
        isVibrationEnable = prefUtil.read(VIBRATION, OFF);
        isFlashEnable = prefUtil.read(FLASH, OFF);
        isSoundEnable = prefUtil.read(SOUND, OFF);
    }

    private void initializeDetector() {
        mediaPlayer = MediaPlayer.create(this, R.raw.alarm);
        mediaPlayer.setOnCompletionListener(this);
/*        recorderThread = new RecorderThread();
        recorderThread.start();*/
        AudioDispatcherFactory factory = new AudioDispatcherFactory();
        detectClapClap = new DetectClapClap(factory);
        detectorThread = new DetectorThread(factory);
        detectorThread.start();
        detectorThread.setOnSignalsDetectedListener(this);
        detectClapClap.setOnSignalsDetectedListener(this);
    }

    private void buildNotification() {
        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent =
                PendingIntent.getActivity(this, 0, notificationIntent, 0);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            String NOTIFICATION_CHANNEL_ID = "com.example.clapandwhistleDetector";
            String channelName = "My Background Service";
            NotificationChannel chan = new NotificationChannel(NOTIFICATION_CHANNEL_ID, channelName, NotificationManager.IMPORTANCE_LOW);
            chan.setLightColor(Color.BLUE);
            chan.setLockscreenVisibility(Notification.VISIBILITY_PRIVATE);
            NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            manager.createNotificationChannel(chan);

            NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID);
            notification = notificationBuilder.setOngoing(true)
                    .setSmallIcon(R.drawable.ic__group_whistle)
                    .setContentTitle("Whistle and clap detector is running in the background!")
                    .setPriority(NotificationManager.IMPORTANCE_DEFAULT)
                    .setCategory(Notification.CATEGORY_SERVICE)
                    .setContentIntent(pendingIntent)
                    .build();
        }
    }

    @Override
    public void onDestroy() {
        Toast.makeText(this, "Detection Stopped", Toast.LENGTH_SHORT).show();
        if (vibrator != null) {
            vibrator.cancel();
            vibrator = null;
        }
        keepVibrate = false;
        keepFlashOn = false;
        threadGroups.interrupt();
        if (recorderThread != null) {
            recorderThread.stopRecording();
            recorderThread = null;
        }
        if (detectorThread != null) {
            detectorThread.stopDetection();
            detectorThread = null;
        }
        if (detectClapClap != null) {
            detectClapClap.stop();
            detectClapClap = null;
        }
        if (mediaPlayer != null) {
            mediaPlayer.reset();
            mediaPlayer.release();
        }
        super.onDestroy();
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    public void onWhistleDetected() {
        if (isVibrationEnable) {
            vibrate();
        }
        if (isFlashEnable) {
            startTimer(1000, true);
        }
        if (isSoundEnable) {
            playSound();
        }
    }


    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    public void onClapDetected() {
        if (isVibrationEnable) {
            vibrate();
        }
        if (isFlashEnable) {
            startTimer(1000, true);
        }
        if (isSoundEnable) {
            playSound();
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    private void startTimer(int i, Boolean z) {
        keepFlashOn = z;
        if (keepFlashOn) {
            new Handler(Looper.getMainLooper()).post(() -> mTimer = new CountDownTimer(i, 1000) {
                public void onTick(long j) {
                    Log.d("TAG", "onTick: 1");
                }

                public void onFinish() {
                    if (isFlashOn) {
                        Log.d("TAG", "onFinish: 1");
                        turnOffFlash();
                    } else {
                        Log.d("TAG", "onFinish: 2");
                        turnOnFlash();
                    }
                    startTimer(1000, keepFlashOn);
                }
            }.start());
        } else {
            mTimer.cancel();
            turnOffFlash();
            Log.d("TAG", "startTimer: jjjjhjh");
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    private void turnOnFlash() {
        if (!isFlashOn) {
            isFlashOn = true;
            try {
                cameraManager.setTorchMode(cameraId, true);
            } catch (CameraAccessException e) {
                e.printStackTrace();
            }
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    private void turnOffFlash() {
        if (isFlashOn) {
            isFlashOn = false;
            try {
                cameraManager.setTorchMode(cameraId, false);
            } catch (CameraAccessException e) {
                e.printStackTrace();
            }
        }
    }

    private void vibrate() {
        keepVibrate = true;
        new Thread(threadGroups, () -> {
            while (keepVibrate && !Thread.currentThread().isInterrupted()) {
                Log.d("TAG", "runvibrate: " + keepVibrate);
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                try {
                    vibrator.vibrate(1000);
                } catch (Exception unused) {
                }
            }
        }).start();

    }

    private void playSound() {
        try {
            if (mediaPlayer != null && !mediaPlayer.isPlaying()) {
                mediaPlayer.start();
            }
        } catch (IllegalStateException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onCompletion(MediaPlayer mediaPlayer) {
        playSound();
    }
}

