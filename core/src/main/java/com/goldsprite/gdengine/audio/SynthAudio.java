package com.goldsprite.gdengine.audio;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.audio.AudioDevice;
import com.badlogic.gdx.math.MathUtils;

import java.util.Iterator;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * SynthAudio v2.0: 加入了 BGM 音序器
 */
public class SynthAudio {

	public enum WaveType { SINE, SQUARE, SAWTOOTH, TRIANGLE, NOISE }

	private static final int SAMPLE_RATE = 44100;
	private static final int BUFFER_SIZE = 2048;

	private static AudioDevice device;
	private static AudioThread audioThread;
	private static boolean isInitialized = false;

	// BGM 控制开关
	private static volatile boolean bgmEnabled = false;

	private static final Queue<Voice> activeVoices = new ConcurrentLinkedQueue<>();

	public static void init() {
		if (isInitialized) return;
		try {
			device = Gdx.audio.newAudioDevice(SAMPLE_RATE, true);
			audioThread = new AudioThread();
			audioThread.start();
			isInitialized = true;
		} catch (Exception e) {
			Gdx.app.error("SynthAudio", "Init failed", e);
		}
	}

	public static void dispose() {
		if (!isInitialized) return;
		audioThread.running = false;
		try { audioThread.join(500); } catch (InterruptedException ignored) {}
		device.dispose();
		isInitialized = false;
	}

	public static void playTone(float freq, WaveType type, float duration, float vol, float slideFreq) {
		if (!isInitialized) return;
		activeVoices.add(new Voice(freq, type, duration, vol, slideFreq));
	}

	public static void playTone(float freq, WaveType type, float duration, float vol) {
		playTone(freq, type, duration, vol, 0);
	}

	// --- BGM Control ---
	public static void playBGM() { bgmEnabled = true; }
	public static void stopBGM() { bgmEnabled = false; }

	// --- 内部类：发声体 (Voice) ---
	private static class Voice {
		float freqStart, freqEnd;
		WaveType type;
		float duration;
		float volume;
		float currentSample = 0;
		float phase = 0;

		public Voice(float freq, WaveType type, float durationSec, float vol, float slideFreq) {
			this.freqStart = freq;
			this.freqEnd = (slideFreq <= 0) ? freq : slideFreq;
			this.type = type;
			this.duration = durationSec * SAMPLE_RATE;
			this.volume = vol;
		}

		public boolean process(float[] mixBuffer, int len) {
			for (int i = 0; i < len; i++) {
				if (currentSample >= duration) return false;
				float t = currentSample / duration;
				float currFreq = freqStart + (freqEnd - freqStart) * t;
				// 简单的线性衰减包络 (ADSR 的 Decay)
				float currVol = volume * (1f - t);

				float phaseIncrement = (float) (Math.PI * 2 * currFreq / SAMPLE_RATE);
				phase += phaseIncrement;
				if (phase > Math.PI * 2) phase -= Math.PI * 2;

				float sampleValue = 0;
				switch (type) {
					case SINE: sampleValue = (float) Math.sin(phase); break;
					case SQUARE: sampleValue = (phase < Math.PI) ? 1f : -1f; break;
					case SAWTOOTH: sampleValue = (float) (phase / Math.PI - 1f); break;
					case TRIANGLE:
						float raw = (float) (phase / Math.PI - 1f);
						sampleValue = 2f * (0.5f - Math.abs(raw));
						break;
					case NOISE: sampleValue = MathUtils.random(-1f, 1f); break;
				}
				mixBuffer[i] += sampleValue * currVol;
				currentSample++;
			}
			return true;
		}
	}

	// --- 内部类：音频线程 (含音序器逻辑) ---
	private static class AudioThread extends Thread {
		volatile boolean running = true;
		float[] mixBuffer = new float[BUFFER_SIZE];
		short[] outBuffer = new short[BUFFER_SIZE];

