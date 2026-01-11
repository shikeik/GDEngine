package com.goldsprite.solofight.ui.widget;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Group;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.WidgetGroup;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.goldsprite.gdengine.assets.FontUtils;
import com.goldsprite.gdengine.core.command.CommandManager;
import com.goldsprite.gdengine.core.command.ICommand;

import java.util.ArrayList;
import java.util.List;

/**
 * 指令历史 UI (v4.0 重设计版)
 * 改进：
 * 1. 移动到右下角
 * 2. 当前节点亮色，其他节点暗色
 * 3. 滑动布局
 * 4. 半透明白净背景
 * 5. 无最大显示数量限制
 * 6. 点击历史节点可切换操作历史
 * 7. 可展开/收起面板
 */
public class CommandHistoryUI extends WidgetGroup {
	private static final float ITEM_WIDTH = 220; // 加宽以容纳更多内容
	private static final float ITEM_HEIGHT = 28; // 增加高度提升可读性
	private static final float PANEL_WIDTH = 250; // 面板宽度
	private static final float MAX_PANEL_HEIGHT = 400; // 最大面板高度
	private static final float COLLAPSE_BUTTON_SIZE = 24; // 展开/收起按钮大小

	// 静态资源
	private static TextureRegion whitePixel;
	private static Label.LabelStyle styleItem, styleCurrentItem;
	private static boolean isInit = false;

	// 颜色常量
	private final Color COL_BG_PANEL = new Color(1, 1, 1, 1f); // 半透明白色背景
	private final Color COL_BORDER = new Color(0.3f, 0.3f, 0.3f, 1f); // 边框颜色
	private final Color COL_ITEM_NORMAL = new Color(0, 0, 0, 1); // 普通项文字颜色（深色以便在白色背景上显示）
	private final Color COL_ITEM_CURRENT = new Color(0, 0, 1, 1); // 当前项文字颜色（更深色）
	private final Color COL_ITEM_BACKGROUND = new Color(1, 1, 1, 1f); // 项背景色（白色半透明）
	private final Color COL_ITEM_CURRENT_BACKGROUND = new Color(1, 1, 1, 1f); // 当前项背景色（白色半透明，更不透明）

	// 组件
	private Table contentTable;
	private ScrollPane scrollPane;
	private Table container;
	private Actor collapseButton;
	private boolean isExpanded = true;
	
	private boolean autoPosition = true;

	// 历史数据
	private List<HistoryItem> historyItems = new ArrayList<>();
	private int currentHistoryIndex = -1;
	private CommandManager commandManager;
	private com.badlogic.gdx.scenes.scene2d.Actor inspectorPanel;

	public CommandHistoryUI() {
		this(null, null);
	}

	public CommandHistoryUI(CommandManager commandManager) {
		this(commandManager, null);
	}

	public CommandHistoryUI(CommandManager commandManager, com.badlogic.gdx.scenes.scene2d.Actor inspectorPanel) {
		this.commandManager = commandManager;
		this.inspectorPanel = inspectorPanel;
		initResources();
		initUI();
		initListeners();

		// 加载已有的历史命令
		loadExistingHistory();

		// 注册命令管理器监听器
		if (commandManager != null) {
			commandManager.addListener(new CommandManager.CommandListener() {
				@Override
				public void onCommandExecuted(ICommand cmd) {
					// 添加新的历史项
					String type = "raw";
					if(cmd.getSource().equals("Gizmo")) type = "move";
					addHistory("CMD_" + cmd.getName(), cmd.getSource(), type, cmd.getIcon());
				}

				@Override
				public void onUndo(ICommand cmd) {
					// 重新加载历史命令，不更新当前索引
					loadExistingHistory();
				}

				@Override
				public void onRedo(ICommand cmd) {
					// 重新加载历史命令，不更新当前索引
					loadExistingHistory();
				}

				@Override
				public void onHistoryChanged() {
					// 历史变化时重新加载所有历史命令
					loadExistingHistory();
				}

				@Override
				public void onHistoryNavigated(int position) {
					// 当导航到特定历史位置时更新索引
					updateCurrentIndex(position);
				}
			});
		}
	}

