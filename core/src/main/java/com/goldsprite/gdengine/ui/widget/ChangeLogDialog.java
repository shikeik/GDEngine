package com.goldsprite.gdengine.ui.widget;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Event;
import com.badlogic.gdx.scenes.scene2d.EventListener;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Tree.Node;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.ObjectMap;
import com.goldsprite.gdengine.BuildConfig;
import com.goldsprite.gdengine.ui.widget.richtext.RichText;
import com.goldsprite.gdengine.ui.widget.richtext.RichTextEvent;
import com.kotcrab.vis.ui.widget.VisDialog;
import com.kotcrab.vis.ui.widget.VisLabel;
import com.kotcrab.vis.ui.widget.VisScrollPane;
import com.kotcrab.vis.ui.widget.VisSplitPane;
import com.kotcrab.vis.ui.widget.VisTable;
import com.kotcrab.vis.ui.widget.VisTree;

public class ChangeLogDialog extends BaseDialog {

	private final RichText contentText;
	private final VisTree<LogNode, VersionMock> navTree;
	private final VisScrollPane navScroll;

	// [新增] 快速查找表 (Title -> Node) 用于跳转
	private final ObjectMap<String, LogNode> nodeMap = new ObjectMap<>();

	public ChangeLogDialog() {
		super("GDEngine 更新日志");

//		debugAll();

		top().left();

		// --- Data & Tree Construction ---
		navTree = new VisTree<>();
		navTree.getSelection().setProgrammaticChangeEvents(true); // 允许代码触发选中事件
		navTree.setIndentSpacing(15f); // [可选] 减小缩进间距 (默认可能是 20 或更多)

		buildTreeData();

		// 选中监听 (Tree Selection -> Render Content)
		navTree.addListener(new ChangeListener() {
			@Override
			public void changed(ChangeEvent event, Actor actor) {
				LogNode selected = navTree.getSelection().first();
				if (selected != null) {
					renderContent(selected.getValue());
				}
			}
		});

		navScroll = new VisScrollPane(navTree);
		navScroll.setFadeScrollBars(false);

		// --- Right: Content ---
		VisTable contentTable = new VisTable();
		contentTable.top().left().pad(20);

		contentText = new RichText("", 800);
		contentTable.add(contentText).growX().top();

		// [新增] 监听富文本内部的跳转事件
		contentText.addListener(new EventListener() {
			@Override
			public boolean handle(Event e) {
				if (e instanceof RichTextEvent) {
					String eventId = ((RichTextEvent) e).eventId;
					if (eventId.startsWith("goto:")) {
						String targetTitle = eventId.substring(5); // "goto:v1.0" -> "v1.0"
						navigateTo(targetTitle);
						return true;
					}
				}
				return false;
			}
		});

		VisScrollPane contentScroll = new VisScrollPane(contentTable);
		contentScroll.setFadeScrollBars(false);

		// --- Layout ---
		VisSplitPane split = new VisSplitPane(navScroll, contentScroll, false);
		split.setSplitAmount(0.25f);

		getContentTable().add(split).minWidth(0).grow();
		setFillParent(true);
		invalidate();

		// 默认选中 Current
		navigateTo("v" + BuildConfig.DEV_VERSION);
	}

	/** 核心跳转逻辑 */
	private void navigateTo(String title) {
		LogNode target = nodeMap.get(title);
		if (target != null) {
			// 1. 展开所有父节点
			Node parent = target.getParent();
			while(parent != null) {
				parent.setExpanded(true);
				parent = parent.getParent();
			}
			// 2. 选中节点
			navTree.getSelection().choose(target);

			// 3. 滚动到可见区域 (简单实现：先不做复杂的Y计算，VisTree的自动布局通常能处理大部分情况)
			// 如果需要强制滚动，可以计算 node 的 Y 坐标并设置 navScroll.setScrollY
		}
	}

	private void buildTreeData() {
		Array<VersionMock> data = getMockData();

		// 1. 根节点 (概览)
		VersionMock overview = findMock(data, VersionType.OVERVIEW);
		LogNode rootNode = createNode(overview);
		navTree.add(rootNode);
		rootNode.setExpanded(true);

		// 2. 规划节点 (Plan Category)
		LogNode planGroup = new LogNode(new VersionMock("未来规划", VersionType.CATEGORY, ""));
		rootNode.add(planGroup);
		planGroup.setExpanded(true);

		for(VersionMock v : data) {
			if(v.type == VersionType.PLAN) planGroup.add(createNode(v));
		}

		// 3. 发布节点 (Releases Category)
		LogNode releaseGroup = new LogNode(new VersionMock("版本发布", VersionType.CATEGORY, ""));
		rootNode.add(releaseGroup);
		releaseGroup.setExpanded(true);

		for(VersionMock v : data) {
			if(v.type == VersionType.CURRENT || v.type == VersionType.HISTORY) {
				releaseGroup.add(createNode(v));
			}
		}
	}

