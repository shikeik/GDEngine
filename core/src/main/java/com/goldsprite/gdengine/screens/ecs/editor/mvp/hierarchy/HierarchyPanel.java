package com.goldsprite.gdengine.screens.ecs.editor.mvp.hierarchy;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.DragAndDrop;
import com.badlogic.gdx.scenes.scene2d.utils.DragAndDrop.Target;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.goldsprite.gdengine.ecs.entity.GObject;
import com.goldsprite.gdengine.screens.ecs.editor.mvp.EditorPanel;
import com.goldsprite.gdengine.ui.event.ContextListener;
import com.kotcrab.vis.ui.VisUI;
import com.kotcrab.vis.ui.widget.*;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import com.goldsprite.gdengine.ecs.GameWorld;

public class HierarchyPanel extends EditorPanel implements IHierarchyView {

	private HierarchyPresenter presenter;
	private VisTree<GObjectNode, GObject> tree;
	private DragAndDrop dragAndDrop;

	// [修复] 缓存外部注册的 Targets (例如 SceneView)，防止刷新时丢失
	private List<Target> externalTargets = new ArrayList<>();

	// 1. 在 HierarchyPanel 类中定义一些常量，方便调整
	private static final float INDENT_WIDTH = 20f; // 缩进宽度，必须与 tree.setIndentSpacing(20f) 一致
	private static final float LINE_ALPHA = 0.3f;
	private static final float LINE_OFFSET_X = 10f; // 线条相对于缩进格的偏移 (居中)

	public HierarchyPanel() {
		super("Hierarchy");
//		debugAll();

		dragAndDrop = new DragAndDrop();

		tree = new VisTree<>();
		//tree.debugAll();
		tree.setIndentSpacing(20f);
		tree.getSelection().setProgrammaticChangeEvents(false);

		VisScrollPane scrollPane = new VisScrollPane(tree);
		scrollPane.setFadeScrollBars(false);

		// 背景右键菜单
		scrollPane.addListener(new ContextListener() {
				@Override public void onShowMenu(float stageX, float stageY) {
					if (tree.getOverNode() == null) showMenu(null, stageX, stageY);
				}
			});

		addContent(scrollPane);
	}

	// [新增] 实现接口：在树中查找并选中节点
	@Override
	public void selectNode(GObject target) {
		if (target == null) {
			tree.getSelection().clear();
			return;
		}
		// 递归查找节点
		GObjectNode node = findNode(tree.getNodes(), target);
		if (node != null) {
			// 展开父节点以确保可见
			expandParents(node);
			tree.getSelection().choose(node);
		}
	}

	private GObjectNode findNode(com.badlogic.gdx.utils.Array<GObjectNode> nodes, GObject target) {
		for (GObjectNode node : nodes) {
			if (node.getValue() == target) return node;
			if (node.getChildren().size > 0) {
				GObjectNode found = findNode(node.getChildren(), target);
				if (found != null) return found;
			}
		}
		return null;
	}

	private void expandParents(GObjectNode node) {
		if (node.getParent() != null) {
			node.getParent().setExpanded(true);
			expandParents(node.getParent());
		}
	}

	@Override
	public void setPresenter(HierarchyPresenter presenter) {
		this.presenter = presenter;
	}

	// [修复] 驱动 Presenter 的 update 循环 (处理节流阀)
	@Override
	public void act(float delta) {
		super.act(delta);
		if (presenter != null) {
			presenter.update(delta);
		}
	}

	@Override
	public DragAndDrop getDragAndDrop() {
		return dragAndDrop;
	}

	// [修复] 专门用于添加不会被刷新的 Target (给 EditorController 用)
	public void addSceneDropTarget(Target target) {
		dragAndDrop.addTarget(target);
		externalTargets.add(target);
	}

	@Override
	public void showNodes(List<GObject> roots) {
		// 1. 保存展开状态
		Set<String> expanded = new HashSet<>();
		saveExpansion(tree.getNodes(), expanded);

		// 2. 清理 UI
		tree.clearChildren();

		// [关键修复] 清理旧的 Source/Target，但要把之前保存的外部 Target 加回来
		dragAndDrop.clear();

		for (Target t : externalTargets) {
			dragAndDrop.addTarget(t);
		}

		// 3. 重建节点
		for (GObject root : roots) {
			buildNode(root, null);
		}

		// 4. 恢复展开
		restoreExpansion(tree.getNodes(), expanded);
	}

	private void buildNode(GObject obj, GObjectNode parent) {
		GObjectNode node = new GObjectNode(obj);
		if (parent == null) tree.add(node);
		else parent.add(node);

		for (GObject child : obj.getChildren()) {
			buildNode(child, node);
		}
	}

	// --- 内部类与辅助方法 ---

	enum DropState { NONE, INSERT_ABOVE, INSERT_BELOW, REPARENT }

	class GObjectNode extends VisTree.Node<GObjectNode, GObject, VisTable> {
		DropState dropState = DropState.NONE;

