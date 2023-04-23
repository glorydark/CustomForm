package glorydark.customform;

import cn.nukkit.Player;
import cn.nukkit.command.Command;
import cn.nukkit.command.CommandSender;
import cn.nukkit.lang.TranslationContainer;
import cn.nukkit.plugin.Plugin;
import cn.nukkit.plugin.PluginBase;
import cn.nukkit.utils.Config;
import cn.nukkit.utils.TextFormat;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import glorydark.customform.forms.FormCreator;
import glorydark.customform.forms.FormListener;
import tip.utils.Api;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Objects;

public class CustomFormMain extends PluginBase {

    public String path;

    public static Plugin plugin;

    /*
      enableEconomyAPI  -> EconomyAPI loaded
      enablePoints -> PointsAPI loaded
      enableDCurrency -> DCurrency loaded
      enableTips -> Tips Loaded
    */
    public static boolean enableTips = false;

    public static boolean enableEconomyAPI = false;

    public static boolean enablePoints = false;

    public static boolean enableDCurrency = false;

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
        Config config = new Config(path+"/config.yml",Config.YAML);
        coolDownMillis = config.getLong("coolDown", 200L);
        language = new Language(config.getString("default_lang", "zh_cn"), path+"/languages/", path+"/languages/playerLanguageCache.yml");
        File formDic = new File(path+"/forms/");
        if(!formDic.exists()){
            if(!formDic.mkdirs()){
                this.getLogger().warning(language.translateString(null, "plugin_dictionary_created_failed"));
                this.setEnabled(false);
            }
        }
        enableTips = checkSoftDepend("Tips");
        enableDCurrency = checkSoftDepend("DCurrency");
        enableEconomyAPI = checkSoftDepend("EconomyAPI");
        enablePoints = checkSoftDepend("playerPoints");
        if(enableTips){
            if(config.getBoolean("enable_expansion_variable", true)){
                Api.registerVariables("CustomFormVariableExpansion", ExpansionVariable.class); // Register ExpansionVariable.class
            }
        }
        this.loadScriptWindows();
        this.getLogger().info("CustomForm onLoad");
        this.getServer().getCommandMap().register("", new Commands("form"));
        this.getServer().getPluginManager().registerEvents(new FormListener(), this);
    }

    /*
        This method is to check whether a soft dependence is loaded or not.
     */
    public boolean checkSoftDepend(String pluginName){
        Plugin pl = this.getServer().getPluginManager().getPlugin(pluginName);
        if(pl != null){
            this.getLogger().info(language.translateString(null, "soft_depend_found", pluginName));
        }else{
            this.getLogger().info(language.translateString(null, "soft_depend_not_found", pluginName));
        }
        return (pl != null);
    }

    /*
        This method is to read the script forms configuration,
        and converted it to a ScriptForm-type variable.
     */
    public void loadScriptWindows(){
        FormCreator.formScripts.clear();
        FormCreator.UI_CACHE.clear();
        File dic = new File(path+"/forms/");
        for(File file: Objects.requireNonNull(dic.listFiles())){
            if(file.getName().endsWith(".json")){
                InputStream stream;
                try {
                    stream = new FileInputStream(file);
                } catch (FileNotFoundException e) {
                    throw new RuntimeException(e);
                }
                InputStreamReader streamReader = new InputStreamReader(stream, StandardCharsets.UTF_8); //一定要以utf-8读取
                JsonReader reader = new JsonReader(streamReader);
                Gson gson = new GsonBuilder().registerTypeAdapter(new TypeToken<Map<String, Object>>() {}.getType(), new GsonAdapter()).create();
                Map<String, Object> mainMap = gson.fromJson(reader, new TypeToken<Map<String, Object>>(){}.getType());

                String identifier = file.getName().replace(".json","");
                if(FormCreator.loadForm(identifier, mainMap)){
                    this.getLogger().info(language.translateString(null, "form_loaded", identifier));
                }else{
                    this.getLogger().error(language.translateString(null, "form_loaded_failed", identifier));
                }
                try {
                    reader.close();
                    streamReader.close();
                    stream.close();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
            if(file.getName().endsWith(".yml")){
                String identifier = file.getName().replace(".yml","");
                Map<String, Object> mainMap = new Config(file, Config.YAML).getAll();
                if(FormCreator.loadForm(identifier, mainMap)){
                    this.getLogger().info(language.translateString(null, "form_loaded", identifier));
                }else{
                    this.getLogger().error(language.translateString(null, "form_loaded_failed", identifier));
                }
            }
        }
        this.getLogger().info(language.translateString(null, "form_loaded_in_total", FormCreator.formScripts.keySet().size()));
    }

    private class Commands extends Command {

        public Commands(String name) {
            super(name);
        }

        @Override
        public boolean execute(CommandSender commandSender, String s, String[] strings) {
            if(strings.length == 0){ return false; }
            switch (strings[0].toLowerCase()){
                case "reload":
                    if(commandSender.isOp() || !commandSender.isPlayer()){
                        loadScriptWindows();
                        commandSender.sendMessage(language.translateString(commandSender.isPlayer()? (Player) commandSender : null, "plugin_reloaded"));
                    }else{
                        commandSender.sendMessage(new TranslationContainer(TextFormat.RED + "%commands.generic.unknown", s));
                    }
                    break;
                case "show":
                    if(commandSender.isPlayer()){
                        if(strings.length == 2) {
                            FormCreator.showScriptForm((Player) commandSender, strings[1]);
                        }
                    }else{
                        commandSender.sendMessage(language.translateString(null, "command_use_in_game"));
                    }
                    break;
                case "setlang":
                    if(commandSender.isOp() || !commandSender.isPlayer()){
                        if(strings.length == 3) {
                            if (language.setPlayerLanguage(strings[1], strings[2])) {
                                language.translateString(null, "language_set", strings[1], strings[2]);
                            }else{
                                language.translateString(null, "language_set_failed", strings[1], strings[2]);
                            }
                        }
                    }else{
                        commandSender.sendMessage(new TranslationContainer(TextFormat.RED + "%commands.generic.unknown", s));
                    }
                    break;
            }
            return true;
        }
    }
}