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
import com.goldsprite.solofight.core.DebugUI;
import com.goldsprite.solofight.core.SimpleCameraController;
import com.goldsprite.solofight.core.SmartCameraController;
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
	private SimpleCameraController freeCam;
	private ShapeRenderer shapeRenderer;
	private Stage uiStage;
	private SpriteBatch batch;
	private BitmapFont debugFont;

	private List<Vector2> targets = new ArrayList<>();
	private Vector2 dragTarget = null;

	// [v3.5] 开关分离
	private boolean ghostMode = true; // 是否不应用智能相机位置
	private boolean freeCamControl = false; // 是否启用自由相机输入
	private boolean drawBackground = false;
	
	@Override
    public ScreenManager.Orientation getOrientation() {
        return ScreenManager.Orientation.Landscape;
    }

	@Override
	public void create() {
		batch = new SpriteBatch();
		shapeRenderer = new ShapeRenderer();
		debugFont = new BitmapFont();

		smartCam = new SmartCameraController(getWorldCamera());
		smartCam.setMapBounds(-1000, -800, 2000, 1600);

		freeCam = new SimpleCameraController(getWorldCamera());
		freeCam.setInputEnabled(false); // 默认关闭自由操作

		targets.add(new Vector2(-200, 0));
		targets.add(new Vector2(200, 0));

		initUI();
		initInput();
	}

	private void initInput() {
		// [v3.5] 注册顺序: UI -> FreeCam -> Target
		getImp().addProcessor(freeCam);

		getImp().addProcessor(new InputAdapter() {
				@Override
				public boolean touchDown(int screenX, int screenY, int pointer, int button) {
					// [v3.5] 如果开启了自由相机控制，这里不处理目标逻辑
					if (freeCamControl) return false;

					Vector2 worldPos = screenToWorldCoord(screenX, screenY);
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
		uiStage = new Stage(getUIViewport());
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

		// 1. Ghost Mode
		VisCheckBox cbGhost = new VisCheckBox("Ghost Mode (Only Debug)", true);
		cbGhost.addListener(new ChangeListener() {
				@Override
				public void changed(ChangeEvent event, Actor actor) {
					ghostMode = cbGhost.isChecked();
					if (ghostMode && !freeCamControl) {
						// 仅当没有开启自由控制时，重置视角方便观察
						getWorldCamera().position.set(0, 0, 0);
						getWorldCamera().zoom = 1f;
						getWorldCamera().update();
					}
				}
			});
		panel.add(cbGhost).row();

		// [v3.5] Free Cam Control (分离的开关)
		VisCheckBox cbFree = new VisCheckBox("Free Cam Control", false);
		cbFree.addListener(new ChangeListener() {
				@Override
				public void changed(ChangeEvent event, Actor actor) {
					freeCamControl = cbFree.isChecked();
					freeCam.setInputEnabled(freeCamControl);
				}
			});
		panel.add(cbFree).row();

		// [v3.5] Map Constraint
		VisCheckBox cbMap = new VisCheckBox("Map Constraint", false);
		cbMap.addListener(new ChangeListener() {
				@Override
				public void changed(ChangeEvent event, Actor actor) {
					smartCam.mapConstraint = cbMap.isChecked();
				}
			});
		panel.add(cbMap).row();

		// [v3.5] Draw Bg
		VisCheckBox cbBg = new VisCheckBox("Draw Backgrounds", false);
		cbBg.addListener(new ChangeListener() {
				@Override
				public void changed(ChangeEvent event, Actor actor) { drawBackground = cbBg.isChecked(); }
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
		smartCam.update(targets, delta);

		if (freeCamControl) {
			freeCam.update(delta); // 自由控制时，应用 FreeCam 逻辑
		} else if (!ghostMode) {
			smartCam.apply(); // 非幽灵模式，应用 SmartCam 逻辑
		}

		drawGrid();

		// Targets
		shapeRenderer.setProjectionMatrix(getWorldCamera().combined);
		shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
		for (Vector2 t : targets) {
			if (t == dragTarget) shapeRenderer.setColor(Color.WHITE);
			else shapeRenderer.setColor(Color.GREEN);
			shapeRenderer.rect(t.x - 15, t.y - 15, 30, 30);
		}
		shapeRenderer.end();

		if (ghostMode) {
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
		for (int x = -2000; x <= 3000; x += size) shapeRenderer.line(x, -2000, x, 3000);
		for (int y = -2000; y <= 3000; y += size) shapeRenderer.line(-2000, y, 3000, y);
		shapeRenderer.setColor(Color.ORANGE);
		// 画地图边界
		shapeRenderer.rect(-1000, -800, 2000, 1600); // 必须和 SmartCam 里的设置一致
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