		// BGM 状态机
		int bpm = 110; // 赛博朋克常用速度
		int samplesPerBeat = (SAMPLE_RATE * 60) / (bpm * 4); // 16分音符的采样数
		long sampleCounter = 0;
		int step = 0; // 当前走到第几步 (0-15)

		// C Minor Pentatonic Scale (C小调五声: C, Eb, F, G, Bb)
		// 频率: C2=65.41, Eb2=77.78, F2=87.31, G2=98.00, Bb2=116.54
		float[] bassScale = { 65.41f, 77.78f, 87.31f, 98.00f, 116.54f };
		// 高八度用于 Arp
		float[] arpScale = { 261.63f, 311.13f, 349.23f, 392.00f, 466.16f, 523.25f };

		@Override
		public void run() {
			while (running) {
				for (int i = 0; i < BUFFER_SIZE; i++) mixBuffer[i] = 0;

				// --- 1. BGM Sequencer Logic (产生新的 Voice) ---
				if (bgmEnabled) {
					// 这是一个简化的处理：每帧只检查一次 beat 触发，而不是精确到 sample
					// 对于 BUFFER_SIZE=2048 (46ms)，误差可接受。
					// 严谨做法是在 mixBuffer 循环内部检查，但那样太耗性能。

					sampleCounter += BUFFER_SIZE;
					if (sampleCounter >= samplesPerBeat) {
						sampleCounter -= samplesPerBeat;
						triggerBeat(step);
						step = (step + 1) % 16; // 16步循环 (1小节)
					}
				}

				// --- 2. Mixing Voices ---
				Iterator<Voice> it = activeVoices.iterator();
				boolean hasSound = false;
				while (it.hasNext()) {
					Voice v = it.next();
					boolean alive = v.process(mixBuffer, BUFFER_SIZE);
					if (!alive) it.remove();
					else hasSound = true;
				}

				// --- 3. Output ---
				if (hasSound) {
					for (int i = 0; i < BUFFER_SIZE; i++) {
						float val = mixBuffer[i];
						if (val > 1f) val = 1f; if (val < -1f) val = -1f;
						outBuffer[i] = (short) (val * 32767);
					}
					device.writeSamples(outBuffer, 0, BUFFER_SIZE);
				} else {
					try { Thread.sleep(10); } catch (InterruptedException e) {}
				}
			}
		}

		// 这里的逻辑决定了曲子的风格
		private void triggerBeat(int s) {
			// 1. Kick Drum (底鼓): 4/4 拍 (0, 4, 8, 12)
			if (s % 4 == 0) {
				// 频率快速下降的正弦波 = 鼓声
				activeVoices.add(new Voice(150, WaveType.SINE, 0.1f, 0.5f, 10));
			}

			// 2. Hi-Hat (踩镲): 反拍 (2, 6, 10, 14)
			if (s % 4 == 2) {
				// 极短的噪音
				activeVoices.add(new Voice(0, WaveType.NOISE, 0.05f, 0.1f, 0));
			}

			// 3. Bassline (贝斯): 16分音符律动
			// 模式: 根音轰炸 (Cyberpunk/EBM 风格)
			// 逻辑: 随机选低音，锯齿波
			if (s % 2 == 0) { // 8分音符
				float note = bassScale[MathUtils.random(bassScale.length - 1)];
				// 稍微 detune 一点点增加厚度
				activeVoices.add(new Voice(note, WaveType.SAWTOOTH, 0.15f, 0.2f, 0));
				activeVoices.add(new Voice(note + 2f, WaveType.SAWTOOTH, 0.15f, 0.2f, 0));
			}

			// 4. Arpeggio (琶音): 随机点缀的合成器
			if (MathUtils.randomBoolean(0.3f)) { // 30% 概率触发
				float note = arpScale[MathUtils.random(arpScale.length - 1)];
				// 简单的方波 beep
				activeVoices.add(new Voice(note, WaveType.SQUARE, 0.1f, 0.05f, 0));
			}
		}
	}
}
