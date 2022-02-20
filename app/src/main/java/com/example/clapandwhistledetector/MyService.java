package com.example.clapandwhistledetector;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import java.io.IOException;

public class MyService extends Service implements OnSignalsDetectedListener {

    private static final String CHANNEL_DEFAULT_IMPORTANCE = "Low";
    private static final int ONGOING_NOTIFICATION_ID = 2;
    private DetectorThread detectorThread;
    private RecorderThread recorderThread;
    private boolean isFirst = true;
    MediaPlayer mediaPlayer;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Toast.makeText(this, "Detection Started", Toast.LENGTH_SHORT).show();
        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent =
                PendingIntent.getActivity(this, 0, notificationIntent, 0);

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            String NOTIFICATION_CHANNEL_ID = "com.example.simpleapp";
            String channelName = "My Background Service";
            NotificationChannel chan = new NotificationChannel(NOTIFICATION_CHANNEL_ID, channelName, NotificationManager.IMPORTANCE_LOW);
            chan.setLightColor(Color.BLUE);
            chan.setLockscreenVisibility(Notification.VISIBILITY_PRIVATE);
            NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            manager.createNotificationChannel(chan);

            NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID);
            Notification notification = notificationBuilder.setOngoing(true)
                    .setSmallIcon(R.mipmap.ic_launcher)
                    .setContentTitle("App is running in background")
                    .setPriority(NotificationManager.IMPORTANCE_DEFAULT)
                    .setCategory(Notification.CATEGORY_SERVICE)
                    .setContentIntent(pendingIntent)
                    .build();

            // Notification ID cannot be 0.
            startForeground(ONGOING_NOTIFICATION_ID, notification);
        }
        mediaPlayer = new MediaPlayer();

        recorderThread = new RecorderThread();
        recorderThread.start();
        detectorThread = new DetectorThread(recorderThread);
        detectorThread.start();
        detectorThread.setOnSignalsDetectedListener(this);
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (recorderThread != null) {
            recorderThread.stopRecording();
            recorderThread = null;
        }
        if (detectorThread != null) {
            detectorThread.stopDetection();
            detectorThread = null;
        }
        if(mediaPlayer != null){
            mediaPlayer.reset();
            mediaPlayer.release();
        }
    }

    @Override
    public void onWhistleDetected() {
        Uri mediaPath = Uri.parse("android.resource://" + this.getPackageName() + "/raw/thank");
        Log.d("TAG", "onWhistleDetected: " + mediaPath);
        try {
            mediaPlayer.reset();
            mediaPlayer.setDataSource(this, mediaPath);
            mediaPlayer.prepareAsync();
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (!mediaPlayer.isPlaying()) {
            mediaPlayer.start();
        }
    }

    @Override
    public void onClapDetected() {
        Uri mediaPath = Uri.parse("android.resource://" + this.getPackageName() + "/raw/v1");
        try {
            mediaPlayer.reset();
            mediaPlayer.setDataSource(this, mediaPath);
            mediaPlayer.prepareAsync();
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (!mediaPlayer.isPlaying()) {
            mediaPlayer.start();
        }
    }
}