		public GObjectNode(GObject obj) {
			super(new VisTable());
			//debugAll();
			setValue(obj);

			VisTable table = getActor();
			table.setBackground("button");

			// 名字
			VisLabel lbl = new VisLabel(obj.getName());
			table.add(lbl).expandX().fillX().left().padLeft(5);

			// 手柄
			VisLabel handle = new VisLabel("::");
			handle.setColor(Color.GRAY);
			table.add(handle).right().padRight(10).width(20);

			// [核心修复 Item 3] 给 Handle 增加一个无操作的 Touchable，防止事件穿透？
			// 不，ContextListener 是加在 table 上的。handle 是 table 的子元素。
			// 只要 handle 处理了 touchDown，父级 table 就收不到了（前提是 return true）。
			// 但 DragAndDrop.Source 内部也是监听 input。
			// 最简单的办法：给 handle 加一个空的 ClickListener 并停止冒泡，或者依赖 DragSource 的 consume。
			// 实际上 DragAndDrop.Source 默认不阻止事件冒泡。
			// 我们手动给 handle 加一个 InputFilter。
			handle.addListener(new com.badlogic.gdx.scenes.scene2d.InputListener() {
				@Override
				public boolean touchDown(InputEvent event, float x, float y, int pointer, int button) {
					// 如果点击了手柄，这就是拖拽操作的开始
					// 我们返回 false 让 DragSource 去处理？DragSource 也是 InputLisener。
					// 关键是 ContextListener 在 table 上。
					// 如果这里返回 true，table 上的 listener 就收不到 touchDown 了。
					// 但 DragSource 需要收到。
					// 只要确保 DragSource 先于 ContextListener 执行即可？
					// 或者更简单：在 ContextListener 里判断 target 是否是 handle。
					return false;
				}
			});
			// 修正策略：修改 ContextListener 的判定逻辑比修改这里更稳妥。
			// 见下方 table 的 ContextListener 修改。
			// [修改] 交互逻辑：排除手柄区域
			table.addListener(new ContextListener() {
				@Override public void onShowMenu(float stageX, float stageY) {
					showMenu(obj, stageX, stageY);
				}
				@Override public void onLeftClick(InputEvent event, float x, float y, int count) {
					// [修复 Item 3] 如果点击目标是 handle，则不触发选中逻辑（让它去拖拽）
					if (event.getTarget() == handle) return;
					presenter.selectObject(obj);
				}

				@Override
				public boolean longPress(Actor actor, float x, float y) {
					// [修复 Item 3] 长按手柄也不弹菜单
					Actor hit = actor.hit(x, y, true);
					if (hit == handle) return false;
					return super.longPress(actor, x, y);
				}
			});

			// 绘制插入线 (使用匿名子类)
			VisTable content = new VisTable() {
				@Override
				public void draw(Batch batch, float parentAlpha) {
					// --- 1. 绘制引导线 (新增逻辑) ---
					if (getParent() != null) { // 根节点通常不画线
						Drawable white = VisUI.getSkin().getDrawable("white");
						Color old = batch.getColor();

						// 使用灰色，带透明度
						batch.setColor(0.5f, 0.5f, 0.5f, LINE_ALPHA * parentAlpha);

						int level = getLevel(); // 获取当前节点深度 (VisTree.Node 自带方法)
						float h = getHeight();

						// 循环绘制每一级父级的竖线
						// 每一级缩进 INDENT_WIDTH
						for (int i = 1; i < level; i++) {
							// 计算线条 X 坐标: (i * indent) - indent/2 (居中)
							// 注意：这里是相对于 Node Actor 的坐标，Tree 的缩进是靠 Padding 实现的吗？
							// VisTree 的实现通常是 Actor 自己偏移。
							// 实际上，VisTree 的 Node Actor 的 X 坐标通常是 0，Tree 也就是个 VerticalGroup。
							// 等等，VisTree 的 indent 是通过 padLeft 实现的吗？
							// 经过查阅 VisTree 源码，它是通过在 Actor 左侧留空实现的。
							// 我们可以简单地根据 level 反推 X 位置。

							// 修正：我们不需要画所有的竖线，只需要画 "连接线"。
							// 但简单的 IDE 风格通常是画很多条淡淡的竖线表示层级轨道。

							float lineX = getX() - (level - i) * INDENT_WIDTH + LINE_OFFSET_X;

							// 画一条竖线贯穿整个 Item 高度
							white.draw(batch, lineX, getY(), 1, h);
						}

						// 画 "L" 型连接线 (指向自己的横线)
						float currentLineX = getX() + LINE_OFFSET_X;
						float midY = getY() + h / 2f;

						// 竖线部分 (上半截)
						white.draw(batch, currentLineX, midY, 1, h / 2f);
						// 横线部分
						white.draw(batch, currentLineX, midY, 8f, 1);

						batch.setColor(old);
					}

					// --- 2. 原有的宽度调整逻辑 (保持不变) ---
					if (tree != null) {
						float targetWidth = tree.getWidth() - getX();
						if (targetWidth > 0 && getWidth() != targetWidth) {
							setWidth(targetWidth);
							invalidate();
						}
					}

					super.draw(batch, parentAlpha);

					// --- 3. 原有的 DropLine 逻辑 (保持不变) ---
					if (dropState != DropState.NONE) {
						drawDropLine(batch, getX(), getY(), getWidth(), getHeight(), dropState);
					}
				}
			};
			setActor(content);
			content.add(lbl).expandX().fillX().left().padLeft(5);
			content.add(handle).right().padRight(10).width(20);
			// 重新绑定 Listener 到 content
			content.addListener(table.getListeners().first());

			setupDragAndDrop(handle, content, obj);
		}

