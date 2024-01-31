package glorydark.customform;

import cn.nukkit.Player;
import cn.nukkit.Server;
import cn.nukkit.command.Command;
import cn.nukkit.command.CommandSender;
import cn.nukkit.lang.TranslationContainer;
import cn.nukkit.plugin.Plugin;
import cn.nukkit.plugin.PluginBase;
import cn.nukkit.utils.Config;
import cn.nukkit.utils.TextFormat;
import com.smallaswater.npc.variable.VariableManage;
import glorydark.customform.chestMenu.ChestMenuListener;
import glorydark.customform.chestMenu.ChestMenuMain;
import glorydark.customform.forms.FormCreator;
import glorydark.customform.forms.FormListener;
import tip.utils.Api;

import java.io.File;
import java.util.Map;
import java.util.Objects;

public class CustomFormMain extends PluginBase {

    public static String path;

    public static Plugin plugin;

    public static boolean enableDoubleCheckMenu;

    public static boolean enableCameraAnimation;

    /*
      enableEconomyAPI  -> EconomyAPI loaded
      enablePoints -> PointsAPI loaded
      enableDCurrency -> DCurrency loaded
      enableTips -> Tips Loaded
    */
    public static boolean enableTips;

    public static boolean enableRsNPCX;

    public static boolean enableEconomyAPI;

    public static boolean enablePoints;

    public static boolean enableDCurrency;

    public static boolean enablePlaceHolderAPI;

    // Set the intervals for player to open the next form.
    public static long coolDownMillis;

    public static Language language;

    @Override
    public void onEnable() {
        path = this.getDataFolder().getPath();
        plugin = this;
        this.saveDefaultConfig();
        this.saveResource("languages/zh_cn.properties", false);
        this.saveResource("languages/en_us.properties", false);
        Config config = new Config(path + "/config.yml", Config.YAML);
        enableDoubleCheckMenu = config.getBoolean("enable_doubleCheckMenu", true);
        enableCameraAnimation = config.getBoolean("enable_cameraAnimation", false);
        coolDownMillis = config.getLong("coolDown", 200L);
        language = new Language(config.getString("default_lang", "zh_cn"), path + "/languages/", path + "/languages/playerLanguageCache.yml");
        enableTips = checkSoftDepend("Tips") && config.getBoolean("enable_tips", true);
        enableDCurrency = checkSoftDepend("DCurrency");
        enableEconomyAPI = checkSoftDepend("EconomyAPI");
        enablePoints = checkSoftDepend("playerPoints");
        enableRsNPCX = checkSoftDepend("RsNPC") && config.getBoolean("enable_rsNPCX", true);
        enablePlaceHolderAPI = checkSoftDepend("PlaceholderAPI");
        if (enableRsNPCX) {
            VariableManage.removeVariable("%npcName%");
            VariableManage.addVariable("%npcName%", (player, rsNpcConfig) -> {
                if (rsNpcConfig == null) {
                    return "";
                }
                return rsNpcConfig.getName();
            });
        }
        if (enableTips) {
            if (config.getBoolean("enable_expansion_variable", true)) {
                Api.registerVariables("CustomFormVariableExpansion", ExpansionVariable.class); // Register ExpansionVariable.class
            }
        }
        File formDic = new File(path + "/forms/");
        if (!formDic.exists()) {
            if (!formDic.mkdirs()) {
                this.getLogger().warning(language.translateString(null, "plugin_dictionary_created_failed"));
                this.setEnabled(false);
            }
        }

        File minecartChestWindowDic = new File(path + "/minecart_chest_windows/");
        if (!minecartChestWindowDic.exists()) {
            if (!minecartChestWindowDic.mkdirs()) {
                this.getLogger().warning(language.translateString(null, "plugin_dictionary_created_failed"));
                this.setEnabled(false);
            }
        }
        this.loadScriptMineCartWindows(new File(path + "/minecart_chest_windows/"));
        this.loadScriptWindows(new File(path + "/forms/"));
        this.getLogger().info("CustomForm onLoad");
        this.getServer().getCommandMap().register("", new Commands("form"));
        this.getServer().getPluginManager().registerEvents(new FormListener(), this);
        this.getServer().getPluginManager().registerEvents(new ChestMenuListener(), this);
    }

    @Override
    public void onDisable() {
        ChestMenuMain.mineCartChests.forEach((player, playerMineCartChestTempData) -> {
            player.removeWindow(playerMineCartChestTempData.getEntityMinecartChest().getInventory());
            ChestMenuMain.closeDoubleChestInventory(player);
        });
    }

    /**
     * This method is to check whether a soft dependence is loaded or not.
     **/
    public boolean checkSoftDepend(String pluginName) {
        Plugin pl = this.getServer().getPluginManager().getPlugin(pluginName);
        if (pl != null) {
            this.getLogger().info(language.translateString(null, "soft_depend_found", pluginName));
        } else {
            this.getLogger().info(language.translateString(null, "soft_depend_not_found", pluginName));
        }
        return (pl != null);
    }