	/**
	 * 加载CommandManager中已有的历史命令
	 */
	private void loadExistingHistory() {
		if (commandManager == null) return;

		// 清空当前历史项
		historyItems.clear();
		contentTable.clear();

		// 获取所有历史命令（包括undo和redo栈中的命令）
		List<ICommand> commands = commandManager.getAllHistoryCommands();

		// 为每个命令创建历史项，不更新当前索引
		for (int i = 0; i < commands.size(); i++) {
			ICommand cmd = commands.get(i);
			String type = "raw";
			if(cmd.getSource().equals("Gizmo")) type = "move";
			addHistory("CMD_" + cmd.getName(), cmd.getSource(), type, cmd.getIcon(), false);
		}

		// 设置当前历史索引
		currentHistoryIndex = commandManager.getUndoStackSize() - 1;
		updateItemStates();
	}

	private void initResources() {
		if (isInit) return;

		// 1. 生成 1x1 白点用于绘制
		Pixmap p = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
		p.setColor(new Color(1, 1, 1, 1)); // 使用自己的白色副本，避免全局污染
		p.fill();
		whitePixel = new TextureRegion(new Texture(p));
		p.dispose();

		// 2. 字体样式
		BitmapFont font = FontUtils.generate(14);

		styleItem = new Label.LabelStyle(font, COL_ITEM_NORMAL);
		styleCurrentItem = new Label.LabelStyle(font, COL_ITEM_CURRENT);

		isInit = true;
	}

	private void initUI() {
		// 创建容器
		container = new Table();
		container.setSize(PANEL_WIDTH, MAX_PANEL_HEIGHT);
		container.setPosition(0, 0);

		// 创建内容表格
		contentTable = new Table();
		contentTable.top().left();

		// 创建滚动面板
		scrollPane = new ScrollPane(contentTable);
		scrollPane.setOverscroll(false, false);
		scrollPane.setFadeScrollBars(false); // 禁用滑动条淡出效果，使其始终可见
		scrollPane.setScrollbarsVisible(true); // 确保滑动条始终可见
		scrollPane.setForceScroll(false, true); // 禁用水平滚动，启用垂直滚动
		scrollPane.setFlickScroll(true); // 禁用拖拽滑动
		scrollPane.setClamp(true); // 确保内容不会超出边界
		scrollPane.setTouchable(Touchable.childrenOnly); // 只允许子组件接收触摸事件，禁用面板拖拽

		// 创建展开/收起按钮
		collapseButton = createCollapseButton();

		// 添加到容器
		container.add(collapseButton).width(COLLAPSE_BUTTON_SIZE).height(COLLAPSE_BUTTON_SIZE).pad(5).top().right();
		container.row();
		container.add(scrollPane).grow().pad(5);

		// 添加容器到WidgetGroup
		addActor(container);

		// 设置WidgetGroup大小
		setSize(PANEL_WIDTH, MAX_PANEL_HEIGHT);

		// 定位到检查器面板的左边下方
		updatePosition();
	}

	/**
	 * 更新面板位置，使其附着于检查器面板的左边下方
	 */
	private void updatePosition() {
		// 如果没有检查器面板引用，使用默认位置
		if (inspectorPanel == null) {
			// 获取屏幕尺寸
			float screenWidth = Gdx.graphics.getWidth();

			// 计算检查器面板的左边界位置（屏幕宽度的75%处）
			float inspectorLeft = screenWidth * 0.75f;

			// 设置历史面板的位置（检查器面板的左边下方）
			// 向左偏移10像素，距离底部10像素
			float panelX = inspectorLeft - PANEL_WIDTH - 10;
			float panelY = 10; // 距离底部10像素

			setPosition(panelX, panelY);
			return;
		}

		// 获取检查器面板的实际位置
		float inspectorX = inspectorPanel.getX();
		float inspectorY = inspectorPanel.getY();
		float inspectorWidth = inspectorPanel.getWidth();
		float inspectorHeight = inspectorPanel.getHeight();

		// 将检查器面板的局部坐标转换为屏幕坐标
		com.badlogic.gdx.scenes.scene2d.Actor parent = inspectorPanel.getParent();
		while (parent != null) {
			inspectorX += parent.getX();
			inspectorY += parent.getY();
			parent = parent.getParent();
		}

		// 设置历史面板的位置（检查器面板的左边下方）
		// 向左偏移10像素，距离底部10像素
		float panelX = inspectorX - PANEL_WIDTH - 10;
		float panelY = inspectorY + 10; // 距离检查器面板底部10像素

		setPosition(panelX, panelY);
	}

