package com.goldsprite.solofight.refactor.ecs.component;

import com.goldsprite.solofight.core.DebugUI;

public class LifecycleLogComponent extends Component {

    @Override
    public void awake() {
        super.awake();
        DebugUI.log("[Lifecycle] %s: Awake!", getGObject().getName());
    }

    @Override
    public void update(float delta) {
        super.update(delta);
        // 不打印 update，否则日志会炸
    }

    @Override
    public void destroy() {
        DebugUI.log("[Lifecycle] %s: Destroy Called (Available next frame)", getGObject().getName());
        super.destroy();
    }

    @Override
    public void destroyImmediate() {
        DebugUI.log("[Lifecycle] %s: Destroy Immediate (Memory Freed)", getGObject().getName());
        super.destroyImmediate();
    }
}
