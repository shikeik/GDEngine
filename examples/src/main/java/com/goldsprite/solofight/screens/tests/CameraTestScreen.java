package com.goldsprite.solofight.screens.tests;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.viewport.ExtendViewport;
import com.goldsprite.biowar.core.DebugUI;
import com.goldsprite.biowar.core.SimpleCameraController;
import com.goldsprite.biowar.core.SmartCameraController;
import com.goldsprite.gameframeworks.screens.ScreenManager;
import com.goldsprite.gameframeworks.screens.basics.ExampleGScreen;
import com.kotcrab.vis.ui.widget.VisCheckBox;
import com.kotcrab.vis.ui.widget.VisLabel;
import com.kotcrab.vis.ui.widget.VisSlider;
import com.kotcrab.vis.ui.widget.VisTextButton;
import java.util.ArrayList;
import java.util.List;

public class CameraTestScreen extends ExampleGScreen {

	private SmartCameraController smartCam;
	private SimpleCameraController freeCam; // [v3.4] 自由相机控制器
	private ShapeRenderer shapeRenderer;
	private Stage uiStage;
	private SpriteBatch batch;
	private BitmapFont debugFont;

	private List<Vector2> targets = new ArrayList<>();
	private Vector2 dragTarget = null;

	// 调试开关
	private boolean ghostMode = true; 
	private boolean drawBackground = false; // [v3.4] 默认不画底色

	@Override
	public String getIntroduction() { return ""; }
	@Override
	protected void drawIntros() {}

	@Override
	protected void initViewportAndCamera() {
		// [v3.4] 视口扩大 1.4 倍 -> 1344 x 756
		float scl = 1.4f;
		worldCamera = new OrthographicCamera();
		uiViewport = new ExtendViewport(960 * scl, 540 * scl); 
	}

	@Override
	public void create() {
		batch = new SpriteBatch();
		shapeRenderer = new ShapeRenderer();
		debugFont = new BitmapFont(); 

		smartCam = new SmartCameraController(getWorldCamera());
		smartCam.setMapBounds(-1000, -800, 2000, 1600);

		// [v3.4] 初始化自由相机
		freeCam = new SimpleCameraController(getWorldCamera());
		freeCam.setInputEnabled(ghostMode); // 初始状态跟随 ghostMode

		targets.add(new Vector2(-200, 0));
		targets.add(new Vector2(200, 0));

		initUI();
		initInput();
	}

	@Override
	public void show() {
		super.show();
		getScreenManager().setOrientation(ScreenManager.Orientation.LANDSCAPE);
	}

	@Override
	public void hide() {
		super.hide();
		getScreenManager().setOrientation(ScreenManager.Orientation.PORTRAIT);
	}

	private void initInput() {
		// [v3.4] 注册输入处理器顺序：UI -> FreeCam -> TargetLogic
		getImp().addProcessor(freeCam); // 自由相机优先处理滚轮/右键拖拽

		getImp().addProcessor(new InputAdapter() {
				@Override
				public boolean touchDown(int screenX, int screenY, int pointer, int button) {
					Vector2 worldPos = screenToWorldCoord(screenX, screenY);

					// [v3.4] 如果是右键且在 GhostMode 下，可能是拖拽相机，不触发移除
					// SimpleCameraController 使用右键或中键拖拽
					if ((button == Input.Buttons.RIGHT || button == Input.Buttons.MIDDLE) && ghostMode) {
						return false; // 交给 freeCam 处理
					}

					// 左键逻辑：选中或添加
					if (button == Input.Buttons.LEFT) {
						Vector2 hit = null;
						for (Vector2 t : targets) {
							if (t.dst(worldPos) < 40) { hit = t; break; }
						}
						if (hit != null) dragTarget = hit;
						else { dragTarget = new Vector2(worldPos); targets.add(dragTarget); }
						return true;
					}
					return false;
				}
				@Override
				public boolean touchDragged(int screenX, int screenY, int pointer) {
					if (dragTarget != null) {
						dragTarget.set(screenToWorldCoord(screenX, screenY));
						return true;
					}
					return false;
				}
				@Override
				public boolean touchUp(int screenX, int screenY, int pointer, int button) {
					dragTarget = null; return false;
				}
			});
	}

