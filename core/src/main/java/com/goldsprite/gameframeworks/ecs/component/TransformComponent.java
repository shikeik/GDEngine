package com.goldsprite.gameframeworks.ecs.component;

import com.badlogic.gdx.math.Affine2;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;

/**
 * 变换组件 (升级版 - API 修正)
 */
public class TransformComponent extends Component {

	public final Vector2 position = new Vector2();
	public final Vector2 scale = new Vector2(1, 1);
	public float rotation = 0f;

	// 世界数据 (由 updateWorldTransform 计算)
	public final Affine2 worldTransform = new Affine2();
	public final Vector2 worldPosition = new Vector2();
	public float worldRotation = 0f;

	public TransformComponent() {
		super();
	}

	/** 核心矩阵计算：World = Parent * Local */
	public void updateWorldTransform(TransformComponent parentTransform) {
		// 1. 设置自己的局部矩阵
		worldTransform.setToTrnRotScl(position.x, position.y, rotation, scale.x, scale.y);

		// 2. 乘以父级矩阵
		if (parentTransform != null) {
			worldTransform.preMul(parentTransform.worldTransform);
		}

		// 3. 提取世界坐标 (m02=x, m12=y)
		worldPosition.set(worldTransform.m02, worldTransform.m12);

		// 4. 提取近似世界旋转 (非倾斜情况下)
		if (parentTransform != null) {
			worldRotation = parentTransform.worldRotation + rotation;
		} else {
			worldRotation = rotation;
		}
	}

	public void setPosition(float x, float y) { this.position.set(x, y); }
	public void setRotation(float degrees) { this.rotation = degrees; }
	public void setScale(float s) { this.scale.set(s, s); }

	/** [修正] 局部 -> 世界 */
	public Vector2 localToWorld(Vector2 localPoint, Vector2 result) {
		result.set(localPoint); // 先拷贝
		worldTransform.applyTo(result); // 再变换
		return result;
	}

	/** [修正] 世界 -> 局部 */
	public Vector2 worldToLocal(Vector2 worldPoint, Vector2 result) {
		Affine2 inv = new Affine2(worldTransform).inv();
		result.set(worldPoint);
		inv.applyTo(result);
		return result;
	}

	@Override
	public String toString() {
		return String.format("%s [L:(%.1f, %.1f) W:(%.1f, %.1f)]", 
							 super.toString(), position.x, position.y, worldPosition.x, worldPosition.y);
	}
}
