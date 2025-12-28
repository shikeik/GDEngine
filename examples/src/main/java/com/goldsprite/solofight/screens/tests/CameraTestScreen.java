package com.goldsprite.solofight.screens.tests;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.graphics.Color;
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
import com.goldsprite.biowar.core.SmartCameraController;
import com.goldsprite.gameframeworks.screens.ScreenManager;
import com.goldsprite.gameframeworks.screens.basics.ExampleGScreen;
import com.kotcrab.vis.ui.widget.VisCheckBox;
import com.kotcrab.vis.ui.widget.VisLabel;
import com.kotcrab.vis.ui.widget.VisSlider;
import com.kotcrab.vis.ui.widget.VisTextButton;

import java.util.ArrayList;
import java.util.List;
import com.badlogic.gdx.graphics.GL20;

public class CameraTestScreen extends ExampleGScreen {

	private SmartCameraController smartCam;
	private ShapeRenderer shapeRenderer;
	private Stage uiStage;
	private SpriteBatch batch;
	private BitmapFont debugFont;

	private List<Vector2> targets = new ArrayList<>();
	private Vector2 dragTarget = null;
	private boolean ghostMode = true;

	@Override
	public String getIntroduction() { return ""; }
	@Override
	protected void drawIntros() {}

	@Override
	protected void initViewportAndCamera() {
		// [v3.3] 强制自定义视口 960x540，世界缩放 1:1
		// 这样 UI 在手机上显示大小合适，且世界坐标系清晰
		worldCamera = new OrthographicCamera();
		uiViewport = new ExtendViewport(960, 540); 
	}

	@Override
	public void create() {
		batch = new SpriteBatch();
		shapeRenderer = new ShapeRenderer();
		debugFont = new BitmapFont(); // 默认字体用于 Debug 标签

		smartCam = new SmartCameraController(getWorldCamera());
		// [v3.3] 地图边界设大一点，以(0,0)为中心
		smartCam.setMapBounds(-1000, -800, 2000, 1600);

		// [v3.3] 初始目标围绕原点
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
		getImp().addProcessor(new InputAdapter() {
				@Override
				public boolean touchDown(int screenX, int screenY, int pointer, int button) {
					Vector2 worldPos = screenToWorldCoord(screenX, screenY);
					Vector2 hit = null;
					for (Vector2 t : targets) {
						if (t.dst(worldPos) < 40) { hit = t; break; }
					}
					if (hit != null) dragTarget = hit;
					else { dragTarget = new Vector2(worldPos); targets.add(dragTarget); }
					return true;
				}
				@Override
				public boolean touchDragged(int screenX, int screenY, int pointer) {
					if (dragTarget != null) dragTarget.set(screenToWorldCoord(screenX, screenY));
					return true;
				}
				@Override
				public boolean touchUp(int screenX, int screenY, int pointer, int button) {
					dragTarget = null; return true;
				}
			});
	}

