package com.goldsprite.biowar.core;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.utils.viewport.ExtendViewport;
import com.badlogic.gdx.utils.viewport.Viewport;
import com.goldsprite.solofight.BuildConfig;
import com.goldsprite.gameframeworks.assets.FontUtils;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class DebugUI {
	private static DebugUI instance;
	private List<String> logs = new ArrayList<>();
	// [新增] 键值对监控信息
	private List<String> debugInfos = new ArrayList<>();
	private SpriteBatch uiBatch;
	private BitmapFont font;
	private OrthographicCamera camera;
	private ExtendViewport viewport;
	
	public static boolean showDebugUI = true;

	// 新增：背景纹理
	private Texture bgTexture;

	// --- 布局配置 ---
	public float padding = 15f; // 背景内边距
	public float marginHori = 40f;
	public float marginBottom = 40f;
	public float marginTop = 20f;

	// 背景颜色 (黑色半透明)
	private final Color bgColor = new Color(0, 0, 0, 0.1f);

	// 定义基准分辨率 (手机常用比例)
	float screenScl = 1.5f;
	float shortSide = 1080f*screenScl;
	float longSide = 1920f*screenScl;
	
	public DebugUI() {
		instance = this;
		camera = new OrthographicCamera();
		viewport = new ExtendViewport(shortSide, longSide, camera);
		resize(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
		uiBatch = new SpriteBatch();

		// 1. 生成 1x1 纯白纹理 (用于拉伸做背景)
		Pixmap p = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
		p.setColor(Color.WHITE);
		p.fill();
		bgTexture = new Texture(p);
		p.dispose();

		try {
			font = FontUtils.generate(30);
			font.getData().scale(0.1f);
		} catch (Exception e) {
			font = new BitmapFont();
		}
	}

	public static DebugUI getInstance() {
		if (instance == null) new DebugUI();
		return instance;
	}

	// [新增] 供外部 UI 获取日志数据的接口
	public static List<String> getLogs() {
		return getInstance().logs;
	}

	public static String getLastLog() {
		List<String> list = getInstance().logs;
		if (list.isEmpty()) return "";
		return list.get(list.size() - 1);
	}

	// [修改] 使用 String.format + 异常捕获
	public static void log(Object... value) {
		if (value == null || value.length == 0) return;

		String msg;
		// 只有1个参数时，直接转字符串，无需格式化
		if (value.length == 1) {
			msg = String.valueOf(value[0]);
		} else {
			try {
				// 第一个参数作为 Format String，后续作为 Args
				String format = String.valueOf(value[0]);
				Object[] args = Arrays.copyOfRange(value, 1, value.length);
				msg = String.format(format, args);
			} catch (Exception e) {
				// [防崩溃] 格式化失败时降级显示，保留原始信息
				msg = String.valueOf(value[0]) + " <FmtErr> " + Arrays.toString(Arrays.copyOfRange(value, 1, value.length));
			}
		}

		getInstance().logs.add(msg);
		if (getInstance().logs.size() > 50) getInstance().logs.remove(0);
		Gdx.app.log("DEBUG", msg);
	}

	// [修改] 使用 String.format + 异常捕获
	public static void info(Object... value) {
		if (value == null || value.length == 0) return;

		String finalStr;
		if (value.length == 1) {
			finalStr = String.valueOf(value[0]);
		} else {
			try {
				String format = String.valueOf(value[0]);
				Object[] args = Arrays.copyOfRange(value, 1, value.length);
				finalStr = String.format(format, args);
			} catch (Exception e) {
				finalStr = String.valueOf(value[0]) + " <FmtErr> " + Arrays.toString(Arrays.copyOfRange(value, 1, value.length));
			}
		}

		getInstance().debugInfos.add(finalStr);
	}

	// 复用 Layout 避免 GC
	GlyphLayout infoLayout = new GlyphLayout();
	GlyphLayout logLayout = new GlyphLayout();

	public void render() {
		if (uiBatch == null) return;

		viewport.apply(true);

		// --- 1. 准备文本内容 ---
		// 构建 Info 字符串
		StringBuilder infoBuilder = new StringBuilder();
		infoBuilder.append(BuildConfig.PROJECT_NAME).append(": V").append(BuildConfig.DEV_VERSION).append("\n");
		infoBuilder.append("FPS: ").append(Gdx.graphics.getFramesPerSecond()).append("\n");
		infoBuilder.append("Heap: ").append(Gdx.app.getJavaHeap() / 1024 / 1024).append("MB\n");
		// 追加自定义监控信息
		infoBuilder.append(String.join("\n", debugInfos));
		debugInfos.clear();

		String logsStr = "> " + String.join("\n> ", logs);
		String infoStr = infoBuilder.toString();
		
		if(!showDebugUI) return;
		
		// --- 2. 计算尺寸 (Pack) ---
		infoLayout.setText(font, infoStr);
		logLayout.setText(font, logsStr);

		// --- 3. 绘制 Info 背景 (右上角) ---
		float topY = viewport.getWorldHeight() - marginTop;
		float rightX = viewport.getWorldWidth() - marginHori;

		uiBatch.setColor(bgColor);
		uiBatch.setProjectionMatrix(camera.combined);
		uiBatch.begin();
		
		// 矩形位置：X = marginLeft - padding
		// 矩形Y：因为文字是从 topY 向下画的，所以矩形底边是 topY - height - padding
		uiBatch.draw(bgTexture,
			rightX - infoLayout.width - padding,
			topY - infoLayout.height - padding,
			infoLayout.width + padding * 2,
			infoLayout.height + padding * 2
		);

		// --- 4. 绘制 Log 背景 (左下角) ---
		// 你的 Log 逻辑是：logY 是文字的最顶端，marginBottom 是最底端 (logY = margin + height)
		// 所以矩形的底边就是 marginBottom
		float logY = marginBottom + logLayout.height;

		if (!logs.isEmpty()) {
			uiBatch.draw(bgTexture,
				marginHori - padding,
				marginBottom - padding, // 底部留白
				logLayout.width + padding * 2,
				logLayout.height + padding * 2
			);
		}

		// --- 5. 绘制文字 (覆盖在背景之上) ---
		uiBatch.setColor(Color.WHITE); // 恢复白色画笔

		font.setColor(Color.YELLOW);
		font.draw(uiBatch, infoStr, viewport.getWorldWidth() - marginHori - infoLayout.width, topY);

		font.setColor(Color.WHITE);
		font.draw(uiBatch, logsStr, marginHori, logY);

		uiBatch.end();
	}

	public void dispose() {
		uiBatch.dispose();
		if (font != null) font.dispose();
		if (bgTexture != null) bgTexture.dispose(); // 记得释放纹理
	}

	// [核心修改] 根据屏幕方向调整视口参考尺寸
		public void resize(float width, float height) {
		// 动态调整 ExtendViewport 的最小世界尺寸
		if (height > width) {
			// 竖屏
			viewport.setMinWorldWidth(shortSide);
			viewport.setMinWorldHeight(longSide);
		} else {
			// 横屏
			viewport.setMinWorldWidth(longSide);
			viewport.setMinWorldHeight(shortSide);
		}

		viewport.update((int) width, (int) height, true);
	}
}
