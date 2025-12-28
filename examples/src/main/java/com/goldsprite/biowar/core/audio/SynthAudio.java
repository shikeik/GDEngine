package com.goldsprite.biowar.core.audio;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.audio.AudioDevice;
import com.badlogic.gdx.math.MathUtils;

import java.util.Iterator;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * 程序化音频合成器 (Web Audio API 复刻版)
 * 特性：
 * 1. 零素材，纯数学生成
 * 2. 多复音混音 (Polyphonic Mixing)
 * 3. 支持频率滑音 (Frequency Slide) 和音量包络 (Volume Decay)
 */
public class SynthAudio {

    public enum WaveType {
        SINE,       // 正弦波 (柔和)
        SQUARE,     // 方波 (8-bit 游戏感)
        SAWTOOTH,   // 锯齿波 (尖锐)
        TRIANGLE,   // 三角波 (清脆)
        NOISE       // 白噪 (爆炸/打击感)
		}

    private static final int SAMPLE_RATE = 44100;
    private static final int BUFFER_SIZE = 2048; // 约 46ms 延迟，平衡性能与实时性

    private static AudioDevice device;
    private static AudioThread audioThread;
    private static boolean isInitialized = false;

    // 线程安全的任务队列
    private static final Queue<Voice> activeVoices = new ConcurrentLinkedQueue<>();

    public static void init() {
        if (isInitialized) return;

        try {
            device = Gdx.audio.newAudioDevice(SAMPLE_RATE, true); // Mono is enough
            audioThread = new AudioThread();
            audioThread.start();
            isInitialized = true;
            Gdx.app.log("SynthAudio", "Synthesizer initialized @ " + SAMPLE_RATE + "Hz");
        } catch (Exception e) {
            Gdx.app.error("SynthAudio", "Init failed", e);
        }
    }

    public static void dispose() {
        if (!isInitialized) return;
        audioThread.running = false;
        try {
            audioThread.join(500);
        } catch (InterruptedException ignored) {}
        device.dispose();
        isInitialized = false;
    }

    /**
     * 播放程序化音效
     * @param freq 起始频率 (Hz)
     * @param type 波形类型
     * @param duration 持续时间 (秒)
     * @param vol 音量 (0.0 - 1.0)
     * @param slideFreq 结束频率 (若为0则频率不变，用于模拟 Jump 音效的升调)
     */
    public static void playTone(float freq, WaveType type, float duration, float vol, float slideFreq) {
        if (!isInitialized) return;
        activeVoices.add(new Voice(freq, type, duration, vol, slideFreq));
    }

    // 简化版重载
    public static void playTone(float freq, WaveType type, float duration, float vol) {
        playTone(freq, type, duration, vol, 0);
    }

    // --- 内部类：发声体 (Voice) ---
    private static class Voice {
        float freqStart, freqEnd;
        WaveType type;
        float duration; // 总时长 (samples)
        float volume;

        float currentSample = 0; // 当前播放进度
        float phase = 0; // 波形相位

        public Voice(float freq, WaveType type, float durationSec, float vol, float slideFreq) {
            this.freqStart = freq;
            this.freqEnd = (slideFreq <= 0) ? freq : slideFreq;
            this.type = type;
            this.duration = durationSec * SAMPLE_RATE;
            this.volume = vol;
        }

        /**
         * 生成一段音频数据并累加到 buffer 中
         * @return 如果播放结束返回 false
         */
        public boolean process(float[] mixBuffer, int len) {
            for (int i = 0; i < len; i++) {
                if (currentSample >= duration) return false;

                // 1. 计算当前时间进度 (0.0 -> 1.0)
                float t = currentSample / duration;

                // 2. 频率插值 (Slide)
                float currFreq = freqStart + (freqEnd - freqStart) * t;

                // 3. 音量包络 (Exponential Decay 模拟)
                // 模拟 H5: gain.exponentialRampToValueAtTime(0.01)
                // 简单实现：线性减弱 或者 平方衰减
                float currVol = volume * (1f - t) * (1f - t); 

                // 4. 波形生成
                float sampleValue = 0;

                // 相位步进: 2 * PI * freq / sampleRate
                float phaseIncrement = (float) (Math.PI * 2 * currFreq / SAMPLE_RATE);
                phase += phaseIncrement;
                if (phase > Math.PI * 2) phase -= Math.PI * 2;

                switch (type) {
                    case SINE:
                        sampleValue = (float) Math.sin(phase);
                        break;
                    case SQUARE:
                        sampleValue = (phase < Math.PI) ? 1f : -1f;
                        break;
                    case SAWTOOTH:
                        // 0~2PI -> -1~1
                        sampleValue = (float) (phase / Math.PI - 1f);
                        break;
                    case TRIANGLE:
                        // 0~2PI -> -1~1 (Folded)
                        float raw = (float) (phase / Math.PI - 1f);
                        sampleValue = 2f * (0.5f - Math.abs(raw)); // 简单三角近似
                        break;
                    case NOISE:
                        sampleValue = MathUtils.random(-1f, 1f);
                        break;
                }

                // 5. 混音累加
                mixBuffer[i] += sampleValue * currVol;

                currentSample++;
            }
            return true;
        }
    }

    // --- 内部类：音频线程 ---
    private static class AudioThread extends Thread {
        volatile boolean running = true;
        float[] mixBuffer = new float[BUFFER_SIZE];
        short[] outBuffer = new short[BUFFER_SIZE];

        @Override
        public void run() {
            while (running) {
                // 1. 清空混音缓冲区
                for (int i = 0; i < BUFFER_SIZE; i++) mixBuffer[i] = 0;

                // 2. 遍历所有 Voice 进行混音
                Iterator<Voice> it = activeVoices.iterator();
                boolean hasSound = false;

                while (it.hasNext()) {
                    Voice v = it.next();
                    boolean alive = v.process(mixBuffer, BUFFER_SIZE);
                    if (!alive) {
                        it.remove();
                    } else {
                        hasSound = true;
                    }
                }

                // 3. 如果有声音，输出到设备
                if (hasSound) {
                    for (int i = 0; i < BUFFER_SIZE; i++) {
                        // Hard Clipper (防止爆音)
                        float val = mixBuffer[i];
                        if (val > 1f) val = 1f;
                        if (val < -1f) val = -1f;

                        // Float to Short (16-bit)
                        outBuffer[i] = (short) (val * 32767);
                    }
                    device.writeSamples(outBuffer, 0, BUFFER_SIZE);
                } else {
                    // 没声音时短暂休眠，避免空转烧CPU
                    try { Thread.sleep(10); } catch (InterruptedException e) {}
                }
            }
        }
    }
}
