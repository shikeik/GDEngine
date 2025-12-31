package com.goldsprite.solofight.screens.newyear;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.utils.Array;
import com.goldsprite.gameframeworks.assets.FontUtils;
import com.goldsprite.gameframeworks.screens.ScreenManager;
import com.goldsprite.gameframeworks.screens.basics.ExampleGScreen;
import com.goldsprite.solofight.core.neonbatch.BloomRenderer;
import com.goldsprite.solofight.core.neonbatch.NeonBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;

public class NewYearScreen extends ExampleGScreen {

    private NeonBatch batch;
    private ShapeRenderer shapes;
    private BloomRenderer bloom;
    private BitmapFont font;
    
    private Array<FireworkParticle> particles = new Array<>();
    private float[] starsX = new float[100];
    private float[] starsY = new float[100];
    private float[] starsAlpha = new float[100];
    
    private float timer = 0;
    private float gridScroll = 0;
    private float beatPulse = 1f;

    @Override
    public ScreenManager.Orientation getOrientation() { return ScreenManager.Orientation.Portrait; }

    @Override
    public void create() {
        batch = new NeonBatch();
        shapes = new ShapeRenderer();
        bloom = new BloomRenderer();
        bloom.resize((int)getViewSize().x, (int)getViewSize().y);
        bloom.intensity = 1.8f; // 极致辉光
        bloom.blurScale = 4f; // 大范围光晕
        
        font = FontUtils.generate(70); // 巨大字体
        
        // 初始化星空
        for(int i=0; i<100; i++) {
            starsX[i] = MathUtils.random(0, 2000); // 宽范围覆盖
            starsY[i] = MathUtils.random(300, 1200); // 只在天上
            starsAlpha[i] = MathUtils.random();
        }
        
        NewYearSymphony.start();
    }

    @Override
    public void render0(float delta) {
        timer += delta;
        gridScroll += delta * 60;
        
        // 节奏脉冲 (100 BPM approx 0.6s)
        beatPulse -= delta * 2f;
        if (beatPulse < 1f) beatPulse = 1f;
        if (timer % 0.6f < delta) beatPulse = 1.1f; // 动次打次

        // --- 逻辑更新 ---
        
        // 1. 发射烟花 (频率随时间加快)
        float spawnRate = Math.max(0.3f, 1.5f - timer * 0.05f);
        if (MathUtils.random() < delta / spawnRate) {
            spawnRocket();
        }
        
        // 2. 随机流星
        if (MathUtils.random() < delta * 0.3f) {
            particles.add(new FireworkParticle(getUIViewport().getWorldWidth()+100, MathUtils.random(500, 900), 2, Color.WHITE));
        }

        // 3. 粒子更新
        for (int i = particles.size - 1; i >= 0; i--) {
            FireworkParticle p = particles.get(i);
            if (!p.update(delta)) {
                if (p.type == 0) explode(p.pos.x, p.pos.y, p.color); // 火箭爆炸
                particles.removeIndex(i);
            }
        }

        // --- 渲染流程 ---
		
        // 1. 画渐变夜空 (使用 ShapeRenderer)
        shapes.setProjectionMatrix(getUIViewport().getCamera().combined);
        shapes.begin(ShapeRenderer.ShapeType.Filled);
        // 天顶: 深紫, 地平线: 深蓝黑
        shapes.rect(0, 0, getUIViewport().getWorldWidth(), getUIViewport().getWorldHeight(), 
            Color.valueOf("050011"), Color.valueOf("050011"), 
            Color.valueOf("1a0033"), Color.valueOf("1a0033"));
        shapes.end();

        // 2. 启动 Bloom 绘制高亮物体
        bloom.captureStart(batch);
        batch.setProjectionMatrix(getUIViewport().getCamera().combined);
        batch.begin();
        
        // A. 星空 (闪烁)
        batch.setColor(Color.WHITE);
        for(int i=0; i<100; i++) {
            starsAlpha[i] += (MathUtils.random()-0.5f) * delta * 5;
            batch.drawRect(starsX[i], starsY[i], 2, 2, 0, 0, Color.WHITE, true);
        }

        // B. 复古网格 (Retro Grid)
        drawRetroGrid();

        // C. 粒子 (烟花 & 流星)
        for (FireworkParticle p : particles) p.draw(batch);

        // D. 巨型标题 (带 Pulse)
        if (timer > 2f) {
            String text = "HAPPY NEW YEAR\n2026";
            float scale = beatPulse;
            font.getData().setScale(scale);
            
            GlyphLayout layout = new GlyphLayout(font, text);
            float cx = getUIViewport().getWorldWidth()/2;
            float cy = getUIViewport().getWorldHeight()/2 + 150;
            
            // 幻影偏移
            font.setColor(0, 1, 1, 0.3f);
            font.draw(batch, text, cx - layout.width/2 - 5, cy + layout.height/2 + 5);
            font.setColor(1, 0, 1, 0.3f);
            font.draw(batch, text, cx - layout.width/2 + 5, cy + layout.height/2 - 5);
            
            // 本体
            font.setColor(1, 1, 1, 1f);
            font.draw(batch, text, cx - layout.width/2, cy + layout.height/2);
        }

        batch.end();
        bloom.captureEnd();
        // 3. 合成辉光
		bloom.process();
        
        bloom.render(batch);
    }

