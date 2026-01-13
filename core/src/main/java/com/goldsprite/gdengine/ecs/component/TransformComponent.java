package com.goldsprite.gdengine.ecs.component;

import com.badlogic.gdx.math.Affine2;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.goldsprite.gdengine.core.annotations.Hide;
import com.goldsprite.gdengine.core.annotations.ReadOnly;
import com.goldsprite.gdengine.core.annotations.Tooltip;

/**
 * 变换组件 (Matrix v3.0)
 * 基于 Affine2 矩阵的层级变换系统。
 */
public class TransformComponent extends Component {

	// --- 核心数据 (Local - 序列化) ---
	@Tooltip("变换组件的位置")
	public final Vector2 position = new Vector2();
	@Tooltip("变换组件的旋转角度 (单位: 度)")
	public float rotation = 0f;
	public final Vector2 scale = new Vector2(1, 1);

	// --- 矩阵缓存 (World - 运行时计算) ---
	@Hide public final Affine2 localTransform = new Affine2();
	@Hide public final Affine2 worldTransform = new Affine2();

	// --- 派生数据缓存 (World Cache - 只读) ---
	public final Vector2 worldPosition = new Vector2();
	public float worldRotation = 0f;
	public final Vector2 worldScale = new Vector2(1, 1);

	public TransformComponent() {
		super();
	}

	/**
	 * 核心矩阵计算 和 数据分解
	 * World = ParentWorld * Local
	 */
	public void updateWorldTransform(TransformComponent parentTransform) {
		// 1. 构建局部矩阵 (T * R * S)
		localTransform.setToTrnRotScl(position.x, position.y, rotation, scale.x, scale.y);

		// 2. 计算世界矩阵
		if (parentTransform != null) {
			worldTransform.set(parentTransform.worldTransform).mul(localTransform);
		} else {
			worldTransform.set(localTransform);
		}

		// 3. 立即分解 (Decompose) 更新缓存
		// 位移
		worldPosition.set(worldTransform.m02, worldTransform.m12);

		// 旋转 (atan2)
		worldRotation = MathUtils.radiansToDegrees * (float)Math.atan2(worldTransform.m10, worldTransform.m00);

		// 缩放 (基向量长度)
		// sx = len(m00, m10), sy = len(m01, m11)
		worldScale.x = (float)Math.sqrt(worldTransform.m00 * worldTransform.m00 + worldTransform.m10 * worldTransform.m10);
		worldScale.y = (float)Math.sqrt(worldTransform.m01 * worldTransform.m01 + worldTransform.m11 * worldTransform.m11);
	}

	// --- API (Local) ---
	public void setPosition(float x, float y) { position.set(x, y); }
	public void setRotation(float deg) { rotation = deg; }
	public void setScale(float s) { scale.set(s, s); }
	public void setScale(float x, float y) { scale.set(x, y); }

	// --- API (World - 逆向计算) ---

	/**
	 * 设置世界坐标
	 * 自动计算逆矩阵，反推 LocalPosition
	 */
	public void setWorldPosition(float wx, float wy) {
		// [修复] 移除了错误的 `transform == null` 判断
		// 只要有父级，就需要将世界坐标转换到父级的局部空间
		if (gobject != null && gobject.getParent() != null) {
			// 获取父级组件引用
			TransformComponent parent = gobject.getParent().transform;
			if (parent != null) {
				// Local = ParentInv * World
				Affine2 inv = new Affine2(parent.worldTransform).inv();
				Vector2 local = new Vector2(wx, wy);
				inv.applyTo(local);
				this.position.set(local);
				return;
			}
		}
		// 无父级，Local = World
		this.position.set(wx, wy);
	}

	public void setWorldPosition(Vector2 wPos) {
		setWorldPosition(wPos.x, wPos.y);
	}

	/**
	 * 设置世界旋转
	 * Local = Target - ParentWorld
	 */
	public void setWorldRotation(float wRot) {
		float parentRot = 0;
		if (gobject != null && gobject.getParent() != null) {
			parentRot = gobject.getParent().transform.worldRotation;
		}
		this.rotation = wRot - parentRot;
	}

	// --- 坐标转换工具 ---
	public Vector2 worldToLocal(Vector2 worldPoint, Vector2 result) {
		Affine2 inv = new Affine2(worldTransform).inv();
		result.set(worldPoint);
		inv.applyTo(result);
		return result;
	}

	public Vector2 localToWorld(Vector2 localPoint, Vector2 result) {
		result.set(localPoint);
		worldTransform.applyTo(result);
		return result;
	}

	@Override
	public String toString() {
		return "Transform";
	}
}
