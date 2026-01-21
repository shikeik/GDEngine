# 编辑器扩展

你可以通过注解（Annotations）来定制你的组件在 Inspector 面板中的显示方式。

## 支持的注解

这些注解位于 `com.goldsprite.gdengine.core.annotations` 包中。

| 注解 | 作用 | 示例 |
| :--- | :--- | :--- |
| `@Header("Title")` | 在字段上方显示一个青色标题 | `@Header("Movement Settings")` |
| `@Tooltip("Msg")` | 鼠标悬停在字段标签上时显示提示 | `@Tooltip("Speed in m/s")` |
| `@ReadOnly` | 强制字段为只读（灰色，不可编辑） | `@ReadOnly public float debugVal;` |
| `@Hide` | 在 Inspector 中隐藏该 Public 字段 | `@Hide public float internalVal;` |
| `@Show` | 强制显示 Private/Protected 字段 | `@Show private int id;` |
| `@ExecuteInEditMode` | **(类注解)** 允许组件在编辑器模式下运行 Update | 见下文 |

## 编辑器模式运行

默认情况下，组件的 `update()` 方法只在游戏运行 (Play Mode) 时调用。
如果你希望你的组件在编辑器里也能实时预览（例如：根据参数自动排列子物体、程序化生成网格），可以在类上添加 `@ExecuteInEditMode`。

```java
@ExecuteInEditMode
public class AutoLayout extends Component {
    public float gap = 10f;

    @Override
    public void update(float delta) {
        // 这段代码在编辑器里拖动 gap 数值时会实时生效
        float y = 0;
        for (GObject child : transform.getChildren()) {
            child.transform.setPosition(0, y);
            y += gap;
        }
    }
}
```