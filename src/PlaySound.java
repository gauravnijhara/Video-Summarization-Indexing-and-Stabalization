//package org.wikijava.sound.playWave;

import java.io.*;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;
import javax.sound.sampled.UnsupportedAudioFileException;
import javax.sound.sampled.DataLine.Info;
import java.util.List;

import static java.lang.Thread.sleep;

/**
 * 
 * <Replace this with a short description of the class.>
 * 
 * @author Giulio
 */
public class PlaySound {

	AudioInputStream audioInputStream = null;
    private InputStream waveStream;
	private float sampleRate;
	private SourceDataLine dataLine;
	byte[] summarizedByte;
	private static AudioFormat audioFormat;
	private Info info;
	public String fileName;
    //private final int EXTERNAL_BUFFER_SIZE = 524288; // 128Kb

	//24000 samples per second, 24000/15 samples per frame, that*2 bytes
	private final int EXTERNAL_BUFFER_SIZE = 3200;

    /**
     * CONSTRUCTOR
     */
    public PlaySound(InputStream waveStream) {
	//this.waveStream = waveStream;
	this.waveStream = new BufferedInputStream(waveStream);
    }

	public void audioInitialize(List<Integer> frames) throws PlayWaveException, InterruptedException {
	//Copying bytes from required frames in to summarizedByte byte array

	try {
	    audioInputStream = AudioSystem.getAudioInputStream(this.waveStream);
	} catch (UnsupportedAudioFileException e1) {
	    throw new PlayWaveException(e1);
	} catch (IOException e1) {
	    throw new PlayWaveException(e1);
	}

	// Obtain the information about the AudioInputStream
	audioFormat = audioInputStream.getFormat();
	info = new Info(SourceDataLine.class, audioFormat);
	this.sampleRate = audioFormat.getSampleRate();
	// opens the audio channel
	

	
	ByteArrayOutputStream out = new ByteArrayOutputStream();
	byte[] dataBuffer = new byte[EXTERNAL_BUFFER_SIZE];
	int size = 0, sumSize = 0;
	int frameIndex = 0;

	try {
	while (frameIndex<frames.size() && (size = this.waveStream.read(dataBuffer, 0, EXTERNAL_BUFFER_SIZE)) != -1) {
		sumSize+=size;

		if(sumSize == frames.get(frameIndex) * 3200) {	
		    out.write(dataBuffer, 0, size);
			frameIndex++;
		}

		
	}
	summarizedByte = out.toByteArray();
	//System.out.println(summarizedByte.length);
	} catch (IOException e1) {
	    throw new PlayWaveException(e1);
	}

    }




	public void playSpecificFrames() throws PlayWaveException, InterruptedException {
		dataLine = null;
			try {
		//dataLine has audio bytes
		    dataLine = (SourceDataLine) AudioSystem.getLine(info);
		    dataLine.open(audioFormat, this.EXTERNAL_BUFFER_SIZE);
		} catch (LineUnavailableException e1) {
		    throw new PlayWaveException(e1);
		}
		// Starts the music :P
		dataLine.start();
		int readBytes = 0, index = 0, arrSize = summarizedByte.length;
		byte[] audioBuffer = new byte[this.EXTERNAL_BUFFER_SIZE];
		try {
		    while (index < arrSize) {
			long lStartTime = System.currentTimeMillis();
		        dataLine.write(summarizedByte, index, EXTERNAL_BUFFER_SIZE);
			index+=EXTERNAL_BUFFER_SIZE;
			long difference = (1000/15) - (System.currentTimeMillis() - lStartTime);
			    if(difference>0) {
				sleep(difference);
			    }

		    }
		} /*catch (IOException e1) {
		    throw new PlayWaveException(e1);
		}*/ finally {
		    // plays what's left and and closes the audioChannel
		    dataLine.drain();
		    dataLine.close();
		}
	}

	public static float getDominantFrequency(String fileName) throws Exception {
		WaveDecoder decoder = new WaveDecoder(new FileInputStream(fileName));
		FFT fft = new FFT(4096, audioFormat.getSampleRate());
		float[] samples = new float[4096];

		float max = -1;
		while (decoder.readSamples(samples) > 0) {
			fft.forward(samples);

			for (int i = 0; i < fft.getSpectrum().length; i++)
				if (max < fft.getSpectrum()[i]) max = fft.getSpectrum()[i];
		}

		return max;
	}
}
