package glorydark.nukkit.customform.plugin;

import cn.nukkit.plugin.InternalPlugin;
import cn.nukkit.plugin.PluginBase;

/**
 * @author glorydark
 */
public final class FakePlugin extends PluginBase {

    public static final FakePlugin INSTANCE = new FakePlugin();

    @Override
    public void onDisable() {
        this.getLogger().warning("InternalPlugin is disabled.");
    }
}