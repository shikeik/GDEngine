package com.goldsprite.solofight.screens.tests.iconeditor.system;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;
import com.goldsprite.gdengine.core.command.CommandManager;
import com.goldsprite.solofight.screens.tests.IconEditorDemo;
import com.goldsprite.solofight.screens.tests.iconeditor.commands.TransformCommand;
import com.goldsprite.solofight.screens.tests.iconeditor.model.EditorTarget;

public class EditorInput extends InputAdapter {
    private final IconEditorDemo screen;
    private final SceneManager sceneManager;
    private final GizmoSystem gizmo;

    private enum DragMode { NONE, BODY, ROTATE, SCALE_X, SCALE_Y, MOVE_X, MOVE_Y }
    private DragMode currentDragMode = DragMode.NONE;

    private float lastX, lastY;
    private float startValX, startValY, startValRot; // 记录初始值用于精确计算

    private float undoStartX, undoStartY, undoStartRot, undoStartSX, undoStartSY;
    private final CommandManager commandManager;

    // 对齐功能参数
    private static final float GRID_SIZE = 10.0f; // 网格大小
    private static final float ROTATION_STEP = 15.0f; // 旋转增量（度）
    private static final float SCALE_PRECISION = 0.1f; // 缩放精度（精确到0.1）
    private static final float SCALE_PRECISION_LARGE = 1.0f; // 缩放精度（精确到1.0，按住Shift时使用）

    public EditorInput(IconEditorDemo screen, SceneManager sm, GizmoSystem gizmo, CommandManager cm) {
        this.screen = screen; this.sceneManager = sm; this.gizmo = gizmo; this.commandManager = cm;
    }

    @Override
    public boolean keyDown(int keycode) {
        boolean ctrl = Gdx.input.isKeyPressed(Input.Keys.CONTROL_LEFT) || Gdx.input.isKeyPressed(Input.Keys.CONTROL_RIGHT);
        boolean shift = Gdx.input.isKeyPressed(Input.Keys.SHIFT_LEFT) || Gdx.input.isKeyPressed(Input.Keys.SHIFT_RIGHT);

        if (ctrl) {
            if (keycode == Input.Keys.Z) {
                if (shift) {
                    commandManager.redo();
                } else {
                    commandManager.undo();
                }
                return true;
            } else if (keycode == Input.Keys.Y) {
                commandManager.redo();
                return true;
            } else if (keycode == Input.Keys.S) {
                screen.saveProject();
                return true;
            }
        } else {
            // MRS Mode Switch
            if (keycode == Input.Keys.W) {
                gizmo.mode = GizmoSystem.Mode.MOVE;
                return true;
            } else if (keycode == Input.Keys.E) {
                gizmo.mode = GizmoSystem.Mode.ROTATE;
                return true;
            } else if (keycode == Input.Keys.R) {
                gizmo.mode = GizmoSystem.Mode.SCALE;
                return true;
            }
        }
        return false;
    }

    private EditorTarget findTarget(float wx, float wy) {
        return findRecursive(sceneManager.getRoot(), wx, wy);
    }

    private EditorTarget findRecursive(EditorTarget node, float wx, float wy) {
        Array<EditorTarget> children = node.getChildren();
        for (int i = children.size - 1; i >= 0; i--) {
            EditorTarget child = children.get(i);
            EditorTarget found = findRecursive(child, wx, wy);
            if (found != null) return found;
        }
        if (node.hitTest(wx, wy)) return node;
        return null;
    }

