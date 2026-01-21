# NeonBatch 绘图(未审查)

`NeonBatch` 是 `SpriteBatch` 的增强版，内置了强大的矢量图形绘制能力。它在编辑器 Gizmo 绘制和游戏内 UI/HUD 绘制中被广泛使用。

## 基础图形

```java
// 绘制矩形 (支持旋转)
// x, y, width, height, rotation, lineWidth, color, filled
batch.drawRect(100, 100, 50, 50, 45, 2, Color.RED, false);

// 绘制圆形
batch.drawCircle(200, 200, 30, 2, Color.BLUE, 32, true);

// 绘制线段
batch.drawLine(0, 0, 100, 100, 5, Color.GREEN);
```

## 高级特性

1.  **Miter Join (斜接)**:
    当使用 `pathStroke` 绘制折线或多边形边框时，`NeonBatch` 会自动计算拐角处的斜接顶点，确保线条连接处尖锐平滑，而不是断开的。

2.  **贝塞尔曲线**:
    支持二阶和三阶贝塞尔曲线绘制。
    ```java
    // 绘制二阶贝塞尔 (起点, 控制点, 终点)
    batch.drawQuadraticBezier(x0, y0, cx, cy, x1, y1, 2f, Color.YELLOW, 20);
    ```

3.  **多边形与星形**:
    ```java
    // 绘制五角星
    batch.drawStar(x, y, outerRadius, innerRadius, 5, rotation, 2f, Color.GOLD, true);
    ```