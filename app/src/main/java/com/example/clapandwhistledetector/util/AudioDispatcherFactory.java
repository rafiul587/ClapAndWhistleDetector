package com.example.clapandwhistledetector.util;

import android.annotation.SuppressLint;
import android.media.AudioRecord;
import android.media.MediaRecorder;

import be.tarsos.dsp.AudioDispatcher;
import be.tarsos.dsp.io.TarsosDSPAudioFormat;
import be.tarsos.dsp.io.TarsosDSPAudioInputStream;
import be.tarsos.dsp.io.android.AndroidAudioInputStream;

/**
 * The Factory creates {@link AudioDispatcher} objects from the
 * configured default microphone of an Android device.
 * It depends on the android runtime and does not work on the standard Java runtime.
 *
 * @author Joren Six
 * @see AudioDispatcher
 */
public class AudioDispatcherFactory {

    AudioRecord audioInputStream;
    byte[] buffer;
    // for 1024 fft size (16bit sample size)
    int frameByteSize = 2048;
    public AudioDispatcherFactory(){
        buffer = new byte[frameByteSize];
    }

    @SuppressLint("MissingPermission")
    public AudioDispatcher fromDefaultMicrophone(final int sampleRate,
                                                 final int audioBufferSize, final int bufferOverlap) {
        int minAudioBufferSize = AudioRecord.getMinBufferSize(sampleRate,
                android.media.AudioFormat.CHANNEL_IN_MONO,
                android.media.AudioFormat.ENCODING_PCM_16BIT);
        int minAudioBufferSizeInSamples = minAudioBufferSize / 2;
        if (minAudioBufferSizeInSamples <= audioBufferSize) {
            audioInputStream = new AudioRecord(
                    MediaRecorder.AudioSource.MIC, sampleRate,
                    android.media.AudioFormat.CHANNEL_IN_MONO,
                    android.media.AudioFormat.ENCODING_PCM_16BIT,
                    audioBufferSize * 2);

            TarsosDSPAudioFormat format = new TarsosDSPAudioFormat(sampleRate, 16, 1, true, false);

            TarsosDSPAudioInputStream audioStream = new AndroidAudioInputStream(audioInputStream, format);
            //start recording ! Opens the stream.
            audioInputStream.startRecording();
            return new AudioDispatcher(audioStream, audioBufferSize, bufferOverlap);
        } else {
            throw new IllegalArgumentException("Buffer size too small should be at least " + (minAudioBufferSize * 2));
        }
    }

    public AudioRecord getRecorder() {
        return audioInputStream;

    }

    public byte[] getFrameBytes(){
        audioInputStream.read(buffer, 0, frameByteSize);

        // analyze sound
        int totalAbsValue = 0;
        short sample = 0;
        float averageAbsValue = 0.0f;

        for (int i = 0; i < frameByteSize; i += 2) {
            sample = (short)((buffer[i]) | buffer[i + 1] << 8);
            totalAbsValue += Math.abs(sample);
        }
        averageAbsValue = totalAbsValue / frameByteSize / 2;

        //System.out.println(averageAbsValue);

        // no input
        if (averageAbsValue < 30){
            return null;
        }

        return buffer;
    }
}