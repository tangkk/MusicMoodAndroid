/**
 * Created by tangkk on 27/7/14.
 */

package com.example.musicmood;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.media.MediaCodec;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.os.AsyncTask;
import android.os.Environment;
import android.util.Log;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.lang.Math;

public class FeatureExtraction {

    public final String LOG_TAG = "FeatureExtraction";

    protected MediaExtractor extractor;
    protected MediaCodec codec;
    protected AudioTrack audioTrack;

    protected int inputBufIndex;

    /**
     * String The url of the radio stream
     */
    private String mUrlString;
    private String[] mUrlStringSet;
    String[] songArray;
    // zeroCrossingRate, Unit Power, Low Energy Rate, BPM, Spectral Centroid, Tonal Type
    private float[] output = new float[6]; // six dimensional music representation
    private int[] outputMoodMapping = new int[3];

//    public void setUrlString(String mUrlString) {
//        this.mUrlString = mUrlString;
//    }
    /**
     * mUrlString getter
     */
    public String getUrlString() {
        return mUrlString;
    }

    /**
     * Constructor
     *
     * @param url String The url of the radio stream
     */
    public FeatureExtraction(String[] mUrlStringSet){
        this.mUrlStringSet = mUrlStringSet;
    }

    /**
     * Attempts to fetch mp3 data from the mUrlString location, decode it and feed it to an AudioTrack instance
     *
     * @return void
     * @throws java.io.IOException
     */
    public void start() throws IOException {
        songArray = this.readDataStorageToStringArray();
        new DecodeOperation().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    /**
     * @throws IOException
     *
     *
     */

    // 8388608 = 2^23, about 3 minis of a song with 44.1kHz sample rate
    // 32768 = 2^15 - 1, which is the max_value of short
    float[] songAvg = new float[8388608]; // might be a bug with higher sample rate
    short maxData = (short) 32768;
    int numberofChannelData = 0; // typically stereo channel + 16bit PCM, 4 times the length of a matlab audio read
    private void writeRaw (byte[] chunk, ByteOrder endian) {
        if (numberofChannelData + chunk.length < songAvg.length) { // no greater than 5 mins with 44100 sampling rate
            for (int i = 0; i < chunk.length; i += 4) {
                short leftData = 0;
                short rightData = 0;
                if (endian == ByteOrder.LITTLE_ENDIAN) {
                    leftData = (short) ((chunk[i + 1] << 8) | (chunk[i] & 0xff));
                    rightData = (short) ((chunk[i + 3] << 8) | (chunk[i + 2] & 0xff));
                } else {
                    leftData = (short) ((chunk[i] << 8) | (chunk[i+1] & 0xff));
                    rightData = (short) ((chunk[i + 2] << 8) | (chunk[i + 3] & 0xff));
                }
                short difData = (short) (leftData - rightData);
                float avgData = ((float)leftData + (float)rightData) / 2;
                float avgDataFloat = avgData / (float) maxData; // convert to float number
                songAvg[numberofChannelData + i / 4] = avgDataFloat;
            }
            numberofChannelData += chunk.length / 4;
        }
    }

    private void writeRepresentation (int i, float data) {
        output[i] = data;
    }

    private String[] readDataStorageToStringArray() {
        Log.i(LOG_TAG, "readDataStorageToStringArray");
        String[] myArray = new String[500];
        int numRead= 0;
        try {
            File txt = new File(Environment.getExternalStorageDirectory()+"/SafeDJ/MusicRepresentation.txt");
            BufferedReader br = new BufferedReader(new FileReader(txt));
            int i=0;
            String readLine = "";
            while((readLine = br.readLine()) != null){
                int idx = readLine.indexOf(":");
                String songPath = readLine.substring(0, idx);
                myArray[i] = songPath;
                Log.i(LOG_TAG, myArray[i]);
                i++;
            }
            numRead=i;
            System.out.println("numRead right after ass: "+numRead);
            br.close();
        }catch (Exception e) {
            Log.i(LOG_TAG, "readDataStorage Exception");
            e.printStackTrace();
        }
        return myArray;
    }

    private void writeDataStorage () {
        Log.i(LOG_TAG, "writeDataStorage");
        String data = mUrlString + ":"
                + "Mood - ["
                + Integer.toString(outputMoodMapping[0]) + ";"
                + Integer.toString(outputMoodMapping[1]) + ";"
                + Integer.toString(outputMoodMapping[2]) + "]"
                + "|"
                + "Raw - ["
                + Float.toString(output[0])+";"
                +Float.toString(output[1])+";"+ Float.toString(output[2])+";"
                +Float.toString(output[3])+";"+Float.toString(output[4])+";"+ Float.toString(output[5]) + "]";
        try{
            BufferedWriter bw = new BufferedWriter(new FileWriter(Environment.getExternalStorageDirectory()+"/SafeDJ/MusicRepresentation.txt", true));
            bw.write(data);
            bw.newLine();
            //bw.flush();
            bw.close();
        }catch (IOException ioe) {
            System.out.println("IO Exception");
        }
    }

    private void moodMapping () {
        // Music Representation: zeroCrossingRate, Unit Power, Low Energy Rate, BPM, Spectral Centroid, Tonal Type
        // Mood Representation: Aggressiveness(0 Yes, 1 No), Happiness(Angry, Sad, Happy, Neutral), Patience (0 Impatient, 1 pleasant)
        // Aggressiveness -> Unit Power , BPM
        // Happiness -> Spectral Centroid , Tonal Type
        // Patience -> Low Energy, zeroCrossingRate

        /* Calculate Aggressiveness */
        outputMoodMapping[0] = (output[3] > 90) ? ((output[1] > Math.pow(1.7,11)) ? 0 : 1) : 1;
        /* Calculate Happiness */
        outputMoodMapping[1] = (output[5] == 1) ? ((output[4] > 0.2) ? 2 : 3) : ((output[4] > 0.2) ? 0 : 1);
        /* Calculate Patience */
        outputMoodMapping[2] = (output[2] > 0.9) ? ((output[0] > 0.05) ? 0 : 1) : 0;
    }

    private void musicFeatureExtraction (int sampleRate) {
        this.temperalFeature(sampleRate);
        this.spectralFeature(sampleRate);
        this.moodMapping();
        this.writeDataStorage();
    }

    private void temperalFeature(int sampleRate) {
        /****** Normalized floating point mono channel ******/
        float maxDatafloat = 0;
        for (int i = 0; i < numberofChannelData; i++) {
            maxDatafloat = Math.max(songAvg[i], maxDatafloat);
        }

        /****** ZeroCrossing Rate ******/
        int zeroCrossing = 0;
        for (int i  = 0; i < numberofChannelData - 1; i++) {
            zeroCrossing += Math.abs(Math.signum(songAvg[i]) - Math.signum(songAvg[i+1]));
        }
        float zeroCrossingRate = (float)zeroCrossing / (float)numberofChannelData;
        Show.show(zeroCrossingRate, "zeroCrossingRate");
        writeRepresentation(0, zeroCrossingRate);

        /****** unit power ******/
        float unitPower = 0;
        float sum = 0;
        for (int i = 0; i < numberofChannelData; i++) {
            sum += songAvg[i]*songAvg[i];
        }
        unitPower = sum / numberofChannelData;
        Show.show(unitPower, "unitPower");
        writeRepresentation(1, unitPower);

        /****** low energy ******/
        float lowEnergy = 0;
        float lowEnergyRate = 0;
        for (int i = 0; i < numberofChannelData; i++) {
            if (songAvg[i]*songAvg[i] < unitPower)
                lowEnergy += 1;
        }
        lowEnergyRate = lowEnergy / numberofChannelData;
        Show.show(lowEnergyRate, "lowEnergyRate");
        writeRepresentation(2, lowEnergyRate);

        /****** tempo and rhythm pattern ******/
        // downsample the original data
        int downSampleRate = 32;
        int downNumberofData = (int)Math.ceil((float)numberofChannelData / downSampleRate);
        Show.show(numberofChannelData, "numberofChannelData");
        Show.show(downNumberofData, "downNumberofData");
        float[] downSampledSong = new float[downNumberofData];
        float sumDownSampledSong = 0;
        for(int i = 0; i < numberofChannelData; i+=downSampleRate) {
            float absData = Math.abs(songAvg[i]);
            downSampledSong[i/downSampleRate] = absData;
            sumDownSampledSong += absData;
        }
        float meanDownSampledSong = sumDownSampledSong / downNumberofData;

        // auto-correlation of data
        int beginShift = (int)Math.round((float)sampleRate / (downSampleRate*4));
        int fourSeconds = (int)Math.round((float)4*sampleRate / (float)downSampleRate);
        int secLength = fourSeconds - beginShift;
        Show.show(beginShift, "beginShift");
        Show.show(fourSeconds, "fourSeconds");
        double[] autocorr = new double[fourSeconds]; // to store the autocorrelation result for 10 second
        for (int i = 0; i < fourSeconds; i++) {
            autocorr[i] = 0;
        }
        for (int i = 0; i < downNumberofData; i++) {
            autocorr[0] += (downSampledSong[i]-meanDownSampledSong)*(downSampledSong[i]-meanDownSampledSong);
        }
        autocorr[0] /= downNumberofData;
        for (int j = beginShift; j < fourSeconds; j++) {
            for (int i = j; i < downNumberofData; i++) {
                autocorr[j] += (downSampledSong[i]-meanDownSampledSong)*(downSampledSong[i-j]-meanDownSampledSong);
            }
            autocorr[j] /= (downNumberofData*autocorr[0]);
        }
        Show.show("autocorr fin...");

        // extract a section which cuts the beginshift
        double[] autocorrSec = new double[autocorr.length - beginShift];
        for (int i = 0; i < autocorrSec.length; i++) {
            autocorrSec[i] = autocorr[beginShift + i];
        }
        Show.show("autocorrSec fin...");

        // median filter the autocorr result
        MedianFilter MF = new MedianFilter();
        autocorrSec = MF.filter(autocorrSec, 50);
        Show.show("Medianfilter fin...");

        // polyfit the autocorr and subtract the baseline
        PolynomialRegression regression = new PolynomialRegression(autocorrSec, 5);
        Show.show("PolynomialRegression fin...");
        double[] polyval = regression.polyval();
        double maxAutoCorr = 0;
        for (int i = 0; i < autocorrSec.length; i++) {
            autocorrSec[i] = autocorrSec[i] - polyval[i];
            maxAutoCorr = Math.max(maxAutoCorr, autocorrSec[i]);
        }
        for (int i = 0; i < autocorrSec.length; i++) {
            autocorrSec[i] /= maxAutoCorr;
        }
        Show.show("Polyfit fin...");

        // find peaks algorithm to finally find out the tempo of the song
        PeakDetector PD = new PeakDetector(autocorrSec);
        double stringency = 2.3;
        int window = 200;
        int minDistance = 345;
        int[] locs = PD.process(window, stringency);
        locs = PD.reducePeak(locs, minDistance);
        Show.show(locs, "locs");
        Show.show("PeakDetector fin...");

        // loc[0] to bpm: the bar tempo
//        int barSample = locs[locs.length - 1] + beginShift;
//        double barPerMin = (double)(60*sampleRate) / (barSample*downSampleRate);
//        int bpm = 4*(int)Math.round(barPerMin);
        //Show.show(barPerMin, "barPerMin");
        int beatSample = 0;
        if (locs.length > 0)
        	beatSample = locs[0] + beginShift;
        	
        int bpm = (int)Math.round((double)(60*sampleRate) / (beatSample*downSampleRate));
        // maximum 160 bpm
        if (bpm > 160)
            bpm = bpm / 2;
        Show.show(bpm, "bpm");
        writeRepresentation(3, bpm);
    }

    private void spectralFeature(int sampleRate) {
        /****** FFT transformation ******/
        int NFFT = (int)Math.pow(2,23);
        // find out the next power of 2 of N
//        for (int i = 21; i < 30; i++) {
//            if (Math.pow(2,i) > numberofChannelData) {
//                NFFT = (int) (Math.pow(2, i));
//                break;
//            }
//        }
        Show.show(NFFT, "NFFT");

        float[] im = new float[NFFT];
        for (int i = 0; i < NFFT; i++) {
            im[i] = 0;
        }
        FFT fft = new FFT(NFFT);
        fft.fft(songAvg, im);

        // Magnitude spectrum
        int oneSideNFFT = (NFFT / 2) + 1;
        for (int i = 0; i < oneSideNFFT; i++) {
            songAvg[i] = (float)Math.hypot(songAvg[i], im[i]);
        }

        /****** Spectral Centroid ******/
        double spectrumWeight = 0;
        double ampSum = 0;
        float frequencyInterval = (float) sampleRate / (float)(2*oneSideNFFT);
        float specCentroid = 0;
        for (int i = 0; i < oneSideNFFT; i++) {
            spectrumWeight += songAvg[i] * frequencyInterval * i;
            ampSum += songAvg[i];
        }
        specCentroid = (float)(spectrumWeight / ampSum) / (sampleRate / 2);
        Show.show(specCentroid, "specCentroid");
        writeRepresentation(4, specCentroid);

        /****** Spectral Roll-off ******/
        double sumAmp = 0;
        float specRolloff = 0;
        for (int i = 0; i < oneSideNFFT; i++) {
            sumAmp += songAvg[i];
            if (sumAmp > 0.85 * ampSum) {
                specRolloff = i*frequencyInterval;
                specRolloff = specRolloff / (sampleRate/2);
                break;
            }
        }
        Show.show(specRolloff, "specRolloff");

        /****** Tonal Gravity and Tonal Type ******/
        // the mid feature is chroma of the song
        // A = 69, mod (69, 12) = 9, thus
        // C 1
        // C# 2
        // D 3
        // D# 4
        // E 5
        // F 6
        // F# 7
        // G 8
        // G# 9
        // A 10
        // A# 11
        // B 12
        int midiNumber = 0;
        int midiPitchClass = 0;
        float[] chroma = new float[13];
        for (int i = 0; i < 13; i++) {
            chroma[i] = 0;
        }
        for (int i = 0; i < oneSideNFFT; i++) {
            if (frequencyInterval * i >= 20) {
                float fm = frequencyInterval * i;
                midiNumber = (int)Math.round(12*(Math.log(fm/440)/Math.log(2)) + 69);
                midiPitchClass = midiNumber % 12 + 1;
                chroma[midiPitchClass] += songAvg[i];
            }
        }
        float chromaMax = 0;
        for (int i = 0; i < 13; i++) {
            chromaMax = Math.max(chromaMax,chroma[i]);
        }
        // Normalization
        for (int i = 0; i < 13; i++) {
            chroma[i] /= chromaMax;
            //Show.show(chroma, "chroma");
        }
        // C C# D D# E F F# G G# A  A# B
        // 1 2  3 4  5 6 7  8 9  10 11 12
        // The tonal is calculated according to major and minor scale

        float[]  tonalMajorScale = new float[13];
        tonalMajorScale[1] = chroma[1]+chroma[3]+chroma[5]+chroma[6]+chroma[8]+chroma[10]+chroma[12];
        tonalMajorScale[2] = chroma[2]+chroma[4]+chroma[6]+chroma[7]+chroma[9]+chroma[11]+chroma[1];
        tonalMajorScale[3] = chroma[3]+chroma[5]+chroma[7]+chroma[8]+chroma[10]+chroma[12]+chroma[2];
        tonalMajorScale[4] = chroma[4]+chroma[6]+chroma[8]+chroma[9]+chroma[11]+chroma[1]+chroma[3];
        tonalMajorScale[5] = chroma[5]+chroma[7]+chroma[9]+chroma[10]+chroma[12]+chroma[2]+chroma[4];
        tonalMajorScale[6] = chroma[6]+chroma[8]+chroma[10]+chroma[11]+chroma[1]+chroma[3]+chroma[5];
        tonalMajorScale[7] = chroma[7]+chroma[9]+chroma[11]+chroma[12]+chroma[2]+chroma[4]+chroma[6];
        tonalMajorScale[8] = chroma[8]+chroma[10]+chroma[12]+chroma[1]+chroma[3]+chroma[5]+chroma[7];
        tonalMajorScale[9] = chroma[9]+chroma[11]+chroma[1]+chroma[2]+chroma[4]+chroma[6]+chroma[8];
        tonalMajorScale[10] = chroma[10]+chroma[12]+chroma[2]+chroma[3]+chroma[5]+chroma[7]+chroma[9];
        tonalMajorScale[11] = chroma[11]+chroma[1]+chroma[3]+chroma[4]+chroma[6]+chroma[8]+chroma[10];
        tonalMajorScale[12] = chroma[12]+chroma[2]+chroma[4]+chroma[5]+chroma[7]+chroma[9]+chroma[11];

        float[]  tonalMinorScale = new float[13];
        tonalMinorScale[1] = chroma[1]+chroma[3]+chroma[4]+chroma[6]+chroma[8]+chroma[9]+chroma[11];
        tonalMinorScale[2] = chroma[2]+chroma[4]+chroma[5]+chroma[7]+chroma[9]+chroma[10]+chroma[12];
        tonalMinorScale[3] = chroma[3]+chroma[5]+chroma[6]+chroma[8]+chroma[10]+chroma[11]+chroma[1];
        tonalMinorScale[4] = chroma[4]+chroma[6]+chroma[7]+chroma[9]+chroma[11]+chroma[12]+chroma[2];
        tonalMinorScale[5] = chroma[5]+chroma[7]+chroma[8]+chroma[10]+chroma[12]+chroma[1]+chroma[3];
        tonalMinorScale[6] = chroma[6]+chroma[8]+chroma[9]+chroma[11]+chroma[1]+chroma[2]+chroma[4];
        tonalMinorScale[7] = chroma[7]+chroma[9]+chroma[10]+chroma[12]+chroma[2]+chroma[3]+chroma[5];
        tonalMinorScale[8] = chroma[8]+chroma[10]+chroma[11]+chroma[1]+chroma[3]+chroma[4]+chroma[6];
        tonalMinorScale[9] = chroma[9]+chroma[11]+chroma[12]+chroma[2]+chroma[4]+chroma[5]+chroma[7];
        tonalMinorScale[10] = chroma[10]+chroma[12]+chroma[1]+chroma[3]+chroma[5]+chroma[6]+chroma[8];
        tonalMinorScale[11] = chroma[11]+chroma[1]+chroma[2]+chroma[4]+chroma[6]+chroma[7]+chroma[9];
        tonalMinorScale[12] = chroma[12]+chroma[2]+chroma[3]+chroma[5]+chroma[7]+chroma[8]+chroma[10];

        float MajorMax = 0;
        float MinorMax = 0;
        int MajorIdx = 0;
        int MinorIdx = 0;
        for (int i = 0; i < 13; i++) {
            MajorMax = Math.max(MajorMax, tonalMajorScale[i]);
            if (MajorMax == tonalMajorScale[i])
                MajorIdx = i;

            MinorMax = Math.max(MinorMax, tonalMinorScale[i]);
            if (MinorMax == tonalMinorScale[i])
                MinorIdx = i;
        }

        int[] tonal = new int[2];
        tonal[0] = 0; tonal[1] = 0;
        if (chroma[MajorIdx] == chroma[MinorIdx]) {
            tonal[0] = 0;
            tonal[1] = 0;
        } else {
            if (chroma[MajorIdx] > chroma[MinorIdx]) {
                tonal[0] = 1;
                tonal[1] = MajorIdx;
            } else {
                tonal[0] = -1;
                tonal[1] = MinorIdx;
            }
        }
        Show.show(tonal, "tonal");
        writeRepresentation(5, tonal[0]);
    }

    private void decodeLoop()
    {

        ByteBuffer[] codecInputBuffers;
        ByteBuffer[] codecOutputBuffers;

        // extractor gets information about the stream
        extractor = new MediaExtractor();
        try {
            extractor.setDataSource(this.mUrlString);
        } catch (Exception e) {
            Log.e(LOG_TAG, "Url failed");
            return;
        }

        MediaFormat format = extractor.getTrackFormat(0);
        String mime = format.getString(MediaFormat.KEY_MIME);

        // the actual decoder
        codec = MediaCodec.createDecoderByType(mime);
        codec.configure(format, null /* surface */, null /* crypto */, 0 /* flags */);
        codec.start();
        codecInputBuffers = codec.getInputBuffers();
        codecOutputBuffers = codec.getOutputBuffers();

        // get the sample rate to configure AudioTrack
        int sampleRate = format.getInteger(MediaFormat.KEY_SAMPLE_RATE);

        Log.i(LOG_TAG,"mime "+mime);
        Log.i(LOG_TAG,"sampleRate "+sampleRate);

        // create our AudioTrack instance
        audioTrack = new AudioTrack(
                AudioManager.STREAM_MUSIC,
                sampleRate,
                //AudioFormat.CHANNEL_OUT_STEREO,
                AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                AudioTrack.getMinBufferSize (
                        sampleRate,
                        //AudioFormat.CHANNEL_OUT_STEREO,
                        AudioFormat.CHANNEL_OUT_MONO,
                        AudioFormat.ENCODING_PCM_16BIT
                ),
                AudioTrack.MODE_STREAM
        );

        // start playing, we will feed you later
        audioTrack.play();
        extractor.selectTrack(0);

        // start decoding
        final long kTimeOutUs = 10000;
        MediaCodec.BufferInfo info = new MediaCodec.BufferInfo();
        boolean sawInputEOS = false;
        boolean sawOutputEOS = false;

        while (!sawOutputEOS) {
            inputBufIndex = codec.dequeueInputBuffer(kTimeOutUs);
            if (inputBufIndex >= 0) {
                ByteBuffer dstBuf = codecInputBuffers[inputBufIndex];

                int sampleSize =
                        extractor.readSampleData(dstBuf, 0 /* offset */);

                long presentationTimeUs = 0;

                if (sampleSize < 0) {
                    Log.i(LOG_TAG, "saw input EOS.");
                    sawInputEOS = true;
                    sampleSize = 0;
                } else {
                    presentationTimeUs = extractor.getSampleTime();
                    //Log.i(LOG_TAG,"presentationTimeUs "+presentationTimeUs);
                }
                // can throw illegal state exception (???)

                codec.queueInputBuffer(
                        inputBufIndex,
                        0 /* offset */,
                        sampleSize,
                        presentationTimeUs,
                        sawInputEOS ? MediaCodec.BUFFER_FLAG_END_OF_STREAM : 0);

                if (!sawInputEOS) {
                    extractor.advance();
                }
            } else {
                //do nothing
            }

            // The decoded frame of audios
            int res = codec.dequeueOutputBuffer(info, kTimeOutUs);

            if (res >= 0) {
                int outputBufIndex = res;
                ByteBuffer buf = codecOutputBuffers[outputBufIndex];

                final byte[] chunk = new byte[info.size];
                buf.get(chunk);
                buf.clear();
                ByteOrder endian = buf.order();
                if(chunk.length > 0){
                    this.writeRaw(chunk, endian);
                    final byte[] difchunk = new byte[info.size/2];
                    for (int i = 0; i<chunk.length; i+=4) {
                        short leftData = (short) ((chunk[i+1] << 8) | (chunk[i] & 0xff));
                        short rightData = (short) ((chunk[i+3] << 8) | (chunk[i+2] & 0xff));
                        short difData = (short) (leftData - rightData);
                        short avgData = (short) ((leftData + rightData) / 2);
                        difchunk[i/2] = (byte) (avgData & 0xff);
                        difchunk[i/2+1] = (byte) ((avgData >> 8) & 0xff);
                        //difchunk[i/2] = chunk[i+2];
                        //difchunk[i/2+1] = chunk[i+3];
                    }
                    //audioTrack.write(chunk,0,chunk.length);
                    //audioTrack.write(difchunk,0,difchunk.length);
                }
                codec.releaseOutputBuffer(outputBufIndex, false /* render */);
                if ((info.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                    Log.i(LOG_TAG, "saw output EOS.");
                    Log.i(LOG_TAG, "numberofChannelData " + numberofChannelData);
                    this.musicFeatureExtraction(sampleRate);
                    sawOutputEOS = true;
                }
            } else if (res == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED) {
                codecOutputBuffers = codec.getOutputBuffers();

                Log.d(LOG_TAG, "output buffers have changed.");
            } else if (res == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                MediaFormat oformat = codec.getOutputFormat();

                Log.d(LOG_TAG, "output format has changed to " + oformat);
            } else {
                Log.d(LOG_TAG, "dequeueOutputBuffer returned " + res);
            }
            //break;
        }

        Log.i(LOG_TAG, "stopping...");

        relaxResources();

    }

    private void relaxResources() {
        if (extractor != null) {
            extractor.release();
            extractor = null;
        }
        if (codec != null) {
            codec.stop();
            codec.release();
            codec = null;
        }
        if (audioTrack != null) {
            audioTrack.flush();
            audioTrack.release();
            audioTrack = null;
        }
    }

    /**
     * AsyncTask that takes care of running the decode/playback loop
     *
     */
    private class DecodeOperation extends AsyncTask<Void, Void, Void> {

        @Override
        protected Void doInBackground(Void... values) {
            for (int i = 0; i < mUrlStringSet.length; i++) {
                Boolean hasExtracted = false;
                mUrlString = mUrlStringSet[i];
                Log.i(LOG_TAG, "songArray.0 " + songArray[0]);
                Log.i(LOG_TAG, "songArray.1 " + songArray[1]);
                Log.i(LOG_TAG, "mUrlString " + mUrlString);
                for (int j = 0; j < songArray.length; j++) {
                    if (songArray[j] != null) {
                        if (songArray[j].equals(mUrlString)) {
                            Log.i(LOG_TAG, "hasExtracted!");
                            hasExtracted = true;
                            break;
                        }
                    }
                }
                if (hasExtracted == false)
                    FeatureExtraction.this.decodeLoop();
            }

            return null;
        }

        @Override
        protected void onPreExecute() {
        }

        @Override
        protected void onProgressUpdate(Void... values) {
        }
    }
}
