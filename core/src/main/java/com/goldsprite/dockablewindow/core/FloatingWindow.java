package com.goldsprite.dockablewindow.core;

import com.badlogic.gdx.Application;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Cursor;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.goldsprite.gdengine.input.Event;
import com.badlogic.gdx.scenes.scene2d.*;

public class FloatingWindow extends Table {
	private final Table content;
	protected Table titleBar;
	private Label titleLabel;
	private TextButton closeBtn;
	private final ShapeRenderer shapeRenderer;
	private final float windowLimitShreshold = -2;//窗口移动限制
	private final float draggingAlpha = 0.7f;
	private final float edgeShreshold = 24f;//悬停边缘容差
	private final float edgeHighLightThickness = 5f;
	private float minWidth, minHeight;
	protected InputListener titleDraggingListener;
	protected boolean enableTitleDrag = true;
	protected boolean enableEdgeDrag = true;
	protected boolean enterDraggingTitleBar;

	public Event<Object> onTitleDraggingFinish = new Event<>();

	public FloatingWindow(String titleText, Skin skin) {
		super(skin);

		shapeRenderer = new ShapeRenderer();

		setBackground("panel1");
		addListener(new InputListener() {
			@Override
			public boolean touchDown(InputEvent event, float x, float y, int pointer, int button) {
				setZIndex(getParent().getChildren().size - 1);
				return false;
			}
		});

		//设置标题栏
		setupTitleBar(titleText, skin);

		//创建内容
		content = new Table(skin);
//		content.setBackground("panel1");
		add(content).grow();

		refreshMinSize();
	}

	public void setTitleLayout(Table titleBar, Label titleLabel) {
		this.titleBar = titleBar;
		this.titleLabel = titleLabel;
		add(titleBar).growX().row();
	}

	public void refreshMinSize() {
		float minSizeX = Math.max(titleBar.getMinWidth(), content.getMinWidth()) + 20;
//		float minSizeY = titleBar.getMinHeight()+content.getMinHeight();//这里没有真正实现, 需要递归获取content所有子级中最大的minHeight
		float minSizeY = 80;
//		setMinSize(minSizeX, minSizeY);//暂时取消不完善的minSize设定
		setSize(minSizeX, minSizeY);
	}

	public float getMinWidth() {
		return minWidth;
	}

	public float getMinHeight() {
		return minHeight;
	}

	public void setMinSize(float minWidth, float minHeight) {
		this.minWidth = minWidth;
		this.minHeight = minHeight;
	}

	public void clampPos(){
		float newPosX = getX(), newPosY = getY();
		newPosX = MathUtils.clamp(newPosX, windowLimitShreshold, getValidParentSize().x - getWidth() - windowLimitShreshold);
		newPosY = MathUtils.clamp(newPosY, windowLimitShreshold, getValidParentSize().y - getHeight() - windowLimitShreshold);
		setPosition(newPosX, newPosY);
	}

	private void setupTitleBar(String titleText, Skin skin) {
		titleBar = new Table(skin);
		titleLabel = new Label(titleText, skin);
		closeBtn = new TextButton("", skin, "radio");

		titleBar.add(titleLabel).top().left().growX();
		titleBar.add(closeBtn).top().right().width(20).height(20).padBottom(5);
		titleBar.setBackground("list");
		titleBar.setHeight(30);
		add(titleBar).growX().row();

		registerCloseBtnListener();

		registerTitleDraggingListener();
	}

	public void setupTitleBar(Table titleBar, Label titleLabel, Skin skin) {
		clear();

		this.titleBar = titleBar;
		this.titleLabel = titleLabel;
		closeBtn = new TextButton("", skin, "radio");

		//titleBar.add(titleLabel).top().left().growX();
		titleBar.add(closeBtn).top().right().width(20).height(20).padBottom(5);
		//titleBar.setBackground("list");
		titleBar.setHeight(20);
		add(titleBar).growX().row();

		registerCloseBtnListener();

		registerTitleDraggingListener();

		add(content).grow();
	}

	public void registerCloseBtnListener() {
		closeBtn.addListener(new InputListener() {
				boolean moved;

				@Override
				public boolean touchDown(InputEvent event, float x, float y, int pointer, int button) {
					closeBtn.setChecked(true);
					moved = false;
					return true;
				}

				@Override
				public void touchDragged(InputEvent event, float x, float y, int pointer) {
					moved = true;
				}

				@Override
				public void touchUp(InputEvent event, float x, float y, int pointer, int button) {
					closeBtn.setChecked(false);
					if (!moved) remove();
				}
			});
	}

