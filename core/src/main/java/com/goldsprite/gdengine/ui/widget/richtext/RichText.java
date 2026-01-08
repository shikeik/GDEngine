package com.goldsprite.gdengine.ui.widget.richtext;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.WidgetGroup;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.SnapshotArray;
import com.goldsprite.gdengine.assets.CustomAtlasLoader;
import com.goldsprite.gdengine.assets.FontUtils;
import com.kotcrab.vis.ui.widget.VisLabel;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 富文本组件 (RichText)
 * <p>
 * 一个基于 Scene2D 的富文本容器，支持通过 BBCode 风格的标签混合显示不同样式文本和图片。
 * 支持流式布局 (Flow Layout)，自动换行。
 * </p>
 *
 * <h2>支持的标签 (Tags):</h2>
 * <ul>
 *     <li><b>Color:</b> {@code [color=red]Text[/color]} 或 {@code [#RRGGBB]Text[/color]}</li>
 *     <li><b>Size:</b> {@code [size=32]Text[/size]} (绝对字号)</li>
 *     <li><b>Image:</b> {@code [img=path/to/icon.png]} 或 {@code [img=path|widthxheight]}</li>
 *     <li><b>Event:</b> {@code [event=my_event]Click Me[/event]} (点击触发 {@link RichTextEvent})</li>
 *     <li><b>Break:</b> {@code [br]} 或 {@code [n]} (强制换行)</li>
 * </ul>
 *
 * <h2>使用示例:</h2>
 * <pre>
 * RichText rt = new RichText("获得物品: [color=gold]传说之剑[/color] [img=icon.png|32x32]");
 * stage.addActor(rt);
 *
 * // 监听点击事件
 * rt.addListener(new InputListener() {
 *     public boolean handle(Event e) {
 *         if (e instanceof RichTextEvent) {
 *             // 处理点击
 *         }
 *         return false;
 *     }
 * });
 * </pre>
 */
public class RichText extends WidgetGroup {
    
    private static final Map<Integer, BitmapFont> fontCache = new HashMap<>();
    
    private float prefHeight = 0;

    public RichText(String text) {
        this(text, 600);
    }

    public RichText(String text, float widthLimit) {
        setWidth(widthLimit);
        rebuild(text);
    }
    
    private void rebuild(String text) {
        clearChildren();
        List<RichElement> elements = RichTextParser.parse(text, new RichStyle());
        
        for (RichElement el : elements) {
            if (el.type == RichElement.Type.TEXT) {
                String[] lines = el.text.split("\n", -1);
                for (int i = 0; i < lines.length; i++) {
                    if (i > 0) addActor(new NewLineActor());
                    if (!lines[i].isEmpty()) {
                         addTextActor(lines[i], el.style);
                    }
                }
            } else if (el.type == RichElement.Type.IMAGE) {
                addImageActor(el);
            }
        }
        
        // Force layout calculation to update height immediately
        layout();
    }
    
    private void addTextActor(String text, RichStyle style) {
        BitmapFont font = getFont(style.fontSize);
        Label.LabelStyle ls = new Label.LabelStyle(font, style.color);
        VisLabel label = new VisLabel(text, ls);
        
        if (style.event != null) {
            final String eventId = style.event;
            label.addListener(new ClickListener() {
                @Override
                public void clicked(InputEvent event, float x, float y) {
                    RichText.this.fire(new RichTextEvent(eventId));
                }
            });
        }
        
        addActor(label);
    }
    
    private void addImageActor(RichElement el) {
        try {
            // Use CustomAtlasLoader to support region slicing (#regionName)
            TextureRegion region = CustomAtlasLoader.inst().getRegion(el.imagePath, el.regionName);
            
            if (region == null) {
                // If loading failed completely (file not found)
                throw new RuntimeException("Failed to load: " + el.imagePath);
            }
            
            Image img = new Image(new TextureRegionDrawable(region));
            
            float w = el.imgWidth > 0 ? el.imgWidth : region.getRegionWidth();
            float h = el.imgHeight > 0 ? el.imgHeight : region.getRegionHeight();
            
            img.setSize(w, h);
            addActor(img);
        } catch (Exception e) {
            Gdx.app.error("RichText", "Failed to load image: " + el.imagePath + (el.regionName!=null?"#"+el.regionName:""));
            VisLabel err = new VisLabel("[?]");
            err.setColor(Color.RED);
            addActor(err);
        }
    }
    
    private BitmapFont getFont(float size) {
        int s = (int)size;
        if (!fontCache.containsKey(s)) {
            fontCache.put(s, FontUtils.generate(s));
        }
        return fontCache.get(s);
    }
    
    public static void disposeStaticCache() {
        for (BitmapFont font : fontCache.values()) {
            font.dispose();
        }
        fontCache.clear();
    }
    
    private static class NewLineActor extends Actor {}

    @Override
    public void layout() {
        float maxWidth = getWidth();
        if (maxWidth <= 0) maxWidth = 600; // Fallback

        float x = 0;
        float y = 0; // We will shift later
        
        float currentLineHeight = 0;
        float totalHeight = 0;
        
        SnapshotArray<Actor> children = getChildren();
        int lineStartIndex = 0;
        
        for (int i = 0; i < children.size; i++) {
            Actor child = children.get(i);
            
            boolean isNewLine = (child instanceof NewLineActor);
            boolean isWrap = !isNewLine && (x + child.getWidth() > maxWidth && x > 0);
            
            if (isNewLine || isWrap) {
                // Finish previous line
                alignLine(lineStartIndex, i, y, currentLineHeight);
                y -= currentLineHeight; // Move down
                totalHeight += currentLineHeight;
                
                x = 0;
                currentLineHeight = 0;
                lineStartIndex = i + (isNewLine ? 1 : 0); 
            }
            
            if (isNewLine) continue;
            
            child.setX(x);
            x += child.getWidth();
            currentLineHeight = Math.max(currentLineHeight, child.getHeight());
        }
        
        // Last line
        alignLine(lineStartIndex, children.size, y, currentLineHeight);
        totalHeight += currentLineHeight;
        
        this.prefHeight = totalHeight;
        
        // Shift all actors up so that bottom-left is (0,0) or top-left is (0, height)?
        // WidgetGroup coordinate system: (0,0) is bottom-left.
        // Our 'y' started at 0 and went negative.
        // To make the top line appear at (0, height), we need to shift by +totalHeight.
        
        for (Actor child : children) {
            child.setY(child.getY() + totalHeight);
        }
    }
    
    private void alignLine(int start, int end, float lineTopY, float lineHeight) {
        SnapshotArray<Actor> children = getChildren();
        for (int i = start; i < end; i++) {
            Actor child = children.get(i);
            if (child instanceof NewLineActor) continue;
            
            // Middle align
            float offset = (lineHeight - child.getHeight()) / 2;
            child.setY(lineTopY - lineHeight + offset);
        }
    }

    @Override
    public float getPrefWidth() {
        return getWidth();
    }

    @Override
    public float getPrefHeight() {
        return prefHeight;
    }
}