	private void initUI() {
		uiStage = new Stage(getViewport());
		getImp().addProcessor(uiStage);

		Table root = new Table();
		root.setFillParent(true);
		root.top().left().pad(20);
		uiStage.addActor(root);

		Table panel = new Table();
		panel.setBackground(createColorDrawable(new Color(0, 0, 0, 0.5f)));
		panel.pad(15); 
		panel.defaults().width(220).padBottom(5).left();
		root.add(panel);

		// 1. 幽灵模式 (自由相机)
		VisCheckBox cbGhost = new VisCheckBox("Ghost Mode (Free Cam)", true);
		cbGhost.addListener(new ChangeListener() {
				@Override
				public void changed(ChangeEvent event, Actor actor) {
					ghostMode = cbGhost.isChecked();
					freeCam.setInputEnabled(ghostMode); // 开关自由相机输入
					if (!ghostMode) {
						// 关闭时，瞬间应用智能相机位置，防止跳变
						smartCam.apply();
					}
				}
			});
		panel.add(cbGhost).row();

		// [v3.4] 底色开关
		VisCheckBox cbBg = new VisCheckBox("Draw Backgrounds", false);
		cbBg.addListener(new ChangeListener() {
				@Override
				public void changed(ChangeEvent event, Actor actor) {
					drawBackground = cbBg.isChecked();
				}
			});
		panel.add(cbBg).row();

		VisCheckBox cbSmooth = new VisCheckBox("Enable Smoothing", true);
		cbSmooth.addListener(new ChangeListener() {
				@Override
				public void changed(ChangeEvent event, Actor actor) { smartCam.smoothEnabled = cbSmooth.isChecked(); }
			});
		panel.add(cbSmooth).row();

		panel.add(new VisLabel("Smooth Speed")).row();
		VisSlider slSpeed = new VisSlider(1f, 20f, 0.5f, false);
		slSpeed.setValue(5f);
		slSpeed.addListener(new ChangeListener() {
				@Override
				public void changed(ChangeEvent event, Actor actor) { smartCam.smoothSpeed = slSpeed.getValue(); }
			});
		panel.add(slSpeed).fillX().row();

		panel.add(new VisLabel("Padding")).row();
		VisSlider slPad = new VisSlider(0, 500, 10, false);
		slPad.setValue(100);
		slPad.addListener(new ChangeListener() {
				@Override
				public void changed(ChangeEvent event, Actor actor) { smartCam.padding = slPad.getValue(); }
			});
		panel.add(slPad).fillX().row();

		panel.add(new VisLabel("Min Zoom")).row();
		VisSlider slMinZ = new VisSlider(0.1f, 2f, 0.1f, false);
		slMinZ.setValue(0.5f);
		slMinZ.addListener(new ChangeListener() {
				@Override
				public void changed(ChangeEvent event, Actor actor) { smartCam.minZoom = slMinZ.getValue(); }
			});
		panel.add(slMinZ).fillX().row();

		VisTextButton btnShake = new VisTextButton("Add Trauma (Shake!)");
		btnShake.addListener(new ChangeListener() {
				@Override
				public void changed(ChangeEvent event, Actor actor) { smartCam.addTrauma(0.5f); }
			});
		panel.add(btnShake).fillX().padTop(10).row();

		VisTextButton btnClear = new VisTextButton("Reset Targets");
		btnClear.addListener(new ChangeListener() {
				@Override
				public void changed(ChangeEvent event, Actor actor) {
					targets.clear();
					targets.add(new Vector2(-200, 0));
					targets.add(new Vector2(200, 0));
				}
			});
		panel.add(btnClear).fillX().row();
	}

	private Drawable createColorDrawable(Color color) {
		Pixmap pixmap = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
		pixmap.setColor(color); pixmap.fill();
		Texture texture = new Texture(pixmap); pixmap.dispose();
		return new TextureRegionDrawable(new TextureRegion(texture));
	}

	@Override
	public void render0(float delta) {
		// 计算智能相机数据 (但不一定应用)
		smartCam.update(targets, delta);

		if (ghostMode) {
			// 幽灵模式：使用自由相机控制视角
			freeCam.update(delta); 
		} else {
			// 实机模式：智能相机接管
			smartCam.apply();
		}

		drawGrid();

		// 绘制目标
		shapeRenderer.setProjectionMatrix(getWorldCamera().combined);
		shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
		for (Vector2 t : targets) {
			if (t == dragTarget) shapeRenderer.setColor(Color.WHITE);
			else shapeRenderer.setColor(Color.GREEN);
			shapeRenderer.rect(t.x - 15, t.y - 15, 30, 30);
		}
		shapeRenderer.end();

		// 绘制调试框 (在 Ghost Mode 或你想观察的时候)
		if (ghostMode) {
			// [v3.4] 传入 drawBackground 开关
			smartCam.drawDebug(shapeRenderer, batch, debugFont, drawBackground);
		}

		DebugUI.info("Targets: %d", targets.size());
		DebugUI.info("Cam Pos: %.0f, %.0f", getWorldCamera().position.x, getWorldCamera().position.y);
		DebugUI.info("Cam Zoom: %.2f", getWorldCamera().zoom);

		uiStage.act(delta);
		uiStage.draw();
	}

	private void drawGrid() {
		shapeRenderer.setProjectionMatrix(getWorldCamera().combined);
		Gdx.gl.glEnable(GL20.GL_BLEND);
		shapeRenderer.begin(ShapeRenderer.ShapeType.Line);
		shapeRenderer.setColor(0.3f, 0.3f, 0.3f, 1f);
		int size = 100;
		// 画大一点的网格以便自由相机观察
		for (int x = -2000; x <= 3000; x += size) shapeRenderer.line(x, -2000, x, 3000);
		for (int y = -2000; y <= 3000; y += size) shapeRenderer.line(-2000, y, 3000, y);
		shapeRenderer.end();
		Gdx.gl.glDisable(GL20.GL_BLEND);
	}

	@Override
	public void dispose() {
		super.dispose();
		if (uiStage != null) uiStage.dispose();
		shapeRenderer.dispose();
		batch.dispose();
		debugFont.dispose();
	}
}