	@Override
	public void act(float delta) {
		super.act(delta);
		// 每帧更新位置，确保始终附着于检查器面板
		if (autoPosition) {
			updatePosition();
		}
	}

	public void setAutoPosition(boolean autoPosition) {
		this.autoPosition = autoPosition;
	}

	private Actor createCollapseButton() {
		Group button = new Group();
		button.setSize(COLLAPSE_BUTTON_SIZE, COLLAPSE_BUTTON_SIZE);
		button.setTouchable(Touchable.enabled);

		// 创建箭头图标
		TextureRegion arrow = createArrowTexture();
		Actor arrowActor = new Actor() {
			@Override
			public void draw(Batch batch, float parentAlpha) {
				Color color = getColor();
				batch.setColor(color.r, color.g, color.b, color.a * parentAlpha);
				batch.draw(arrow, getX(), getY(), getWidth(), getHeight());
			}
		};
		arrowActor.setSize(16, 16);
		arrowActor.setPosition((COLLAPSE_BUTTON_SIZE - 16) / 2, (COLLAPSE_BUTTON_SIZE - 16) / 2);
		button.addActor(arrowActor);

		// 添加点击事件
		button.addListener(new ClickListener() {
			@Override
			public void clicked(InputEvent event, float x, float y) {
				toggleCollapse();
			}
		});

		return button;
	}

	private TextureRegion createArrowTexture() {
		Pixmap p = new Pixmap(16, 16, Pixmap.Format.RGBA8888);
		p.setColor(new Color(1, 1, 1, 1)); // 使用自己的白色副本，避免全局污染

		// 绘制向下箭头
		int centerX = 8;
		int centerY = 8;
		int size = 6;

		for (int i = 0; i <= size; i++) {
			int x = centerX - i;
			int y = centerY + i - size / 2;
			p.drawLine(x, y, x + i * 2, y);
		}

		Texture texture = new Texture(p);
		p.dispose();
		return new TextureRegion(texture);
	}

	private void initListeners() {
		// 监听窗口大小变化，保持在右下角
		addListener(new ClickListener() {
			@Override
			public void enter(InputEvent event, float x, float y, int pointer, Actor fromActor) {
				// 鼠标进入时，添加轻微高亮效果
				container.setColor(1, 1, 1, 1f); // alpha设置为低于1将导致字体完全不可见
			}

			@Override
			public void exit(InputEvent event, float x, float y, int pointer, Actor toActor) {
				// 鼠标离开时，恢复正常效果
				container.setColor(1, 1, 1, 1f); // alpha设置为低于1将导致字体完全不可见
			}
		});
	}

	public void addHistory(String cmdId, String src, String type, String icon) {
		addHistory(cmdId, src, type, icon, true);
	}

	/**
	 * 添加历史项
	 * @param cmdId 命令ID
	 * @param src 来源
	 * @param type 类型
	 * @param icon 图标
	 * @param updateCurrentIndex 是否更新当前历史索引
	 */
	public void addHistory(String cmdId, String src, String type, String icon, boolean updateCurrentIndex) {
		HistoryItem item = new HistoryItem(cmdId, src, type, icon, historyItems.size());
		historyItems.add(item);
		contentTable.add(item).width(ITEM_WIDTH).height(ITEM_HEIGHT).padBottom(2).row();

		// 更新当前历史索引（仅在需要时）
		if (updateCurrentIndex) {
			currentHistoryIndex = historyItems.size() - 1;
			updateItemStates();
		}

		// 移除自动滚动，让用户手动控制滚动位置
		// scrollPane.layout();
		// scrollPane.scrollTo(0, 0, 0, 0);
	}

