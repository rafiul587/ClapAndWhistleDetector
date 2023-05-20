package com.example.clapandwhistledetector;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.example.clapandwhistledetector.activities.DetectActivity;
import com.example.clapandwhistledetector.activities.MainActivity;
import com.example.clapandwhistledetector.util.DetectClapClap;
import com.example.clapandwhistledetector.util.DetectorThread;
import com.example.clapandwhistledetector.util.OnSignalsDetectedListener;
import com.example.clapandwhistledetector.util.PreferenceUtil;
import com.example.clapandwhistledetector.util.RecorderThread;

public class MyService extends Service implements OnSignalsDetectedListener, MediaPlayer.OnCompletionListener {

    private static final int ONGOING_NOTIFICATION_ID = 2;
    private static final long[] VIBRATE_PATTERN = {300, 700};
    final static String SELECTED_FILE_URI = "selected_file_uri";
    public static Boolean isRestartNeeded = false;
    private DetectorThread detectorThread;
    private RecorderThread recorderThread;
    private DetectClapClap detectClapClap;
    String cameraId;
    MediaPlayer mediaPlayer;
    public static Vibrator vibrator;
    CameraManager cameraManager;
    CountDownTimer mTimer;
    CountDownTimer playTimer;
    private boolean isPlayTimerRunning = false;
    Notification notification;
    PreferenceUtil prefUtil;
    final Boolean OFF = false;
    final String FLASH = "flash";
    final String VIBRATION = "vibration";
    final String SOUND = "sound";
    final String WHISTLE = "whistle";
    final String CLAP = "clap";

    public static volatile Boolean keepFlashOn = false;
    private boolean deviceHasCameraFlash, isFlashOn, isFlashEnable, isVibrationEnable, isSoundEnable;