	private LogNode createNode(VersionMock v) {
		if (v == null) return new LogNode(new VersionMock("Error", VersionType.HISTORY, ""));
		LogNode node = new LogNode(v);
		// 索引版本号 (用于 goto 跳转)
		// 注意: 我们用 mock 数据的 title (例如 "v1.10.5") 作为 key
		// 但 VersionMock.toString() 加了 emoji，这里我们只取原始 title 索引
		if (v.title != null) {
			nodeMap.put(v.title, node);
		}
		return node;
	}

	private VersionMock findMock(Array<VersionMock> data, VersionType type) {
		for(VersionMock v : data) if(v.type == type) return v;
		return null;
	}

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
		// Category 节点不显示内容或显示默认提示
		if (v.type == VersionType.CATEGORY) return;

		String raw = v.content;

		String rich = raw
			.replaceAll("## (.*)", "\n[size=38][color=cyan]$1[/color][/size]\n")
			.replaceAll("- \\[ \\]", "[color=gray]□[/color]")
			.replaceAll("- \\[x\\]", "[color=green]■[/color]")
			.replaceAll("\\[New\\]", "[color=green][New][/color]")
			.replaceAll("\\[Fix\\]", "[color=salmon][Fix][/color]")
			.replaceAll("\\[Adj\\]", "[color=gold][Adj][/color]")
			.replaceAll("\\[Plan\\]", "[color=slate][Plan][/color]")
			.replaceAll("\\[Refactor\\]", "[color=orange][Refactor][/color]");

		contentText.setText(rich);
		contentText.setWidth(this.getWidth() * 0.7f);
		contentText.layout();
		contentText.invalidateHierarchy();
	}

	// --- Structures ---

	private enum VersionType { OVERVIEW, CATEGORY, PLAN, CURRENT, HISTORY }

	private static class VersionMock {
		String title;
		VersionType type;
		String content;

		public VersionMock(String t, VersionType type, String c) {
			this.title = t; this.type = type; this.content = c;
		}
		@Override public String toString() {
			if(type == VersionType.OVERVIEW) return "📘 " + title;
			if(type == VersionType.CATEGORY) return "📂 " + title;
			if(type == VersionType.CURRENT) return "🌟 " + title + " (当前)";
			if(type == VersionType.PLAN) return "🚀 " + title;
			return title; // History
		}
	}

	// 自定义树节点
	private static class LogNode extends VisTree.Node<LogNode, VersionMock, VisLabel> {
		public LogNode(VersionMock v) {
			super(new VisLabel(v.toString()));
			setValue(v);

			// [核心修改] 强制设置 Label 内部文字左对齐, 没用, 先算了
			getActor().setAlignment(Align.left);

			// 针对不同类型设置颜色
			if (v.type == VersionType.CURRENT) getActor().setColor(Color.GREEN);
			else if (v.type == VersionType.PLAN) getActor().setColor(Color.ORANGE);
			else if (v.type == VersionType.CATEGORY) getActor().setColor(Color.LIGHT_GRAY);
		}
	}

	private Array<VersionMock> getMockData() {
		Array<VersionMock> list = new Array<>();

		// [修改] 在总览里添加跳转链接测试
		String curVer = "v" + BuildConfig.DEV_VERSION;
		list.add(new VersionMock("引擎总览", VersionType.OVERVIEW,
			"## GDEngine 引擎总览\n\n" +
				"GDEngine 是一个基于 LibGDX 的现代化 2D 游戏引擎。\n" +
				"旨在提供类似 Unity 的开发体验，同时保持轻量级。\n\n" +
				"👉 [event=goto:" + curVer + "][color=gold]点击查看当前版本更新详情[/color][/event]\n\n" +
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

		list.add(new VersionMock("v1.10.9 (Prefab)", VersionType.PLAN,
			"## [v1.10.9] 资源与预制体\n\n" +
				"解决硬编码路径痛点，实现拖拽式资源管理。\n\n" +
				"## 待办事项\n" +
				"- [ ] [New] 资源浏览器 (Project Window)\n" +
				"- [ ] [New] 资源拖拽绑定 (Drag & Drop Assets)\n" +
				"- [ ] [New] 预制体系统 (.prefab 序列化与实例化)"
		));

		list.add(new VersionMock("v1.10.8 (Physics)", VersionType.PLAN,
			"## [v1.10.8] 物理系统集成\n\n" +
				"引入 Box2D，打通 ECS 物理管线。\n\n" +
				"## 待办事项\n" +
				"- [ ] [New] PhysicsSystem (FixedUpdate 驱动)\n" +
				"- [ ] [New] RigidBodyComponent (刚体)\n" +
				"- [ ] [New] ColliderComponent (碰撞体 Gizmo 可视化)"
		));

		list.add(new VersionMock("v1.10.7 (UX)", VersionType.PLAN,
			"## [v1.10.7] 交互与信息优化\n\n" +
				"提升编辑器易用性，实装日志系统。\n\n" +
				"## 待办事项\n" +
				"- [ ] [New] 实装日志读取器 (解析 .md 文件)\n" +
				"- [ ] [Adj] Inspector 体验优化 (数值拖拽变速)\n" +
				"- [ ] [Fix] Hierarchy 拖拽排序视觉反馈"
		));

		list.add(new VersionMock(curVer, VersionType.CURRENT,
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
