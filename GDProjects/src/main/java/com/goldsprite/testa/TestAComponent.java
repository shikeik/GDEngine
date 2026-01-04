package com.goldsprite.testa;

import com.badlogic.gdx.math.MathUtils;
import com.goldsprite.gdengine.ecs.GameWorld;
import com.goldsprite.gdengine.ecs.component.Component;
import com.goldsprite.gdengine.log.Debug;

public class TestAComponent extends Component {
	@Override public void update(float delta) {
		float roundX = MathUtils.sinDeg(360 * GameWorld.getTotalTime() * 0.5f * 100);
		transform.position.set(roundX, 0);
		Debug.logT("GDProject-TEST", "TestAComponent Moving... %s, %s", transform.position, roundX);
	}
}
