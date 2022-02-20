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
import android.app.Service;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.app.Activity;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

public class MainActivity extends Activity {

    public static final int DETECT_NONE = 0;
    public static final int DETECT_WHISTLE = 1;
    public static int selectedDetection = DETECT_NONE;

    // detection parameters
    private DetectorThread detectorThread;
    private RecorderThread recorderThread;
    private Thread detectedTextThread;
    public static int whistleValue = 0;
    public static int clapsValue = 0;

    // views
    private View mainView, listeningView;
    private Button whistleButton, checkService;
    private TextView totalWhistlesDetectedNumberText;

    Button stopService;
    private MyService service = null;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTitle("musicg WhistleAPI Demo");

        // set views
        LayoutInflater inflater = LayoutInflater.from(this);
        mainView = inflater.inflate(R.layout.main, null);
        listeningView = inflater.inflate(R.layout.listening, null);
        setContentView(mainView);
        stopService = findViewById(R.id.stopService);
        check();
        stopService.setOnClickListener(view -> {
                    stopService(new Intent(MainActivity.this, MyService.class));
                    Toast.makeText(this, "Detection Stopped", Toast.LENGTH_SHORT).show();
                }
        );
        ClickEvent clickEvent = new ClickEvent();
        whistleButton = (Button) this.findViewById(R.id.whistleButton);
        whistleButton.setOnClickListener(clickEvent);
        checkService = findViewById(R.id.check);
        checkService.setOnClickListener(clickEvent);
    }

    private void goHomeView() {
        setContentView(mainView);
        if (recorderThread != null) {
            recorderThread.stopRecording();
            recorderThread = null;
        }
        if (detectorThread != null) {
            detectorThread.stopDetection();
            detectorThread = null;
        }
        selectedDetection = DETECT_NONE;
    }

    private void goListeningView() {
        setContentView(listeningView);

        if (totalWhistlesDetectedNumberText == null) {
            totalWhistlesDetectedNumberText = (TextView) this.findViewById(R.id.detectedNumberText);
        }

        // thread for detecting environmental noise
        if (detectedTextThread == null) {
            detectedTextThread = new Thread() {
                public void run() {
                    try {
                        while (recorderThread != null && detectorThread != null) {
                            runOnUiThread(new Runnable() {
                                public void run() {
                                    if (detectorThread != null) {
                                        totalWhistlesDetectedNumberText.setText(String.valueOf(detectorThread.getTotalWhistlesDetected()));
                                    }
                                }
                            });
                            sleep(100);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    } finally {
                        detectedTextThread = null;
                    }
                }
            };
            detectedTextThread.start();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        menu.add(0, 0, 0, "Quit demo");
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case 0:
                finish();
                break;
            default:
        }
        return super.onOptionsItemSelected(item);
    }

    public void check() {
        if (ContextCompat.checkSelfPermission(this, "android.permission.RECORD_AUDIO") != 0) {
            ActivityCompat.requestPermissions(this, new String[]{"android.permission.RECORD_AUDIO"}, 123);
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

    class ClickEvent implements OnClickListener {
        public void onClick(View view) {
            if (view == whistleButton) {
                if (service == null) {
                    Intent i = new Intent(MainActivity.this, MyService.class);
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        MainActivity.this.startForegroundService(i);
                    }
                }
				/*selectedDetection = DETECT_WHISTLE;
				recorderThread = new RecorderThread();
				recorderThread.start();
				//detectorThread = new DetectorThread(recorderThread);
				detectorThread.start();
				goListeningView();*/
            } else if (view == checkService) {
                if(isMyServiceRunning()) {
                    Toast.makeText(MainActivity.this, "Running" + "", Toast.LENGTH_SHORT).show();
                }else {
                    Toast.makeText(MainActivity.this, "Not Running" + "", Toast.LENGTH_SHORT).show();
                }
            }
        }
    }
}