    public void loadScriptMineCartWindows(File dic) {
        ChestMenuMain.mineCartChests.forEach((player, playerMineCartChestTempData) -> {
            player.removeWindow(playerMineCartChestTempData.getEntityMinecartChest().getInventory());
            ChestMenuMain.closeDoubleChestInventory(player);
        });
        ChestMenuMain.mineCartChests.clear();
        ChestMenuMain.chestMenus.clear();
        for (File file : Objects.requireNonNull(dic.listFiles())) {
            if (file.isDirectory()) {
                String subFolder = file.getName() + "/";
                for (File subFolderFile : Objects.requireNonNull(file.listFiles())) {
                    loadScriptMinecartWindow(subFolderFile, subFolder);
                }
            } else {
                loadScriptMinecartWindow(file, "");
            }
        }
        this.getLogger().info(language.translateString(null, "chest_window_minecart_loaded_in_total", ChestMenuMain.chestMenus.keySet().size()));
    }

    protected void loadScriptMinecartWindow(File file, String prefix) {
        if (file.isDirectory()) {
            for (File subFolderFile : Objects.requireNonNull(file.listFiles())) {
                loadScriptMinecartWindow(subFolderFile, prefix + file.getName() + "/");
            }
        } else {
            Map<String, Object> mainMap = FormCreator.convertConfigToMap(file);
            String identifier = prefix + file.getName().replace(".json", "").replace(".yml", "");
            if (ChestMenuMain.registerMinecartChestMenu(identifier, mainMap)) {
                this.getLogger().info(language.translateString(null, "chest_window_minecart_loaded", identifier));
            } else {
                this.getLogger().error(language.translateString(null, "chest_window_minecart_loaded_failed", identifier));
            }
        }
    }

    /**
     * This method is to read the script forms configuration,
     * and converted it to a ScriptForm-type variable.
     **/
    public void loadScriptWindows(File dic) {
        FormCreator.UI_CACHE.clear();
        FormCreator.formScripts.clear();
        for (File file : Objects.requireNonNull(dic.listFiles())) {
            if (file.isDirectory()) {
                String subFolder = file.getName() + "/";
                for (File subFolderFile : Objects.requireNonNull(file.listFiles())) {
                    loadScriptWindow(subFolderFile, subFolder);
                }
            } else {
                loadScriptWindow(file, "");
            }
        }
        this.getLogger().info(language.translateString(null, "form_loaded_in_total", FormCreator.formScripts.keySet().size()));
    }

    protected void loadScriptWindow(File file, String prefix) {
        if (file.isDirectory()) {
            for (File subFolderFile : Objects.requireNonNull(file.listFiles())) {
                loadScriptWindow(subFolderFile, prefix + file.getName() + "/");
            }
        } else {
            Map<String, Object> mainMap = FormCreator.convertConfigToMap(file);
            String identifier = prefix + file.getName().replace(".json", "").replace(".yml", "");
            if (FormCreator.loadForm(identifier, mainMap)) {
                this.getLogger().info(language.translateString(null, "form_loaded", identifier));
            } else {
                this.getLogger().error(language.translateString(null, "form_loaded_failed", identifier));
            }
        }
    }

    private class Commands extends Command {

        public Commands(String name) {
            super(name);
        }

        @Override
        public boolean execute(CommandSender commandSender, String s, String[] strings) {
            if (strings.length == 0) {
                return false;
            }
            switch (strings[0].toLowerCase()) {
                case "reload":
                    if (commandSender.isOp() || !commandSender.isPlayer()) {
                        loadScriptWindows(new File(path + "/forms/"));
                        loadScriptMineCartWindows(new File(path + "/minecart_chest_windows/"));
                        commandSender.sendMessage(language.translateString(commandSender.isPlayer() ? (Player) commandSender : null, "plugin_reloaded"));
                    } else {
                        commandSender.sendMessage(new TranslationContainer(TextFormat.RED + "%commands.generic.unknown", s));
                    }
                    break;
                case "show":
                    if (commandSender.isPlayer()) {
                        if (strings.length == 2) {
                            FormCreator.showScriptForm((Player) commandSender, strings[1], false);
                        }
                    } else {
                        if (strings.length == 3) {
                            Player player = Server.getInstance().getPlayer(strings[1]);
                            if (player != null) {
                                FormCreator.showScriptForm(player, strings[2], true);
                            } else {
                                commandSender.sendMessage(language.translateString(null, "command_player_not_found", strings[1]));
                            }
                        }
                    }
                    break;
                case "showminecartmenu":
                    if (commandSender.isPlayer()) {
                        if (strings.length == 2) {
                            ChestMenuMain.showMinecartChestMenu((Player) commandSender, strings[1]);
                        }
                    } else {
                        commandSender.sendMessage(language.translateString(null, "command_player_not_found", strings[1]));
                    }
                    break;
                case "setlang":
                    if (commandSender.isOp() || !commandSender.isPlayer()) {
                        if (strings.length == 3) {
                            if (language.setPlayerLanguage(strings[1], strings[2])) {
                                language.translateString(null, "language_set", strings[1], strings[2]);
                            } else {
                                language.translateString(null, "language_set_failed", strings[1], strings[2]);
                            }
                        }
                    } else {
                        commandSender.sendMessage(new TranslationContainer(TextFormat.RED + "%commands.generic.unknown", s));
                    }
                    break;
                case "list":
                    commandSender.sendMessage(FormCreator.formScripts.keySet().size() + " form scripts loaded: ");
                    for (String string : FormCreator.formScripts.keySet()) {
                        commandSender.sendMessage("- " + string);
                    }
                    break;
            }
            return true;
        }
    }
}