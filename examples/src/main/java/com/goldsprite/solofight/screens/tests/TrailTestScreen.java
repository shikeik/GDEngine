package com.goldsprite.solofight.screens.tests;

import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.utils.Array;
import com.goldsprite.gameframeworks.screens.basics.ExampleGScreen;
import com.goldsprite.solofight.core.NeonBatch;
import com.goldsprite.solofight.core.ui.SmartNumInput; // 引入新控件
import com.kotcrab.vis.ui.widget.VisWindow;

public class TrailTestScreen extends ExampleGScreen {

	private Stage stage;
	private SpriteBatch batch;
	private NeonBatch neonBatch;

	private Vector2 cursor = new Vector2();
	private RibbonTrail ribbonTrail;

	// --- 可配置参数 ---
	private float cfgSegmentLen = 5f;
	private float cfgMaxLen = 600f;
	private float cfgStartWidth = 60f;

	@Override
	public String getIntroduction() {
		return "高级拖尾测试\nSmooth Ribbon Trail";
	}

	@Override
	public void create() {
		batch = new SpriteBatch();
		neonBatch = new NeonBatch(batch);
		stage = new Stage(getViewport());
		getImp().addProcessor(stage);

		cursor.set(getViewport().getWorldWidth() / 2, getViewport().getWorldHeight() / 2);
		ribbonTrail = new RibbonTrail(2000);

		initUI();
		initInput();
	}

	private void initInput() {
		getImp().addProcessor(new InputAdapter() {
				@Override
				public boolean touchDown(int screenX, int screenY, int pointer, int button) {
					Vector2 world = screenToWorldCoord(screenX, screenY);
					cursor.set(world);
					ribbonTrail.reset(world.x, world.y);
					return true;
				}

				@Override
				public boolean touchDragged(int screenX, int screenY, int pointer) {
					Vector2 world = screenToWorldCoord(screenX, screenY);
					cursor.set(world);
					return true;
				}
			});
	}

	private void initUI() {
		// 使用 Table 布局或者直接添加 Actor
		// 这里为了精确控制初始位置在左上角，我们不使用全屏 Table 的 align，
		// 而是直接设置 Window 的位置。

		VisWindow win = new VisWindow("Trail Settings");
		win.setMovable(true); // 允许拖动
		win.setResizable(false);
		win.addCloseButton();

		Table t = new Table();
		t.pad(10);

		// 1. Segment Length (Smoothness)
		// 步进 0.1，变化时更新逻辑
		t.add(new SmartNumInput("Seg Len:", cfgSegmentLen, 0.5f, v -> {
			cfgSegmentLen = v;
			ribbonTrail.setSegmentLength(v);
		})).fillX().row();

		// 2. Max Length
		// 步进 10
		t.add(new SmartNumInput("Max Len:", cfgMaxLen, 10f, v -> {
			cfgMaxLen = v;
			ribbonTrail.setMaxLength(v);
		})).fillX().row();

		// 3. Head Width
		// 步进 5
		t.add(new SmartNumInput("Width:", cfgStartWidth, 5f, v -> {
			cfgStartWidth = v;
			ribbonTrail.setStartWidth(v);
		})).fillX().row();

		win.add(t);
		win.pack();

		// 设置位置：左上角 (20, Top - 20)
		// 注意：Stage坐标系 (0,0) 在左下角
		win.setPosition(20, getViewport().getWorldHeight() - win.getHeight() - 20);

		stage.addActor(win);
	}

	@Override
	public void render0(float delta) {
		// Update logic
		ribbonTrail.update(cursor.x, cursor.y);

		// Draw
		batch.setProjectionMatrix(getWorldCamera().combined);
		neonBatch.begin();

		drawGrid(neonBatch);

		// 绘制拖尾 (头部亮青色 -> 尾部透明蓝)
		ribbonTrail.draw(neonBatch, Color.CYAN, Color.BLUE);

		// 绘制光标
		neonBatch.drawCircle(cursor.x, cursor.y, 10, 0, Color.WHITE, 16, true);

		neonBatch.end();

		stage.act(delta);
		stage.draw();
	}

