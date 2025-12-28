package com.goldsprite.biowar.core;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.FrameBuffer;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.utils.Disposable;
import com.badlogic.gdx.utils.GdxRuntimeException;
import com.badlogic.gdx.math.Matrix4;

/**
 * 极简版辉光渲染器 (Neon Bloom)
 * 原理：
 * 1. 将场景绘制到低分辨率 FBO (1/4 尺寸)
 * 2. 使用 Shader 对 FBO 进行模糊处理
 * 3. 将模糊后的纹理(光晕)与原始场景叠加
 */
public class NeonGlowRenderer implements Disposable {

	// --- GLSL Shaders (内嵌以简化文件管理) ---

	// 标准顶点着色器
	private static final String VERTEX_SHADER = 
	"attribute vec4 " + ShaderProgram.POSITION_ATTRIBUTE + ";\n" +
	"attribute vec4 " + ShaderProgram.COLOR_ATTRIBUTE + ";\n" +
	"attribute vec2 " + ShaderProgram.TEXCOORD_ATTRIBUTE + "0;\n" +
	"uniform mat4 u_projTrans;\n" +
	"varying vec4 v_color;\n" +
	"varying vec2 v_texCoords;\n" +
	"void main() {\n" +
	"   v_color = " + ShaderProgram.COLOR_ATTRIBUTE + ";\n" +
	"   v_texCoords = " + ShaderProgram.TEXCOORD_ATTRIBUTE + "0;\n" +
	"   gl_Position =  u_projTrans * " + ShaderProgram.POSITION_ATTRIBUTE + ";\n" +
	"}";

	// 简单的 5-Tap 高斯模糊着色器 (配合降采样效果很好)
	private static final String BLUR_SHADER = 
	"#ifdef GL_ES\n" +
	"precision mediump float;\n" +
	"#endif\n" +
	"varying vec4 v_color;\n" +
	"varying vec2 v_texCoords;\n" +
	"uniform sampler2D u_texture;\n" +
	"uniform vec2 u_resolution;\n" + // FBO 尺寸
	"uniform float u_radius;\n" +     // 模糊半径
	"uniform float u_intensity;\n" +  // 发光强度

	"void main() {\n" +
	"    vec2 dir = vec2(1.0) / u_resolution * u_radius;\n" +
	"    vec4 sum = vec4(0.0);\n" +
	"    // 中心点\n" +
	"    sum += texture2D(u_texture, v_texCoords) * 0.227027;\n" +
	"    // 简单的十字采样 + 对角采样 (共5点近似，性能极高)\n" +
	"    sum += texture2D(u_texture, v_texCoords + dir * vec2(1.0, 0.0)) * 0.1945946;\n" +
	"    sum += texture2D(u_texture, v_texCoords + dir * vec2(-1.0, 0.0)) * 0.1945946;\n" +
	"    sum += texture2D(u_texture, v_texCoords + dir * vec2(0.0, 1.0)) * 0.1945946;\n" +
	"    sum += texture2D(u_texture, v_texCoords + dir * vec2(0.0, -1.0)) * 0.1945946;\n" +
	"    // 增强亮度\n" +
	"    gl_FragColor = v_color * sum * u_intensity;\n" +
	"}";

	private FrameBuffer fbo;
	private TextureRegion fboRegion;
	private ShaderProgram blurShader;

	// 配置
	private float fboScale = 0.25f; // 1/4 分辨率 (兼顾性能与模糊半径)
	private float blurRadius = 1.5f; // 模糊半径
	private float intensity = 1.2f;  // 发光强度

	private boolean isDrawing = false;
	private int screenW, screenH;

	public NeonGlowRenderer() {
		initShader();
	}

	private void initShader() {
		blurShader = new ShaderProgram(VERTEX_SHADER, BLUR_SHADER);
		if (!blurShader.isCompiled()) {
			throw new GdxRuntimeException("Shader compile error: " + blurShader.getLog());
		}
	}