	protected Vector2 startStageCursorPos = new Vector2();
	protected float startX, startY;

	private void registerTitleDraggingListener() {
		titleBar.addListener(titleDraggingListener = new InputListener() {
			final Vector2 stageCursorPos = new Vector2();
			float movedX, movedY;
			float startAlpha;

			@Override
			public boolean touchDown(InputEvent event, float x, float y, int pointer, int button) {
				setZIndex(getParent().getChildren().size - 1);
				Actor hitActor = titleBar.hit(x, y, true);
				if (hitActor != titleLabel) return false;
				if (closeBtn.isChecked()) return false;
				if (checkEdgeHover()) return false;
				enterDraggingTitleBar = true;
				localToStageCoordinates(startStageCursorPos.set(x, y));
//				Log.log("titleDragging", "startX: " + startStageCursorPos.x + ", startY: " + startStageCursorPos.y + ", movedX: " + movedX + ", movedY: " + movedY);
				startX = getX();
				startY = getY();
				startAlpha = getColor().a;
				setColor(getColor().r, getColor().g, getColor().b, draggingAlpha);
				return true;
			}

			@Override
			public void touchDragged(InputEvent event, float x, float y, int pointer) {
				if (!enableTitleDrag) return;

				localToStageCoordinates(stageCursorPos.set(x, y));
				movedX = stageCursorPos.x - startStageCursorPos.x;
				movedY = stageCursorPos.y - startStageCursorPos.y;
//				Log.log("titleDragging", "startX: " + startStageCursorPos.x + ", startY: " + startStageCursorPos.y + ", movedX: " + movedX + ", movedY: " + movedY);
				float newPosX = startX + movedX;
				float newPosY = startY + movedY;
				//拖动窗口
				setPosition(newPosX, newPosY);
				//限制XY在屏幕内
				clampPos();
			}

			Object[] onTitleDraggingFinishData = new Object[2];
			@Override
			public void touchUp(InputEvent event, float x, float y, int pointer, int button) {
				enterDraggingTitleBar = false;
				setColor(getColor().r, getColor().g, getColor().b, startAlpha);

				//拖动抬起回调
				titleBar.localToStageCoordinates(stageCursorPos.set(x, y));
				onTitleDraggingFinishData[0] = FloatingWindow.this;
				onTitleDraggingFinishData[1] = stageCursorPos;
				onTitleDraggingFinish.invoke(onTitleDraggingFinishData);
			}
		});
	}

	public void registerEdgeDraggingListener(Stage stage) {
		stage.addListener(new InputListener() {
			private boolean enterEdge;//标记按下时是否在edgeHover区域
			private final Vector2 startLocalCursor = new Vector2();
			private final Vector2 currentLocalCursor = new Vector2();
			private float startWidth, startHeight;
			public final boolean isAndroid = Gdx.app.getType() == Application.ApplicationType.Android;
			public final boolean isDesktop = Gdx.app.getType() == Application.ApplicationType.Desktop;

			@Override
			public boolean mouseMoved(InputEvent event, float x, float y) {
				if (isDesktop) {
					return checkEdgeHover();
				}
				return false;
			}

			@Override
			public boolean touchDown(InputEvent event, float stageX, float stageY, int pointer, int button) {
				enterEdge = checkEdgeHover();
				if (enterEdge && enableEdgeDrag) {
					stageToLocalCoordinates(startLocalCursor.set(stageX, stageY));
					startWidth = getWidth();
					startHeight = getHeight();
					return true;
				}
				return false;
			}

			@Override
			public void touchDragged(InputEvent event, float stageX, float stageY, int pointer) {
				if (!enterEdge) return;
				stageToLocalCoordinates(currentLocalCursor.set(stageX, stageY));
				float movedX = currentLocalCursor.x - startLocalCursor.x;
				float movedY = currentLocalCursor.y - startLocalCursor.y;
				windowEdgeResize(hoverEdge, movedX, movedY, startWidth, startHeight);
			}

			@Override
			public void touchUp(InputEvent event, float x, float y, int pointer, int button) {
				if (isAndroid) {
					Gdx.graphics.setSystemCursor(Cursor.SystemCursor.Arrow);
					drawHighLight = false;
				}
			}
		});
	}

	public Table getTitleBar() {
		return titleBar;
	}
	public Label getTitleLabel() {
		return titleLabel;
	}
	public String getTitleText() {
		return titleLabel.getText().toString();
	}