    private void spawnRocket() {
        float x = MathUtils.random(100, getUIViewport().getWorldWidth()-100);
        Color c = MathUtils.randomBoolean() ? Color.valueOf("00eaff") : // Cyan
                 (MathUtils.randomBoolean() ? Color.valueOf("ff0055") : Color.valueOf("ffcc00")); // Magenta / Gold
        particles.add(new FireworkParticle(x, 0, 0, c));
    }

    private void explode(float x, float y, Color c) {
        NewYearSymphony.playExplosion(); // 💥 音效
        int count = MathUtils.random(40, 80);
        for(int i=0; i<count; i++) {
            particles.add(new FireworkParticle(x, y, 1, c));
        }
        // 加个白色闪光核心
        particles.add(new FireworkParticle(x, y, 1, Color.WHITE));
    }

    private void drawRetroGrid() {
        float w = getUIViewport().getWorldWidth();
        float h = 500;        // 地平线高度
        float horizonY = 500; // 消失点 Y 坐标
        float centerX = w / 2f; // 消失点 X 坐标

        // 太阳/月亮在地平线 (保持不变)
        batch.drawCircle(centerX, horizonY, 80, 0, Color.valueOf("ff0055"), 32, true);

        batch.setColor(1, 0, 1, 0.4f); // Neon Purple

        // --- 1. 定义网格的底座宽度 ---
        // 为了让网格铺满屏幕底部，我们需要让它比屏幕宽很多
        // 假设底部宽度向左右各延伸 1.5 倍屏幕宽
        float bottomSpread = w * 1.5f; 

        // --- 2. 绘制纵向放射线 (Vertical Lines) ---
        // 从消失点射向底部
        int vLineCount = 10; // 线条数量
        for (int i = -vLineCount / 2; i <= vLineCount / 2; i++) {
            // 计算底部的 X 坐标 (均匀分布)
            // i=0 是中心，往两边扩散
            float bottomX = centerX + i * (bottomSpread * 2 / vLineCount);

            // 起点：消失点 (centerX, horizonY)
            // 终点：屏幕底部 (bottomX, 0)
            // 实际上为了不穿帮，我们画到 y=0
            batch.drawLine(centerX, horizonY, bottomX, 0, 2f, batch.getColor());
        }

        // --- 3. 绘制横向滚动线 (Horizontal Lines) ---
        // 关键：宽度随高度变化
        for (float y = 0; y < h; y += 60) {
            // 计算滚动的视觉 Y 坐标
            float visualY = (y + gridScroll) % h;

            // 计算透视比例 (Ratio)
            // visualY = 0 (底部) -> ratio = 1.0 (最宽)
            // visualY = h (地平线) -> ratio = 0.0 (汇聚成点)
            float ratio = 1f - (visualY / h);

            // 根据比例计算当前的线宽
            float currentHalfWidth = bottomSpread * ratio;

            // 计算左右端点
            float x1 = centerX - currentHalfWidth;
            float x2 = centerX + currentHalfWidth;

            // 越远越暗，越细
            // alpha: 底部 0.4 -> 地平线 0
            float alpha = ratio * 0.4f; 
            Color c = batch.getColor();
            batch.setColor(c.r, c.g, c.b, alpha*2);

            // 绘制横线
            float lineWidth = 2f + ratio * 1f; // 近处粗，远处细
            batch.drawLine(x1, visualY, x2, visualY, lineWidth, batch.getColor());
        }

        // --- 4. 地平线发光 ---
        batch.setColor(Color.CYAN);
        batch.drawLine(0, horizonY, w, horizonY, 4f, batch.getColor());
    }

    @Override
    public void dispose() {
        NewYearSymphony.stop();
        if (batch != null) batch.dispose();
        if (bloom != null) bloom.dispose();
        if (font != null) font.dispose();
        if (shapes != null) shapes.dispose();
    }
}
