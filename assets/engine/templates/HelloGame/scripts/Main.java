package ${PACKAGE};

import com.goldsprite.gdengine.log.Debug;

public class ${MAIN_CLASS} implements IGameScriptEntry {
	@Override
	public void onStart(GameWorld world) {
		Debug.log("Game Started: " + "${PROJECT_NAME}");
	}
}