	public void setTitleText(String titleText) {
		titleLabel.setText(titleText);
	}

	public Table getContent() {
		return content;
	}

	//	@Override
//	public void setPosition(float x, float y) {
//		setX(x);
//		setY(y);
//	}
//	@Override
//	public void setX(float x) {
//		x = MathUtils.clamp(x, windowLimitShreshold, getStage().getWidth() - getWidth() - windowLimitShreshold);
//		super.setX(x);
//	}
//	@Override
//	public void setY(float y) {
//		y = MathUtils.clamp(y, windowLimitShreshold, getStage().getHeight() - getHeight() - windowLimitShreshold);
//		super.setY(y);
//	}

	final float inner = 6;
	boolean drawHighLight = true;
	Vector2 hoverEdge = new Vector2();

	@Override
	public void act(float delta) {
//		checkEdgeHover();
	}

	@Override
	public void draw(Batch batch, float parentAlpha) {
		super.draw(batch, parentAlpha);
		drawHighLightBar(batch);
	}

	boolean lastLeft, lastBottom, lastRight, lastTop;

	private boolean checkEdgeHover() {
		if(!enableEdgeDrag) return false;

		Vector2 cursorLocalPosition = getCursorLocalPosition();
		float cursorX = cursorLocalPosition.x;
		float cursorY = cursorLocalPosition.y;
		boolean left = Math.abs(-inner + cursorX) < edgeShreshold / 2 && (cursorY >= -edgeShreshold / 2 && cursorY <= getHeight() - edgeShreshold / 2);
		boolean bottom = Math.abs(-inner + cursorY) < edgeShreshold / 2 && (cursorX >= -edgeShreshold / 2 && cursorX <= getWidth() + edgeShreshold / 2);
		boolean right = Math.abs(getWidth() - inner - cursorX) < edgeShreshold / 2 && (cursorY >= -edgeShreshold / 2 && cursorY <= getHeight() - edgeShreshold / 2);
		boolean top = Math.abs(getHeight() - (inner-6) - cursorY) < edgeShreshold / 2 && (cursorX >= -edgeShreshold / 2 && cursorX <= getWidth() + edgeShreshold / 2);
//		//调试代码
//		if (left != lastLeft || bottom != lastBottom || right != lastRight || top != lastTop)
//			Log.log("CheckEdgeHover", "left: " + left + ", bottom: " + bottom + ", right: " + right + ", top: " + top);
//		lastLeft = left;
//		lastBottom = bottom;
//		lastRight = right;
//		lastTop = top;
		boolean leftBottom = left && bottom;
		boolean rightBottom = right && bottom;
		boolean rightTop = right && top;
		boolean leftTop = left && top;

		drawHighLight = true;
		hoverEdge.set(left ? -1 : (right ? 1 : 0), bottom ? -1 : (top ? 1 : 0));
		if (leftBottom) {
			Gdx.graphics.setSystemCursor(Cursor.SystemCursor.NESWResize);
		} else if (rightTop) {
			Gdx.graphics.setSystemCursor(Cursor.SystemCursor.NESWResize);
		} else if (rightBottom) {
			Gdx.graphics.setSystemCursor(Cursor.SystemCursor.NWSEResize);
		} else if (leftTop) {
			Gdx.graphics.setSystemCursor(Cursor.SystemCursor.NWSEResize);
		} else if (left) {
			Gdx.graphics.setSystemCursor(Cursor.SystemCursor.HorizontalResize);
		} else if (right) {
			Gdx.graphics.setSystemCursor(Cursor.SystemCursor.HorizontalResize);
		} else if (bottom) {
			Gdx.graphics.setSystemCursor(Cursor.SystemCursor.VerticalResize);
		} else if (top) {
			Gdx.graphics.setSystemCursor(Cursor.SystemCursor.VerticalResize);
		} else {
			drawHighLight = false;
			Gdx.graphics.setSystemCursor(Cursor.SystemCursor.Arrow);
		}

		return left || right || bottom || top;
	}

