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
import android.util.Log;

public class DetectorThread extends Thread{

	private RecorderThread recorder;
	private WaveHeader waveHeader;
	private WhistleApi whistleApi;
	private ClapApi clapApi;
	private volatile Thread _thread;

	private LinkedList<Boolean> whistleResultList = new LinkedList<Boolean>();
	private LinkedList<Boolean> clapResultList = new LinkedList<Boolean>();
	private int numWhistles;
	private int numClaps;
	private int totalClapsDetected = 0;
	private int clapsCheckLength = 3;
	private int clapsPassScore = 3;
	private int totalWhistlesDetected = 0;
	private int whistleCheckLength = 3;
	private int whistlePassScore = 3;
	
	public DetectorThread(RecorderThread recorder){
		this.recorder = recorder;
		AudioRecord audioRecord = recorder.getAudioRecord();
		
		int bitsPerSample = 0;
		if (audioRecord.getAudioFormat() == AudioFormat.ENCODING_PCM_16BIT){
			bitsPerSample = 16;
		}
		else if (audioRecord.getAudioFormat() == AudioFormat.ENCODING_PCM_8BIT){
			bitsPerSample = 8;
		}
		
		int channel = 0;
		// whistle detection only supports mono channel
		if (audioRecord.getChannelConfiguration() == AudioFormat.CHANNEL_IN_MONO){
			channel = 1;
		}

		waveHeader = new WaveHeader();
		waveHeader.setChannels(channel);
		waveHeader.setBitsPerSample(bitsPerSample);
		waveHeader.setSampleRate(audioRecord.getSampleRate());
		clapApi = new ClapApi(waveHeader);
		whistleApi = new WhistleApi(waveHeader);
	}

	private void initBuffer() {
		numWhistles = 0;
		whistleResultList.clear();
		
		// init the first frames
		for (int i = 0; i < whistleCheckLength; i++) {
			whistleResultList.add(false);
		}
		// end init the first frames

		numClaps = 0;
		clapResultList.clear();

		// init the first frames
		for (int i = 0; i < clapsCheckLength; i++) {
			clapResultList.add(false);
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
				buffer = recorder.getFrameBytes();
				
				// audio analyst
				if (buffer != null) {
					// sound detected
					MainActivity.whistleValue = numWhistles;
					MainActivity.clapsValue = numClaps;

					// whistle detection
					//System.out.println("*Whistle:");
					boolean isWhistle = whistleApi.isWhistle(buffer);
					boolean isClaps = clapApi.isClap(buffer);
					Log.d("detection", "claps: " + isClaps);
					Log.d("detection", "whistle: " + isWhistle);

					if(clapResultList.getFirst()){
						numClaps--;
					}
					clapResultList.removeFirst();
					clapResultList.add(isClaps);

					if(isClaps){
						numClaps++;
						initBuffer();
						totalClapsDetected++;
					}

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
						initBuffer();
						totalWhistlesDetected++;
					}
				// end whistle detection
				}
				else{

					if(clapResultList.getFirst()){
						numClaps--;
					}
					clapResultList.removeFirst();
					clapResultList.add(false);
					MainActivity.clapsValue = numClaps;


					// no sound detected
					if (whistleResultList.getFirst()) {
						numWhistles--;
					}
					whistleResultList.removeFirst();
					whistleResultList.add(false);
					
					MainActivity.whistleValue = numWhistles;
				}
				// end audio analyst
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public int getTotalWhistlesDetected(){
		return totalWhistlesDetected;
	}
	public int getTotalClapsDetectedDetected(){
		return totalClapsDetected;
	}
}