	private void initUI() {
		uiStage = new Stage(getViewport());
		getImp().addProcessor(uiStage);

		Table root = new Table();
		root.setFillParent(true);
		// [v3.3] UI 移至左上角
		root.top().left().pad(20);
		uiStage.addActor(root);

		root.setBackground(createColorDrawable(new Color(0, 0, 0, 0.5f)));
		root.defaults().width(220).padBottom(5).left();

		VisCheckBox cbGhost = new VisCheckBox("Ghost Mode (Debug)", true);
		cbGhost.addListener(new ChangeListener() {
				@Override
				public void changed(ChangeEvent event, Actor actor) {
					ghostMode = cbGhost.isChecked();
					if (ghostMode) {
						// 幽灵模式：相机归位
						getWorldCamera().position.set(0, 0, 0);
						getWorldCamera().zoom = 1f;
						getWorldCamera().update();
					}
				}
			});
		root.add(cbGhost).row();

		VisCheckBox cbSmooth = new VisCheckBox("Enable Smoothing", true);
		cbSmooth.addListener(new ChangeListener() {
				@Override
				public void changed(ChangeEvent event, Actor actor) { smartCam.smoothEnabled = cbSmooth.isChecked(); }
			});
		root.add(cbSmooth).row();

		root.add(new VisLabel("Smooth Speed")).row();
		VisSlider slSpeed = new VisSlider(1f, 20f, 0.5f, false);
		slSpeed.setValue(5f);
		slSpeed.addListener(new ChangeListener() {
				@Override
				public void changed(ChangeEvent event, Actor actor) { smartCam.smoothSpeed = slSpeed.getValue(); }
			});
		root.add(slSpeed).fillX().row();

		root.add(new VisLabel("Padding")).row();
		VisSlider slPad = new VisSlider(0, 500, 10, false);
		slPad.setValue(100);
		slPad.addListener(new ChangeListener() {
				@Override
				public void changed(ChangeEvent event, Actor actor) { smartCam.padding = slPad.getValue(); }
			});
		root.add(slPad).fillX().row();

		root.add(new VisLabel("Zoom (Min - Max)")).row();
		// 为了简化演示，这里只调 MinZoom
		VisSlider slMinZ = new VisSlider(0.1f, 2f, 0.1f, false);
		slMinZ.setValue(0.5f);
		slMinZ.addListener(new ChangeListener() {
				@Override
				public void changed(ChangeEvent event, Actor actor) { smartCam.minZoom = slMinZ.getValue(); }
			});
		root.add(slMinZ).fillX().row();

		VisTextButton btnShake = new VisTextButton("Add Trauma (Shake!)");
		btnShake.addListener(new ChangeListener() {
				@Override
				public void changed(ChangeEvent event, Actor actor) { smartCam.addTrauma(0.5f); }
			});
		root.add(btnShake).fillX().padTop(10).row();

		VisTextButton btnClear = new VisTextButton("Reset Targets");
		btnClear.addListener(new ChangeListener() {
				@Override
				public void changed(ChangeEvent event, Actor actor) {
					targets.clear();
					targets.add(new Vector2(-200, 0));
					targets.add(new Vector2(200, 0));
				}
			});
		root.add(btnClear).fillX().row();
	}

	private Drawable createColorDrawable(Color color) {
		Pixmap pixmap = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
		pixmap.setColor(color); pixmap.fill();
		Texture texture = new Texture(pixmap); pixmap.dispose();
		return new TextureRegionDrawable(new TextureRegion(texture));
	}

	@Override
	public void render0(float delta) {
		smartCam.update(targets, delta);

		if (!ghostMode) smartCam.apply();

		drawGrid();

		// 绘制目标点 (亮绿)
		shapeRenderer.setProjectionMatrix(getWorldCamera().combined);
		shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
		for (Vector2 t : targets) {
			if (t == dragTarget) shapeRenderer.setColor(Color.WHITE);
			else shapeRenderer.setColor(Color.GREEN);
			shapeRenderer.rect(t.x - 15, t.y - 15, 30, 30);
		}
		shapeRenderer.end();

		// [v3.3] 传入 batch 和 font 绘制标签
		if (ghostMode) {
			smartCam.drawDebug(shapeRenderer, batch, debugFont);
		}

		DebugUI.info("Targets: %d", targets.size());
		DebugUI.info("Cam Pos: %.0f, %.0f", smartCam.getCamera().position.x, smartCam.getCamera().position.y);
		DebugUI.info("Zoom: %.2f", smartCam.getCamera().zoom);

		uiStage.act(delta);
		uiStage.draw();
	}

	private void drawGrid() {
		shapeRenderer.setProjectionMatrix(getWorldCamera().combined);
		Gdx.gl.glEnable(GL20.GL_BLEND);
		shapeRenderer.begin(ShapeRenderer.ShapeType.Line);
		// 网格线稍微亮一点
		shapeRenderer.setColor(0.3f, 0.3f, 0.3f, 1f);
		int size = 100;
		for (int x = -1000; x <= 2000; x += size) shapeRenderer.line(x, -1000, x, 2000);
		for (int y = -1000; y <= 2000; y += size) shapeRenderer.line(-1000, y, 2000, y);
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
