# 富文本组件 (RichText) 设计案

## 1. 核心需求确认
目标是创建一个开箱即用的富文本组件，API 简单：
```java
// 预期用法
RichText text = new RichText("获得物品：[color=ff0000]屠龙刀[/color] x [size=40]1[/size] [img=icons/sword.png]");
stage.addActor(text);
```

## 2. 功能特性 (Tags)
基于通用习惯，支持以下标签（支持嵌套）：

*   **颜色**: `[color=red]文本[/color]` 或 `[#FF0000]文本[/color]` (支持 16 进制和常用色名)
*   **字号**: `[size=32]大号文本[/size]` (相对于默认字号的绝对值)
*   **字体**: `[font=pixel]特殊字体[/font]` (需要预先注册字体名，或直接指定文件路径)
*   **图片**: `[img=path/to/image.png]` (内联图片，支持可选宽高 `[img=path|32x32]`)
*   **样式**: `[b]粗体[/b]` (如果字体支持), `[i]斜体[/i]` (如果字体支持)
*   **对齐**: 虽然 RichText 本身是流式布局，但支持 `[align=center]...[/align]` 可用于控制整段文本的对齐方式（可选）。
*   **点击事件**: `[event=click_sword]点击查看详情[/event]` (支持交互)

## 3. 技术实现方案

### 3.1 类结构
*   **RichText (extends VisTable)**
    *   作为容器，管理内部所有的子 Actor（Label, Image）。
    *   默认使用流式布局 (Flow Layout)，自动换行。
*   **RichTextParser**
    *   负责将原始字符串解析为 `List<Segment>`。
    *   使用**状态机 + 栈**的方式解析，完美支持标签嵌套（如 `[color=red][size=20]文本[/size][/color]`）。
*   **Segment (POJO)**
    *   包含：`text`, `imagePath`, `style` (Color, FontSize, FontName, Event)。

### 3.2 资源管理
*   **字体**: 复用 `FontUtils`。由于 Libgdx 的 `BitmapFont` 生成成本较高，建议建立一个 `FontCache`，根据 `size` 和 `path` 缓存字体实例。
*   **图片**: 默认使用 `new Texture(Gdx.files.internal(path))` 加载。为了性能，建议后续接入 `AssetManager` 或 `TextureAtlas`，但第一版先直接加载文件以保证“开箱即用”。

### 3.3 布局逻辑
*   使用 `VisTable` 的 cell 自动换行功能可能不够灵活（因为它通常是基于 grid 的）。
*   **改进方案**: 这是一个自定义的 `WidgetGroup`，在 `layout()` 方法中，遍历所有子 Actor，计算 X/Y 坐标。当 `currentX + actorWidth > width` 时，自动换行 (`currentY -= lineHeight`)。
*   **基线对齐**: 图片和不同字号的文字混排时，默认**垂直居中对齐**。

## 4. 补充疑问与自行决定项
1.  **图片加载源**: 默认假设图片路径是相对于 `assets` 目录的相对路径。
2.  **点击交互**: 我将默认实现点击事件支持，通过 `addListener` 捕获 `RichTextEvent`。
3.  **性能**: 为了减少 Draw Calls，相邻的相同样式的文本会被合并成一个 Label。

## 5. 执行计划
1.  创建 `com.goldsprite.gdengine.ui.widget.richtext` 包。
2.  实现 `RichTextParser` (核心解析逻辑)。
3.  实现 `RichText` (布局与渲染)。
4.  在 `examples` 项目中创建一个测试 Screen 验证效果。

**请确认以上设计是否符合您的预期？**
