package com.goldsprite.solofight.core;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.utils.viewport.ExtendViewport;
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
	static int maxLogsCache = 100, maxRenderLogs = 8;

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
	float screenScl = 1.3f;
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
		if (getInstance().logs.size() > maxLogsCache) getInstance().logs.remove(0);
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

		// --- 1. 准备数据 (String Building) ---
		// Info
		StringBuilder infoBuilder = new StringBuilder();
		infoBuilder.append(BuildConfig.PROJECT_NAME).append(": V").append(BuildConfig.DEV_VERSION).append("\n");
		infoBuilder.append("FPS: ").append(Gdx.graphics.getFramesPerSecond()).append("\n");
		infoBuilder.append("Heap: ").append(Gdx.app.getJavaHeap() / 1024 / 1024).append("MB");
		if(!debugInfos.isEmpty()) infoBuilder.append("\n").append(String.join("\n", debugInfos)); // 自定义监控
		debugInfos.clear();

		//截获log
		int startIndex = logs.size() - maxRenderLogs;
		List<String> subLogs = logs;
		if(startIndex >= 0) subLogs = logs.subList(startIndex, logs.size());
		//Log
		String logsStr = "> " + String.join("\n> ", subLogs);
		String infoStr = infoBuilder.toString();

		if (!showDebugUI) return;

		// --- 2. 计算尺寸 (Layout Packing) ---
		infoLayout.setText(font, infoStr);
		logLayout.setText(font, logsStr);

		// --- 3. 统一计算坐标 (Coordinate Calculation) ---
		float screenW = viewport.getWorldWidth();
		// float screenH = viewport.getWorldHeight(); // 暂时用不到高度了，因为都沉底了

		// 左下角 Log 面板位置
		// font.draw 的 Y 是文字顶端，所以 Y = margin + height
		float logTextX = marginHori;
		float logTextY = marginBottom + logLayout.height;

		// [新] 右下角 Info 面板位置
		float infoTextX = screenW - marginHori - infoLayout.width;
		float infoTextY = marginBottom + infoLayout.height;

		// --- 4. 开始绘制 (Batching) ---
		uiBatch.setProjectionMatrix(camera.combined);
		uiBatch.begin();

		// >> Step A: 绘制所有背景 (减少渲染状态切换)
		uiBatch.setColor(bgColor);

		// Info 背景
		uiBatch.draw(bgTexture,
			infoTextX - padding,
			marginBottom - padding, // 底部固定对齐 margin
			infoLayout.width + padding * 2,
			infoLayout.height + padding * 2
		);

		// Log 背景 (仅当有日志时)
		if (!logs.isEmpty()) {
			uiBatch.draw(bgTexture,
				logTextX - padding,
				marginBottom - padding,
				logLayout.width + padding * 2,
				logLayout.height + padding * 2
			);
		}

		// >> Step B: 绘制所有文字
		uiBatch.setColor(Color.WHITE); // 恢复画笔颜色

		// Info 文字 (黄色高亮)
		font.setColor(Color.YELLOW);
		font.draw(uiBatch, infoStr, infoTextX, infoTextY);

		// Log 文字 (白色)
		font.setColor(Color.WHITE);
		font.draw(uiBatch, logsStr, logTextX, logTextY);

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
