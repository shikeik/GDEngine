package com.goldsprite.solofight.core;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.FrameBuffer;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.math.Matrix4;

public class BloomRenderer {
	// 配置参数
	public float blurScale = 0.5f; // 降采样比例 (建议0.5或0.25，越小性能越好光晕越大)
	public int iterations = 3;     // 模糊迭代次数 (2-3次足够，太多费性能)
	public float intensity = 1.1f; // 发光强度
	public float saturation = 1.2f;// 饱和度增强 (防止变白)
	public float baseRadius = 1.0f;// 基础扩散
	public float bloomSpreadMul = 1.0f; // 扩散递增系数

	// --- Shaders ---
	private static final String VERT =
	"attribute vec4 a_position; attribute vec4 a_color; attribute vec2 a_texCoord0; " +
	"uniform mat4 u_projTrans; varying vec4 v_color; varying vec2 v_texCoords; " +
	"void main() { v_color = a_color; v_texCoords = a_texCoord0; gl_Position = u_projTrans * a_position; }";

	// 高斯模糊 (保持原样)
	private static final String BLUR_FRAG =
	"#ifdef GL_ES\nprecision mediump float;\n#endif\n" +
	"varying vec4 v_color; varying vec2 v_texCoords; uniform sampler2D u_texture; uniform vec2 u_dir; " +
	"void main() { " +
	"  float weight[3]; weight[0] = 0.227027; weight[1] = 0.316216; weight[2] = 0.070270; " +
	"  vec2 offset1 = vec2(1.3846153846) * u_dir; vec2 offset2 = vec2(3.2307692308) * u_dir; " +
	"  vec4 sum = texture2D(u_texture, v_texCoords) * weight[0]; " +
	"  sum += texture2D(u_texture, v_texCoords + offset1) * weight[1]; " +
	"  sum += texture2D(u_texture, v_texCoords - offset1) * weight[1]; " +
	"  sum += texture2D(u_texture, v_texCoords + offset2) * weight[2]; " +
	"  sum += texture2D(u_texture, v_texCoords - offset2) * weight[2]; " +
	"  gl_FragColor = sum * v_color; " +
	"}";

	// [修复核心] 合成 Shader
	private static final String COMBINE_FRAG =
	"#ifdef GL_ES\nprecision mediump float;\n#endif\n" +
	"varying vec2 v_texCoords; " +
	"uniform sampler2D u_texture; " +  // Texture 0: Original (Base)
	"uniform sampler2D u_texture1; " + // Texture 1: Bloom (Blur)
	"uniform float u_intensity; " +
	"uniform float u_saturation; " +

	"vec3 adjustSaturation(vec3 color, float saturation) { " +
	"  float grey = dot(color, vec3(0.299, 0.587, 0.114)); " +
	"  return mix(vec3(grey), color, saturation); " +
	"} " +

	"void main() { " +
	"  vec4 base = texture2D(u_texture, v_texCoords); " +
	"  vec4 bloom = texture2D(u_texture1, v_texCoords); " +

	"  // 1. 增强光晕强度 & 饱和度 " +
	"  vec3 bloomColor = bloom.rgb * u_intensity; " +
	"  bloomColor = adjustSaturation(bloomColor, u_saturation); " +

	"  // 2. 混合算法: Screen Blending (滤色) " +
	"  // 公式: 1 - (1 - Base) * (1 - Bloom) " +
	"  // 特性: 永远不会超过 1.0，保留色彩，比 Additive 柔和 " +
	"  vec3 screenResult = 1.0 - (1.0 - base.rgb) * (1.0 - bloomColor); " +

	"  // 3. [关键修复] 透明度合成 " +
	"  // 最终 Alpha = 原始 Alpha + 光晕 Alpha (取最大值或相加) " +
	"  // 如果不加这一步，物体周围的光晕会被透明背景吃掉 " +
	"  float finalAlpha = max(base.a, bloom.a); " + // 或者 min(1.0, base.a + bloom.a * 1.5)

	"  gl_FragColor = vec4(screenResult, finalAlpha); " +
	"}";

	private FrameBuffer mainFBO, pingFBO, pongFBO;
	private ShaderProgram blurShader;
	private ShaderProgram combineShader;

	// 缓存矩阵避免 GC
	private Matrix4 fboMatrix = new Matrix4();
	private Matrix4 screenMatrix = new Matrix4();

	private int screenWidth, screenHeight;
	private SpriteBatch batch; // 内部 Batch，避免污染全局状态