	private void drawGrid(NeonBatch batch) {
		Color c = new Color(1, 1, 1, 0.05f);
		for(int i=-1000; i<2000; i+=100) batch.drawLine(i, -1000, i, 1000, 1, c);
		for(int i=-1000; i<1000; i+=100) batch.drawLine(-1000, i, 2000, i, 1, c);
	}

	@Override
	public void dispose() {
		super.dispose();
		if (stage != null) stage.dispose();
		if (batch != null) batch.dispose();
	}

	// ==========================================
	// 核心算法：Smooth Ribbon Trail (保持不变)
	// ==========================================
	public static class RibbonTrail {
		// ... (RibbonTrail 代码与之前一致，无需变动，此处省略以节省空间) ...
		// 存储所有的细分点
		private final Array<Vector2> points = new Array<>();
		private float[] vertsCache;
		private float[] colorsCache;
		private float segmentLength = 5f;
		private float maxLength = 600f;
		private float startWidth = 60f;

		public RibbonTrail(int capacity) {
			points.ensureCapacity(capacity);
			vertsCache = new float[capacity * 4];
			colorsCache = new float[capacity * 2];
		}

		public void setSegmentLength(float v) { this.segmentLength = Math.max(1f, v); }
		public void setMaxLength(float v) { this.maxLength = v; }
		public void setStartWidth(float v) { this.startWidth = v; }

		public void reset(float x, float y) {
			points.clear();
			points.add(new Vector2(x, y));
		}

		public void update(float x, float y) {
			if (points.size == 0) {
				points.add(new Vector2(x, y));
				return;
			}
			if (points.size == 1) {
				if (points.get(0).dst(x, y) > segmentLength) points.insert(0, new Vector2(x, y));
				else points.get(0).set(x, y);
			} else {
				Vector2 head = points.get(0);
				Vector2 next = points.get(1);
				head.set(x, y);
				float d = head.dst(next);
				if (d > segmentLength) {
					int count = (int)(d / segmentLength);
					for (int i = 1; i <= count; i++) {
						float t = (float)i / (count + 1);
						Vector2 p = new Vector2(next).lerp(head, t);
						points.insert(1, p);
					}
				}
			}
			float currentLen = 0;
			for (int i = 0; i < points.size - 1; i++) {
				currentLen += points.get(i).dst(points.get(i+1));
				if (currentLen > maxLength) {
					points.truncate(i + 2); 
					break;
				}
			}
			int reqVerts = points.size * 4;
			if (vertsCache.length < reqVerts) {
				vertsCache = new float[reqVerts * 2];
				colorsCache = new float[points.size * 2 * 2];
			}
		}

		public void draw(NeonBatch batch, Color headColor, Color tailColor) {
			if (points.size < 2) return;
			batch.drawCircle(points.first().x, points.first().y, startWidth / 2f, 0, headColor, 16, true);
			int idx = 0;
			Vector2 dir = new Vector2();
			Vector2 nor = new Vector2();
			for (int i = 0; i < points.size; i++) {
				Vector2 curr = points.get(i);
				if (i == 0) dir.set(curr).sub(points.get(i+1)).nor();
				else if (i == points.size - 1) dir.set(points.get(i-1)).sub(curr).nor();
				else {
					Vector2 v1 = new Vector2(curr).sub(points.get(i+1)).nor();
					Vector2 v2 = new Vector2(points.get(i-1)).sub(curr).nor();
					dir.set(v1).add(v2).nor();
				}
				nor.set(-dir.y, dir.x);
				float t = (float)i / (points.size - 1);
				float w = startWidth * (1f - t);
				Color c = new Color(headColor).lerp(tailColor, t);
				c.a = headColor.a * (1f - t * t);
				if(c.a < 0) c.a = 0;
				float cBits = c.toFloatBits();
				vertsCache[idx*2] = curr.x + nor.x * w * 0.5f; vertsCache[idx*2 + 1] = curr.y + nor.y * w * 0.5f; colorsCache[idx] = cBits; idx++;
				vertsCache[idx*2] = curr.x - nor.x * w * 0.5f; vertsCache[idx*2 + 1] = curr.y - nor.y * w * 0.5f; colorsCache[idx] = cBits; idx++;
			}
			batch.drawTriangleStrip(vertsCache, colorsCache, idx);
		}
	}
}
