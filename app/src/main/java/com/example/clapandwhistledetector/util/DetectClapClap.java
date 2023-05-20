package com.example.clapandwhistledetector.util;

import android.media.AudioRecord;

import be.tarsos.dsp.AudioDispatcher;
import be.tarsos.dsp.io.android.AudioDispatcherFactory;
import be.tarsos.dsp.onsets.OnsetHandler;
import be.tarsos.dsp.onsets.PercussionOnsetDetector;


public class DetectClapClap implements OnsetHandler {
    ;
    private PercussionOnsetDetector mPercussionOnsetDetector;
    private OnSignalsDetectedListener onSignalsDetectedListener;
    private int nb_claps = 3;
    private int rateSupported;
    private boolean rate_send;
    AudioDispatcher dispatcher;


    public DetectClapClap() {
        listen();
    }

    public int getValidSampleRates() {
        for (int i : new int[]{44100, 22050, 16000, 11025, 8000}) {
            if (AudioRecord.getMinBufferSize(i, 1, 2) > 0 && !this.rate_send) {
                this.rateSupported = i;
                this.rate_send = true;
            }
        }
        return this.rateSupported;
    }

    public void handleOnset(double d, double d2) {
        onClapDetected();
    }

    public void listen() {
        try {
            dispatcher = AudioDispatcherFactory.fromDefaultMicrophone(22050, 1024, 0);
        } catch (Exception e) {
            e.printStackTrace();
        }

        double threshold = 8;
        double sensitivity = 20;

        PercussionOnsetDetector mPercussionDetector = new PercussionOnsetDetector(22050, 1024,
                (time, salience) -> onClapDetected(), sensitivity, threshold);

        dispatcher.addAudioProcessor(mPercussionDetector);
        new Thread(dispatcher, "Audio Dispatcher").start();
    }

    public void stop() {
        dispatcher.stop();
    }

    private void onClapDetected() {
        if (onSignalsDetectedListener != null) {
            onSignalsDetectedListener.onClapDetected();
        }
    }

    public void setOnSignalsDetectedListener(OnSignalsDetectedListener onSignalsDetectedListener) {
        this.onSignalsDetectedListener = onSignalsDetectedListener;
    }
}
