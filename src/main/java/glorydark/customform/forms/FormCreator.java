package glorydark.customform.forms;

import cn.nukkit.Player;
import cn.nukkit.Server;
import cn.nukkit.form.window.FormWindow;
import cn.nukkit.form.window.FormWindowCustom;
import cn.nukkit.form.window.FormWindowModal;
import cn.nukkit.form.window.FormWindowSimple;
import cn.nukkit.network.protocol.ModalFormRequestPacket;
import cn.nukkit.utils.Config;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import glorydark.customform.CustomFormMain;
import glorydark.customform.GsonAdapter;
import glorydark.customform.annotations.Api;
import glorydark.customform.event.FormPreOpenEvent;
import glorydark.customform.scriptForms.data.SoundData;
import glorydark.customform.scriptForms.data.execute_data.ResponseExecuteData;
import glorydark.customform.scriptForms.data.execute_data.SimpleResponseExecuteData;
import glorydark.customform.scriptForms.data.execute_data.StepResponseExecuteData;
import glorydark.customform.scriptForms.data.execute_data.ToggleResponseExecuteData;
import glorydark.customform.scriptForms.data.execute_data.config.ConfigModification;
import glorydark.customform.scriptForms.data.execute_data.config.ConfigModificationType;
import glorydark.customform.scriptForms.data.requirement.Requirements;
import glorydark.customform.scriptForms.data.requirement.config.ConfigRequirementData;
import glorydark.customform.scriptForms.data.requirement.config.ConfigRequirementType;
import glorydark.customform.scriptForms.data.requirement.economy.EconomyRequirementData;
import glorydark.customform.scriptForms.data.requirement.economy.EconomyRequirementType;
import glorydark.customform.scriptForms.data.requirement.item.ItemRequirementData;
import glorydark.customform.scriptForms.data.requirement.item.NeedItem;
import glorydark.customform.scriptForms.data.requirement.tips.TipsRequirementData;
import glorydark.customform.scriptForms.data.requirement.tips.TipsRequirementType;
import glorydark.customform.scriptForms.form.ScriptForm;
import glorydark.customform.scriptForms.form.ScriptFormCustom;
import glorydark.customform.scriptForms.form.ScriptFormModal;
import glorydark.customform.scriptForms.form.ScriptFormSimple;
import lombok.Data;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class FormCreator {
    public static final LinkedHashMap<String, WindowInfo> UI_CACHE = new LinkedHashMap<>();

    // Stored information and configuration about loaded forms.
    public static LinkedHashMap<String, ScriptForm> formScripts = new LinkedHashMap<>();

    // This value effectively reduces the conflicts brought by the duplication of ID value inside the Player.class(Nukkit)
    public static int formId = -1;

    /*
        Basic Refined Method.
        Modified from MurderMystery sourcecode one or two year ago.
        Especially thanks to lt-name(LT-name)!
    */
    @Deprecated
    public static void showFormToPlayer(Player player, FormType formType, String identifier) {
        showFormToPlayer(player, formType, FormCreator.formScripts.get(identifier), identifier);
    }

    @Api
    // You can show your own scriptForm without former registry by defining a certain scriptForm.
    public static void showFormToPlayer(Player player, FormType formType, ScriptForm scriptForm, String identifier) {
        if (scriptForm.getStartMillis() != -1L || scriptForm.getExpiredMillis() != -1L) {
            if (scriptForm.getStartMillis() < System.currentTimeMillis() && scriptForm.getExpiredMillis() < System.currentTimeMillis()) {
                player.sendMessage("This form is expired!");
                return;
            }
        }
        if (player.namedTag.contains("lastFormRequestMillis") && System.currentTimeMillis() - player.namedTag.getLong("lastFormRequestMillis") < CustomFormMain.coolDownMillis) {
            player.sendMessage(CustomFormMain.language.translateString(player, "operation_so_fast"));
            return;
        }
        FormWindow window = scriptForm.getWindow(player);
        ModalFormRequestPacket packet = new ModalFormRequestPacket();
        packet.formId = formId;
        packet.data = window.getJSONData();
        player.dataPacket(packet);
        player.namedTag.putLong("lastFormRequestMillis", System.currentTimeMillis());
        UI_CACHE.put(player.getName(), new WindowInfo(formType, identifier, scriptForm));
    }

    /*
        By this function, you can show a certain form whose identifier is the same as identifier.
    */
    public static void showScriptForm(Player player, String identifier) {
        if (formScripts.containsKey(identifier)) {
            ScriptForm script = formScripts.get(identifier);
            showScriptForm(player, script, identifier);
        }
    }

    @Api
    // This function can use as a way to customize your form.
    public static void showScriptForm(Player player, ScriptForm script, String identifier) {
        Server.getInstance().getPluginManager().callEvent(new FormPreOpenEvent(script, player));
        FormWindow window = script.getWindow(player);
        if (script.getOpenSound() != null) {
            script.getOpenSound().addSound(player);
        }
        if (window instanceof FormWindowSimple) {
            showFormToPlayer(player, FormType.ScriptSimple, script, identifier);
        }
        if (window instanceof FormWindowModal) {
            showFormToPlayer(player, FormType.ScriptModal, script, identifier);
        }
        if (window instanceof FormWindowCustom) {
            showFormToPlayer(player, FormType.ScriptCustom, script, identifier);
        }
        Server.getInstance().getPluginManager().callEvent(new FormPreOpenEvent(script, player));
    }

    /*
        By this function, you can build up a requirement set.
        A requirement set consists of Tips_RequirementsData and EconomyRequirementsData.
        You can see some provided method inside the Requirements.class.

        * If you want to add a new type of requirement,
        please make some tiny modifications inside the Requirement.class.
    */
    public static Requirements buildRequirements(List<Map<String, Object>> requirementConfig, boolean globalChargable) {
        Requirements requirements = new Requirements(new ArrayList<>(), new ArrayList<>(), new ArrayList<>(), new ArrayList<>(), new ArrayList<>(), new ArrayList<>(), new ArrayList<>(), new ArrayList<>(), globalChargable);
        for (Map<String, Object> map : requirementConfig) {
            String type = (String) map.get("type");
            EconomyRequirementData data = null;
            TipsRequirementData tips_data = null;
            ItemRequirementData itemRequirementData = null;
            ConfigRequirementData configRequirementData = null;
            boolean chargeable = (Boolean) map.getOrDefault("chargeable", globalChargable);
            switch (type) {
                case "EconomyAPI":
                    // This is the way we deal with EconomyAPI-type requirements
                    data = new EconomyRequirementData(null, 0d, chargeable, new Object());
                    data.setType(EconomyRequirementType.EconomyAPI);
                    data.setAmount(Double.parseDouble(map.get("cost").toString()));
                    break;
                case "Points":
                    // This is the way we deal with Points-type requirements
                    data = new EconomyRequirementData(null, 0d, chargeable, new Object());
                    data.setType(EconomyRequirementType.Points);
                    data.setAmount(Double.parseDouble(map.get("cost").toString()));
                    break;
                case "DCurrency":
                    // This is the way we deal with DCurrency-type requirements
                    data = new EconomyRequirementData(null, 0d, chargeable, new Object());
                    data.setType(EconomyRequirementType.DCurrency);
                    data.setAmount(Double.parseDouble(map.get("cost").toString()));
                    data.setExtraData(new String[]{(String) map.get("currencyType")});
                    break;
                case "Item":
                    itemRequirementData = new ItemRequirementData((boolean) map.get("reduce"), (Boolean) map.getOrDefault("check_tag", true));
                    List<NeedItem> needItems = new ArrayList<>();
                    List<Map<String, Object>> needItemMapList = (List<Map<String, Object>>) map.getOrDefault("costs", new ArrayList<>());
                    if (needItemMapList.size() > 0) {
                        for (Map<String, Object> subMap : needItemMapList) {
                            NeedItem item = new NeedItem((String) subMap.get("item"), (List<String>) subMap.getOrDefault("alternatives", new ArrayList<>()));
                            needItems.add(item);
                        }
                    }
                    itemRequirementData.setNeedItems(needItems);
                    break;
                case "Tips":
                    // This is the way we deal with Tips-type requirements
                    String identifier = (String) map.get("identifier");
                    String comparedSign = (String) map.get("compared_sign");
                    Object comparedValue = map.get("compared_value");
                    String displayName = (String) map.get("display_name");
                    List<String> failed_messages = (List<String>) map.getOrDefault("failed_messages", new ArrayList<>());
                    switch (comparedSign) {
                        case ">":
                            tips_data = new TipsRequirementData(TipsRequirementType.Bigger, identifier, comparedValue, displayName, failed_messages);
                            break;
                        case ">=":
                            tips_data = new TipsRequirementData(TipsRequirementType.BiggerOrEqual, identifier, comparedValue, displayName, failed_messages);
                            break;
                        case "=":
                            tips_data = new TipsRequirementData(TipsRequirementType.Equal, identifier, comparedValue, displayName, failed_messages);
                            break;
                        case "<":
                            tips_data = new TipsRequirementData(TipsRequirementType.Smaller, identifier, comparedValue, displayName, failed_messages);
                            break;
                        case "<=":
                            tips_data = new TipsRequirementData(TipsRequirementType.SmallerOrEqual, identifier, comparedValue, displayName, failed_messages);
                            break;
                    }
                    break;
                case "extraData":
                    requirements.setCommands((List<String>) map.getOrDefault("commands", new ArrayList<>()));
                    requirements.setMessages((List<String>) map.getOrDefault("messages", new ArrayList<>()));
                    requirements.setFailedCommands((List<String>) map.getOrDefault("failed_commands", new ArrayList<>()));
                    requirements.setFailedMessages((List<String>) map.getOrDefault("failed_messages", new ArrayList<>()));
                    break;
                case "Config":
                    // This is the way we deal with Tips-type requirements
                    int config_type = (Integer) map.get("config_type");
                    if (config_type == 0) {
                        String keyString = (String) map.get("key_name");
                        String config_compared_type = (String) map.get("compared_sign");
                        Object config_compared_value = map.get("compared_value");
                        failed_messages = (List<String>) map.getOrDefault("failed_messages", new ArrayList<>());
                        switch (config_compared_type) {
                            case "bigger":
                                configRequirementData = new ConfigRequirementData(0, ConfigRequirementType.BIGGER, keyString, config_compared_value, failed_messages);
                                break;
                            case "bigger_or_equal":
                                configRequirementData = new ConfigRequirementData(0, ConfigRequirementType.BIGGER_OR_EQUAL, keyString, config_compared_value, failed_messages);
                                break;
                            case "equal":
                                configRequirementData = new ConfigRequirementData(0, ConfigRequirementType.EQUAL, keyString, config_compared_value, failed_messages);
                                break;
                            case "smaller":
                                configRequirementData = new ConfigRequirementData(0, ConfigRequirementType.SMALLER, keyString, config_compared_value, failed_messages);
                                break;
                            case "smaller_or_equal":
                                configRequirementData = new ConfigRequirementData(0, ConfigRequirementType.SMALLER_OR_EQUAL, keyString, config_compared_value, failed_messages);
                                break;
                            case "exist":
                                configRequirementData = new ConfigRequirementData(0, ConfigRequirementType.EXIST, keyString, config_compared_value, failed_messages);
                                break;
                        }
                    } else if (config_type == 1) {
                        String config_name = (String) map.get("config_name");
                        String config_compared_type = (String) map.get("compared_sign");
                        Object config_compared_value = map.get("compared_value");
                        failed_messages = (List<String>) map.getOrDefault("failed_messages", new ArrayList<>());
                        switch (config_compared_type) {
                            case "bigger":
                                configRequirementData = new ConfigRequirementData(1, ConfigRequirementType.BIGGER, config_name, config_compared_value, failed_messages);
                                break;
                            case "bigger_or_equal":
                                configRequirementData = new ConfigRequirementData(1, ConfigRequirementType.BIGGER_OR_EQUAL, config_name, config_compared_value, failed_messages);
                                break;
                            case "equal":
                                configRequirementData = new ConfigRequirementData(1, ConfigRequirementType.EQUAL, config_name, config_compared_value, failed_messages);
                                break;
                            case "smaller":
                                configRequirementData = new ConfigRequirementData(1, ConfigRequirementType.SMALLER, config_name, config_compared_value, failed_messages);
                                break;
                            case "smaller_or_equal":
                                configRequirementData = new ConfigRequirementData(1, ConfigRequirementType.SMALLER_OR_EQUAL, config_name, config_compared_value, failed_messages);
                                break;
                            case "exist":
                                configRequirementData = new ConfigRequirementData(1, ConfigRequirementType.EXIST, config_name, config_compared_value, failed_messages);
                                break;
                        }
                    }
                    break;
            }
            if (data != null) {
                requirements.addEconomyRequirements(data);
            }
            if (tips_data != null) {
                requirements.addTipsRequirements(tips_data);
            }
            if (itemRequirementData != null) {
                requirements.addItemRequirementData(itemRequirementData);
            }
            if (configRequirementData != null) {
                requirements.addConfigRequirementData(configRequirementData);
            }
        }
        return requirements;
    }

    /*
        This is how we preload our form info.
        And we identify the type of the form by the Integer.
        0: simple  1: custom  2: modal
     */
    public static boolean loadForm(String identifier, Map<String, Object> config) {
        ScriptForm scriptForm = getScriptFormByMap(config);
        if (scriptForm != null) {
            FormCreator.formScripts.put(identifier, scriptForm);
            return true;
        } else {
            CustomFormMain.plugin.getLogger().warning("Can not load scriptForm: " + identifier);
        }
        return false;
    }

    @Api
    public static ScriptForm getScriptFormByMap(Map<String, Object> config) {
        switch ((int) config.get("type")) {
            case 0:
                //simple
                List<SimpleResponseExecuteData> simpleResponseExecuteDataList = new ArrayList<>();
                if (config.containsKey("components")) {
                    for (Map<String, Object> component : (List<Map<String, Object>>) config.getOrDefault("components", new ArrayList<>())) {
                        SimpleResponseExecuteData data = new SimpleResponseExecuteData((List<String>) component.getOrDefault("commands", new ArrayList<>()), (List<String>) component.getOrDefault("messages", new ArrayList<>()), (List<String>) component.getOrDefault("failed_commands", new ArrayList<>()), (List<String>) component.getOrDefault("failed_messages", new ArrayList<>()), new ArrayList<>(), new ArrayList<>());
                        if (component.containsKey("requirements")) {
                            List<Requirements> requirementsList = new ArrayList<>();
                            Map<String, Object> requirementData = (Map<String, Object>) component.get("requirements");
                            for (List<Map<String, Object>> object : (List<List<Map<String, Object>>>) requirementData.get("data")) {
                                requirementsList.add(buildRequirements(object, (Boolean) requirementData.getOrDefault("chargeable", true)));
                            }
                            data.setRequirements(requirementsList);
                        }
                        List<ConfigModification> configModifications = new ArrayList<>();
                        if (component.containsKey("configs")) {
                            for (Map<String, Object> configEntry : (List<Map<String, Object>>) component.getOrDefault("configs", new ArrayList<>())) {
                                int configType = (int) configEntry.get("type");
                                String extraData;
                                if (configType == 0) {
                                    extraData = configEntry.get("key_name").toString();
                                } else {
                                    extraData = configEntry.get("config_name").toString();
                                }
                                String deal_type = configEntry.get("deal_type").toString();
                                ConfigModificationType modificationType;
                                switch (deal_type) {
                                    case "add":
                                        modificationType = ConfigModificationType.ADD;
                                        break;
                                    case "deduct":
                                        modificationType = ConfigModificationType.DEDUCT;
                                        break;
                                    case "set":
                                        modificationType = ConfigModificationType.SET;
                                        break;
                                    case "remove":
                                        modificationType = ConfigModificationType.REMOVE;
                                        break;
                                    default:
                                        continue;
                                }
                                ConfigModification modification = new ConfigModification(configType, extraData, configEntry.get("deal_value"), modificationType);
                                configModifications.add(modification);
                            }
                        }
                        data.setConfigModifications(configModifications);
                        simpleResponseExecuteDataList.add(data);
                    }
                }
                ScriptFormSimple simple = new ScriptFormSimple(config, simpleResponseExecuteDataList, new SoundData("", 1f, 0f, true));
                if (config.containsKey("open_sound")) {
                    Map<String, Object> openSoundMap = (Map<String, Object>) config.get("open_sound");
                    simple.setOpenSound(new SoundData((String) openSoundMap.get("name"), Float.parseFloat(openSoundMap.getOrDefault("volume", 1f).toString()), Float.parseFloat(openSoundMap.getOrDefault("pitch", 0f).toString()), (Boolean) openSoundMap.getOrDefault("personal", true)));
                }
                simple.setStartMillis((Long) config.getOrDefault("startMillis", -1L));
                simple.setExpiredMillis((Long) config.getOrDefault("expiredMillis", -1L));
                if (simple.getWindow() != null) {
                    return simple;
                }
                break;
            case 1:
                List<ResponseExecuteData> out = new ArrayList<>();
                //custom
                for (Map<String, Object> component : (List<Map<String, Object>>) config.getOrDefault("components", new ArrayList<>())) {
                    String type = (String) component.getOrDefault("type", "");
                    if (type.equals("StepSlider") || type.equals("Dropdown")) {
                        List<SimpleResponseExecuteData> data = new ArrayList<>();
                        List<Map<String, Object>> maps = (List<Map<String, Object>>) component.getOrDefault("responses", new LinkedHashMap<>());
                        List<ConfigModification> configModifications = new ArrayList<>();
                        // todo 看看能否在某些地方能够运用到requirements
                        if (component.containsKey("config")) {
                            for (Map<String, Object> configEntry : (List<Map<String, Object>>) component.getOrDefault("configs", new ArrayList<>())) {
                                int configType = (int) configEntry.get("type");
                                String config_name = configEntry.get("key_name").toString();
                                String deal_type = configEntry.get("deal_type").toString();
                                ConfigModificationType modificationType = null;
                                switch (deal_type) {
                                    case "add":
                                        modificationType = ConfigModificationType.ADD;
                                        break;
                                    case "deduct":
                                        modificationType = ConfigModificationType.DEDUCT;
                                        break;
                                    case "set":
                                        modificationType = ConfigModificationType.SET;
                                        break;
                                    case "remove":
                                        modificationType = ConfigModificationType.REMOVE;
                                        break;
                                    default:
                                        continue;
                                }
                                ConfigModification modification = new ConfigModification(configType, config_name, configEntry.get("deal_value"), modificationType);
                                configModifications.add(modification);
                            }
                        }
                        for (Map<String, Object> map : maps) {
                            data.add(new SimpleResponseExecuteData((List<String>) map.getOrDefault("commands", new ArrayList<>()), (List<String>) map.getOrDefault("messages", new ArrayList<>()), new ArrayList<>(), new ArrayList<>(), new ArrayList<>(), configModifications));
                        }
                        out.add(new StepResponseExecuteData(data));
                    } else {
                        if (type.equals("Toggle")) {
                            Map<String, Object> maps = (Map<String, Object>) component.getOrDefault("responses", new LinkedHashMap<>());
                            out.add(new ToggleResponseExecuteData((List<String>) maps.get("true_commands"), (List<String>) maps.get("true_messages"), (List<String>) maps.get("false_commands"), (List<String>) maps.get("false_messages")));
                        } else {
                            List<ConfigModification> configModifications = new ArrayList<>();
                            out = new ArrayList<>();
                            for (Map<String, Object> configEntry : (List<Map<String, Object>>) component.getOrDefault("configs", new ArrayList<>())) {
                                int configType = (int) configEntry.get("type");
                                String config_name = configEntry.get("key_name").toString();
                                String deal_type = configEntry.get("deal_type").toString();
                                ConfigModificationType modificationType = null;
                                switch (deal_type) {
                                    case "add":
                                        modificationType = ConfigModificationType.ADD;
                                        break;
                                    case "deduct":
                                        modificationType = ConfigModificationType.DEDUCT;
                                        break;
                                    case "set":
                                        modificationType = ConfigModificationType.SET;
                                        break;
                                    case "remove":
                                        modificationType = ConfigModificationType.REMOVE;
                                        break;
                                    default:
                                        continue;
                                }
                                ConfigModification modification = new ConfigModification(configType, config_name, configEntry.get("deal_value"), modificationType);
                                configModifications.add(modification);
                            }
                            SimpleResponseExecuteData data = new SimpleResponseExecuteData((List<String>) component.getOrDefault("commands", new ArrayList<>()), (List<String>) component.getOrDefault("messages", new ArrayList<>()), (List<String>) component.getOrDefault("failed_commands", new ArrayList<>()), (List<String>) component.getOrDefault("failed_messages", new ArrayList<>()), new ArrayList<>(), configModifications);
                            if (component.containsKey("requirements")) {
                                List<Requirements> requirements = new ArrayList<>();
                                Map<String, Object> requirementData = (Map<String, Object>) component.get("requirements");
                                for (List<Map<String, Object>> object : (List<List<Map<String, Object>>>) requirementData.get("data")) {
                                    requirements.add(buildRequirements(object, (Boolean) requirementData.getOrDefault("chargeable", true)));
                                }
                                data.setRequirements(requirements);
                            }
                            out.add(data);
                        }
                    }
                }
                ScriptFormCustom custom = new ScriptFormCustom(config, out, new SoundData("", 1f, 0f, true));
                if (config.containsKey("open_sound")) {
                    Map<String, Object> openSoundMap = (Map<String, Object>) config.get("open_sound");
                    custom.setOpenSound(new SoundData((String) openSoundMap.get("name"), Float.parseFloat(openSoundMap.getOrDefault("volume", 1f).toString()), Float.parseFloat(openSoundMap.getOrDefault("pitch", 0f).toString()), (Boolean) openSoundMap.getOrDefault("personal", true)));
                }
                custom.setStartMillis((Long) config.getOrDefault("startMillis", -1L));
                custom.setExpiredMillis((Long) config.getOrDefault("expiredMillis", -1L));
                if (custom.getWindow() != null) {
                    return custom;
                }
                break;
            case 2:
                //modal
                simpleResponseExecuteDataList = new ArrayList<>();
                for (Map<String, Object> component : (List<Map<String, Object>>) config.getOrDefault("components", new ArrayList<>())) {
                    SimpleResponseExecuteData data = new SimpleResponseExecuteData((List<String>) component.getOrDefault("commands", new ArrayList<>()), (List<String>) component.getOrDefault("messages", new ArrayList<>()), new ArrayList<>(), new ArrayList<>(), new ArrayList<>(), new ArrayList<>());
                    simpleResponseExecuteDataList.add(data);
                }
                ScriptFormModal modal = new ScriptFormModal(config, simpleResponseExecuteDataList, new SoundData("", 1f, 0f, true));
                if (config.containsKey("open_sound")) {
                    Map<String, Object> openSoundMap = (Map<String, Object>) config.get("open_sound");
                    modal.setOpenSound(new SoundData((String) openSoundMap.get("name"), Float.parseFloat(openSoundMap.getOrDefault("volume", 1f).toString()), Float.parseFloat(openSoundMap.getOrDefault("pitch", 0f).toString()), (Boolean) openSoundMap.getOrDefault("personal", true)));
                }
                modal.setStartMillis((Long) config.getOrDefault("startMillis", -1L));
                modal.setExpiredMillis((Long) config.getOrDefault("expiredMillis", -1L));
                if (modal.getWindow() != null) {
                    return modal;
                }
                break;
        }
        return null;
    }

    @Api
    public static Map<String, Object> convertConfigToMap(File file) {
        if (file.getName().endsWith(".json")) {
            InputStream stream;
            try {
                stream = new FileInputStream(file);
            } catch (FileNotFoundException e) {
                throw new RuntimeException(e);
            }
            InputStreamReader streamReader = new InputStreamReader(stream, StandardCharsets.UTF_8); //一定要以utf-8读取
            JsonReader reader = new JsonReader(streamReader);
            Gson gson = new GsonBuilder().registerTypeAdapter(new TypeToken<Map<String, Object>>() {
            }.getType(), new GsonAdapter()).create();
            Map<String, Object> mainMap = gson.fromJson(reader, new TypeToken<Map<String, Object>>() {
            }.getType());

            // Remember to close the streamReader after your implementation.
            try {
                reader.close();
                streamReader.close();
                stream.close();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            return mainMap;
        } else if (file.getName().endsWith(".yml")) {
            return new Config(file, Config.YAML).getAll();
        }
        return new HashMap<>();
    }

    /*
        WindowInfo stores the information, including its type and script name.
    */
    @Data
    public static class WindowInfo {
        private FormType type;

        private String script;

        // This is provided to customize your form more easily.
        private ScriptForm customizedScriptForm;

        public WindowInfo(FormType type, String script) {
            this.type = type;
            this.script = script;
        }

        public WindowInfo(FormType type, String script, ScriptForm customizedScriptForm) {
            this.type = type;
            this.script = script;
            this.customizedScriptForm = customizedScriptForm;
        }

    }
}
