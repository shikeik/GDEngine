package com.goldsprite.solofight.screens.tests.iconeditor.system;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.MathUtils;
import com.goldsprite.gdengine.core.command.CommandManager;
import com.goldsprite.gdengine.neonbatch.NeonBatch;
import com.goldsprite.solofight.screens.tests.iconeditor.model.EditorTarget;

public class GizmoSystem {
    public enum Mode { MOVE, ROTATE, SCALE }
    public Mode mode = Mode.MOVE;
    private final SceneManager sceneManager;

    // [复刻] 视觉配置 (参考 BioGizmoDrawer)
    public static float HANDLE_SIZE = 15f;
    public static float AXIS_LEN = 60f;
    public static float GIZMO_OUTLINE_WIDTH = 0.8f;

    // 缓存数组 (绘制箭头用)
    private static final float[] tmpPoly = new float[8];
    
    private final CommandManager commandManager;

    public GizmoSystem(SceneManager sm, CommandManager cm) { this.sceneManager = sm; this.commandManager = cm; }

    public void render(NeonBatch batch, float zoom) {
        EditorTarget t = sceneManager.getSelection();
        if (t == null) return;

        float x = t.getX();
        float y = t.getY();
        float rot = t.getRotation();
        float s = zoom * 1.4f; // 缩放系数调整

        // 1. 中心点
        drawDualCircle(batch, s, x, y, 5f * s, Color.YELLOW, true);

        float rad = rot * MathUtils.degreesToRadians;
        float cos = MathUtils.cos(rad);
        float sin = MathUtils.sin(rad);
        float centerDist = AXIS_LEN * s;

        // 2. 根据模式绘制手柄
        if (mode == Mode.MOVE) {
            float arrowSize = 14f * s;
            float halfArrow = arrowSize * 0.6f;
            float lineLen = centerDist - halfArrow;

            // X轴 (红)
            float endXx = x + cos * lineLen;
            float endXy = y + sin * lineLen;
            drawDualLine(batch, s, x, y, endXx, endXy, 2f * s, Color.RED);
            drawArrowHead(batch, s, endXx, endXy, rot, arrowSize, Color.RED);

            // Y轴 (绿)
            float endYx = x - sin * lineLen;
            float endYy = y + cos * lineLen;
            drawDualLine(batch, s, x, y, endYx, endYy, 2f * s, Color.GREEN);
            drawArrowHead(batch, s, endYx, endYy, rot + 90f, arrowSize, Color.GREEN);
        } 
        else if (mode == Mode.ROTATE) {
            float hx = x + cos * centerDist;
            float hy = y + sin * centerDist;
            drawDualLine(batch, s, x, y, hx, hy, 1.5f * s, Color.YELLOW);
            drawDualCircle(batch, s, x, y, centerDist, Color.CYAN, false); // 大圆环
            drawDualCircle(batch, s, hx, hy, HANDLE_SIZE/2 * s, Color.YELLOW, true); // 手柄
        } 
        else if (mode == Mode.SCALE) {
            float boxSize = 10f * s;

            // X轴
            float endXx = x + cos * centerDist;
            float endXy = y + sin * centerDist;
            drawDualLine(batch, s, x, y, endXx, endXy, 1.5f * s, Color.RED);
            drawDualRect(batch, s, endXx, endXy, boxSize, boxSize, rot, Color.RED);

            // Y轴
            float endYx = x - sin * centerDist;
            float endYy = y + cos * centerDist;
            drawDualLine(batch, s, x, y, endYx, endYy, 1.5f * s, Color.GREEN);
            drawDualRect(batch, s, endYx, endYy, boxSize, boxSize, rot, Color.GREEN);
        }
    }

    // --- 绘图辅助 (移植自 BioGizmoDrawer) ---

    private void drawDualLine(NeonBatch batch, float s, float x1, float y1, float x2, float y2, float w, Color c) {
        batch.drawLine(x1, y1, x2, y2, w + GIZMO_OUTLINE_WIDTH*s*2, Color.WHITE); // 包边
        batch.drawLine(x1, y1, x2, y2, w, c);
    }

    private void drawDualCircle(NeonBatch batch, float s, float x, float y, float r, Color c, boolean fill) {
        float w = 1.5f * s;
        if(fill) {
            batch.drawCircle(x, y, r + GIZMO_OUTLINE_WIDTH*s, 0, Color.WHITE, 16, true);
            batch.drawCircle(x, y, r, 0, c, 16, true);
        } else {
            batch.drawCircle(x, y, r, w + GIZMO_OUTLINE_WIDTH*s*2, Color.WHITE, 32, false);
            batch.drawCircle(x, y, r, w, c, 32, false);
        }
    }

    private void drawDualRect(NeonBatch batch, float s, float cx, float cy, float w, float h, float rot, Color c) {
        float ow = GIZMO_OUTLINE_WIDTH * s;
        float outW = w + ow * 2;
        float outH = h + ow * 2;
        batch.drawRect(cx - outW/2f, cy - outH/2f, outW, outH, rot, 0, Color.WHITE, true);
        batch.drawRect(cx - w/2f, cy - h/2f, w, h, rot, 0, c, true);
    }

    private void drawArrowHead(NeonBatch batch, float s, float bx, float by, float angleDeg, float size, Color c) {
        float rad = angleDeg * MathUtils.degreesToRadians;
        float cos = MathUtils.cos(rad);
        float sin = MathUtils.sin(rad);

        float tipX = bx + cos * size;
        float tipY = by + sin * size;
        float halfW = size * 0.5f;

        float p1x = bx - sin * halfW;
        float p1y = by + cos * halfW;
        float p2x = bx + sin * halfW;
        float p2y = by - cos * halfW;

        tmpPoly[0] = tipX; tmpPoly[1] = tipY;
        tmpPoly[2] = p1x;  tmpPoly[3] = p1y;
        tmpPoly[4] = p2x;  tmpPoly[5] = p2y;

        // 描边模拟
        float ow = GIZMO_OUTLINE_WIDTH * s * 2f;
        batch.drawPolygon(tmpPoly, 3, 0, Color.WHITE, true); // White BG
        batch.drawPolygon(tmpPoly, 3, ow, Color.WHITE, false); // White Outline
        batch.drawPolygon(tmpPoly, 3, 0, c, true); // Inner Color
    }
}
