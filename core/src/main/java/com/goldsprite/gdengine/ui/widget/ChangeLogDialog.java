package com.goldsprite.gdengine.ui.widget;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
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
import com.goldsprite.gdengine.core.utils.LogParser; // Import Parser
import com.goldsprite.gdengine.core.utils.LogParser.LogEntry; // Import Entry
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
	private final VisTree<LogNode, LogEntry> navTree;
	private final ObjectMap<String, LogNode> nodeMap = new ObjectMap<>();

	public ChangeLogDialog() {
		super("GDEngine 更新日志");
		top().left();

		// --- Data ---
		// [核心修改] 读取真实文件
		// 策略: 优先读项目根目录 docs (Dev环境)，其次读 assets/docs (Release环境)
		FileHandle file = Gdx.files.local("docs/ProjectHistoryLog.md");
		if (!file.exists()) {
			file = Gdx.files.internal("docs/changelog.md");
		}

		Array<LogEntry> data = LogParser.parse(file);

		// --- UI Init ---
		navTree = new VisTree<>();
		navTree.getSelection().setProgrammaticChangeEvents(true);
		navTree.setIndentSpacing(15f);

		if (data.size == 0) {
			navTree.add(new LogNode(new LogEntry("未找到日志文件", LogParser.EntryType.OVERVIEW)));
		} else {
			buildTree(data);
		}

		navTree.addListener(new ChangeListener() {
			@Override
			public void changed(ChangeEvent event, Actor actor) {
				LogNode selected = navTree.getSelection().first();
				if (selected != null) {
					renderContent(selected.getValue());
				}
			}
		});

		VisScrollPane navScroll = new VisScrollPane(navTree);
		navScroll.setFadeScrollBars(false);

		VisTable contentTable = new VisTable();
		contentTable.top().left().pad(20);
		contentText = new RichText("", 800);
		contentTable.add(contentText).growX().top();

		// 跳转监听
		contentText.addListener(new EventListener() {
			@Override
			public boolean handle(Event e) {
				if (e instanceof RichTextEvent) {
					String eventId = ((RichTextEvent) e).eventId;
					if (eventId.startsWith("goto:")) {
						// 这里的 targetTitle 需要匹配 MD 文件里的 "v1.10.x" 这种字符串
						String targetTitle = eventId.substring(5);
						navigateTo(targetTitle);
						return true;
					}
				}
				return false;
			}
		});

		VisScrollPane contentScroll = new VisScrollPane(contentTable);
		contentScroll.setFadeScrollBars(false);

		VisSplitPane split = new VisSplitPane(navScroll, contentScroll, false);
		split.setSplitAmount(0.25f);

		getContentTable().add(split).minWidth(0).grow();
		setFillParent(true);

		// 默认选中 Current 版本 (简单逻辑：包含当前版本号的节点)
		findAndSelectCurrent();
	}

	private void buildTree(Array<LogEntry> data) {
		// 使用 Map 暂存 Category 节点，以便将 Version 挂载上去
		// LogParser 已经帮我们标记了 parentCategory，这里只需要对应查找即可

		// 但由于 LogParser 返回的是扁平列表 (为了保持文件顺序)，我们需要一次遍历重建树
		// 或者 LogParser 已经处理好了 parent 引用? 是的。

		// 维护一个 Map: Entry -> Node，用于查找父节点 Node
		ObjectMap<LogEntry, LogNode> entryToNodeMap = new ObjectMap<>();

		for (LogEntry entry : data) {
			LogNode node = new LogNode(entry);
			entryToNodeMap.put(entry, node);

			// 索引，用于 goto
			// 简单处理：提取版本号部分作为 Key (例如 "v1.10.6.x")
			// 我们的 title 可能是 "v1.10.6.x (Current)"，需要模糊匹配或精确匹配
			// 这里直接用 Full Title 索引，跳转链接需写全名
			// 或者我们在 LogNode 里做包含匹配
			nodeMap.put(entry.title, node);

			if (entry.type == LogParser.EntryType.VERSION) {
				// 如果有父级，挂到父级下
				if (entry.parentCategory != null) {
					LogNode parentNode = entryToNodeMap.get(entry.parentCategory);
					if (parentNode != null) {
						parentNode.add(node);
						parentNode.setExpanded(true); // 默认展开
						continue;
					}
				}
			}

			// 否则作为根节点 (Overview, Category)
			navTree.add(node);
			if(entry.type == LogParser.EntryType.CATEGORY) {
				node.setExpanded(true);
			}
		}
	}

	private void renderContent(LogEntry entry) {
		String text = entry.content.toString();
		if (text.isEmpty()) {
			text = "[color=gray]（无详细内容）[/color]";
		}

		// 标题
		String header = "\n[size=38][color=cyan]" + entry.title + "[/color][/size]\n\n";

		contentText.setText(header + text);
		contentText.setWidth(this.getWidth() * 0.7f);
		contentText.layout();
		contentText.invalidateHierarchy();
	}

	private void findAndSelectCurrent() {
		// 尝试找到包含当前版本号的节点
		String curVer = BuildConfig.DEV_VERSION;
		for (ObjectMap.Entry<String, LogNode> entry : nodeMap.entries()) {
			if (entry.key.contains(curVer)) {
				navigateTo(entry.key);
				return;
			}
		}
		// 找不到就选第一个
		if (navTree.getNodes().size > 0) {
			navTree.getSelection().choose(navTree.getNodes().get(0));
		}
	}

	private void navigateTo(String title) {
		// 模糊搜索
		LogNode target = null;
		if (nodeMap.containsKey(title)) {
			target = nodeMap.get(title);
		} else {
			for (ObjectMap.Entry<String, LogNode> entry : nodeMap.entries()) {
				if (entry.key.contains(title)) { // 包含匹配
					target = entry.value;
					break;
				}
			}
		}

		if (target != null) {
			Node parent = target.getParent();
			while(parent != null) {
				parent.setExpanded(true);
				parent = parent.getParent();
			}
			navTree.getSelection().choose(target);
		}
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

	// --- Node ---
	private static class LogNode extends VisTree.Node<LogNode, LogEntry, VisLabel> {
		public LogNode(LogEntry v) {
			super(new VisLabel(v.title));
			setValue(v);
			getActor().setAlignment(Align.left);

			// 颜色逻辑
			if (v.title.contains("[Current]") || v.title.contains("当前")) getActor().setColor(Color.GREEN);
			else if (v.title.contains("[Plan]") || v.title.contains("规划")) getActor().setColor(Color.ORANGE);
			else if (v.type == LogParser.EntryType.OVERVIEW) getActor().setColor(Color.SKY);
			else if (v.type == LogParser.EntryType.CATEGORY) getActor().setColor(Color.LIGHT_GRAY);
		}
	}
}
