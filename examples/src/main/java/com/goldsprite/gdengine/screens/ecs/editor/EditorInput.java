package com.goldsprite.gdengine.screens.ecs.editor;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.InputProcessor;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.goldsprite.solofight.screens.tests.iconeditor.model.EditorTarget;
import com.goldsprite.solofight.screens.tests.iconeditor.system.GizmoSystem;
import com.goldsprite.solofight.screens.tests.iconeditor.system.SceneManager;

/**
 * 编辑器输入处理器
 * 职责：处理编辑器模式下的鼠标和键盘输入，包括对象选择、Gizmo操作等
 */
public class EditorInput extends InputAdapter {
    private final OrthographicCamera camera;
    private final SceneManager sceneManager;
    private final GizmoSystem gizmoSystem;

    // 状态变量
    private boolean isDragging = false;
    private Vector2 lastTouchPos = new Vector2();
    private Vector3 tempVec3 = new Vector3();

    // Gizmo操作状态
    private boolean isGizmoDragging = false;
    private GizmoSystem.Mode gizmoMode = GizmoSystem.Mode.MOVE;

    public EditorInput(OrthographicCamera camera, SceneManager sceneManager, GizmoSystem gizmoSystem) {
        this.camera = camera;
        this.sceneManager = sceneManager;
        this.gizmoSystem = gizmoSystem;
    }

    @Override
    public boolean touchDown(int screenX, int screenY, int pointer, int button) {
        if (button != 0) return false; // 只处理左键

        // 转换为世界坐标
        Vector3 worldPos = camera.unproject(tempVec3.set(screenX, screenY, 0));

        // 检查是否点击了Gizmo
        EditorTarget selection = sceneManager.getSelection();
        if (selection != null) {
            // 这里应该添加Gizmo的点击检测逻辑
            // 暂时简化处理
        }

        // 检查是否点击了对象
        EditorTarget hitTarget = hitTest(worldPos.x, worldPos.y);
        if (hitTarget != null) {
            sceneManager.selectNode(hitTarget);
            isDragging = true;
            lastTouchPos.set(worldPos.x, worldPos.y);
            return true;
        } else {
            // 点击空白处，取消选择
            sceneManager.selectNode(null);
        }

        return false;
    }

    @Override
    public boolean touchDragged(int screenX, int screenY, int pointer) {
        if (!isDragging) return false;

        // 转换为世界坐标
        Vector3 worldPos = camera.unproject(tempVec3.set(screenX, screenY, 0));

        EditorTarget selection = sceneManager.getSelection();
        if (selection != null) {
            // 根据Gizmo模式执行不同的操作
            switch (gizmoMode) {
                case MOVE:
                    float dx = worldPos.x - lastTouchPos.x;
                    float dy = worldPos.y - lastTouchPos.y;
                    selection.setX(selection.getX() + dx);
                    selection.setY(selection.getY() + dy);
                    break;
                case ROTATE:
                    // 计算旋转角度
                    Vector2 center = new Vector2(selection.getX(), selection.getY());
                    Vector2 lastDir = lastTouchPos.cpy().sub(center).nor();
                    Vector2 currentDir = new Vector2(worldPos.x, worldPos.y).sub(center).nor();
                    float angle = lastDir.angle(currentDir);
                    selection.setRotation(selection.getRotation() + angle);
                    break;
                case SCALE:
                    // 计算缩放
                    Vector2 center2 = new Vector2(selection.getX(), selection.getY());
                    float lastDist = lastTouchPos.dst(center2);
                    float currentDist = new Vector2(worldPos.x, worldPos.y).dst(center2);
                    float scale = currentDist / lastDist;
                    selection.setScaleX(selection.getScaleX() * scale);
                    selection.setScaleY(selection.getScaleY() * scale);
                    break;
            }

            lastTouchPos.set(worldPos.x, worldPos.y);
            return true;
        }

        return false;
    }

    @Override
    public boolean touchUp(int screenX, int screenY, int pointer, int button) {
        if (button != 0) return false; // 只处理左键

        isDragging = false;
        isGizmoDragging = false;
        return false;
    }

    @Override
    public boolean keyDown(int keycode) {
        // 切换Gizmo模式
        if (keycode == com.badlogic.gdx.Input.Keys.G) {
            switch (gizmoMode) {
                case MOVE:
                    gizmoMode = GizmoSystem.Mode.ROTATE;
                    break;
                case ROTATE:
                    gizmoMode = GizmoSystem.Mode.SCALE;
                    break;
                case SCALE:
                    gizmoMode = GizmoSystem.Mode.MOVE;
                    break;
            }
            gizmoSystem.mode = gizmoMode;
            return true;
        }

        // 删除选中的对象
        if (keycode == com.badlogic.gdx.Input.Keys.DEL || keycode == com.badlogic.gdx.Input.Keys.FORWARD_DEL) {
            EditorTarget selection = sceneManager.getSelection();
            if (selection != null) {
                sceneManager.deleteNode(selection);
                return true;
            }
        }

        return false;
    }

    /**
     * 简单的点击测试，返回点击的对象
     */
    private EditorTarget hitTest(float worldX, float worldY) {
        // 这里应该实现更精确的点击测试
        // 暂时使用简单的边界框测试

        // 从根节点开始递归检查
        EditorTarget root = sceneManager.getRoot();
        if (root != null) {
            return hitTestRecursive(root, worldX, worldY);
        }

        return null;
    }

    private EditorTarget hitTestRecursive(EditorTarget parent, float worldX, float worldY) {
        // 先检查子节点
        for (int i = parent.getChildren().size - 1; i >= 0; i--) {
            EditorTarget child = parent.getChildren().get(i);
            EditorTarget hit = hitTestRecursive(child, worldX, worldY);
            if (hit != null) return hit;
        }

        // 检查当前节点
        if (isPointInObject(parent, worldX, worldY)) {
            return parent;
        }

        return null;
    }

    private boolean isPointInObject(EditorTarget target, float worldX, float worldY) {
        // 简单的边界框测试
        // 实际实现应该考虑对象的旋转和缩放
        float x = target.getX();
        float y = target.getY();
        float scaleX = target.getScaleX();
        float scaleY = target.getScaleY();

        // 假设对象大小为50x50
        float width = 50 * scaleX;
        float height = 50 * scaleY;

        return worldX >= x - width/2 && worldX <= x + width/2 &&
               worldY >= y - height/2 && worldY <= y + height/2;
    }

    /**
     * 设置Gizmo模式
     */
    public void setGizmoMode(GizmoSystem.Mode mode) {
        this.gizmoMode = mode;
        gizmoSystem.mode = mode;
    }

    /**
     * 获取当前Gizmo模式
     */
    public GizmoSystem.Mode getGizmoMode() {
        return gizmoMode;
    }
}
