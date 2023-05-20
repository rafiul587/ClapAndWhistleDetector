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

package com.example.clapandwhistledetector.util;

import java.util.LinkedList;

import com.musicg.api.WhistleApi;
import com.musicg.wave.WaveHeader;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.util.Log;

public class DetectorThread extends Thread{

	private final RecorderThread recorderThread;
	private AudioRecord recorder;
	private WaveHeader waveHeader;
	private WhistleApi whistleApi;
	private volatile Thread _thread;

	private OnSignalsDetectedListener onSignalsDetectedListener;

	private LinkedList<Boolean> whistleResultList = new LinkedList<Boolean>();
	private int numWhistles;
	private int totalClapsDetected = 0;
	private int totalWhistlesDetected = 0;
	private int whistleCheckLength = 3;
	private int whistlePassScore = 3;
	
	public DetectorThread(RecorderThread recorderThread){
		this.recorderThread = recorderThread;
		recorder = recorderThread.getAudioRecord();
		int bitsPerSample = 0;
		if (recorder.getAudioFormat() == AudioFormat.ENCODING_PCM_16BIT){
			bitsPerSample = 16;
		}
		else if (recorder.getAudioFormat() == AudioFormat.ENCODING_PCM_8BIT){
			bitsPerSample = 8;
		}
		
		int channel = 0;
		// whistle detection only supports mono channel
		if (recorder.getChannelConfiguration() == AudioFormat.CHANNEL_IN_MONO){
			channel = 1;
		}

		waveHeader = new WaveHeader();
		waveHeader.setChannels(channel);
		waveHeader.setBitsPerSample(bitsPerSample);
		waveHeader.setSampleRate(recorder.getSampleRate());
		whistleApi = new WhistleApi(waveHeader);
	}

	private void initBuffer() {
		numWhistles = 0;
		whistleResultList.clear();
		
		// init the first frames
		for (int i = 0; i < whistleCheckLength; i++) {
			whistleResultList.add(false);
		}
	}

	public void start() {
		_thread = new Thread(this);
        _thread.start();
    }
	
	public void stopDetection(){
		_thread = null;
	}
	
	public void run() {
		try {
			byte[] buffer;
			initBuffer();
			
			Thread thisThread = Thread.currentThread();
			while (_thread == thisThread) {
				// detect sound
				buffer = recorderThread.getFrameBytes();
				
				// audio analyst
				if (buffer != null) {
					// sound detected

					// whistle detection
					boolean isWhistle = whistleApi.isWhistle(buffer);
					Log.d("detection", "whistle: " + isWhistle);

					if (whistleResultList.getFirst()) {
						numWhistles--;
					}
		
					whistleResultList.removeFirst();
					whistleResultList.add(isWhistle);
		
					if (isWhistle) {
						numWhistles++;
					}

					if (numWhistles >= whistlePassScore) {
						// clear buffer
						initBuffer();
						onWhistleDetected();
					}
				// end whistle detection
				}
				else{
					// no sound detected
					if (whistleResultList.getFirst()) {
						numWhistles--;
					}
					whistleResultList.removeFirst();
					whistleResultList.add(false);
				}
				// end audio analyst
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void onWhistleDetected() {
		if (onSignalsDetectedListener != null) {
			onSignalsDetectedListener.onWhistleDetected();
		}
	}


	public void setOnSignalsDetectedListener(OnSignalsDetectedListener onSignalsDetectedListener) {
		this.onSignalsDetectedListener = onSignalsDetectedListener;
	}
	
	public int getTotalWhistlesDetected(){
		return totalWhistlesDetected;
	}
	public int getTotalClapsDetectedDetected(){
		return totalClapsDetected;
	}
}
