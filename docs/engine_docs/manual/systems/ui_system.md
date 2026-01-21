# UI 与富文本(未审查)

GDEngine 集成了 VisUI 作为标准 UI 库，并扩展了若干自定义控件。

## RichText (富文本)

`RichText` 组件支持 BBCode 风格的标签，允许在一段文本中混合不同的样式和图片。

### 支持标签

| 标签 | 示例 | 说明 |
| :--- | :--- | :--- |
| **Color** | `[color=red]...[/color]` | 预定义颜色名 |
| **Hex** | `[#FF0000]...[/color]` | 十六进制颜色值 |
| **Size** | `[size=32]...[/size]` | 绝对字体大小 |
| **Image** | `[img=path/icon.png]` | 插入图片 (原尺寸) |
| **Image** | `[img=icon.png\|32x32]` | 指定宽高 |
| **Event** | `[event=click_id]...[/event]` | 点击事件 |
| **Break** | `[br]` 或 `[n]` | 强制换行 |

### 使用示例

```java
String content = "获得物品: [color=gold]传说之剑[/color] [img=icons/sword.png|32x32]\n" +
                 "[size=18][color=gray]点击查看详情:[/color][/size] [event=show_info][#00FFFF]INFO[/color][/event]";

RichText rt = new RichText(content, 400); // 400px 自动换行
stage.addActor(rt);

// 监听点击
rt.addListener(new EventListener() {
    public boolean handle(Event e) {
        if (e instanceof RichTextEvent) {
            String id = ((RichTextEvent)e).eventId;
            if (id.equals("show_info")) {
                // ...
            }
            return true;
        }
        return false;
    }
});
```