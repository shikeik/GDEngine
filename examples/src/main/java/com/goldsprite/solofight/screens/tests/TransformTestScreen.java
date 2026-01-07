package com.goldsprite.solofight.screens.tests;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.goldsprite.gdengine.core.Gd;
import com.goldsprite.gdengine.ecs.GameWorld;
import com.goldsprite.gdengine.ecs.component.SpriteComponent;
import com.goldsprite.gdengine.ecs.component.TransformComponent;
import com.goldsprite.gdengine.ecs.entity.GObject;
import com.goldsprite.gdengine.ecs.system.SpriteSystem;
import com.goldsprite.gdengine.neonbatch.NeonBatch;
import com.goldsprite.gdengine.screens.ScreenManager;
import com.goldsprite.gdengine.screens.basics.ExampleGScreen;
import com.goldsprite.gdengine.log.Debug;

public class TransformTestScreen extends ExampleGScreen {

    private NeonBatch neonBatch;
    private GameWorld world;
    private GObject testObj;
    private float time;

    @Override
    public String getIntroduction() {
        return "Transform Scaling Test\nRed Box should pulse uniformly";
    }

    @Override
    public ScreenManager.Orientation getOrientation() {
        return ScreenManager.Orientation.Landscape;
    }

    @Override
    public void create() {
        neonBatch = new NeonBatch();
        world = new GameWorld();
        
        // Initialize Systems
        new SpriteSystem(neonBatch, getWorldCamera());

        // Create Test Entity
        testObj = new GObject("TestBox");
        // Center the object
        testObj.transform.setPosition(0, 0);
        
        SpriteComponent sprite = new SpriteComponent();
        
        // Use a generated 1x1 white texture to avoid dependency on assets
        com.badlogic.gdx.graphics.Pixmap pixmap = new com.badlogic.gdx.graphics.Pixmap(1, 1, com.badlogic.gdx.graphics.Pixmap.Format.RGBA8888);
        pixmap.setColor(Color.WHITE);
        pixmap.fill();
        Texture tex = new Texture(pixmap);
        pixmap.dispose();
        
        sprite.region = new TextureRegion(tex);
        
        sprite.width = 100;
        sprite.height = 100;
        sprite.color = Color.RED;
        testObj.addComponent(sprite);
    }

    @Override
    public void render0(float delta) {
        time += delta;
        
        // Test Logic: Pulse Scale
        float scale = 1.0f + 0.5f * (float)Math.sin(time * 2);
        testObj.transform.setScale(scale);
        testObj.transform.rotation += 45 * delta;

        // Update World
        world.update(delta);
        
        // Draw Debug Info
        neonBatch.setProjectionMatrix(getUIViewport().getCamera().combined);
        neonBatch.begin();
        // Simple text debug if needed, but visual is key
        neonBatch.end();
    }
}
