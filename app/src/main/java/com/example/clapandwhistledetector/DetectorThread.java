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

import java.util.LinkedList;

import com.musicg.api.ClapApi;
import com.musicg.api.WhistleApi;
import com.musicg.wave.WaveHeader;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaPlayer;
import android.util.Log;

public class DetectorThread extends Thread{

	private AudioRecord recorder;
	private WaveHeader waveHeader;
	private WhistleApi whistleApi;
	private ClapApi clapApi;
	private volatile Thread _thread;

	private OnSignalsDetectedListener onSignalsDetectedListener;

	private LinkedList<Boolean> whistleResultList = new LinkedList<Boolean>();
	private LinkedList<Boolean> clapResultList = new LinkedList<Boolean>();
	private int numWhistles;
	private int numClaps;
	private int totalClapsDetected = 0;
	private final int clapsCheckLength = 3;
	private final int clapsPassScore = 3;
	private int totalWhistlesDetected = 0;
	private int whistleCheckLength = 3;
	private int whistlePassScore = 3;
	AudioDispatcherFactory factory;
	
	public DetectorThread(AudioDispatcherFactory factory){
		this.factory = factory;
		recorder = factory.getRecorder();
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
		//clapApi = new ClapApi(waveHeader);
	}

	private void initBuffer() {
		numWhistles = 0;
		whistleResultList.clear();
		
		// init the first frames
		for (int i = 0; i < whistleCheckLength; i++) {
			whistleResultList.add(false);
		}
		// end init the first frames

		/*numClaps = 0;
		clapResultList.clear();

		// init the first frames
		for (int i = 0; i < clapsCheckLength; i++) {
			clapResultList.add(false);
		}*/
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
				buffer = factory.getFrameBytes();
				
				// audio analyst
				if (buffer != null) {
					// sound detected

					// whistle detection
					//System.out.println("*Whistle:");
					boolean isWhistle = whistleApi.isWhistle(buffer);
					//boolean isClaps = clapApi.isClap(buffer);
					Log.d("detection", "whistle: " + isWhistle);

/*					if(clapResultList.getFirst()){
						numClaps--;
					}
					clapResultList.removeFirst();
					clapResultList.add(isClaps);

					if(isClaps){
						numClaps++;
						initBuffer();
						totalClapsDetected++;
						onClapDetected();
					}*/

						// clear buffer


					if (whistleResultList.getFirst()) {
						numWhistles--;
					}
		
					whistleResultList.removeFirst();
					whistleResultList.add(isWhistle);
		
					if (isWhistle) {
						numWhistles++;
					}
					//System.out.println("num:" + numWhistles);

					if (numWhistles >= whistlePassScore) {
						// clear buffer
						Log.d("TAG", "numWhistle: " + numWhistles);
						initBuffer();
						totalWhistlesDetected++;
						onWhistleDetected();
					}
				// end whistle detection
				}
				else{

					/*if(clapResultList.getFirst()){
						numClaps--;
					}
					clapResultList.removeFirst();
					clapResultList.add(false);
					MainActivity.clapsValue = numClaps;*/


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

interface OnSignalsDetectedListener {
	void onWhistleDetected();
	void onClapDetected();
}