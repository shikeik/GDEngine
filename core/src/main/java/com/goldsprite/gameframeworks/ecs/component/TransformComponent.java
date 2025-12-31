package com.goldsprite.gameframeworks.ecs.component;

import com.badlogic.gdx.math.Affine2;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;

/**
 * 变换组件 (升级版)
 * 核心职责：维护 Local 数据，计算 World 矩阵。
 */
public class TransformComponent extends Component {
    
    // ==========================================
    // 1. 局部属性 (Local - 用于逻辑控制)
    // ==========================================
    public final Vector2 position = new Vector2(); // Local Position
    public final Vector2 scale = new Vector2(1, 1);
    public float rotation = 0f; // Local Rotation (Degrees)

    // ==========================================
    // 2. 世界属性 (World - 用于渲染和物理)
    // ==========================================
    /** 
     * 世界变换矩阵 (仿射矩阵)
     * 包含了位置、旋转、缩放的所有信息。
     * 替代了旧代码中的手动计算 worldX/Y/Rot
     */
    public final Affine2 worldTransform = new Affine2();
    
    // 缓存一些分解后的世界数据 (方便快速访问，不用每次从矩阵解)
    public final Vector2 worldPosition = new Vector2();
    public float worldRotation = 0f;
    // public final Vector2 worldScale = new Vector2(); // 世界缩放稍微复杂点，暂不缓存

    public TransformComponent() {
        super();
    }

    // ==========================================
    // 3. 核心计算逻辑 (复刻 BioMath)
    // ==========================================
    
    /**
     * 更新世界矩阵
     * 逻辑：World = ParentWorld * Local
     * @param parentTransform 父级变换 (如果为 null 则视为顶层)
     */
    public void updateWorldTransform(TransformComponent parentTransform) {
        // 1. 先重置为自己的局部变换
        // 对应 BioMath: 准备 localX, localY, localRot, localScale
        worldTransform.setToTrnRotScl(position.x, position.y, rotation, scale.x, scale.y);

        // 2. 如果有父级，乘上父级矩阵
        // 对应 BioMath: 那个复杂的 localToWorld 公式
        if (parentTransform != null) {
            // LibGDX 的 preMul 相当于: Result = Parent * This
            worldTransform.preMul(parentTransform.worldTransform);
        }

        // 3. 提取常用的世界坐标 (方便其他系统使用)
        // 矩阵的 m02, m12 位置就是位移 (Translation)
        worldPosition.set(worldTransform.m02, worldTransform.m12);
        
        // 提取世界旋转 (近似值，假设无倾斜)
        if (parentTransform != null) {
            worldRotation = parentTransform.worldRotation + rotation;
        } else {
            worldRotation = rotation;
        }
    }

    // ==========================================
    // 4. 工具方法 (方便 API)
    // ==========================================

    public void setPosition(float x, float y) {
        this.position.set(x, y);
    }
    
    public void setRotation(float degrees) {
        this.rotation = degrees;
    }
    
    public void setScale(float s) {
        this.scale.set(s, s);
    }

    /** 
     * 将局部坐标点转为世界坐标 (复刻 BioMath.localToWorld)
     * 结果存入 result
     */
    public Vector2 localToWorld(Vector2 localPoint, Vector2 result) {
        worldTransform.applyTo(localPoint, result);
        return result;
    }

    /**
     * 将世界坐标点转为局部坐标 (复刻 BioMath.worldToLocal)
     */
    public Vector2 worldToLocal(Vector2 worldPoint, Vector2 result) {
        // 矩阵求逆比较耗时，如果频繁使用建议缓存逆矩阵
        Affine2 inv = new Affine2(worldTransform).inv();
        inv.applyTo(worldPoint, result);
        return result;
    }

    @Override
    public String toString() {
        return String.format("%s [L:(%.1f, %.1f) W:(%.1f, %.1f)]", 
            super.toString(), position.x, position.y, worldPosition.x, worldPosition.y);
    }
}