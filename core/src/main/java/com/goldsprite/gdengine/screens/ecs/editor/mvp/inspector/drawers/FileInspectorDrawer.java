package com.goldsprite.gdengine.screens.ecs.editor.mvp.inspector.drawers;

import com.badlogic.gdx.files.FileHandle;
	import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Align;
import com.kotcrab.vis.ui.widget.VisLabel;
import com.kotcrab.vis.ui.widget.VisScrollPane;
import com.kotcrab.vis.ui.widget.VisTable;

public class FileInspectorDrawer implements IInspectorDrawer<FileHandle> {

    @Override
    public boolean accept(Object target) {
        return target instanceof FileHandle;
    }

    @Override
    public void draw(FileHandle file, VisTable container) {
        container.defaults().pad(5).left();

        // 1. Header Info
        VisTable header = new VisTable();
        header.setBackground("panel1");
        
        header.add(new VisLabel("File: " + file.name())).growX().row();
        VisLabel pathLabel = new VisLabel(file.path());
        pathLabel.setColor(Color.GRAY);
        pathLabel.setWrap(true);
        header.add(pathLabel).growX().row();
        header.add(new VisLabel("Size: " + file.length() + " bytes")).row();
        
        container.add(header).growX().row();

        // 2. Content Preview
        String ext = file.extension().toLowerCase();
        
        if (ext.equals("png") || ext.equals("jpg")) {
            showImagePreview(file, container);
        } 
        else if (ext.equals("java") || ext.equals("json") || ext.equals("xml") || ext.equals("scene")) {
            showTextPreview(file, container);
        } 
        else {
            VisLabel lbl = new VisLabel("No preview available.");
            lbl.setAlignment(Align.center);
            container.add(lbl).grow().pad(20);
        }
    }

    private void showImagePreview(FileHandle file, VisTable container) {
        try {
            Texture tex = new Texture(file);
            Image img = new Image(new TextureRegionDrawable(new TextureRegion(tex)));
            img.setScaling(com.badlogic.gdx.utils.Scaling.fit);
            container.add(img).grow().maxHeight(300).pad(10);
        } catch (Exception e) {
            container.add(new VisLabel("Image load error")).pad(20);
        }
    }

    private void showTextPreview(FileHandle file, VisTable container) {
        String content = file.readString("UTF-8");
        // 截取前 2000 字符防止卡顿
        if(content.length() > 2000) content = content.substring(0, 2000) + "\n... (Truncated)";
        
        VisLabel codeLabel = new VisLabel(content);
        codeLabel.setColor(Color.LIGHT_GRAY);
        codeLabel.setAlignment(Align.topLeft);
        
        VisScrollPane scroll = new VisScrollPane(codeLabel);
        scroll.setFadeScrollBars(false);
        
        container.add(scroll).grow().pad(5);
    }
}
