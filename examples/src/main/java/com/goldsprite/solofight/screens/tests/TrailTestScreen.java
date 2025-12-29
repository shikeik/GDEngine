package com.goldsprite.solofight.screens.tests;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.utils.Array;
import com.goldsprite.gameframeworks.screens.basics.ExampleGScreen;
import com.goldsprite.solofight.core.NeonBatch;
import com.kotcrab.vis.ui.widget.VisLabel;
import com.kotcrab.vis.ui.widget.VisSlider;
import com.kotcrab.vis.ui.widget.VisWindow;

public class TrailTestScreen extends ExampleGScreen {

	private Stage stage;
	private SpriteBatch batch;
	private NeonBatch neonBatch;

	private Vector2 cursor = new Vector2();
	private RibbonTrail ribbonTrail;

	// --- 可配置参数 ---
	private float cfgSegmentLen = 5f;   // 细分距离 (越小越丝滑)
	private float cfgMaxLen = 600f;     // 拖尾总长度
	private float cfgStartWidth = 60f;  // 头部宽度

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
		ribbonTrail = new RibbonTrail(2000); // 预分配大容量

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
		Table root = new Table();
		root.setFillParent(true);
		root.right().top().pad(20);
		stage.addActor(root);

		VisWindow win = new VisWindow("Trail Settings");
		win.setMovable(true);

		Table t = new Table();
		t.pad(10);

		// 1. Segment Length
		t.add(new VisLabel("Smoothness (Seg Len):")).left();
		final VisLabel lblSeg = new VisLabel(String.format("%.1f", cfgSegmentLen));
		t.add(lblSeg).width(40).right().row();
		VisSlider slSeg = new VisSlider(1f, 20f, 0.5f, false);
		slSeg.setValue(cfgSegmentLen);
		slSeg.addListener(new ChangeListener() {
				@Override
				public void changed(ChangeEvent event, Actor actor) {
					cfgSegmentLen = slSeg.getValue();
					lblSeg.setText(String.format("%.1f", cfgSegmentLen));
					ribbonTrail.setSegmentLength(cfgSegmentLen);
				}
			});
		t.add(slSeg).colspan(2).fillX().row();

		// 2. Max Length
		t.add(new VisLabel("Trail Length:")).left();
		final VisLabel lblLen = new VisLabel(String.format("%.0f", cfgMaxLen));
		t.add(lblLen).right().row();
		VisSlider slLen = new VisSlider(100f, 2000f, 50f, false);
		slLen.setValue(cfgMaxLen);
		slLen.addListener(new ChangeListener() {
				@Override
				public void changed(ChangeEvent event, Actor actor) {
					cfgMaxLen = slLen.getValue();
					lblLen.setText(String.format("%.0f", cfgMaxLen));
					ribbonTrail.setMaxLength(cfgMaxLen);
				}
			});
		t.add(slLen).colspan(2).fillX().row();

		// 3. Width
		t.add(new VisLabel("Head Width:")).left();
		final VisLabel lblWid = new VisLabel(String.format("%.0f", cfgStartWidth));
		t.add(lblWid).right().row();
		VisSlider slWid = new VisSlider(10f, 200f, 5f, false);
		slWid.setValue(cfgStartWidth);
		slWid.addListener(new ChangeListener() {
				@Override
				public void changed(ChangeEvent event, Actor actor) {
					cfgStartWidth = slWid.getValue();
					lblWid.setText(String.format("%.0f", cfgStartWidth));
					ribbonTrail.setStartWidth(cfgStartWidth);
				}
			});
		t.add(slWid).colspan(2).fillX().row();

		win.add(t);
		win.pack();
		root.add(win);
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
	// 核心算法：Smooth Ribbon Trail
	// ==========================================
	public static class RibbonTrail {
		// 存储所有的细分点
		private final Array<Vector2> points = new Array<>();
		// 缓存计算结果，防止 GC
		private float[] vertsCache;
		private float[] colorsCache;

		// 配置
		private float segmentLength = 5f; // 采样/细分距离
		private float maxLength = 600f;   // 总长度
		private float startWidth = 60f;