	protected void windowEdgeResize(Vector2 hoverEdge, float deltaX, float deltaY, float startWidth, float startHeight) {
		float newX = getX();
		float newY = getY();
		float newWidth = getWidth();
		float newHeight = getHeight();

		if (hoverEdge.x != 0) {
			if (hoverEdge.x > 0) {
				newWidth = startWidth + deltaX;
				//如果右边越界则回退
				{
					float rightX = newX + newWidth;
					float diff = rightX - (getValidParentSize().x - windowLimitShreshold);
					if (diff > 0) newWidth -= diff;
				}
			}
			if (hoverEdge.x < 0) {
				float leftX = newX;
				float diff = leftX - windowLimitShreshold;
				//如果左边越界且继续往左则不应用, 否则正常应用
				if (!(diff < 0) || !(deltaX < 0)) {
					newX += deltaX;
					newWidth -= deltaX;
					//如果达到最小宽度则取消右移
					{
						float diffWidth = newWidth - minWidth;
						if (diffWidth < 0) newX += diffWidth;
					}
				}
			}
		}
		if (hoverEdge.y != 0) {
			if (hoverEdge.y > 0) {
				newHeight = startHeight + deltaY;
				float topY = newY + newHeight;
				float diff = topY - (getValidParentSize().y - windowLimitShreshold);
				//如果右边越界则回退
				if (diff > 0) newHeight -= diff;
			}
			if (hoverEdge.y < 0) {
				float bottomY = newY;
				float diff = bottomY - windowLimitShreshold;
				//如果下方越界且继续往下则不应用, 否则正常应用
				if (!(diff < 0) || !(deltaY < 0)) {
					newY += deltaY;
					newHeight += -deltaY;
					//如果达到最小高度则取消上移
					{
						float diffHeight = newHeight - minHeight;
						if (diffHeight < 0) newY += diffHeight;
					}
				}
			}
		}

		newWidth = MathUtils.clamp(newWidth, minWidth, newWidth);
		newHeight = MathUtils.clamp(newHeight, minHeight, newHeight);
		setSize(newWidth, newHeight);
		setPosition(newX, newY);
	}

	public void drawHighLightBar(Batch batch) {
		if (!drawHighLight || !enableEdgeDrag) return;

		batch.end();
		shapeRenderer.setProjectionMatrix(batch.getProjectionMatrix());
		shapeRenderer.setTransformMatrix(batch.getTransformMatrix());
		shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
		if (hoverEdge.x != 0) {
			shapeRenderer.rect(
				getX() + inner * (hoverEdge.x > 0 ? -1 : 1) - edgeHighLightThickness / 2 + getWidth() * (hoverEdge.x > 0 ? 1 : 0),
				getY() + inner * 0.6f,
				edgeHighLightThickness,
				getHeight() - inner * 1.2f);
		}
		if (hoverEdge.y != 0) {
			shapeRenderer.rect(
				getX() + inner * 0.6f,
				getY() + inner * (hoverEdge.y > 0 ? -1 : 1) - edgeHighLightThickness / 2 + getHeight() * (hoverEdge.y > 0 ? 1 : 0),
				getWidth() - inner * 1.2f,
				edgeHighLightThickness);
		}
		shapeRenderer.end();
		batch.begin();
	}

	Vector2 tmpVec = new Vector2();
	public Vector2 getCursorLocalPosition() {
		tmpVec.set(Gdx.input.getX(), Gdx.input.getY());
		return screenToLocalCoordinates(tmpVec);
	}

	public void setEnableTitleDrag(boolean enable) {
		this.enableTitleDrag = enable;
	}
	public void setEnableEdgeDrag(boolean enable) {
		this.enableEdgeDrag = enable;
	}

	Vector2 validParentSize;
	public Vector2 getValidParentSize(){
		if(validParentSize == null) validParentSize = new Vector2();
		if(getParent() == null || getStage() == null) return validParentSize;

		float width = getParent().getWidth();
		if(width == 0) width = getStage().getWidth();
		float height = getParent().getHeight();
		if(height == 0) height = getStage().getHeight();
		return validParentSize.set(width, height);
	}

	@Override
	public Actor hit(float x, float y, boolean touchable) {
		// 先让父类处理子控件
		Actor hit = super.hit(x, y, touchable);
		if (hit != null && hit != this) {
			return hit; // 点击到子控件，返回子控件
		}

		// 点击在 MTable 自己的空白区域
		if (isVisible() && isHittable(x, y)) {
			return this;
		}

		return null; // 其他地方
	}

	Vector2 localPos = new Vector2();
	public boolean isHittableByStage(float stageX, float stageY) {
		stageToLocalCoordinates(localPos.set(stageX, stageY));
		return isHittable(localPos.x, localPos.y);
	}
	public boolean isHittable(float localX, float localY) {
		return localX >= 0 && localX < getWidth() && localY >= 0 && localY < getHeight();
	}

	public void setNoTitle() {
		removeActor(titleBar);
	}
}
