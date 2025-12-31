package com.goldsprite.solofight.screens.newyear;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.goldsprite.solofight.core.neonbatch.NeonBatch;

public class FireworkParticle {
    Vector2 pos = new Vector2();
    Vector2 vel = new Vector2();
    Color color = new Color();
    float life = 1f, maxLife = 1f;
    int type; // 0:Rocket, 1:Spark, 2:Meteor

    // 拖尾历史
    float[] trailX = new float[8];
    float[] trailY = new float[8];

    public FireworkParticle(float x, float y, int type, Color c) {
        pos.set(x, y);
        this.type = type;
        this.color.set(c);
        
        if (type == 0) { // Rocket
            vel.set(MathUtils.random(-30, 30), MathUtils.random(700, 900));
            life = maxLife = MathUtils.random(1.0f, 1.3f);
            NewYearSymphony.playLaunch(); // 🚀 音效
        } else if (type == 1) { // Spark
            float angle = MathUtils.random(0, 360);
            float speed = MathUtils.random(50, 500);
            vel.set(MathUtils.cosDeg(angle)*speed, MathUtils.sinDeg(angle)*speed);
            life = maxLife = MathUtils.random(0.5f, 1.5f);
        } else { // Meteor
            // 【修复点】 必须 小在前，大在后！
            // 之前的写法 (-800, -1200) 导致了 crash
            vel.set(MathUtils.random(-1200, -800), MathUtils.random(-600, -400)); 
            life = maxLife = 2.0f;
        }
    }

    public boolean update(float dt) {
        life -= dt;
        
        // 更新拖尾
        for(int i=trailX.length-1; i>0; i--) { trailX[i]=trailX[i-1]; trailY[i]=trailY[i-1]; }
        trailX[0]=pos.x; trailY[0]=pos.y;

        pos.mulAdd(vel, dt);
        
        if (type == 0) vel.y -= 100 * dt; // 火箭微重力
        else if (type == 1) {
            vel.y -= 300 * dt; // 爆炸碎片重力
            vel.scl(0.98f); // 空气阻力
        }
        
        return life > 0;
    }

    public void draw(NeonBatch batch) {
        float alpha = life / maxLife;
        // 闪烁效果
        if (type == 1 && life < 0.5f && MathUtils.randomBoolean()) alpha = 0.1f;

        batch.setColor(color.r, color.g, color.b, alpha);
        
        // 画长拖尾
        for(int i=0; i<trailX.length-1; i++) {
            if(trailX[i]==0) continue;
            float tAlpha = alpha * (1f - (float)i/trailX.length);
            batch.drawLine(trailX[i], trailY[i], trailX[i+1], trailY[i+1], (type==2?4f:2f)*tAlpha, new Color(color.r, color.g, color.b, tAlpha));
        }

        if (type == 0) {
            batch.drawRect(pos.x, pos.y, 6, 20, vel.angleDeg()-90, 0, color, true);
        } else if (type == 2) {
            // 流星头
            batch.drawCircle(pos.x, pos.y, 5, 0, Color.WHITE, 8, true); 
        } else {
            // 爆炸碎片
            batch.drawRegularPolygon(pos.x, pos.y, 4, 4, life*720, 0, color, true);
        }
    }
}