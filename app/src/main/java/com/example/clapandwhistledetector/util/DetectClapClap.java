package com.example.clapandwhistledetector.util;

import android.content.Context;
import android.media.AudioRecord;
import android.util.Log;

import be.tarsos.dsp.AudioDispatcher;
import be.tarsos.dsp.onsets.OnsetHandler;
import be.tarsos.dsp.onsets.PercussionOnsetDetector;


public class DetectClapClap implements OnsetHandler {

    static int SAMPLE_RATE = 8000;
    private byte[] buffer;
    private int clap;
    private Context mContext;
    private boolean mIsRecording;
    private PercussionOnsetDetector mPercussionOnsetDetector;
    private OnSignalsDetectedListener onSignalsDetectedListener;
    private int nb_claps = 3;
    private int rateSupported;
    private boolean rate_send;
    AudioDispatcher dispatcher;
    AudioDispatcherFactory factory;


    public DetectClapClap(AudioDispatcherFactory factory) {
/*        this.recorder = recorder.getAudioRecord();
        SAMPLE_RATE = getValidSampleRates();
        int minBufferSize = AudioRecord.getMinBufferSize(SAMPLE_RATE, 16, 2);
        this.buffer = new byte[minBufferSize];
        this.mPercussionOnsetDetector = new PercussionOnsetDetector((float) SAMPLE_RATE, minBufferSize / 2, this, 24.0d, 5.0d);
        this.clap = 0;
        this.mIsRecording = true;*/
        this.factory = factory;
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

        dispatcher = factory.fromDefaultMicrophone(44100, 2048, 0);
        double threshold = 8;
        double sensitivity = 20;

        PercussionOnsetDetector mPercussionDetector = new PercussionOnsetDetector(44100, 2048,
                (time, salience) -> {
                    Log.d("TAG", "Clap detected!");
                    onClapDetected();

                }, sensitivity, threshold);

        dispatcher.addAudioProcessor(mPercussionDetector);
        new Thread(dispatcher, "Audio Dispatcher").start();
    }

    public void stop() {
        dispatcher.stop();
    }

    public AudioRecord getRecorder() {
        return factory.getRecorder();
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