		private void setupDragAndDrop(Actor handle, Actor targetActor, GObject obj) {
			dragAndDrop.addSource(new DragAndDrop.Source(handle) {
					@Override
					public DragAndDrop.Payload dragStart(InputEvent event, float x, float y, int pointer) {
						DragAndDrop.Payload payload = new DragAndDrop.Payload();
						payload.setObject(obj);
						Label dragActor = new Label(obj.getName(), VisUI.getSkin());
						dragActor.setColor(Color.YELLOW);
						payload.setDragActor(dragActor);
						return payload;
					}
				});

			dragAndDrop.addTarget(new DragAndDrop.Target(targetActor) {
					@Override
					public boolean drag(DragAndDrop.Source source, DragAndDrop.Payload payload, float x, float y, int pointer) {
						GObject dragging = (GObject) payload.getObject();
						if (dragging == obj) return false;
						float h = getActor().getHeight();
						if (y > h * 0.75f) dropState = DropState.INSERT_ABOVE;
						else if (y < h * 0.25f) dropState = DropState.INSERT_BELOW;
						else dropState = DropState.REPARENT;
						return true;
					}

					@Override
					public void drop(DragAndDrop.Source source, DragAndDrop.Payload payload, float x, float y, int pointer) {
						GObject dragging = (GObject) payload.getObject();
						if (dropState == DropState.INSERT_ABOVE) {
							presenter.moveEntity(dragging, obj.getParent(), getSiblingIndex(obj));
						} else if (dropState == DropState.INSERT_BELOW) {
							presenter.moveEntity(dragging, obj.getParent(), getSiblingIndex(obj) + 1);
						} else {
							presenter.moveEntity(dragging, obj, -1);
						}
						dropState = DropState.NONE;
					}

					@Override
					public void reset(DragAndDrop.Source source, DragAndDrop.Payload payload) {
						dropState = DropState.NONE;
					}
				});
		}

		private int getSiblingIndex(GObject t) {
			List<GObject> list = (t.getParent() != null) ? t.getParent().getChildren() : GameWorld.inst().getRootEntities();
			return list.indexOf(t);
		}
	}

	private void drawDropLine(Batch batch, float x, float y, float w, float h, DropState state) {
		Drawable white = VisUI.getSkin().getDrawable("white");
		Color old = batch.getColor();
		batch.setColor(Color.CYAN);
		if (state == DropState.INSERT_ABOVE) {
			white.draw(batch, x, y + h - 2, w, 2);
		} else if (state == DropState.INSERT_BELOW) {
			white.draw(batch, x, y, w, 2);
		} else if (state == DropState.REPARENT) {
			white.draw(batch, x, y, w, 2);
			white.draw(batch, x, y + h - 2, w, 2);
			white.draw(batch, x, y, 2, h);
			white.draw(batch, x + w - 2, y, 2, h);
		}
		batch.setColor(old);
	}

	private void showMenu(GObject target, float x, float y) {
		PopupMenu menu = new PopupMenu();
		if (target == null) {
			menu.addItem(new MenuItem("Create Empty", new ChangeListener() {
								 @Override public void changed(ChangeEvent event, Actor actor) { presenter.createObject(null); }
							 }));
		} else {
			menu.addItem(new MenuItem("Create Child", new ChangeListener() {
								 @Override public void changed(ChangeEvent event, Actor actor) { presenter.createObject(target); }
							 }));
			MenuItem del = new MenuItem("Delete");
			del.getLabel().setColor(Color.RED);
			del.addListener(new ChangeListener() {
					@Override public void changed(ChangeEvent event, Actor actor) { presenter.deleteObject(target); }
				});
			menu.addItem(del);
		}
		menu.showMenu(getStage(), x, y);
	}

	private void saveExpansion(com.badlogic.gdx.utils.Array<GObjectNode> nodes, Set<String> paths) {
		for(GObjectNode node : nodes) {
			if(node.isExpanded()) paths.add(node.getValue().getName() + "_" + node.getValue().getGid());
			if(node.getChildren().size > 0) saveExpansion(node.getChildren(), paths);
		}
	}
	private void restoreExpansion(com.badlogic.gdx.utils.Array<GObjectNode> nodes, Set<String> paths) {
		for(GObjectNode node : nodes) {
			if(paths.contains(node.getValue().getName() + "_" + node.getValue().getGid())) node.setExpanded(true);
			if(node.getChildren().size > 0) restoreExpansion(node.getChildren(), paths);
		}
	}
}
