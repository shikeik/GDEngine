package com.goldsprite.solofight.refactor.ecs.component;

import com.goldsprite.solofight.core.Debug;

public class LifecycleLogComponent extends Component {

	@Override
	public void awake() {
		super.awake();
		Debug.log("[Lifecycle] %s: Awake!", getGObject().getName());
	}

	@Override
	public void update(float delta) {
		super.update(delta);
		// 不打印 update，否则日志会炸
	}

	@Override
	public void destroy() {
		Debug.log("[Lifecycle] %s: Destroy Called (Available next frame)", getGObject().getName());
		super.destroy();
	}

	@Override
	public void destroyImmediate() {
		Debug.log("[Lifecycle] %s: Destroy Immediate (Memory Freed)", getGObject().getName());
		super.destroyImmediate();
	}
}