    @Override
    public boolean touchDown(int screenX, int screenY, int pointer, int button) {
        Vector2 wPos = screen.screenToWorldCoord(screenX, screenY);
        EditorTarget t = sceneManager.getSelection();

        if (t != null) {
            float zoom = screen.getWorldCamera().zoom * 1.4f; // 匹配渲染缩放
            float tx = t.getX(), ty = t.getY(), rot = t.getRotation();
            float rad = rot * MathUtils.degreesToRadians;
            float c = MathUtils.cos(rad), s = MathUtils.sin(rad);
            float axisLen = GizmoSystem.AXIS_LEN * zoom;

            // Hit Threshold
            float hitR = 15f * zoom;

            // [复刻] 碰撞检测逻辑
            if (gizmo.mode == GizmoSystem.Mode.MOVE) {
                // X Axis Tip
                float xx = tx + c * axisLen;
                float xy = ty + s * axisLen;
                if (Vector2.dst(wPos.x, wPos.y, xx, xy) < hitR) {
                    startDrag(DragMode.MOVE_X, wPos); return true; // 使用X轴移动模式
                }
                // Y Axis Tip
                float yx = tx - s * axisLen;
                float yy = ty + c * axisLen;
                if (Vector2.dst(wPos.x, wPos.y, yx, yy) < hitR) {
                    startDrag(DragMode.MOVE_Y, wPos); return true; // 使用Y轴移动模式
                }
            }
            else if (gizmo.mode == GizmoSystem.Mode.ROTATE) {
                float hx = tx + c * axisLen;
                float hy = ty + s * axisLen;
                if (Vector2.dst(wPos.x, wPos.y, hx, hy) < hitR) {
                    startDrag(DragMode.ROTATE, wPos); return true;
                }
            }
            else if (gizmo.mode == GizmoSystem.Mode.SCALE) {
                // X Scale Handle
                float xx = tx + c * axisLen;
                float xy = ty + s * axisLen;
                if (Vector2.dst(wPos.x, wPos.y, xx, xy) < hitR) {
                    startDrag(DragMode.SCALE_X, wPos); return true;
                }
                // Y Scale Handle
                float yx = tx - s * axisLen;
                float yy = ty + c * axisLen;
                if (Vector2.dst(wPos.x, wPos.y, yx, yy) < hitR) {
                    startDrag(DragMode.SCALE_Y, wPos); return true;
                }
            }

            // Body Hit (Center)
            if (Vector2.dst(wPos.x, wPos.y, tx, ty) < 20 * zoom) {
                startDrag(DragMode.BODY, wPos); return true;
            }
        }

        EditorTarget hit = findTarget(wPos.x, wPos.y);
        if (hit != null) {
            if (hit != t) {
                sceneManager.selectNode(hit);
            }
            startDrag(DragMode.BODY, wPos);
            return true;
        }

        if (t != null) {
            sceneManager.selectNode(null);
        }

        return false;
    }

    private void startDrag(DragMode mode, Vector2 pos) {
        currentDragMode = mode;
        lastX = pos.x; lastY = pos.y;
        screen.getCameraController().setInputEnabled(false);

        if(sceneManager.getSelection() != null) {
            EditorTarget sel = sceneManager.getSelection();
            startValX = sel.getScaleX();
            startValY = sel.getScaleY();
            startValRot = sel.getRotation();

            // [关键修复] 记录 Undo 初始状态
            undoStartX = sel.getX();
            undoStartY = sel.getY();
            undoStartRot = sel.getRotation();
            undoStartSX = sel.getScaleX();
            undoStartSY = sel.getScaleY();
        }
    }

