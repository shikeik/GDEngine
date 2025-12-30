package com.goldsprite.solofight.screens.tests;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.goldsprite.gameframeworks.assets.FontUtils;
import com.goldsprite.gameframeworks.screens.ScreenManager;
import com.goldsprite.gameframeworks.screens.basics.ExampleGScreen;
import com.goldsprite.solofight.core.DebugUI;
import com.goldsprite.solofight.core.NeonBatch;
import com.goldsprite.solofight.core.ui.SmartNumInput;
import com.kotcrab.vis.ui.widget.VisLabel;

public class TempTestScreen extends ExampleGScreen {

	private Stage stage;
	//private SpriteBatch batch;
	private NeonBatch neonBatch;

	// 测试对象
	private VisLabel targetLabel; // 受控 Label
	private VisLabel smallLabel;  // 对照组
	private BitmapFont dynamicFont; // 当前生成的动态字体

	// 参数
	private float targetSize = 40f; // 期望的视觉大小 (像素)
	private float clarity = 1.0f;   // 清晰度乘数 (1=原图, 2=高清图缩小显示)

	@Override
	public String getIntroduction() {
		return "字体生成器精度测试\n调节 Clarity 不应改变文字实际高度";
	}

	@Override
	public ScreenManager.Orientation getOrientation() {
		return ScreenManager.Orientation.Portrait;
	}

	@Override
	public void create() {int k2;
		//batch = new SpriteBatch();
		neonBatch = new NeonBatch();
		stage = new Stage(getUIViewport());
		getImp().addProcessor(stage);

		Table root = new Table();
		root.setFillParent(true);
		root.top().pad(20);
		stage.addActor(root);

		// --- 1. 控制区 ---
		root.add(new VisLabel("--- Generator Settings ---")).row();

		// 控制视觉大小 (Target Visual Size)
		root.add(new SmartNumInput("Visual Size:", targetSize, 1f, val -> {
			targetSize = val;
			regenerateFont();
		})).fillX().padBottom(5).row();

		// 控制清晰度 (Clarity / Resolution Multiplier)
		// 核心测试点：改变这个值，文字应当变清晰/模糊，但“物理大小”不应改变！
		root.add(new SmartNumInput("Clarity:", clarity, 0.1f, val -> {
			clarity = val;
			regenerateFont();
		})).fillX().padBottom(20).row();

		// --- 2. 展示区 ---

		// A. 动态测试对象
		targetLabel = new VisLabel("Testing: " + FontUtils.fntPath);
		root.add(targetLabel).row();

		// 显示当前参数
		root.add(new VisLabel("(Red Line = Top, Green Line = Bottom)")).padBottom(10).row();

		// B. 对照组 (Small)
		smallLabel = new VisLabel("Reference (Small Style)");
		// 这里手动设置一个小字体样式作为对比，或者直接用 VisUI 默认
		smallLabel.setFontScale(0.7f); 
		smallLabel.setColor(Color.GRAY);
		root.add(smallLabel).padTop(20).row();

		// 初始生成
		regenerateFont();
	}

	private void regenerateFont() {int k;
		try {
			// 2. 调用核心生成方法
			// logic: textureSize = size * clarity; scale = 1/clarity;
			// result visual size = textureSize * scale = size.
			BitmapFont dynamicFont = FontUtils.generate((int) targetSize, clarity);

			// 3. 应用到 Label
			Label.LabelStyle style = new Label.LabelStyle(targetLabel.getStyle());
			style.font = dynamicFont;
			targetLabel.setStyle(style);
			
			// 1. 销毁旧字体，防止内存泄漏
			if (this.dynamicFont != null) {
				this.dynamicFont.dispose();
				this.dynamicFont = dynamicFont;
			}

			// 强制重算布局
			targetLabel.pack();

			DebugUI.log("Font Gen: Target=%.0f, Clarity=%.1f -> TexSize=%d, Scale=%.2f", 
						targetSize, clarity, (int)(targetSize*clarity), 1f/clarity);

		} catch (Exception e) {
			DebugUI.log("Font Gen Error: " + e.getMessage());
		}
	}

	@Override
	public void render0(float delta) {
		// 绘制辅助线，验证字体高度是否准确
		//batch.setProjectionMatrix(getUICamera().combined);
		neonBatch.begin();

		if (targetLabel != null) {
			// 获取 Label 在屏幕上的绝对位置
			float x = targetLabel.getX() + stage.getRoot().getX(); // 通常 root x is 0
			float y = targetLabel.getY() + stage.getRoot().getY();
			float h = targetLabel.getHeight(); // Label 计算出的高度
			float w = getUIViewport().getWorldWidth();

			// 1. 绘制 Label 的包围盒 (Blue)
			neonBatch.drawRect(x + targetLabel.getWidth()/2, y + h/2, targetLabel.getWidth(), h, 0, 1, Color.BLUE, false);

			// 2. 绘制 预期的视觉高度基准线 (Based on targetSize)
			// 文字通常是基于 Baseline 绘制的，或者居中。
			// 这里我们以 Label 的 Y 为底，Y + targetSize 为顶，看看字是否撑满
			// 注意：BitmapFont 的 height 通常包含 ascent/descent，可能比 fontSize 略大

			// 绿线：Label 底部 (Y)
			neonBatch.drawLine(0, y, w, y, 1, Color.GREEN);

			// 红线：预期顶部 (Y + TargetSize)
			neonBatch.drawLine(0, y + targetSize, w, y + targetSize, 1, Color.RED);

			// 黄线：实际 BitmapFont CapHeight (大写字母高度)
			if (dynamicFont != null) {
				float capH = dynamicFont.getCapHeight(); // 此时已经应用了 Scale
				neonBatch.drawLine(0, y + capH, w, y + capH, 1, Color.YELLOW);
			}
		}

		neonBatch.end();

		stage.act(delta);
		stage.draw();

		DebugUI.info("Target Size: %.0f px", targetSize);
		DebugUI.info("Clarity: %.2f", clarity);
		if (dynamicFont != null) {
			DebugUI.info("Actual Data Scale: %.3f", dynamicFont.getData().scaleX);
			DebugUI.info("Line Height: %.1f", dynamicFont.getLineHeight());
		}
	}

	@Override
	public void dispose() {
		super.dispose();
		if (stage != null) stage.dispose();
		//if (batch != null) batch.dispose();
		if (dynamicFont != null) dynamicFont.dispose();
	}
}
