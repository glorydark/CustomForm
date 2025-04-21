package glorydark.nukkit.customform;

import cn.nukkit.Server;
import cn.nukkit.plugin.Plugin;
import cn.nukkit.plugin.PluginBase;
import cn.nukkit.scheduler.Task;
import cn.nukkit.utils.Config;
import cn.nukkit.utils.ConfigSection;
import com.smallaswater.npc.data.RsNpcConfig;
import com.smallaswater.npc.utils.exception.RsNpcConfigLoadException;
import com.smallaswater.npc.utils.exception.RsNpcLoadException;
import glorydark.nukkit.customform.chestForm.ChestFormMain;
import glorydark.nukkit.customform.hopperform.HopperFormMain;
import glorydark.nukkit.customform.minecartChestMenu.MinecartChestMenuListener;
import glorydark.nukkit.customform.minecartChestMenu.MinecartChestMenuMain;
import glorydark.nukkit.customform.commands.CustomFormCommands;
import glorydark.nukkit.customform.factory.FormCreator;
import glorydark.nukkit.customform.listener.FormListener;
import glorydark.nukkit.customform.utils.InventoryUtils;
import glorydark.nukkit.customform.utils.Tools;
import glorydark.nukkit.utils.LanguageReader;
import tip.utils.Api;