    @Override
    public boolean touchDragged(int screenX, int screenY, int pointer) {
        if (currentDragMode == DragMode.NONE || sceneManager.getSelection() == null) return false;

        Vector2 wPos = screen.screenToWorldCoord(screenX, screenY);
        float dx = wPos.x - lastX;
        float dy = wPos.y - lastY;

        // 检查是否按住Shift键（用于对齐功能）
        boolean shift = Gdx.input.isKeyPressed(Input.Keys.SHIFT_LEFT) || Gdx.input.isKeyPressed(Input.Keys.SHIFT_RIGHT);

        EditorTarget t = sceneManager.getSelection();
        float rad = t.getRotation() * MathUtils.degreesToRadians;
        float c = MathUtils.cos(rad), s = MathUtils.sin(rad);

        if (currentDragMode == DragMode.BODY) {
            float newX = t.getX() + dx;
            float newY = t.getY() + dy;

            // 应用网格对齐
            if (shift) {
                newX = MathUtils.round(newX / GRID_SIZE) * GRID_SIZE;
                newY = MathUtils.round(newY / GRID_SIZE) * GRID_SIZE;
            }

            t.setX(newX);
            t.setY(newY);
        }
        else if (currentDragMode == DragMode.MOVE_X) {
            // 投影增量到X轴方向
            float proj = dx * c + dy * s;
            float newX = t.getX() + proj * c;
            float newY = t.getY() + proj * s;

            // 应用网格对齐
            if (shift) {
                newX = MathUtils.round(newX / GRID_SIZE) * GRID_SIZE;
                newY = MathUtils.round(newY / GRID_SIZE) * GRID_SIZE;
            }

            t.setX(newX);
            t.setY(newY);
        }
        else if (currentDragMode == DragMode.MOVE_Y) {
            // 投影增量到Y轴方向
            float proj = dx * (-s) + dy * c;
            float newX = t.getX() - proj * s;
            float newY = t.getY() + proj * c;

            // 应用网格对齐
            if (shift) {
                newX = MathUtils.round(newX / GRID_SIZE) * GRID_SIZE;
                newY = MathUtils.round(newY / GRID_SIZE) * GRID_SIZE;
            }

            t.setX(newX);
            t.setY(newY);
        }
        else if (currentDragMode == DragMode.ROTATE) {
            // 计算角度差 (BioWar Logic: atan2 based)
            float oldAng = MathUtils.atan2(lastY - t.getY(), lastX - t.getX()) * MathUtils.radiansToDegrees;
            float newAng = MathUtils.atan2(wPos.y - t.getY(), wPos.x - t.getX()) * MathUtils.radiansToDegrees;
            float deltaRot = newAng - oldAng;

            float newRotation = t.getRotation() + deltaRot;

            // 应用旋转对齐
            if (shift) {
                newRotation = MathUtils.round(newRotation / ROTATION_STEP) * ROTATION_STEP;
            }

            t.setRotation(newRotation);
        }
        else if (currentDragMode == DragMode.SCALE_X || currentDragMode == DragMode.SCALE_Y) {
            // 投影增量到轴向 (Project delta onto local axis)
            float axisLen = GizmoSystem.AXIS_LEN * screen.getWorldCamera().zoom * 1.4f; // 归一化参考
            float precision = shift ? SCALE_PRECISION_LARGE : SCALE_PRECISION;

            if (currentDragMode == DragMode.SCALE_X) {
                // Dot product with X axis (c, s)
                float proj = dx * c + dy * s;
                float newScaleX = t.getScaleX() + (proj / axisLen) * startValX;

                // 应用缩放对齐
                newScaleX = MathUtils.round(newScaleX / precision) * precision;
                newScaleX = Math.max(0.1f, newScaleX); // 防止缩放到0

                t.setScaleX(newScaleX);
            } else {
                // Dot product with Y axis (-s, c)
                float proj = dx * (-s) + dy * c;
                float newScaleY = t.getScaleY() + (proj / axisLen) * startValY;

                // 应用缩放对齐
                newScaleY = MathUtils.round(newScaleY / precision) * precision;
                newScaleY = Math.max(0.1f, newScaleY); // 防止缩放到0

                t.setScaleY(newScaleY);
            }
        }

        if (screen.getInspector() != null) {
            screen.getInspector().refreshValues(); // 刷新 UI 数值
        }

        lastX = wPos.x;
        lastY = wPos.y;
        return true;
    }

    @Override
    public boolean touchUp(int screenX, int screenY, int pointer, int button) {
        if (currentDragMode != DragMode.NONE) {
            currentDragMode = DragMode.NONE;
            screen.getCameraController().setInputEnabled(true);

            EditorTarget sel = sceneManager.getSelection();
            if (sel != null) {
                boolean changed = sel.getX() != undoStartX || sel.getY() != undoStartY ||
                                  sel.getRotation() != undoStartRot ||
                                  sel.getScaleX() != undoStartSX || sel.getScaleY() != undoStartSY;

                if (changed) {
                    commandManager.execute(new TransformCommand(sel,
                        undoStartX, undoStartY, undoStartRot, undoStartSX, undoStartSY,
                        () -> {
                             if(screen.getInspector() != null) screen.getInspector().refreshValues();
                        }
                    ));
                }
            }

            return true;
        }
        return false;
    }
}