    public MyService() {
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);
        prefUtil = new PreferenceUtil(this);
        vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);
        initializeCameraManager();
        checkSettings();
        initializeDetector();
        initializeMediaPlayer();
        initializePlayTimer();
        buildNotification();
        startForeground(ONGOING_NOTIFICATION_ID, notification);
        return START_STICKY;
    }

    private void initializeMediaPlayer() {
        String uriString = prefUtil.readString(SELECTED_FILE_URI, "");
        if (mediaPlayer != null) {
            mediaPlayer.reset();
            mediaPlayer.release();
            mediaPlayer = null;
        }
        if (uriString.length() != 0) {
            Uri uri = Uri.parse(uriString);
            try {
                mediaPlayer = new MediaPlayer();
                mediaPlayer.setDataSource(this, uri);
                mediaPlayer.prepareAsync();
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else mediaPlayer = MediaPlayer.create(this, R.raw.alarm);
        mediaPlayer.setOnCompletionListener(this);
    }

    private void initializeCameraManager() {
        cameraManager = (CameraManager) getSystemService(CAMERA_SERVICE);
        deviceHasCameraFlash = hasCameraFlash();
        try {
            cameraId = cameraManager.getCameraIdList()[0];
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private void initializePlayTimer() {
        new Handler(Looper.getMainLooper()).post(() -> playTimer = new CountDownTimer(60000, 1000) {
            public void onTick(long j) {
            }

            public void onFinish() {
                Log.d("TAG", "onFinish: ");
                isPlayTimerRunning = false;
                try {
                    if (vibrator != null) vibrator.cancel();
                    startTimer(false);
                    mediaPlayer.reset();
                } catch (Exception ignored) {
                }
            }
        }.start());

        new Handler(Looper.getMainLooper()).post(() -> mTimer = new CountDownTimer(1000, 1000) {
            public void onTick(long j) {
            }

            public void onFinish() {
                if (isFlashOn) {
                    turnOffFlash();
                } else {
                    turnOnFlash();
                }
                if (keepFlashOn) {
                    mTimer.start();
                }
            }
        });
    }

    private void checkSettings() {
        isVibrationEnable = prefUtil.read(VIBRATION, OFF);
        isFlashEnable = prefUtil.read(FLASH, OFF);
        isSoundEnable = prefUtil.read(SOUND, OFF);
    }

    private void initializeDetector() {
        boolean isWhistleEnabled = prefUtil.read(WHISTLE, OFF);
        boolean isClapEnabled = prefUtil.read(CLAP, OFF);
        try {
            if (isWhistleEnabled && isClapEnabled) {
                //For Whistle
                recorderThread = new RecorderThread();
                recorderThread.start();
                detectorThread = new DetectorThread(recorderThread);
                detectorThread.start();
                detectorThread.setOnSignalsDetectedListener(this);

                //For Clap
                detectClapClap = new DetectClapClap();
                detectClapClap.setOnSignalsDetectedListener(this);

            } else if (isClapEnabled) {
                detectClapClap = new DetectClapClap();
                detectClapClap.setOnSignalsDetectedListener(this);
            } else if (isWhistleEnabled) {
                recorderThread = new RecorderThread();
                recorderThread.start();
                detectorThread = new DetectorThread(recorderThread);
                detectorThread.start();
                detectorThread.setOnSignalsDetectedListener(this);
            }
        } catch (Exception ignored) {
        }
    }

    private boolean hasCameraFlash() {
        return this.getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA_FLASH);
    }

    private void buildNotification() {
        Intent notificationIntent = new Intent(this, MainActivity.class);
        notificationIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pendingIntent =
                PendingIntent.getActivity(this, 0, notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        String NOTIFICATION_CHANNEL_ID = this.getPackageName();
        String channelName = "Detect Whistle and Clap Service";
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(NOTIFICATION_CHANNEL_ID, channelName, NotificationManager.IMPORTANCE_LOW);
            channel.setLightColor(Color.BLUE);
            channel.setLockscreenVisibility(Notification.VISIBILITY_PRIVATE);
            channel.setImportance(NotificationManager.IMPORTANCE_DEFAULT);
            NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            manager.createNotificationChannel(channel);
        }
        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID);
        notification = notificationBuilder.setOngoing(true)
                .setSmallIcon(R.drawable.whistle)
                .setContentTitle("Whistle and Clap is detecting..")
                .setPriority(Notification.PRIORITY_DEFAULT)
                .setCategory(Notification.CATEGORY_SERVICE)
                .setContentIntent(pendingIntent)
                .build();
    }

    @Override
    public void onDestroy() {
        releaseEverything();
        super.onDestroy();
        if (isRestartNeeded) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(new Intent(this, MyService.class));
                isRestartNeeded = false;
            }
        }
    }

    private void releaseEverything() {
        if (vibrator != null) {
            vibrator.cancel();
            vibrator = null;
        }
        startTimer(false);
        if (playTimer != null) playTimer = null;
        if (mTimer != null) mTimer = null;

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
    }

    @Override
    public void onWhistleDetected() {
        play();
    }

    public void play() {
        if (!isPlayTimerRunning) {
            isPlayTimerRunning = true;
            playTimer.start();
            Intent i = new Intent(this, DetectActivity.class);
            i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(i);
        }
        if (isVibrationEnable) {
            startVibration();
        }
        if (isFlashEnable && deviceHasCameraFlash) {
            startTimer(true);
        }
        if (isSoundEnable) {
            playSound();
        }
    }


    @Override
    public void onClapDetected() {
        play();
    }

    private void startTimer(Boolean z) {
        keepFlashOn = z;
        if (keepFlashOn) {
            mTimer.start();
        } else {
            mTimer.cancel();
            turnOffFlash();
        }
    }

    private void turnOnFlash() {
        if (!isFlashOn) {
            isFlashOn = true;
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    cameraManager.setTorchMode(cameraId, true);
                }
            } catch (CameraAccessException e) {
                e.printStackTrace();
            }
        }
    }


    private void turnOffFlash() {
        if (isFlashOn) {
            isFlashOn = false;
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    cameraManager.setTorchMode(cameraId, false);
                }
            } catch (CameraAccessException e) {
                e.printStackTrace();
            }
        }
    }

    private void startVibration() {
        if (vibrator != null) {
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    // API 26 and above
                    vibrator.vibrate(VibrationEffect.createWaveform(VIBRATE_PATTERN, 0));
                } else {
                    // Below API 26
                    vibrator.vibrate(VIBRATE_PATTERN, 0);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
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