		public RibbonTrail(int capacity) {
			points.ensureCapacity(capacity);
			vertsCache = new float[capacity * 4]; // 每个点2个顶点(左右)，每个顶点2float(xy)
			colorsCache = new float[capacity * 2]; // 每个点2个颜色
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

			// 1. 插值填充 (Interpolation)
			// 计算当前点与最新记录点的距离
			Vector2 last = points.first();
			float dist = last.dst(x, y);

			// 如果距离过大，进行插值填补
			if (dist >= segmentLength) {
				// 需要插入多少个点
				int steps = (int)(dist / segmentLength);
				for (int i = 1; i <= steps; i++) {
					float t = (float)i / steps;
					Vector2 interpolated = new Vector2(last).lerp(new Vector2(x, y), t);
					points.insert(0, interpolated);
				}
				// 确保最后一个点精确对齐当前位置
				// (如果正好整除可能重复，这里简单处理：总是更新最新的点为当前位置，
				// 但上面是 insert，所以其实是生成了一串新点)
				// 这种逻辑下，鼠标极快移动时，中间会有均匀的点补充。
			} else {
				// 距离不够生成新点，但为了跟随鼠标，我们更新头部位置？
				// 为了避免头部抖动，通常策略是：
				// A. 只有超过距离才加点，头部平时不动（会有滞后感）
				// B. 头部点永远跟随鼠标，第二个点才是固定的（推荐）

				// 这里采用 B 策略：
				// 我们的 points[0] 始终设为当前位置。
				// 当 points[0] 与 points[1] 距离超过 segmentLength 时，
				// 我们把 points[0] "固化" 为 points[1]，并插入一个新的 points[0]。

				// 但由于上面的循环插值逻辑比较简单，我们换一种更稳健的写法：

				// 回退：先不改 points[0]，而是看距离。
				// 如果距离不够，直接更新 points[0] 位置？会导致整个链条被拉长？
				// 不，我们用链表思路：points[0] 是头部，它是动态的。
				// 真正的历史轨迹从 points[1] 开始。
			}

			// --- 修正后的 Update 逻辑 ---
			// 1. 始终将当前位置设为 Head (points[0])
			//    如果是第一次，加进去。
			//    如果不是第一次，先计算 Head 与 points[1] 的距离。
			//    如果距离 > segmentLength，则在 Head 和 points[1] 之间插入新节点。

			if (points.size == 1) {
				// 只有一个点，检查距离，如果远了就分裂
				if (points.get(0).dst(x, y) > segmentLength) {
					points.insert(0, new Vector2(x, y));
				} else {
					points.get(0).set(x, y);
				}
			} else {
				// 有多个点
				Vector2 head = points.get(0);
				Vector2 next = points.get(1); // 上一个固定的点

				// 更新头部到当前位置
				head.set(x, y);

				// 检查头部与第二个点的距离
				float d = head.dst(next);
				if (d > segmentLength) {
					// 需要插入中间点
					int count = (int)(d / segmentLength);
					// 从 next 到 head 插值
					for (int i = 1; i <= count; i++) {
						float t = (float)i / (count + 1); // 线性分布
						Vector2 p = new Vector2(next).lerp(head, t);
						// 插入到 index 1 (Head 后面)
						points.insert(1, p);
					}
					// 插入后，Head 依然是 points[0]，距离 next 的距离变短了，无需处理
				}
			}

			// 2. 长度修剪 (Pruning based on total length)
			float currentLen = 0;
			for (int i = 0; i < points.size - 1; i++) {
				currentLen += points.get(i).dst(points.get(i+1));
				if (currentLen > maxLength) {
					// 截断多余的点
					points.truncate(i + 2); 
					break;
				}
			}

			// 保证缓存数组够大
			int reqVerts = points.size * 4;
			if (vertsCache.length < reqVerts) {
				vertsCache = new float[reqVerts * 2];
				colorsCache = new float[points.size * 2 * 2];
			}
		}

		public void draw(NeonBatch batch, Color headColor, Color tailColor) {
			if (points.size < 2) return;

			// 1. 绘制头部圆 (纯色)
			// 为了完美衔接，圆心在 P0，半径为 startWidth/2
			batch.drawCircle(points.first().x, points.first().y, startWidth / 2f, 0, headColor, 16, true);

			// 2. 构建 Triangle Strip
			int idx = 0;
			Vector2 dir = new Vector2();
			Vector2 nor = new Vector2();

			// 预先计算总长度用于精确渐变 (可选，或者简单用 index/size)
			// 用 index/size 比较快，但如果点间距不匀会导致颜色断层。
			// 由于我们有 segmentLength 强制插值，点间距基本均匀，所以 index/size 足够平滑。

			for (int i = 0; i < points.size; i++) {
				Vector2 curr = points.get(i);

				// 计算切线方向 (平滑法线：取前后两段的平均)
				if (i == 0) {
					dir.set(curr).sub(points.get(i+1)).nor();
				} else if (i == points.size - 1) {
					dir.set(points.get(i-1)).sub(curr).nor();
				} else {
					// 中间点：取 (Prev->Curr) + (Curr->Next) 的平均
					Vector2 v1 = new Vector2(curr).sub(points.get(i+1)).nor();
					Vector2 v2 = new Vector2(points.get(i-1)).sub(curr).nor();
					dir.set(v1).add(v2).nor();
				}

				nor.set(-dir.y, dir.x); // 法线

				// 进度 t: 0(Head) -> 1(Tail)
				float t = (float)i / (points.size - 1);

				// 宽度插值: 线性变窄 (HeadWidth -> 0)
				float w = startWidth * (1f - t);

				// 颜色插值: HeadColor -> TailColor (Alpha fade)
				// 这里为了性能，直接改 Alpha
				Color c = new Color(headColor).lerp(tailColor, t);
				c.a = headColor.a * (1f - t * t); // t*t 让尾部消失得更快/更慢，调整手感
				if(c.a < 0) c.a = 0;
				float cBits = c.toFloatBits();

				// 左右顶点
				// Left
				vertsCache[idx*2]     = curr.x + nor.x * w * 0.5f;
				vertsCache[idx*2 + 1] = curr.y + nor.y * w * 0.5f;
				colorsCache[idx]      = cBits;
				idx++;

				// Right
				vertsCache[idx*2]     = curr.x - nor.x * w * 0.5f;
				vertsCache[idx*2 + 1] = curr.y - nor.y * w * 0.5f;
				colorsCache[idx]      = cBits;
				idx++;
			}

			batch.drawTriangleStrip(vertsCache, colorsCache, idx);
		}
	}
}
