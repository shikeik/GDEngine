package com.goldsprite.solofight.screens.newyear;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.audio.AudioDevice;
import com.badlogic.gdx.math.MathUtils;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Iterator;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

public class NewYearSymphony {
    private static final int SAMPLE_RATE = 44100;
    private static final int BUFFER_SIZE = 1024;
    private static AudioDevice device;
    private static AudioThread audioThread;
    private static boolean isPlaying = false;

    // --- [新增] 录音相关 ---
    private static FileOutputStream wavOut;
    private static long totalPcmBytes = 0;

    public static void startRecording(String path) {
        try {
            wavOut = new FileOutputStream(path);
            wavOut.write(new byte[44]); // 预留 WAV 头
            totalPcmBytes = 0;
            Gdx.app.log("Symphony", "Recording started: " + path);
        } catch (Exception e) { e.printStackTrace(); }
    }

    public static void stopRecording() {
        if (wavOut == null) return;
        try {
            // 回填 WAV 头
            long totalDataLen = totalPcmBytes;
            long bitrate = SAMPLE_RATE * 16 * 1 / 8; 
            long totalDataLen36 = totalDataLen + 36;
            
            byte[] header = new byte[44];
            header[0] = 'R'; header[1] = 'I'; header[2] = 'F'; header[3] = 'F';
            header[4] = (byte) (totalDataLen36 & 0xff);
            header[5] = (byte) ((totalDataLen36 >> 8) & 0xff);
            header[6] = (byte) ((totalDataLen36 >> 16) & 0xff);
            header[7] = (byte) ((totalDataLen36 >> 24) & 0xff);
            header[8] = 'W'; header[9] = 'A'; header[10] = 'V'; header[11] = 'E';
            header[12] = 'f'; header[13] = 'm'; header[14] = 't'; header[15] = ' ';
            header[16] = 16; header[17] = 0; header[18] = 0; header[19] = 0;
            header[20] = 1; header[21] = 0; header[22] = 1; header[23] = 0;
            header[24] = (byte) (SAMPLE_RATE & 0xff);
            header[25] = (byte) ((SAMPLE_RATE >> 8) & 0xff);
            header[26] = (byte) ((SAMPLE_RATE >> 16) & 0xff);
            header[27] = (byte) ((SAMPLE_RATE >> 24) & 0xff);
            header[28] = (byte) (bitrate & 0xff);
            header[29] = (byte) ((bitrate >> 8) & 0xff);
            header[30] = (byte) ((bitrate >> 16) & 0xff);
            header[31] = (byte) ((bitrate >> 24) & 0xff);
            header[32] = 2; header[33] = 0; header[34] = 16; header[35] = 0;
            header[36] = 'd'; header[37] = 'a'; header[38] = 't'; header[39] = 'a';
            header[40] = (byte) (totalDataLen & 0xff);
            header[41] = (byte) ((totalDataLen >> 8) & 0xff);
            header[42] = (byte) ((totalDataLen >> 16) & 0xff);
            header[43] = (byte) ((totalDataLen >> 24) & 0xff);

            wavOut.getChannel().position(0);
            wavOut.write(header);
            wavOut.close();
            wavOut = null;
            Gdx.app.log("Symphony", "WAV Saved!");
        } catch (Exception e) { e.printStackTrace(); }
    }
    // ----------------------

    // SFX 接口 (保持不变)
    public static void playLaunch() {
        if(audioThread!=null) audioThread.sfxQueue.add(new Voice(400, 0.3f, 0.5f, 3).setSlide(800));
    }
    public static void playExplosion() {
        if(audioThread!=null) {
            audioThread.sfxQueue.add(new Voice(0, 0.6f, 0.4f, 2).setADSR(0.01f, 0.3f));
            audioThread.sfxQueue.add(new Voice(50, 0.8f, 0.5f, 3).setSlide(20));
        }
    }

    public static void start() {
        if (isPlaying) return;
        try {
            device = Gdx.audio.newAudioDevice(SAMPLE_RATE, false);
            audioThread = new AudioThread();
            audioThread.start();
            isPlaying = true;
        } catch (Exception e) { e.printStackTrace(); }
    }

    public static void stop() {
        if (!isPlaying) return;
        audioThread.running = false;
        try { audioThread.join(200); } catch (Exception ignored) {}
        device.dispose();
        isPlaying = false;
    }

    private static class Voice {
        float freq, vol, duration, time = 0;
        float attack = 0.01f, release = 0.2f;
        float slideTo = -1; 
        int type; 

        public Voice(float freq, float vol, float dur, int type) {
            this.freq = freq; this.vol = vol; this.duration = dur; this.type = type;
        }
        public Voice setSlide(float to) { this.slideTo = to; return this; }
        public Voice setADSR(float a, float r) { this.attack=a; this.release=r; return this; }
    }