	public BloomRenderer() {
		batch = new SpriteBatch(); // 专用 Batch 处理 FBO 内部逻辑
		ShaderProgram.pedantic = false;
		blurShader = new ShaderProgram(VERT, BLUR_FRAG);
		combineShader = new ShaderProgram(VERT, COMBINE_FRAG);

		if (!combineShader.isCompiled()) {
			Gdx.app.error("Bloom", "Combine Shader Error: " + combineShader.getLog());
		}
	}

	public void resize(int width, int height) {
		try {
			this.screenWidth = width;
			this.screenHeight = height;
			if (mainFBO != null) { mainFBO.dispose(); pingFBO.dispose(); pongFBO.dispose(); }

			mainFBO = new FrameBuffer(Pixmap.Format.RGBA8888, width, height, false);

			int smallW = (int)(width * blurScale); // 使用乘法更直观 (0.5 = 半分辨率)
			int smallH = (int)(height * blurScale);

			pingFBO = new FrameBuffer(Pixmap.Format.RGBA8888, smallW, smallH, false);
			pongFBO = new FrameBuffer(Pixmap.Format.RGBA8888, smallW, smallH, false);

			Texture.TextureFilter filter = Texture.TextureFilter.Linear;
			Texture.TextureWrap wrap = Texture.TextureWrap.ClampToEdge;

			for (FrameBuffer fbo : new FrameBuffer[]{mainFBO, pingFBO, pongFBO}) {
				fbo.getColorBufferTexture().setFilter(filter, filter);
				fbo.getColorBufferTexture().setWrap(wrap, wrap);
			}

			fboMatrix.setToOrtho2D(0, 0, smallW, smallH);
			// 屏幕矩阵：左下角为原点
			screenMatrix.setToOrtho2D(0, 0, width, height);

		} catch(Exception ignored) {}
	}

	/**
	 * 开始捕获发光物体
	 * @param gameBatch 游戏主循环使用的 Batch (通常带有 Camera 矩阵)
	 */
	public void captureStart(SpriteBatch gameBatch) {
		if(mainFBO == null) return;
		mainFBO.begin();

		// 1. 清空 FBO 为完全透明 (这是实现透明背景的前提)
		Gdx.gl.glClearColor(0, 0, 0, 0);
		Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

		// 2. 设置混合模式：标准 Alpha 混合
		// 确保绘制的物体保留其 Alpha 通道
		gameBatch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
	}

	public void captureEnd() {
		if(mainFBO == null) return;
		mainFBO.end();
	}

	/**
	 * 处理模糊 (Ping-Pong Blur)
	 */
	public void process() {
		if (mainFBO == null) return;

		// 1. 提取 (Downsample) -> Ping
		pingFBO.begin();
		Gdx.gl.glClearColor(0, 0, 0, 0); // [修正] 必须清除为透明，否则背景是黑色的
		Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

		batch.setProjectionMatrix(fboMatrix);
		batch.setShader(null);
		batch.disableBlending(); // [修正] 关闭混合，直接拷贝像素，防止透明度叠加错误
		batch.begin();
		batch.draw(mainFBO.getColorBufferTexture(), 0, 0, pingFBO.getWidth(), pingFBO.getHeight(), 0, 0, 1, 1);
		batch.end();
		pingFBO.end();

		// 2. 模糊 (Blur) -> Ping/Pong Loop
		batch.setShader(blurShader);
		// [修正] 开启混合，因为模糊本质上是像素扩散
		batch.enableBlending();
		batch.setBlendFunction(GL20.GL_ONE, GL20.GL_ONE_MINUS_SRC_ALPHA); // Premultiplied safe blend

		for (int i = 0; i < iterations; i++) {
			float radius = baseRadius + i * bloomSpreadMul;

			// Ping -> Pong (Horizontal)
			pongFBO.begin();
			Gdx.gl.glClearColor(0,0,0,0); Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
			batch.begin();
			blurShader.setUniformf("u_dir", radius / pongFBO.getWidth(), 0f);
			batch.draw(pingFBO.getColorBufferTexture(), 0, 0, pongFBO.getWidth(), pongFBO.getHeight(), 0, 0, 1, 1);
			batch.end();
			pongFBO.end();

			// Pong -> Ping (Vertical)
			pingFBO.begin();
			Gdx.gl.glClearColor(0,0,0,0); Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
			batch.begin();
			blurShader.setUniformf("u_dir", 0f, radius / pingFBO.getHeight());
			batch.draw(pongFBO.getColorBufferTexture(), 0, 0, pingFBO.getWidth(), pingFBO.getHeight(), 0, 0, 1, 1);
			batch.end();
			pingFBO.end();
		}
		batch.setShader(null);
	}

