# Java 脚本基础(未审查)

## IGameScriptEntry 接口

这是用户代码与引擎的唯一契约。

```java
public interface IGameScriptEntry {
    // 游戏开始时调用 (Editor模式点击 Run 时)
    void onStart(GameWorld world);

    // 每帧调用 (可选)
    default void onUpdate(float delta) {}
}
```

## 全局访问点 (Gd.java)

`com.goldsprite.gdengine.core.Gd` 类提供了对引擎底层的访问：

*   `Gd.input`: 输入检测 (键盘、触摸)。
*   `Gd.files`: 文件系统操作。
*   `Gd.audio`: 音频播放。
*   `Gd.mode`: 获取当前是 `EDITOR` 模式还是 `RELEASE` 模式。

## 输入处理示例

```java
// 键盘
if (Gd.input.isKeyPressed(Input.Keys.W)) {
    // ...
}

// 触摸/鼠标
if (Gd.input.isTouched()) {
    int x = Gd.input.getX();
    int y = Gd.input.getY();
    // 注意：这是屏幕坐标，需要转换到世界坐标
    // 建议使用 Viewport 或 Camera 进行 unproject
}
```