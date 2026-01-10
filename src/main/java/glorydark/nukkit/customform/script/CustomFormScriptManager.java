package glorydark.nukkit.customform.script;

import cn.nukkit.Player;
import cn.nukkit.utils.Utils;
import glorydark.nukkit.customform.CustomFormMain;

import javax.script.ScriptEngine;
import javax.script.ScriptException;
import java.io.File;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * @author glorydark
 */
public class CustomFormScriptManager {

    private static final Map<String, String> scripts = new LinkedHashMap<>();

    private static final ScriptEngine engine = new RhinoScriptEngine();

    public static void loadScripts() {
        scripts.clear();

        File dir = new File(CustomFormMain.path + "/scripts/");
        dir.mkdirs();
        for (File child : Objects.requireNonNull(dir.listFiles())) {
            loadScripts(child, "");
        }
        CustomFormMain.plugin.getLogger().info(CustomFormMain.language.translateString(
                null, "script.load.total", scripts.keySet().size()));
    }

    public static void loadScripts(File file, String prefix) {
        if (file == null || !file.exists()) return;

        if (file.isDirectory()) {
            for (File child : Objects.requireNonNull(file.listFiles())) {
                loadScripts(child, prefix + file.getName() + "/");
            }
            return;
        }

        String identifier = prefix + file.getName().replace(".js", "");
        try {
            scripts.put(identifier, Utils.readFile(file));
            CustomFormMain.plugin.getLogger().info(CustomFormMain.language.translateString(null, "script.load", identifier));
        } catch (IOException e) {
            CustomFormMain.plugin.getLogger().info(CustomFormMain.language.translateString(null, "script.load.failed", identifier));
            e.printStackTrace();
        }
    }

    public static void executeLoadedScript(Player player, String scriptId) {
        if (!scripts.containsKey(scriptId)) {
            CustomFormMain.plugin.getLogger().error("Script not found: " + scriptId);
            return;
        }
        executeScript(player, scripts.getOrDefault(scriptId, ""));
    }

    public static void executeScript(Player player, String script) {
        try {
            engine.put("player", player);
            engine.put("server", CustomFormMain.plugin.getServer());
            engine.put("api", new ScriptPlayerAPI());
            engine.put("plugin", CustomFormMain.plugin);

            engine.eval(script);
        } catch (ScriptException e) {
            player.sendMessage("§cError in loading script: " + e.getMessage());
        }
    }
}
