package com.goldsprite.gdengine.ui.widget;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.utils.Array;
import com.goldsprite.gdengine.BuildConfig;
import com.goldsprite.gdengine.ui.widget.richtext.RichText;
import com.kotcrab.vis.ui.widget.VisDialog;
import com.kotcrab.vis.ui.widget.VisList;
import com.kotcrab.vis.ui.widget.VisScrollPane;
import com.kotcrab.vis.ui.widget.VisSplitPane;
import com.kotcrab.vis.ui.widget.VisTable;

public class ChangeLogDialog extends BaseDialog {

	private final RichText contentText;
	private final VisList<VersionMock> navList;

	public ChangeLogDialog() {
		super("GDEngine 更新日志");

		debugAll();

		top().left();
		clear();

		// --- Data ---
		Array<VersionMock> data = getMockData();

		// --- Left: Nav ---
		navList = new VisList<>();
		navList.setItems(data);

		// 选中监听
		navList.addListener(new ChangeListener() {
			@Override
			public void changed(ChangeEvent event, Actor actor) {
				VersionMock selected = navList.getSelected();
				if (selected != null) {
					renderContent(selected);
				}
			}
		});

		VisScrollPane navScroll = new VisScrollPane(navList);
		navScroll.setFadeScrollBars(false);

		// --- Right: Content ---
		VisTable contentTable = new VisTable();
		contentTable.top().left().pad(20);

		// 这里的宽先给个默认值，layout时会自动调整
		contentText = new RichText("", 800);
		contentTable.add(contentText).growX().top();

		VisScrollPane contentScroll = new VisScrollPane(contentTable);
		contentScroll.setFadeScrollBars(false);

		// --- Layout ---
		VisSplitPane split = new VisSplitPane(navScroll, contentScroll, false);
		split.setSplitAmount(0.25f); // 左侧 25%

		add(split).minWidth(0).grow();
		setFillParent(true);
		invalidate();

		// 默认选中当前版本
		for(VersionMock v : data) {
			if(v.type == VersionType.CURRENT) {
				navList.setSelected(v);
				renderContent(v);
				break;
			}
		}
	}

	// [核心修改] 动态全屏尺寸
	@Override
	public VisDialog show(Stage stage) {
		float margin = 50f;
		float w = stage.getWidth() - margin * 2;
		float h = stage.getHeight() - margin * 2;

		setSize(w, h);
		centerWindow();

		stage.addActor(this.fadeIn());
		return this;
	}

	private void renderContent(VersionMock v) {
		String raw = v.content;

		// 简单的 Markdown -> RichText 转换器 (Mock用)
		String rich = raw
			// 标题
			.replaceAll("## (.*)", "\n[size=38][color=cyan]$1[/color][/size]\n")
			// 列表项
			.replaceAll("- \\[ \\]", "[color=gray]□[/color]")
			.replaceAll("- \\[x\\]", "[color=green]■[/color]")
			// 标签着色
			.replaceAll("\\[New\\]", "[color=green][New][/color]")
			.replaceAll("\\[Fix\\]", "[color=salmon][Fix][/color]")
			.replaceAll("\\[Adj\\]", "[color=gold][Adj][/color]")
			.replaceAll("\\[Plan\\]", "[color=slate][Plan][/color]")
			.replaceAll("\\[Refactor\\]", "[color=orange][Refactor][/color]");

		// 更新 RichText
		// 注意: 为了自适应宽度，我们需要在 resize 后重新计算，但这里简化处理，直接设文本
		// RichText 在 layout() 时会自动根据父容器宽度计算换行
		contentText.setText(rich);
		contentText.setWidth(this.getWidth() * 0.7f); // 估算右侧宽度
		contentText.layout();
		contentText.invalidateHierarchy();
	}

	// --- Mock Data Structures ---

	private enum VersionType { OVERVIEW, PLAN, CURRENT, HISTORY }

	private static class VersionMock {
		String title;
		VersionType type;
		String content;

		public VersionMock(String t, VersionType type, String c) {
			this.title = t; this.type = type; this.content = c;
		}
		@Override public String toString() {
			if(type == VersionType.CURRENT) return "🌟 " + title;
			if(type == VersionType.PLAN) return "🚀 " + title;
			if(type == VersionType.OVERVIEW) return "📘 " + title;
			return "   " + title;
		}
	}

	private Array<VersionMock> getMockData() {
		Array<VersionMock> list = new Array<>();

		list.add(new VersionMock("引擎总览", VersionType.OVERVIEW,
			"## GDEngine 引擎总览\n\n" +
				"GDEngine 是一个基于 LibGDX 的现代化 2D 游戏引擎。\n" +
				"旨在提供类似 Unity 的开发体验，同时保持轻量级。\n\n" +
				"## 核心特性\n" +
				"- [x] ECS 架构 (Entity-Component-System)\n" +
				"- [x] 统一渲染管线 (WorldRenderSystem + LayerManager)\n" +
				"- [x] 可视化编辑器 (Scene/Inspector/Gizmo)\n" +
				"- [x] 热重载脚本支持 (Hot-Reload)\n" +
				"- [x] 骨骼动画系统 (NeonSkeleton + JSON Live Edit)"
		));

		list.add(new VersionMock("v1.11.0 (Next)", VersionType.PLAN,
			"## [v1.11.0] 下版本规划\n\n" +
				"画大饼时间，这里列出即将到来的功能。\n\n" +
				"## 待办事项\n" +
				"- [ ] [New] 物理系统可视化编辑器 (Box2D Gizmos)\n" +
				"- [ ] [New] 预制体系统 (Prefab) 支持\n" +
				"- [ ] [Adj] 优化资源加载流程 (AssetManager 集成)\n" +
				"- [ ] [New] UI 编辑器 (VisUI 可视化配置)"
		));

		list.add(new VersionMock("v" + BuildConfig.DEV_VERSION, VersionType.CURRENT,
			"## [" + BuildConfig.DEV_VERSION + "] 当前版本\n\n" +
				"本次更新重点重构了渲染底层，解决了长期存在的遮挡和选中问题。\n\n" +
				"## 变更日志\n" +
				"- [Refactor] **统一渲染管线**: 引入 `WorldRenderSystem`，替代了散乱的 Sprite/Skeleton 系统。\n" +
				"- [New] **层级管理**: 引入 `RenderLayerManager`，支持自定义 Sorting Layer 和 Depth。\n" +
				"- [New] **渲染组件基类**: `RenderComponent` 统一了所有可渲染对象的接口。\n" +
				"- [Fix] **编辑器交互**: 修复相机拖拽不跟手的问题，实现 1:1 精准漫游。\n" +
				"- [Fix] **精准选中**: 基于渲染层级的倒序检测，现在点击重叠物体时，会准确选中最上面的那个。\n" +
				"- [Fix] **Gizmo 优化**: 修复缩放手柄手感，增加中心等比缩放，完善视觉反馈。\n" +
				"- [Adj] **系统架构**: GameWorld 分离 Update(逻辑) 与 Render(渲染) 循环。"
		));

		list.add(new VersionMock("v1.10.5", VersionType.HISTORY,
			"## [v1.10.5] 历史归档\n\n" +
				"## 变更日志\n" +
				"- [New] 骨骼动画 JSON 实时编辑功能 (Live Edit)\n" +
				"- [Fix] 解决 Android 端权限申请流程卡死问题\n" +
				"- [Adj] 优化 VisUI 字体显示效果 (支持中文)\n" +
				"- [New] 增加 `RotorComponent` 示例组件"
		));

		return list;
	}
}