	public void updateCurrentIndex(int newIndex) {
		currentHistoryIndex = newIndex;
		updateItemStates();
	}

	private void updateItemStates() {
		for (int i = 0; i < historyItems.size(); i++) {
			HistoryItem item = historyItems.get(i);
			item.setCurrent(i == currentHistoryIndex);
		}
	}

	private void toggleCollapse() {
		isExpanded = !isExpanded;

		if (isExpanded) {
			// 展开面板
			container.addAction(Actions.sizeTo(PANEL_WIDTH, MAX_PANEL_HEIGHT, 0.3f));
			scrollPane.addAction(Actions.fadeIn(0.3f));
		} else {
			// 收起面板
			container.addAction(Actions.sizeTo(PANEL_WIDTH, COLLAPSE_BUTTON_SIZE + 10, 0.3f));
			scrollPane.addAction(Actions.fadeOut(0.3f));
		}

		setSize(PANEL_WIDTH, container.getHeight());
	}

	@Override
	public void draw(Batch batch, float parentAlpha) {
		// 绘制不透明背景
		Color c = getColor();
		float alpha = c.a * parentAlpha;

		float x = getX();
		float y = getY();
		float w = container.getWidth();
		float h = container.getHeight();

		// 绘制面板背景（不透明）
		batch.setColor(0.2f, 0.2f, 0.2f, 0.9f); // 深灰色半透明背景，增加不透明度
		batch.draw(whitePixel, x, y, w, h);

		// 绘制边框
		batch.setColor(0.5f, 0.5f, 0.5f, 0.8f); // 边框颜色
		batch.draw(whitePixel, x, y, w, 2); // 上边框
		batch.draw(whitePixel, x, y + h - 2, w, 2); // 下边框
		batch.draw(whitePixel, x, y, 2, h); // 左边框
		batch.draw(whitePixel, x + w - 2, y, 2, h); // 右边框

		// 绘制子组件
		super.draw(batch, parentAlpha);
	}

	private class HistoryItem extends Group {
		private final String cmdId;
		private final String src;
		private final String type;
		private final String icon;
		private final int index;
		private boolean isCurrent = false;

		private Label label;

		public HistoryItem(String cmdId, String src, String type, String icon, int index) {
			this.cmdId = cmdId;
			this.src = src;
			this.type = type;
			this.icon = icon;
			this.index = index;

			setSize(ITEM_WIDTH, ITEM_HEIGHT);
			initItem();
		}

		private void initItem() {
			// 设置可触摸
			setTouchable(Touchable.enabled);

			// 创建标签文本
			String displayText = icon + " " + cmdId.replace("CMD_", "") + " - " + src;
			label = new Label(displayText, styleItem);
			label.setPosition(8, 4);
			addActor(label);

			// 添加点击事件
			addListener(new ClickListener() {
				@Override
				public void clicked(InputEvent event, float x, float y) {
					// 点击历史节点，切换到该历史
					if (commandManager != null) {
						// 直接使用index作为历史位置，因为现在历史列表包含了所有命令
						commandManager.navigateToHistory(index);
					}
				}
			});
		}

		public void setCurrent(boolean current) {
			isCurrent = current;
			label.setStyle(current ? styleCurrentItem : styleItem);
		}

		@Override
		public void draw(Batch batch, float parentAlpha) {
			Color c = getColor();
			float alpha = c.a * parentAlpha;

			float x = getX();
			float y = getY();
			float w = getWidth();
			float h = getHeight();

			// 绘制白色半透明背景
			if (isCurrent) {
				batch.setColor(1, 1, 1, 0.7f); // 当前项白色背景，更不透明
			} else {
				batch.setColor(1, 1, 1, 0.5f); // 普通项白色背景，半透明
			}
			batch.draw(whitePixel, x, y, w, h);

			// 绘制子组件
			super.draw(batch, parentAlpha);
		}
	}
}