import java.io.File;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class CustomFormMain extends PluginBase {

    public static String path;

    public static CustomFormMain plugin;

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

    public static boolean enableLanguageAPI;

    public static boolean enableGameAPI;

    // Set the intervals for player to open the next form.
    public static long coolDownMillis;

    public static Language language;

    public static boolean debug;
    public static List<CompletableFuture<?>> completableFutureList = new ArrayList<>();
    public static boolean ready = false;
    public ExecutorService executor; // 创建一个拥有5个线程的线程池
    public static RsNpcConfig rsNpcConfig = null;

    @Override
    public void onEnable() {
        TimeZone timeZone = TimeZone.getTimeZone("GMT+8");
        TimeZone.setDefault(timeZone);
        path = this.getDataFolder().getPath();
        plugin = this;
        this.saveDefaultConfig();
        this.saveResource("languages/zh_cn.properties", false);
        this.saveResource("languages/en_us.properties", false);
        Config config = new Config(path + "/config.yml", Config.YAML);
        debug = config.getBoolean("debug", false); // todo
        enableDoubleCheckMenu = config.getBoolean("enable_doubleCheckMenu", true);
        enableCameraAnimation = config.getBoolean("enable_cameraAnimation", false);
        coolDownMillis = config.getLong("coolDown", 200L);
        language = new Language(config.getString("default_lang", "zh_cn"), path + "/languages/");
        enableTips = checkSoftDepend("Tips") && config.getBoolean("enable_tips", true);
        enableDCurrency = checkSoftDepend("DCurrency");
        enableEconomyAPI = checkSoftDepend("EconomyAPI");
        enablePoints = checkSoftDepend("playerPoints");
        enableRsNPCX = checkSoftDepend("RsNPC") && config.getBoolean("enable_rsNPCX", true);
        enableGameAPI = checkSoftDepend("GameAPI");
        enablePlaceHolderAPI = checkSoftDepend("PlaceholderAPI");
        enableLanguageAPI = checkSoftDepend("LanguageAPI");
        Config config1 = new Config(path + "/rsnpc_cache.yml", Config.YAML);
        config1.set("坐标", new ConfigSection() {
            {
                this.put("level", Server.getInstance().getDefaultLevel().getName());
                this.put("x", 0);
                this.put("y", 0);
                this.put("z", 0);
            }
        });
        if (enableRsNPCX) {
            Server.getInstance().getScheduler().scheduleRepeatingTask(this, new Task() {
                int tryTimes = 0;
                @Override
                public void onRun(int i) {
                    enableRsNPCX = false;
                    tryTimes++;
                    if (tryTimes > 10) {
                        CustomFormMain.plugin.getLogger().error("Failed to create a RsNpcConfig object! Disabling the RsNPC variable support!");
                        this.cancel();
                    }
                    // hack: to create a file to make constructor believe that this exists.
                    try {
                        rsNpcConfig = new RsNpcConfig("", config1);
                    } catch (RsNpcConfigLoadException | RsNpcLoadException | NoClassDefFoundError ignored) {
                        CustomFormMain.plugin.getLogger().error("Failed to create a RsNpcConfig object! [" + tryTimes + "/10]");
                        return;
                    }
                    enableRsNPCX = true;
                    CustomFormMain.plugin.getLogger().info("Successfully create a RsNpcConfig object!");
                    this.cancel();
                }
            }, 20);
        }
        if (enableTips) {
            if (config.getBoolean("enable_expansion_variable", false)) {
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
        this.loadAll();
        this.getLogger().info("CustomForm onLoad");
        this.getServer().getCommandMap().register("", new CustomFormCommands());
        this.getServer().getPluginManager().registerEvents(new FormListener(), this);
        this.getServer().getPluginManager().registerEvents(new MinecartChestMenuListener(), this);
    }

    public void loadAll() {
        if (enableLanguageAPI) {
            File customLangDic = new File(path + "/custom_languages/");
            customLangDic.mkdirs();
            LanguageReader.loadLanguageFromDictionary(this, customLangDic);
        }
        ready = false;
        executor = Executors.newFixedThreadPool(5);
        ChestFormMain.loadAll();
        HopperFormMain.loadAll();
        this.loadItemStringCaches();
        this.loadScriptMineCartWindows(new File(path + "/minecart_chest_windows/"));
        this.loadScriptWindows(new File(path + "/forms/"));
        long startMillis = System.currentTimeMillis();
        CompletableFuture.allOf(completableFutureList.toArray(new CompletableFuture[0]))
                .thenRun(() -> {
                    if (debug) {
                        this.getLogger().alert("Loading all requirements, size: " + completableFutureList.size() + ", cost time: " + Tools.formatTimeDiff(System.currentTimeMillis(), startMillis));
                    }
                });
        ready = true;
    }

    @Override
    public void onDisable() {
        MinecartChestMenuMain.mineCartChests.forEach((player, playerMineCartChestTempData) -> {
            player.removeWindow(playerMineCartChestTempData.getEntityMinecartChest().getInventory());
            MinecartChestMenuMain.closeDoubleChestInventory(player);
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

    public void loadItemStringCaches() {
        File file = new File(CustomFormMain.path + "/save_nbt_cache.yml");
        if (file.exists()) {
            InventoryUtils.itemStringCaches = new Config(file, Config.YAML).getRootSection();
        }
    }

    public void loadScriptMineCartWindows(File dic) {
        MinecartChestMenuMain.mineCartChests.forEach((player, playerMineCartChestTempData) -> {
            player.removeWindow(playerMineCartChestTempData.getEntityMinecartChest().getInventory());
            MinecartChestMenuMain.closeDoubleChestInventory(player);
        });
        MinecartChestMenuMain.mineCartChests.clear();
        MinecartChestMenuMain.chestMenus.clear();
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
        this.getLogger().info(language.translateString(null, "chest_window_minecart_loaded_in_total", MinecartChestMenuMain.chestMenus.keySet().size()));
    }

    protected void loadScriptMinecartWindow(File file, String prefix) {
        if (file.isDirectory()) {
            for (File subFolderFile : Objects.requireNonNull(file.listFiles())) {
                loadScriptMinecartWindow(subFolderFile, prefix + file.getName() + "/");
            }
        } else {
            Map<String, Object> mainMap = FormCreator.convertConfigToMap(file);
            String identifier = prefix + file.getName().replace(".json", "").replace(".yml", "");
            if (MinecartChestMenuMain.registerMinecartChestMenu(identifier, mainMap)) {
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
                CustomFormMain.plugin.getLogger().info(language.translateString(null, "form_loaded", identifier));
            } else {
                CustomFormMain.plugin.getLogger().error(language.translateString(null, "form_loaded_failed", identifier));
            }
        }
    }
}