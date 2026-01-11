package glorydark.nukkit.customform.script;

import cn.nukkit.command.CommandSender;
import cn.nukkit.command.ConsoleCommandSender;
import cn.nukkit.utils.Utils;
import glorydark.nukkit.customform.CustomFormMain;

import javax.script.ScriptEngine;
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

    public static final RhinoScriptEngine engine = new RhinoScriptEngine();

    public static void loadScripts() {
        scripts.clear();

        File dir = new File(CustomFormMain.path + "/scripts/");
        dir.mkdirs();
        for (File child : Objects.requireNonNull(dir.listFiles())) {
            loadScripts(child, "");
        }

        if (!CustomFormMain.SCRIPTS_RUN_ON_START.isEmpty()) {
            for (String scriptId : CustomFormMain.SCRIPTS_RUN_ON_START) {
                if (scripts.containsKey(scriptId)) {
                    String script = scripts.get(scriptId);
                    CustomFormScriptManager.executeScript(new ConsoleCommandSender(), script);
                }
            }
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

    public static void executeLoadedScript(CommandSender sender, String scriptId) {
        if (!scripts.containsKey(scriptId)) {
            CustomFormMain.plugin.getLogger().error("Script not found: " + scriptId);
            return;
        }
        executeScript(sender, scripts.getOrDefault(scriptId, ""));
    }

    public static void executeScript(CommandSender sender, String script) {
        try {
            engine.put("sender", sender);
            engine.put("server", CustomFormMain.plugin.getServer());
            engine.put("api", new ScriptPlayerAPI());
            engine.put("plugin", CustomFormMain.fakeScriptPlugin);

            engine.eval(script);
        } catch (Exception e) {
            sender.sendMessage("§cError in loading script: " + e.getMessage());
        }
    }
}