	// 必须在 resize 中调用以重建 FBO
	public void resize(int width, int height) {
		if (width == 0 || height == 0) return;
		this.screenW = width;
		this.screenH = height;

		if (fbo != null) fbo.dispose();

		// 创建低分辨率 FBO
		int fboW = (int)(width * fboScale);
		int fboH = (int)(height * fboScale);
		try {
			fbo = new FrameBuffer(Pixmap.Format.RGBA8888, fboW, fboH, false);
			// 纹理需要线性过滤才能让低分图放大后不出现马赛克，而是平滑模糊
			fbo.getColorBufferTexture().setFilter(com.badlogic.gdx.graphics.Texture.TextureFilter.Linear, com.badlogic.gdx.graphics.Texture.TextureFilter.Linear);
			fboRegion = new TextureRegion(fbo.getColorBufferTexture());
			fboRegion.flip(false, true); // FBO 坐标系通常是反的
		} catch (Exception e) {
			Gdx.app.log("NeonGlow", "FBO Create failed (Minimized?)");
		}
	}

	/**
	 * 开始绘制发光对象
	 * 此方法后调用 draw 的内容都会发光
	 */
	public void begin() {
		if (fbo == null) return;
		isDrawing = true;

		// 1. 绑定 FBO
		fbo.begin();

		// 2. 清屏 (透明)
		Gdx.gl.glClearColor(0, 0, 0, 0);
		Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
	}

	/**
     * 结束绘制并上屏
     * @param batch 用于绘制最终结果的 SpriteBatch
     */
    public void end(SpriteBatch batch) {
        if (!isDrawing || fbo == null) return;
        fbo.end(); // Unbind FBO
        isDrawing = false;

        // --- 合成阶段 ---

        // 1. 保存当前 Batch 的状态 (混合模式、投影矩阵、是否正在绘制)
        int srcFunc = batch.getBlendSrcFunc();
        int dstFunc = batch.getBlendDstFunc();
        Matrix4 oldMatrix = batch.getProjectionMatrix().cpy(); // 备份矩阵
        boolean wasDrawing = batch.isDrawing();

        // 如果调用者还没结束 batch，我们帮他结束，以便切换矩阵
        if (wasDrawing) batch.end(); 

        // 2. 切换到屏幕坐标系 (Screen Space)
        // 因为 FBO 纹理是全屏的，我们需要用 1:1 的屏幕投影来绘制它
        batch.getProjectionMatrix().setToOrtho2D(0, 0, screenW, screenH);
        batch.begin();

        // 3. 绘制光晕 (Glow Layer) - Additive Mode
        batch.setShader(blurShader);
        batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE);

        blurShader.bind();
        blurShader.setUniformf("u_resolution", fbo.getWidth(), fbo.getHeight());
        blurShader.setUniformf("u_radius", blurRadius);
        blurShader.setUniformf("u_intensity", intensity);

        // 绘制 FBO 纹理
        batch.draw(fboRegion, 0, 0, screenW, screenH);

        batch.setShader(null); // 恢复默认 Shader

        // 4. 绘制本体 (Original Layer) - Normal Mode
        // 这一步将清晰的原始图像叠加在光晕之上
        batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        batch.draw(fboRegion, 0, 0, screenW, screenH);

        batch.end(); // 结束合成绘制

        // 5. 恢复 Batch 状态
        batch.setProjectionMatrix(oldMatrix); // 恢复原来的相机矩阵
        batch.setBlendFunction(srcFunc, dstFunc); // 恢复混合模式

        // 如果调用者之前是 begin 状态，我们恢复它，保持上下文连贯
        if (wasDrawing) batch.begin();
    }

	// 配置接口
	public void setIntensity(float v) { this.intensity = v; }
	public void setBlurRadius(float v) { this.blurRadius = v; }

	@Override
	public void dispose() {
		if (fbo != null) fbo.dispose();
		if (blurShader != null) blurShader.dispose();
	}
}
