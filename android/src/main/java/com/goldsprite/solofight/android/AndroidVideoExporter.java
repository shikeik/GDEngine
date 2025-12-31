package com.goldsprite.solofight.android;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.media.MediaMuxer;
import android.util.Log;
import android.view.Surface;
import com.badlogic.gdx.graphics.Pixmap;
import com.goldsprite.solofight.VideoExportInterface;
import java.nio.ByteBuffer;

public class AndroidVideoExporter implements VideoExportInterface {
    private MediaCodec encoder;
    private Surface inputSurface;
    private MediaMuxer muxer;
    private int trackIndex;
    private boolean muxerStarted;
    private MediaCodec.BufferInfo bufferInfo;
    
    // 状态
    private boolean isRecording = false;
    private long totalTimeUs = 0; // 模拟的时间戳

    private static final String TAG = "VIDEO_EXP"; // 日志标签
    
    @Override
    public void start(int width, int height, String path) {
        try {
            Log.d(TAG, "Starting Recorder: " + width + "x" + height);
            bufferInfo = new MediaCodec.BufferInfo();
            
            // 1. 配置 H.264 编码器 (1080P, 30FPS, 10Mbps)
            // 增加更详细的配置日志
            MediaFormat format = MediaFormat.createVideoFormat(MediaFormat.MIMETYPE_VIDEO_AVC, width, height);
            format.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface);
            format.setInteger(MediaFormat.KEY_BIT_RATE, 6000000); // 【降级】6Mbps 足够720P了，太高容易崩
            format.setInteger(MediaFormat.KEY_FRAME_RATE, 30);    // 【匹配】30FPS
            format.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1);
            
            Log.d(TAG, "Configuring Encoder...");
            encoder = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_VIDEO_AVC);
            encoder.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
            
            inputSurface = encoder.createInputSurface();
            encoder.start();
            Log.d(TAG, "Encoder Started");

            muxer = new MediaMuxer(path, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);
            
            isRecording = true;
            totalTimeUs = 0;
            Log.d(TAG, "Recorder Ready!");
        } catch (Exception e) {
            Log.e(TAG, "Start Failed!", e); // 【关键】打印启动失败原因
            e.printStackTrace();
        }
    }

    @Override
    public void saveFrame(Pixmap pixmap, float timeStep) {
        if (!isRecording) return;
        try {
            int w = pixmap.getWidth();
            int h = pixmap.getHeight();
            Bitmap bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
            
            ByteBuffer dst = ByteBuffer.allocate(w * h * 4);
            dst.put(pixmap.getPixels());
            dst.rewind();
            bitmap.copyPixelsFromBuffer(dst);

            Canvas canvas = inputSurface.lockCanvas(new Rect(0, 0, w, h));
            canvas.drawBitmap(bitmap, 0, 0, null);
            inputSurface.unlockCanvasAndPost(canvas);
            
            bitmap.recycle(); // 必须立刻回收！

            drainEncoder(false);
            totalTimeUs += (long)(timeStep * 1000000);
        } catch (Exception e) {
            Log.e(TAG, "Frame Save Failed!", e);
        }
    }

    @Override
    public void stop() {
        if (!isRecording) return;
        Log.e(TAG, "Stopping Recorder...");
        isRecording = false;
        try {
            drainEncoder(true); // 写入最后几帧
        } catch (Exception e) {
            Log.e(TAG, "Drain Error (Ignored)", e);
        }
        
        try {
            if (encoder != null) { encoder.stop(); encoder.release(); }
        } catch (Exception e) { Log.e(TAG, "Encoder Release Error", e); }
        
        try {
            if (muxer != null) { 
                muxer.stop(); // 【关键】这一步真正生成 MP4 文件
                muxer.release(); 
            }
        } catch (Exception e) { 
            Log.e(TAG, "Muxer Release Error (Video might be corrupt)", e); 
        }
        
        try {
            if (inputSurface != null) inputSurface.release();
        } catch (Exception e) {}

        Log.e(TAG, "Video Release Sequence Finished.");
    }

    private void drainEncoder(boolean endOfStream) {
        if (endOfStream) encoder.signalEndOfInputStream();

        while (true) {
            int encoderStatus = encoder.dequeueOutputBuffer(bufferInfo, 1000); // 1ms wait
            if (encoderStatus == MediaCodec.INFO_TRY_AGAIN_LATER) {
                if (!endOfStream) break; // 没数据了，继续下一帧
            } else if (encoderStatus == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                if (muxerStarted) throw new RuntimeException("Format changed twice");
                MediaFormat newFormat = encoder.getOutputFormat();
                trackIndex = muxer.addTrack(newFormat);
                muxer.start();
                muxerStarted = true;
            } else if (encoderStatus >= 0) {
                ByteBuffer encodedData = encoder.getOutputBuffer(encoderStatus);
                if ((bufferInfo.flags & MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0) bufferInfo.size = 0;

                if (bufferInfo.size != 0) {
                    if (!muxerStarted) throw new RuntimeException("Muxer not started");
                    encodedData.position(bufferInfo.offset);
                    encodedData.limit(bufferInfo.offset + bufferInfo.size);
                    
                    // 强制修改时间戳，保证视频是匀速 30FPS
                    bufferInfo.presentationTimeUs = totalTimeUs;
                    
                    muxer.writeSampleData(trackIndex, encodedData, bufferInfo);
                }
                encoder.releaseOutputBuffer(encoderStatus, false);
                if ((bufferInfo.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) break;
            }
        }
    }
}