	/**
	 * 旧的渲染方法（保留兼容性），会自动上屏覆盖
	 */
	Color tmpColor = new Color();
	public void renderOld(SpriteBatch batch) {
		// 手动合成绘制 (核心步骤)
		// [修复] 使用屏幕空间矩阵 (Screen Space)，而不是世界空间矩阵
		// FBO 里的纹理已经是经过相机变换后的“照片”，不能再被相机变换一次
		batch.setProjectionMatrix(screenMatrix);
		batch.begin();

		TextureRegion raw = getOriginalRegion();
		TextureRegion glow = getBloomRegion();

		if (raw != null && glow != null) {

			batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);

			batch.setBlendFunction(GL20.GL_ONE, GL20.GL_ONE);
			batch.setColor(tmpColor.set(Color.WHITE));

			// [修复] 绘制位置固定为 (0,0)，宽高使用屏幕物理分辨率
			// 因为 screenMatrix 是 setToOrtho2D(0, 0, width, height)
			for(int i=0; i<1; i++)
				batch.draw(glow, 0, 0, screenWidth, screenHeight);
			batch.draw(raw, 0, 0, screenWidth, screenHeight);

			batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
		}

		batch.end();
	}

	/**
	 * 将合成结果绘制到屏幕 (或当前绑定的 FBO)
	 * 这一步会把 发光物体+光晕 合并后画出来
	 */
	public void render(SpriteBatch batch) {
		if (mainFBO == null || pingFBO == null) return;

		SpriteBatch outBatch = batch;

		// [核心修复 1] 强制重置 Batch 颜色为纯白
		// 避免之前绘制操作(如 NeonBatch)留下的颜色影响 FBO 的显示
		Color oldColor = outBatch.getColor();
		outBatch.setColor(Color.WHITE);

		// [核心修复 2] 强制重置混合模式
		int srcFunc = outBatch.getBlendSrcFunc();
		int dstFunc = outBatch.getBlendDstFunc();
		outBatch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);

		// 1. 设置投影矩阵 (屏幕坐标系)
		Matrix4 oldMatrix = outBatch.getProjectionMatrix().cpy();
		outBatch.setProjectionMatrix(screenMatrix);

		// 2. 绑定合成 Shader
		outBatch.setShader(combineShader);

		// 3. 绑定纹理
		// 注意：FBO 纹理坐标在 LibGDX 中通常是翻转的 (Y向下)
		// 我们需要手动翻转 UV，或者在 Shader 里翻转，或者用 TextureRegion
		Texture texMain = mainFBO.getColorBufferTexture();
		Texture texBloom = pingFBO.getColorBufferTexture();

		// 绑定 Blur 图到 Unit 1
		texBloom.bind(1);
		// 激活 Unit 0 供 Batch 使用
		Gdx.gl.glActiveTexture(GL20.GL_TEXTURE0);

		outBatch.begin();

		// 传递 Uniforms
		combineShader.setUniformi("u_texture1", 1);
		combineShader.setUniformf("u_intensity", intensity);
		combineShader.setUniformf("u_saturation", saturation);

		// [核心修复 3] 绘制时翻转 Y 轴 (v = 1, v2 = 0)
		// 解决 FBO 绘制倒置的问题
		outBatch.draw(texMain, 0, 0, screenWidth, screenHeight, 0, 0, 1, 1);

		outBatch.end();

		// --- 状态恢复 ---
		outBatch.setShader(null);
		outBatch.setProjectionMatrix(oldMatrix);
		outBatch.setColor(oldColor);
		outBatch.setBlendFunction(srcFunc, dstFunc);
	}

	// --- 辅助接口 ---

	public TextureRegion getOriginalRegion() {
		if (mainFBO == null) return null;
		TextureRegion tr = new TextureRegion(mainFBO.getColorBufferTexture());
		tr.flip(false, true);
		return tr;
	}

	public TextureRegion getBloomRegion() {
		if (pingFBO == null) return null;
		TextureRegion tr = new TextureRegion(pingFBO.getColorBufferTexture());
		tr.flip(false, true);
		return tr;
	}

	public void dispose() {
		if(mainFBO!=null)mainFBO.dispose();
		if(pingFBO!=null)pingFBO.dispose();
		if(pongFBO!=null)pongFBO.dispose();
		if(blurShader!=null)blurShader.dispose();
		if(combineShader!=null)combineShader.dispose();
		batch.dispose();
	}
}
