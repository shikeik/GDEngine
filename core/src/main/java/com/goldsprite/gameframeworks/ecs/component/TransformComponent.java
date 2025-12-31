package com.goldsprite.gameframeworks.ecs.component;

import com.badlogic.gdx.math.Vector2;

/**
 * 变换组件：位置、缩放、朝向
 */
public class TransformComponent extends Component {
    public Vector2 position = new Vector2();
    public Vector2 scale = new Vector2(1, 1);
    public int faceDir = 1; // 1: Right, -1: Left

    public TransformComponent() {
        super();
        // 默认位置 0,0
    }

    public void setPosition(float x, float y) {
        this.position.set(x, y);
    }

    @Override
    public void onDrawGizmos() {
        // 未来可以在这里画个十字叉表示位置
    }

    // 简单的 toString，方便调试看坐标
    @Override
    public String toString() {
        return String.format("%s Pos:%.1f,%.1f", super.toString(), position.x, position.y);
    }
}
