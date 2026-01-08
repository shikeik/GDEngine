package com.goldsprite.gdengine.assets;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;
import java.util.HashMap;
import java.util.Map;

/**
 * 通用自定义 Atlas 加载器
 * <p>
 * 支持加载同级目录下 .json 描述文件，实现 TextureRegion 切片。
 * </p>
 */
public class CustomAtlasLoader {
    
    private static CustomAtlasLoader instance;
    
    // 缓存：Image Path -> Texture
    private final Map<String, Texture> textureCache = new HashMap<>();
    // 缓存：Image Path -> Atlas Metadata
    private final Map<String, AtlasData> atlasDataCache = new HashMap<>();
    
    private CustomAtlasLoader() {}
    
    public static CustomAtlasLoader inst() {
        if (instance == null) instance = new CustomAtlasLoader();
        return instance;
    }
    
    /**
     * 获取指定图片或其中的 Region
     * @param imagePath 图片路径 (相对于 assets)
     * @param regionName Region 名称 (可选，null 则返回整图)
     * @return TextureRegion，如果加载失败或找不到 region 则返回 null (或整图)
     */
    public TextureRegion getRegion(String imagePath, String regionName) {
        // 1. 获取 Texture
        if (!textureCache.containsKey(imagePath)) {
            try {
                if (!Gdx.files.internal(imagePath).exists()) {
                    Gdx.app.error("CustomAtlasLoader", "File not found: " + imagePath);
                    return null;
                }
                textureCache.put(imagePath, new Texture(Gdx.files.internal(imagePath)));
                
                // 顺便尝试加载同级 JSON
                loadAtlasData(imagePath);
                
            } catch (Exception e) {
                Gdx.app.error("CustomAtlasLoader", "Failed to load texture: " + imagePath, e);
                return null;
            }
        }
        
        Texture texture = textureCache.get(imagePath);
        
        // 2. 如果没有指定 regionName，直接返回整图
        if (regionName == null || regionName.isEmpty()) {
            return new TextureRegion(texture);
        }
        
        // 3. 查找 Region
        AtlasData data = atlasDataCache.get(imagePath);
        if (data != null && data.regions.containsKey(regionName)) {
            RegionInfo info = data.regions.get(regionName);
            
            // 计算坐标
            int x, y, w, h;
            if (data.gridWidth > 0 && data.gridHeight > 0) {
                // Grid 模式
                int cols = texture.getWidth() / data.gridWidth;
                if (cols <= 0) cols = 1;
                
                // [Bug Fix] 行列计算逻辑
                // LibGDX 纹理坐标原点在左上角 (0,0) ? 不，LibGDX Texture 默认 (0,0) 在左上角（对于 Pixmap），
                // 但绘制时 UV (0,0) 是左上角。
                // 关键是: row = index / cols; col = index % cols;
                // index 0 -> row 0, col 0
                // index 1 -> row 0, col 1
                
                int row = info.index / cols;
                int col = info.index % cols;
                
                x = col * data.gridWidth;
                y = row * data.gridHeight;
                w = data.gridWidth;
                h = data.gridHeight;
                
                // 确保不越界
                if (x + w > texture.getWidth() || y + h > texture.getHeight()) {
                     Gdx.app.error("CustomAtlasLoader", "Region out of bounds: " + regionName + " index=" + info.index);
                     return new TextureRegion(texture); // Fallback
                }
                
                return new TextureRegion(texture, x, y, w, h);
            } else {
                // 暂时只支持 Grid 模式，未来可扩展 XYWH 模式
                x = 0; y = 0; w = texture.getWidth(); h = texture.getHeight();
            }
            
            return new TextureRegion(texture, x, y, w, h);
        }
        
        // 找不到 Region，降级为整图
        Gdx.app.log("CustomAtlasLoader", "Region not found: " + regionName + " in " + imagePath);
        return new TextureRegion(texture);
    }
    
    private void loadAtlasData(String imagePath) {
        String jsonPath = imagePath.substring(0, imagePath.lastIndexOf('.')) + ".json";
        if (!Gdx.files.internal(jsonPath).exists()) return;
        
        try {
            JsonValue root = new JsonReader().parse(Gdx.files.internal(jsonPath));
            AtlasData data = new AtlasData();
            data.gridWidth = root.getInt("gridWidth", 0);
            data.gridHeight = root.getInt("gridHeight", 0);
            
            JsonValue regions = root.get("regions");
            if (regions != null) {
                for (JsonValue entry : regions) {
                    RegionInfo info = new RegionInfo();
                    if (entry.isLong()) {
                        info.index = entry.asInt();
                    }
                    data.regions.put(entry.name, info);
                }
            }
            atlasDataCache.put(imagePath, data);
        } catch (Exception e) {
            Gdx.app.error("CustomAtlasLoader", "Failed to parse json: " + jsonPath, e);
        }
    }
    
    public void dispose() {
        for (Texture t : textureCache.values()) {
            t.dispose();
        }
        textureCache.clear();
        atlasDataCache.clear();
    }
    
    // --- Data Structures ---
    
    private static class AtlasData {
        int gridWidth;
        int gridHeight;
        Map<String, RegionInfo> regions = new HashMap<>();
    }
    
    private static class RegionInfo {
        int index;
        // int x, y, w, h; // Future extension
    }
}
