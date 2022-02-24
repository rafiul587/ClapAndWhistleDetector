package com.example.clapandwhistledetector;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
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
import android.os.Vibrator;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.core.app.NotificationCompat;
import androidx.lifecycle.LifecycleService;

import com.example.clapandwhistledetector.activities.MainActivity;
import com.example.clapandwhistledetector.activities.SplashActivity;
import com.example.clapandwhistledetector.util.AudioDispatcherFactory;
import com.example.clapandwhistledetector.util.DetectClapClap;
import com.example.clapandwhistledetector.util.DetectorThread;
import com.example.clapandwhistledetector.util.OnSignalsDetectedListener;
import com.example.clapandwhistledetector.util.PreferenceUtil;
import com.example.clapandwhistledetector.util.RecorderThread;

public class MyService extends LifecycleService implements OnSignalsDetectedListener, MediaPlayer.OnCompletionListener {

    private static final String CHANNEL_DEFAULT_IMPORTANCE = "Low";
    private static final int ONGOING_NOTIFICATION_ID = 2;
    final static String SELECTED_FILE_URI = "selected_file_uri";
    public static Boolean isRestartNeeded = false;
    private DetectorThread detectorThread;
    private RecorderThread recorderThread;
    private DetectClapClap detectClapClap;
    public static ThreadGroup threadGroups = new ThreadGroup("Thread Groups");
    String cameraId;
    MediaPlayer mediaPlayer;
    public static Vibrator vibrator;
    CameraManager cameraManager;
    CountDownTimer mTimer;
    CountDownTimer playTimer;
    Notification notification;
    PreferenceUtil prefUtil;
    final Boolean OFF = false;
    final String FLASH = "flash";
    final String VIBRATION = "vibration";
    final String SOUND = "sound";
    final String WHISTLE = "whistle";
    final String CLAP = "clap";
    final String WHISTLE_AND_CLAP = "whistleAndClap";

    public static volatile Boolean keepVibrate = false;
    public static volatile Boolean keepFlashOn = false;
    public static Boolean keepPlayingSound = false;
    private boolean deviceHasCameraFlash;
    private boolean isFlashOn;
    private boolean isFlashEnable, isVibrationEnable, isSoundEnable, isClapEnabled, isWhistleEnabled;

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
        if (mediaPlayer != null) mediaPlayer.reset();
        if (uriString.length() != 0) {
            Uri uri = Uri.parse(uriString);
            try {
                if (mediaPlayer == null) mediaPlayer = new MediaPlayer();
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
                if (vibrator != null) vibrator.cancel();
                keepFlashOn = false;
                keepVibrate = false;
                keepPlayingSound = false;
                threadGroups.interrupt();
            }
        }.start());
    }

    private void checkSettings() {
        isVibrationEnable = prefUtil.read(VIBRATION, OFF);
        isFlashEnable = prefUtil.read(FLASH, OFF);
        isSoundEnable = prefUtil.read(SOUND, OFF);
    }

    private void initializeDetector() {
        isWhistleEnabled = prefUtil.read(WHISTLE, OFF);
        isClapEnabled = prefUtil.read(CLAP, OFF);
        AudioDispatcherFactory factory = new AudioDispatcherFactory();

        if (isWhistleEnabled && isClapEnabled) {
/*            recorderThread = new RecorderThread();
            recorderThread.start();*/
            Log.d("TAG", "initializeDetector: Both");
            //For Clap
            detectClapClap = new DetectClapClap(factory);
            detectClapClap.setOnSignalsDetectedListener(this);

            //For Whistle
            detectorThread = new DetectorThread(factory);
            detectorThread.start();
            detectorThread.setOnSignalsDetectedListener(this);

        } else if (isClapEnabled) {
            Log.d("TAG", "initializeDetector: clap");
            detectClapClap = new DetectClapClap(factory);
            detectClapClap.setOnSignalsDetectedListener(this);
        } else if (isWhistleEnabled) {
            Log.d("TAG", "initializeDetector: whistle");
            factory.fromDefaultMicrophone(44100, 2048, 0);
            detectorThread = new DetectorThread(factory);
            detectorThread.start();
            detectorThread.setOnSignalsDetectedListener(this);
        }

    }

    private boolean hasCameraFlash() {
        return this.getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA_FLASH);
    }

    private void buildNotification() {
        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent =
                PendingIntent.getActivity(this, 0, notificationIntent, 0);
        String NOTIFICATION_CHANNEL_ID = "com.example.clapandwhistledetector";
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
                .setPriority(NotificationManager.IMPORTANCE_DEFAULT)
                .setCategory(Notification.CATEGORY_SERVICE)
                .setContentIntent(pendingIntent)
                .build();
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
        if (isRestartNeeded) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(new Intent(this, MyService.class));
                isRestartNeeded = false;
            }
        }
    }


    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    public void onWhistleDetected() {
        Log.d("TAG", "onWhistleDetected: ");
        play();
        Intent intent = new Intent(this, SplashActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    public void play() {
        playTimer.start();
        if (isVibrationEnable) {
            startVibration();
        }
        if (isFlashEnable && deviceHasCameraFlash) {
            startTimer(1000, true);
        }
        if (isSoundEnable) {
            keepPlayingSound = true;
            playSound();
        }
    }


    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    public void onClapDetected() {
        Intent intent = new Intent(this, SplashActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);
        play();
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

    private void startVibration() {
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
        if (keepPlayingSound) {
            playSound();
        }
    }
}

