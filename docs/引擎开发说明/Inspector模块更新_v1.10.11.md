# Inspector 模块更新报告 (v1.10.11.x)

**日期**: 2026-01-16  
**模块**: Editor / Inspector  
**类型**: 架构升级 / Bug修复 / 性能优化  

---

## 1. 目的需求 (Objective)
*   **核心诉求**：解决 Inspector 面板数据无法从逻辑层自动同步到 UI 层的问题（即游戏运行时数据变化不更新）。
*   **特定限制**：
    *   必须实现 **30FPS 节流阀**，避免高频 UI 刷新消耗性能。
    *   **修复 Bug**：解决修改 GameObject 名称时因全量刷新导致的输入框焦点丢失问题。
*   **约束条件**：禁止大范围重构，需在现有架构（MVP + Drawer）上进行增量升级。

## 2. 方案思考原理 (Principles)
*   **数据绑定 (Polling Model)**：
    *   放弃复杂的观察者模式（避免在每个 Component 中植入事件代码），采用 **"被动轮询 (Polling)"** 机制。
    *   利用 Java 8 `Supplier<T>` 函数式接口，让 UI 组件持有 "如何获取最新数据" 的方法引用。
*   **统一驱动 (Tick Driver)**：
    *   在 `InspectorPanel` 的 `act()` 生命周期中建立统一的心跳，控制刷新频率。
*   **差异化更新 (Diff & Patch)**：
    *   只有当 `newValue != currentValue` 时才更新 UI，减少无效的 `setText` 或重绘操作。

## 3. 实际执行策略 (Execution Strategy)

### 第一阶段：基础设施升级 (`SmartInput`)
*   **变更**：在所有属性输入框的基类 `SmartInput` 中注入数据绑定能力。
*   **核心代码**：
    ```java
    // 新增字段：数据提供者
    protected Supplier<T> binding;
    // 新增方法：绑定数据源
    public void bind(Supplier<T> provider) { this.binding = provider; }
    // 新增方法：同步逻辑 (核心差异比较)
    public void sync() {
        if (binding != null) {
            T newVal = binding.get();
            if (newVal != null && !Objects.equals(this.value, newVal)) {
                this.value = newVal; // 更新内部值
                updateUI();          // 刷新界面显示
            }
        }
    }
    ```

### 第二阶段：核心控制回路 (`InspectorPanel`)
*   **变更**：实现 30FPS 节流阀，驱动全局同步。
*   **核心代码**：
    ```java
    private static final float REFRESH_RATE = 1f / 30f; // 33ms
    public void act(float delta) {
        super.act(delta);
        timer += delta;
        if (timer >= REFRESH_RATE) {
            updateValues(); // 触发递归 sync()
            timer = 0f;
        }
    }
    ```

### 第三阶段：业务逻辑适配 (Drawers)
*   **变更**：逐个修改 `GObject`, `Primitive`, `String`, `Color`, `Vector2` 等 Drawer，注入绑定逻辑。
*   **关键修复 (`GObjectInspectorDrawer`)**：
    *   **Before**: 修改名字 -> 触发 `emitStructureChanged` -> 面板重建 -> **焦点丢失**。
    *   **After**: 移除重建事件，仅绑定数据 -> 修改名字 -> 数据更新 -> **焦点保持**。
*   **特殊处理 (`ColorDrawer`)**：
    *   由于 Color 是对象引用，`sync` 时必须返回新的 Color 对象副本 (`new Color(...)`) 才能触发 `Objects.equals` 的差异检测，确保颜色面板实时跳变。

## 4. 最终实现效果 (Final Result)
1.  **实时监控**：现在运行游戏，Inspector 面板上的坐标、生命值、颜色等属性会随游戏逻辑实时跳动。
2.  **性能可控**：刷新频率被严格锁定在 30 帧/秒，即使游戏逻辑跑在 600FPS，编辑器 UI 也不会抢占过多 CPU 资源。
3.  **体验优化**：
    *   输入物体名字不再打断操作。
    *   输入框不再因为外部数据变化而产生"闪烁"或"重置"（仅在值真正变化时更新）。
4.  **稳定性**：经 `lwjgl3:run` 实机验证，系统运行稳定，无崩溃或报错。
