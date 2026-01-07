package ${PACKAGE_NAME};

import com.goldsprite.gdengine.core.scripting.IGameScriptEntry;
import com.goldsprite.gdengine.ecs.GameWorld;
import com.goldsprite.gdengine.log.Debug;

public class ${CLASS_NAME} implements IGameScriptEntry {
    @Override
    public void onStart(GameWorld world) {
        Debug.logT("Script", "${CLASS_NAME} started!");
    }
}
