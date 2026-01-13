package glorydark.nukkit.customform.script;

import cn.nukkit.command.CommandSender;
import cn.nukkit.command.ConsoleCommandSender;
import cn.nukkit.plugin.Plugin;
import cn.nukkit.utils.Utils;
import glorydark.nukkit.customform.CustomFormMain;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Scriptable;

import javax.script.ScriptException;
import java.io.File;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author glorydark
 */
public class CustomFormScriptManager {

    private static final Map<String, String> scripts = new LinkedHashMap<>();

    public static final RhinoScriptEngine engine = new RhinoScriptEngine();

    private static final Pattern REG_PLUGIN_INFO =
            Pattern.compile(
                    "^\\s*regPluginInfo\\s*\\(\\s*\"([^\"]*)\"(?:\\s*,\\s*\"([^\"]*)\")?(?:\\s*,\\s*\"([^\"]*)\")?(?:\\s*,\\s*\"([^\"]*)\")?.*\\)",
                    Pattern.MULTILINE | Pattern.CASE_INSENSITIVE);

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
                    CustomFormScriptManager.executeScript(new ConsoleCommandSender(), scriptId, script);
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
        executeScript(sender, scriptId, scripts.getOrDefault(scriptId, ""));
    }

    public static void executeScript(CommandSender sender, String scriptId, String script) {
        try {
            ScriptPluginData pluginData = derivePluginData(script);
            if (CustomFormMain.plugin.getServer().getPluginManager().getPlugin(pluginData.name()) == null) {
                CustomFormMain.plugin.loadInternalPlugin(pluginData.name(), pluginData.version(), pluginData.author(), pluginData.description());
                CustomFormMain.getFakeScriptPlugin().getLogger().info("Loading script plugin info: " + pluginData);
            } else {
                CustomFormMain.getFakeScriptPlugin().getLogger().warning("Fail to load script plugin info, script id: " + scriptId);
            }
            Context ctx = Context.enter();
            try {
                ctx.setLanguageVersion(Context.VERSION_ES6);
                ctx.setOptimizationLevel(-1);
                Scriptable scope = engine.createFreshScope(ctx, pluginData);
                engine.put(scope, "sender", sender);
                engine.put(scope, "server", CustomFormMain.plugin.getServer());
                engine.put(scope, "api", new ScriptPlayerAPI());
                Plugin plugin = engine.getPlugin(scope);
                engine.put(scope, "plugin", plugin);
                if (script.contains("return")) {
                    script = "(function(){\n" + script + "\n})();";
                }
                ctx.evaluateString(scope, script, pluginData.name().isEmpty()? scriptId: pluginData.name(), 1, null);
            } catch (Exception e) {
                throw new ScriptException(e.getMessage());
            }
        } catch (Exception e) {
            sender.sendMessage("§cError in loading script: " + e.getMessage());
        }
    }

    /**
     * 从脚本代码里解析第一行 regPluginInfo(...)，
     * 找不到就退化成文件名（版本=1.0.0，作者=空，描述=空）。
     */
    public static ScriptPluginData derivePluginData(String scriptCode) {
        if (scriptCode != null) {
            Matcher m = REG_PLUGIN_INFO.matcher(scriptCode);
            if (m.find()) {
                return new ScriptPluginData(
                        m.group(1) == null? CustomFormMain.DEFAULT_SCRIPT_PLUGIN_NAME : m.group(1), // name
                        m.group(2) == null ? "1.0.0" : m.group(2), // version
                        m.group(3) == null ? "" : m.group(3),      // author
                        m.group(4) == null ? "" : m.group(4));     // description
            }
        }
        return new ScriptPluginData(
                CustomFormMain.DEFAULT_SCRIPT_PLUGIN_NAME, "1.0.0", "", "");
    }
}
