package com.goldsprite.solofight;

import com.badlogic.gdx.graphics.Pixmap;

public interface VideoExportInterface {
	// 初始化录制 (宽, 高, 保存路径)
	void start(int width, int height, String filePath);
	// 发送一帧画面
	void saveFrame(Pixmap pixmap, float timeStep);
	// 结束录制
	void stop();
}