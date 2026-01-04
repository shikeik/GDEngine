import com.goldsprite.gdengine.core.scripting.IGameScriptEntry;
import com.goldsprite.gdengine.ecs.GameWorld;
import com.goldsprite.gdengine.log.Debug;

public class Cc implements IGameScriptEntry {
    @Override
    public void onStart(GameWorld world) {
        Debug.logT("Script", "Cc started!");
    }
}