package com.goldsprite.solofight.ui.window;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.goldsprite.gdengine.assets.ColorTextureUtils;
import com.goldsprite.gdengine.assets.FontUtils;
import com.goldsprite.solofight.modules.TextDB;
import com.kotcrab.vis.ui.widget.VisImageTextButton;
import com.kotcrab.vis.ui.widget.VisTextButton;
import com.kotcrab.vis.ui.widget.VisWindow;

public class HelpWindow extends VisWindow {

	private VisTextButton btnTabCtrl, btnTabMoves;
	private Table contentArea;
	private VisTextButton btnClose;

	private static final Color COL_ACTIVE = Color.valueOf("00eaff");
	private static final Color COL_INACTIVE = Color.GRAY;

	public HelpWindow() {
		super("");
		setBackground(ColorTextureUtils.createColorDrawable(new Color(0.1f, 0.1f, 0.1f, 0.95f)));
		pad(2);

		Table root = new Table();
		root.setBackground(ColorTextureUtils.createColorDrawable(Color.valueOf("1a1a1a")));
		add(root).grow();

		// 1. Tabs
		Table tabs = new Table();
		btnTabCtrl = createTabBtn("tab_ctrl", true);
		btnTabMoves = createTabBtn("tab_moves", false);

		tabs.add(btnTabCtrl).growX().height(50);
		tabs.add(btnTabMoves).growX().height(50);
		root.add(tabs).growX().row();

		// 分隔线
		root.add(createSeparator()).height(2).growX().row();

		// 2. Content
		contentArea = new Table();
		contentArea.pad(20);
		root.add(contentArea).grow().minHeight(300).row();

		// 3. Footer
		btnClose = new VisTextButton("", "default");
		btnClose.getLabel().setColor(Color.GRAY);
		btnClose.addListener(new ClickListener() {
			@Override
			public void clicked(InputEvent event, float x, float y) {
				setVisible(false);
			}
		});
		root.add(btnClose).growX().height(40).pad(5);

		showTab(true);
		refreshLang();

		setSize(600, 500);
		setModal(true);
		centerWindow();
	}

	// [新增] 辅助方法：创建带颜色的分隔线 Actor
	private VisImageTextButton createSeparator() {
		VisImageTextButton sep = new VisImageTextButton("", "default");
		sep.setColor(Color.DARK_GRAY);
		return sep; // 返回 Actor 供 Table 添加
	}

	private VisTextButton createTabBtn(String langKey, boolean isActive) {
		VisTextButton btn = new VisTextButton("");
		btn.setUserObject(langKey);
		btn.addListener(new ClickListener() {
			@Override
			public void clicked(InputEvent event, float x, float y) {
				showTab(btn == btnTabCtrl);
			}
		});
		return btn;
	}

	private void showTab(boolean showCtrl) {
		updateBtnStyle(btnTabCtrl, showCtrl);
		updateBtnStyle(btnTabMoves, !showCtrl);
		contentArea.clear();
		if (showCtrl) buildControlsTable();
		else buildMovesTable();
	}

	private void updateBtnStyle(VisTextButton btn, boolean active) {
		btn.getLabel().setColor(active ? COL_ACTIVE : COL_INACTIVE);
	}

	public void refreshLang() {
		btnTabCtrl.setText(TextDB.get("tab_ctrl"));
		btnTabMoves.setText(TextDB.get("tab_moves"));
		btnClose.setText(TextDB.get("btn_close"));
		boolean isCtrl = btnTabCtrl.getLabel().getColor().equals(COL_ACTIVE);
		showTab(isCtrl);
	}

	private void buildControlsTable() {
		addHeaderRow("th_action", "th_key", "th_touch");
		addRow("act_move", "W A S D", "in_stick");
		addRow("act_jump", "K", "in_swipe_u");
		addRow("act_atk", "J", "in_tap");
		addRow("act_ult", "O", "in_swipe_d");
	}

	private void buildMovesTable() {
		addHeaderRow("th_skill", "th_key", "th_touch");
		addRow("skl_dash_l", "key_dash_l", "tch_dash_l");
		addRow("skl_dash_r", "key_dash_r", "tch_dash_r");
		addRow("skl_dash_a", "key_dash_a", "tch_dash_a");
		addRow("skl_flash", "key_flash", "tch_flash", true);
	}

	// [修改] 重写行构建逻辑，使用真正的 Table Column
	private void addHeaderRow(String k1, String k2, String k3) {
		Table row = new Table();
		// 设置固定宽度或权重，确保对齐
		// Action: 20%, Key: 40%, Touch: 40%
		Label l1 = new Label(TextDB.get(k1), new Label.LabelStyle(FontUtils.generate(18), COL_ACTIVE));
		Label l2 = new Label(TextDB.get(k2), new Label.LabelStyle(FontUtils.generate(18), COL_ACTIVE));
		Label l3 = new Label(TextDB.get(k3), new Label.LabelStyle(FontUtils.generate(18), COL_ACTIVE));

		row.add(l1).expandX().width(120).left();
		row.add(l2).expandX().width(200).left();
		row.add(l3).expandX().width(200).left();

		contentArea.add(row).growX().padBottom(5).row();
		contentArea.add(createSeparator()).height(1).growX().padBottom(5).row();
	}

	private void addRow(String k1, String k2, String k3) {
		addRow(k1, k2, k3, false);
	}

	private void addRow(String k1, String k2, String k3, boolean special) {
		Table row = new Table();

		Color c = special ? Color.valueOf("ffeb3b") : Color.LIGHT_GRAY;
		String t1 = TextDB.get(k1);
		String t2 = TextDB.get(k2); // 已经是翻译后的值了? 不，是 Key
		// key_dash_l 是 Key，需要 TextDB.get
		// W A S D 不是 Key，直接显示
		// 简单判断：如果 TextDB 有值则用，否则原样

		row.add(new Label(t1, new Label.LabelStyle(FontUtils.generate(14), c))).expandX().width(120).left();
		row.add(new Label(safeGet(k2), new Label.LabelStyle(FontUtils.generate(14), Color.WHITE))).expandX().width(200).left();
		row.add(new Label(safeGet(k3), new Label.LabelStyle(FontUtils.generate(14), Color.WHITE))).expandX().width(200).left();

		contentArea.add(row).growX().padBottom(5).row();
		contentArea.add(createSeparator()).height(1).growX().padBottom(5).row();
	}

	private String safeGet(String key) {
		String val = TextDB.get(key);
		return val.equals(key) ? key : val; // 如果没翻译，返回原Key(针对 WASD 这种硬编码)
	}
}