    private static class AudioThread extends Thread {
        volatile boolean running = true;
        float[] buffer = new float[BUFFER_SIZE];
        short[] out = new short[BUFFER_SIZE];
        float[] delay = new float[SAMPLE_RATE]; 
        int delayPtr = 0;
        
        Queue<Voice> musicVoices = new ConcurrentLinkedQueue<>();
        Queue<Voice> sfxQueue = new ConcurrentLinkedQueue<>();
        
        long tick = 0;
        int step = 0;
        float[] bassLine = {110.00f, 87.31f, 130.81f, 98.00f}; 
        float[] leadScale = {220f, 261f, 293f, 329f, 392f}; 

        @Override
        public void run() {
            while (running) {
                // ... (Sequencer 逻辑保持不变) ...
                int samplesPer16th = (SAMPLE_RATE * 60) / (110 * 4);
                if (tick % samplesPer16th < BUFFER_SIZE) {
                    int bar = (step / 16);
                    int beat = step % 16;
                    int chordIdx = (bar % 4);
                    float intensity = Math.min(1f, bar / 8f);

                    if (beat % 4 == 0) musicVoices.add(new Voice(100, 0.9f, 0.1f, 3).setSlide(30));
                    if (beat % 4 == 2) {
                        float freq = bassLine[chordIdx] / 2f;
                        musicVoices.add(new Voice(freq, 0.6f * intensity, 0.2f, 0));
                        musicVoices.add(new Voice(freq * 1.01f, 0.4f * intensity, 0.2f, 0)); 
                    }
                    if (intensity > 0.5f) {
                        float note = leadScale[(step * 3) % leadScale.length] * (beat%2==0?1:2);
                        musicVoices.add(new Voice(note, 0.15f, 0.1f, 1));
                    }
                    if (beat % 8 == 4 && intensity > 0.2f) {
                        musicVoices.add(new Voice(0, 0.5f, 0.15f, 2).setADSR(0.01f, 0.1f));
                    }
                    step++;
                }
                tick += BUFFER_SIZE;

                // --- Mixing ---
                for (int i = 0; i < BUFFER_SIZE; i++) buffer[i] = 0;
                mixQueue(musicVoices);
                mixQueue(sfxQueue);

                // --- Mastering ---
                for (int i = 0; i < BUFFER_SIZE; i++) {
                    float dVal = delay[(delayPtr - 12000 + delay.length) % delay.length];
                    float mixed = buffer[i] + dVal * 0.4f;
                    delay[delayPtr] = mixed;
                    delayPtr = (delayPtr + 1) % delay.length;
                    out[i] = (short)(MathUtils.clamp(mixed, -1f, 1f) * 30000);
                }
                
                device.writeSamples(out, 0, BUFFER_SIZE);

                // --- [新增] 写入录音文件 ---
                if (wavOut != null) {
                    try {
                        byte[] bytes = new byte[BUFFER_SIZE * 2];
                        for (int i = 0; i < BUFFER_SIZE; i++) {
                            short val = out[i];
                            bytes[i*2] = (byte)(val & 0xff);
                            bytes[i*2+1] = (byte)((val >> 8) & 0xff);
                        }
                        wavOut.write(bytes);
                        totalPcmBytes += bytes.length;
                    } catch (IOException e) { e.printStackTrace(); }
                }
            }
        }

        private void mixQueue(Queue<Voice> queue) {
            Iterator<Voice> it = queue.iterator();
            float dt = 1f/SAMPLE_RATE;
            while (it.hasNext()) {
                Voice v = it.next();
                for (int i = 0; i < BUFFER_SIZE; i++) {
                    if (v.time > v.duration + v.release) break;
                    float env = 1f;
                    if (v.time < v.attack) env = v.time/v.attack;
                    else if (v.time > v.duration) env = 1f - (v.time-v.duration)/v.release;
                    if (env < 0) env = 0;

                    float currentFreq = v.freq;
                    if (v.slideTo > 0) currentFreq = MathUtils.lerp(v.freq, v.slideTo, Math.min(1, v.time/v.duration));

                    float wave = 0;
                    float ph = (float)(v.time * currentFreq * Math.PI * 2);
                    if (v.type==0) wave = (float)(ph % (Math.PI*2) / Math.PI - 1); 
                    else if (v.type==1) wave = (Math.sin(ph)>0?1:-1); 
                    else if (v.type==2) wave = MathUtils.random(-1f,1f); 
                    else wave = (float)Math.sin(ph); 

                    buffer[i] += wave * v.vol * env * 0.5f;
                    v.time += dt;
                }
                if (v.time > v.duration + v.release) it.remove();
            }
        }
    }
}