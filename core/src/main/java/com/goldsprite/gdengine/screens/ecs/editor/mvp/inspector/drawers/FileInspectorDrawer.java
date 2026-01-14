package com.goldsprite.gdengine.screens.ecs.editor.mvp.inspector.drawers;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Align;
import com.kotcrab.vis.ui.widget.VisLabel;
import com.kotcrab.vis.ui.widget.VisScrollPane;
import com.kotcrab.vis.ui.widget.VisTable;

public class FileInspectorDrawer implements IInspectorDrawer<FileHandle> {

	@Override
	public boolean accept(Object target) {
		return target instanceof FileHandle;
	}

	@Override
	public void draw(FileHandle file, VisTable container) {
		container.defaults().pad(5).left();

		// 1. Header Info
		VisTable header = new VisTable();
		header.setBackground("panel1");

		header.add(new VisLabel("File: " + file.name())).growX().row();
		VisLabel pathLabel = new VisLabel(file.path());
		pathLabel.setColor(Color.GRAY);
		pathLabel.setWrap(true);
		header.add(pathLabel).growX().row();
		header.add(new VisLabel("Size: " + file.length() + " bytes")).row();

		container.add(header).growX().row();

		// 2. Content Preview
		String ext = file.extension().toLowerCase();

		if (ext.equals("png") || ext.equals("jpg")) {
			showImagePreview(file, container);
		} else if (ext.equals("java") || ext.equals("json") || ext.equals("xml") || ext.equals("scene")) {
			showTextPreview(file, container);
		} else {
			VisLabel lbl = new VisLabel("No preview available.");
			lbl.setAlignment(Align.center);
			container.add(lbl).grow().pad(20);
		}
	}

	private void showImagePreview(FileHandle file, VisTable container) {
		try {
			Texture tex = new Texture(file);
			Image img = new Image(new TextureRegionDrawable(new TextureRegion(tex)));
			img.setScaling(com.badlogic.gdx.utils.Scaling.fit);
			container.add(img).grow().maxHeight(300).pad(10);
		} catch (Exception e) {
			container.add(new VisLabel("Image load error")).pad(20);
		}
	}

	private void showTextPreview(FileHandle file, VisTable container) {
		String content = file.readString("UTF-8");
		// 截取前 2000 字符防止卡顿
		if (content.length() > 2000) content = content.substring(0, 2000) + "\n... (Truncated)";

		VisLabel codeLabel = new VisLabel(content);
		codeLabel.setColor(Color.LIGHT_GRAY);
		codeLabel.setAlignment(Align.topLeft);

		// [优化 1] 允许 VisLabel 不自动换行，从而支持横向滚动 (代码查看器通常不软换行)
		// 如果你喜欢软换行，可以保留 setWrap(true) 并只开启 Y 轴滚动
		codeLabel.setWrap(false);

		VisScrollPane scroll = new VisScrollPane(codeLabel);
		scroll.setFadeScrollBars(false);
		// [优化 1] 允许双向滚动
		scroll.setScrollingDisabled(false, false);
		// 允许拖拽滚动 (提升手感)
		scroll.setFlickScroll(true);

		// [优化 2] 缩放逻辑
		scroll.addListener(new InputListener() {
			@Override
			public boolean scrolled(InputEvent event, float x, float y, float amountX, float amountY) {
				// 按住 Ctrl 且滚动, TODO: 这里硬编码, 之后需要优化
				if (Gdx.input.isKeyPressed(Input.Keys.CONTROL_LEFT) || Gdx.input.isKeyPressed(Input.Keys.CONTROL_RIGHT)) {
					float scale = codeLabel.getFontScaleX();
					// amountY: 向上滚是 -1，向下滚是 1 (LibGDX 默认)
					// 向下滚(缩小) -> scale 减小
					scale -= amountY * 0.1f;

					// 限制范围
					if (scale < 0.5f) scale = 0.5f;
					if (scale > 3.0f) scale = 3.0f;

					codeLabel.setFontScale(scale);
					scroll.invalidateHierarchy(); // 触发布局刷新 (Scroll需重新计算范围)
					return true; // 消费事件，防止 ScrollPane 滚动
				}
				return false;
			}

			// 必须让 ScrollPane 能够接收键盘修饰键，通常需要 Focus
			// 这里我们偷懒，直接检测 Gdx.input，不需要 Focus
		});

		container.add(scroll).grow().pad(5);
	}
}
