// 文件: ./tests/src/test/java/com/goldsprite/solofight/tests/GenericAnimationTest.java
package com.goldsprite.solofight.tests;

import com.goldsprite.solofight.CLogAssert;
import com.goldsprite.solofight.GdxTestRunner;
import com.goldsprite.gdengine.ecs.skeleton.animation.NeonCurve;
import com.goldsprite.gdengine.ecs.skeleton.animation.NeonProperty;
import com.goldsprite.gdengine.ecs.skeleton.animation.NeonTimeline;

import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(GdxTestRunner.class)
public class GenericAnimationTest {

    @Test
    public void testFloatRegression() {
        System.out.println(">>> 验证: 浮点数插值回归测试 (Float Value)");

        // 依然使用原来的 API，但底层已经是新的 NeonKeyframe 结构了
        NeonTimeline timeline = new NeonTimeline("Arm", NeonProperty.ROTATION);
        timeline.addKeyframe(0.0f, 0f, NeonCurve.LINEAR);
        timeline.addKeyframe(1.0f, 100f, NeonCurve.LINEAR);

        // 验证 evaluate(float) 是否还能正常工作
        float val = timeline.evaluate(0.5f);
        System.out.println("Float Lerp(0.5): " + val);
        CLogAssert.assertEquals("浮点插值应不受泛型改造影响", 50f, val);
    }

    @Test
    public void testObjectStepped() {
        System.out.println(">>> 验证: 对象关键帧跳变 (Object Value)");

        NeonTimeline timeline = new NeonTimeline("Sprite", NeonProperty.SPRITE);

        // 模拟帧动画数据 (String 代表图片对象)
        // 0.0s -> "Frame_1"
        // 0.5s -> "Frame_2"
        // 1.0s -> "Frame_3"
        timeline.addKeyframe(0.0f, "Frame_1");
        timeline.addKeyframe(0.5f, "Frame_2");
        timeline.addKeyframe(1.0f, "Frame_3");

        // 1. 测试区间内 (0.0 ~ 0.49) -> 应该是 Frame_1
        Object v1 = timeline.evaluateObject(0.25f);
        CLogAssert.assertEquals("0.25s 应为 Frame_1", "Frame_1", v1);

        // 2. 测试边界跳变 (0.5) -> 应该是 Frame_2
        Object v2 = timeline.evaluateObject(0.5f);
        CLogAssert.assertEquals("0.5s 应瞬间跳变为 Frame_2", "Frame_2", v2);

        // 3. 测试区间内 (0.51 ~ 0.99) -> 应该是 Frame_2
        Object v3 = timeline.evaluateObject(0.9f);
        CLogAssert.assertEquals("0.9s 应保持 Frame_2", "Frame_2", v3);

        // 4. 测试结束 (1.5) -> 应该是 Frame_3
        Object v4 = timeline.evaluateObject(1.5f);
        CLogAssert.assertEquals("超时应保持 Frame_3", "Frame_3", v4);
    }
}
