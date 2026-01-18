package ${PACKAGE};

import com.goldsprite.gdengine.core.scripting.IGameScriptEntry;
import com.goldsprite.gdengine.ecs.GameWorld;
import com.goldsprite.gdengine.log.Debug;

public class ${MAIN_CLASS} implements IGameScriptEntry {
	@Override
	public void onStart(GameWorld world) {
		Debug.logT("UserProject", "Game Started: Hello " + "${PROJECT_NAME}!");
	}
}
