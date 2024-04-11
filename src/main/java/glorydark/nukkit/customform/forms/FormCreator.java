package glorydark.nukkit.customform.forms;

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
import glorydark.nukkit.customform.CustomFormMain;
import glorydark.nukkit.customform.GsonAdapter;
import glorydark.nukkit.customform.annotations.Api;
import glorydark.nukkit.customform.event.FormOpenEvent;
import glorydark.nukkit.customform.scriptForms.data.SoundData;
import glorydark.nukkit.customform.scriptForms.data.execute_data.*;
import glorydark.nukkit.customform.scriptForms.data.execute_data.config.ConfigModification;
import glorydark.nukkit.customform.scriptForms.data.execute_data.config.ConfigModificationType;
import glorydark.nukkit.customform.scriptForms.data.requirement.Requirements;
import glorydark.nukkit.customform.scriptForms.data.requirement.config.ConfigRequirementData;
import glorydark.nukkit.customform.scriptForms.data.requirement.config.ConfigRequirementType;
import glorydark.nukkit.customform.scriptForms.data.requirement.economy.EconomyRequirementData;
import glorydark.nukkit.customform.scriptForms.data.requirement.economy.EconomyRequirementType;
import glorydark.nukkit.customform.scriptForms.data.requirement.item.ItemRequirementData;
import glorydark.nukkit.customform.scriptForms.data.requirement.item.NeedItem;
import glorydark.nukkit.customform.scriptForms.data.requirement.tips.TipsRequirementData;
import glorydark.nukkit.customform.scriptForms.data.requirement.tips.TipsRequirementType;
import glorydark.nukkit.customform.scriptForms.form.*;
import glorydark.nukkit.customform.utils.CameraUtils;
import lombok.Data;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.*;

public class FormCreator {
    public static final LinkedHashMap<String, WindowInfo> UI_CACHE = new LinkedHashMap<>();

    // Stored information and configuration about loaded forms.
    public static LinkedHashMap<String, ScriptForm> formScripts = new LinkedHashMap<>();

    // This value effectively reduces the conflicts brought by the duplication of ID value inside the Player.class(Nukkit)
    public static int formId = -1;

    public static String dateToString(Player player, Date date) {
        if (date.getTime() <= 0) {
            return "?";
        } else {
            SimpleDateFormat format = new SimpleDateFormat(CustomFormMain.language.translateString(player, "form_date_string_format"));
            return format.format(date);
        }
    }

    public static Date stringToDate(String string) {
        return stringToDate(string, "yyyy-MM-dd HH-mm-ss");
    }

    public static Date stringToDate(String string, String dateFormat) {
        if (string.equals("")) {
            return new Date(-1);
        }
        SimpleDateFormat format = new SimpleDateFormat(dateFormat);
        Date date;
        try {
            date = format.parse(string);
        } catch (Exception e) {
            date = new Date(-1);
            CustomFormMain.plugin.getLogger().warning("Error in parsing date string: " + string);
        }
        return date;
    }

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
        FormWindow window = scriptForm.getWindow(player);
        ModalFormRequestPacket packet = new ModalFormRequestPacket();
        packet.formId = formId;
        packet.data = window.getJSONData();
        player.dataPacket(packet);
        player.namedTag.putLong("lastFormRequestMillis", System.currentTimeMillis());
        UI_CACHE.put(player.getName(), new WindowInfo(formType, identifier, scriptForm, window));
    }

    /*
        By this function, you can show a certain form whose identifier is the same as identifier.
    */
    public static void showScriptForm(Player player, String identifier, boolean consoleExecute) {
        if (formScripts.containsKey(identifier)) {
            ScriptForm script = formScripts.get(identifier);
            if (!script.isInStartDate(player)) {
                return;
            }
            boolean allowOpen = false;
            if (!script.getOpenPermissions().contains(PermissionEnum.DEFAULT) && !script.getOpenPermissionWhitelist().contains(player.getName())) {
                for (PermissionEnum openPermission : script.getOpenPermissions()) {
                    if (openPermission == PermissionEnum.ONLY_USER) {
                        if (!player.isOp()) {
                            allowOpen = true;
                            break;
                        }
                    } else if (openPermission == PermissionEnum.OP) {
                        if (player.isOp()) {
                            allowOpen = true;
                            break;
                        }
                    } else if (openPermission == PermissionEnum.CONSOLE) {
                        if (consoleExecute) {
                            allowOpen = true;
                            break;
                        }
                    }
                }
            } else {
                allowOpen = true;
            }
            if (allowOpen) {
                showScriptForm(player, script, identifier);
            } else {
                player.sendMessage(CustomFormMain.language.translateString(player, "command_no_permission"));
            }
        }
    }

    @Api
    // This function can use as a way to customize your form.
    public static void showScriptForm(Player player, ScriptForm script, String identifier) {
        if (player.namedTag.contains("lastFormRequestMillis") && System.currentTimeMillis() - player.namedTag.getLong("lastFormRequestMillis") < CustomFormMain.coolDownMillis) {
            String tip = CustomFormMain.language.translateString(player, "operation_so_fast");
            if (!tip.equals("")) {
                player.sendMessage(tip);
            }
            return;
        }
        if (CustomFormMain.enableCameraAnimation) {
            CameraUtils.sendFormOpen(player);
        }
        FormWindow window = script.getWindow(player);
        if (script.getOpenRequirements().size() > 0) {
            boolean b = false;
            for (Requirements openRequirement : script.getOpenRequirements()) {
                if (openRequirement.isAllQualified(player)) {
                    b = true;
                    openRequirement.reduceAllCosts(player, 1);
                    break;
                }
            }
            if (!b) {
                return;
            }
        }
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
        Server.getInstance().getPluginManager().callEvent(new FormOpenEvent(script, player));
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
                    data = new EconomyRequirementData(EconomyRequirementType.EconomyAPI, Double.parseDouble(map.get("cost").toString()), chargeable, new Object());
                    break;
                case "Points":
                    // This is the way we deal with Points-type requirements
                    data = new EconomyRequirementData(EconomyRequirementType.Points, Double.parseDouble(map.get("cost").toString()), chargeable, new Object());
                    break;
                case "DCurrency":
                    // This is the way we deal with DCurrency-type requirements
                    data = new EconomyRequirementData(EconomyRequirementType.DCurrency, Double.parseDouble(map.get("cost").toString()), chargeable, new Object());
                    data.setExtraData(new String[]{(String) map.get("currencyType")});
                    break;
                case "Item":
                    itemRequirementData = new ItemRequirementData((boolean) map.getOrDefault("reduce", true));
                    List<NeedItem> needItems = new ArrayList<>();
                    List<Map<String, Object>> needItemMapList = (List<Map<String, Object>>) map.getOrDefault("costs", new ArrayList<>());
                    if (needItemMapList.size() > 0) {
                        for (Map<String, Object> subMap : needItemMapList) {
                            NeedItem item = new NeedItem((String) subMap.get("item"), (List<Map<String, Object>>) subMap.getOrDefault("alternatives", new ArrayList<>()), (Map<String, Object>) subMap.getOrDefault("must_have_tag", new LinkedHashMap<>()));
                            item.setCheckDamage((Boolean) subMap.getOrDefault("check_damage", true));
                            item.setCheckTag((Boolean) subMap.getOrDefault("check_tag", false));
                            item.setCheckCustomName((Boolean) subMap.getOrDefault("check_custom_name", true));
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
                    String config_compared_type = (String) map.get("compared_sign");
                    Object config_compared_value = map.get("compared_value");
                    failed_messages = (List<String>) map.getOrDefault("failed_messages", new ArrayList<>());
                    Object defaultComparedValue = map.getOrDefault("default_compared_value", null);
                    if (config_type == 0) {
                        String keyString = (String) map.get("key_name");
                        switch (config_compared_type) {
                            case "bigger":
                                configRequirementData = new ConfigRequirementData(0, ConfigRequirementType.BIGGER, keyString, config_compared_value, defaultComparedValue, failed_messages);
                                break;
                            case "bigger_or_equal":
                                configRequirementData = new ConfigRequirementData(0, ConfigRequirementType.BIGGER_OR_EQUAL, keyString, config_compared_value, defaultComparedValue, failed_messages);
                                break;
                            case "equal":
                                configRequirementData = new ConfigRequirementData(0, ConfigRequirementType.EQUAL, keyString, config_compared_value, defaultComparedValue, failed_messages);
                                break;
                            case "smaller":
                                configRequirementData = new ConfigRequirementData(0, ConfigRequirementType.SMALLER, keyString, config_compared_value, defaultComparedValue, failed_messages);
                                break;
                            case "smaller_or_equal":
                                configRequirementData = new ConfigRequirementData(0, ConfigRequirementType.SMALLER_OR_EQUAL, keyString, config_compared_value, defaultComparedValue, failed_messages);
                                break;
                            case "exist":
                                configRequirementData = new ConfigRequirementData(0, ConfigRequirementType.EXIST, keyString, config_compared_value, defaultComparedValue, failed_messages);
                                break;
                        }
                    } else if (config_type == 1) {
                        String config_name = (String) map.get("config_name");
                        switch (config_compared_type) {
                            case "bigger":
                                configRequirementData = new ConfigRequirementData(1, ConfigRequirementType.BIGGER, config_name, config_compared_value, defaultComparedValue, failed_messages);
                                break;
                            case "bigger_or_equal":
                                configRequirementData = new ConfigRequirementData(1, ConfigRequirementType.BIGGER_OR_EQUAL, config_name, config_compared_value, defaultComparedValue, failed_messages);
                                break;
                            case "equal":
                                configRequirementData = new ConfigRequirementData(1, ConfigRequirementType.EQUAL, config_name, config_compared_value, defaultComparedValue, failed_messages);
                                break;
                            case "smaller":
                                configRequirementData = new ConfigRequirementData(1, ConfigRequirementType.SMALLER, config_name, config_compared_value, defaultComparedValue, failed_messages);
                                break;
                            case "smaller_or_equal":
                                configRequirementData = new ConfigRequirementData(1, ConfigRequirementType.SMALLER_OR_EQUAL, config_name, config_compared_value, defaultComparedValue, failed_messages);
                                break;
                            case "exist":
                                configRequirementData = new ConfigRequirementData(1, ConfigRequirementType.EXIST, config_name, config_compared_value, defaultComparedValue, failed_messages);
                                break;
                        }
                    }
                    break;
            }
            if (data != null) {
                requirements.addEconomyRequirements(data);
            } else if (tips_data != null) {
                requirements.addTipsRequirements(tips_data);
            } else if (itemRequirementData != null) {
                requirements.addItemRequirementData(itemRequirementData);
            } else if (configRequirementData != null) {
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
                        data.setStartDate(stringToDate((String) component.getOrDefault("start_time", "")));
                        data.setExpiredDate(stringToDate((String) component.getOrDefault("expire_time", "")));
                        simpleResponseExecuteDataList.add(data);
                    }
                }

                List<Requirements> openRequirementsList = new ArrayList<>();
                if (config.containsKey("open_requirements")) {
                    Map<String, Object> requirementData = (Map<String, Object>) config.get("open_requirements");
                    for (List<Map<String, Object>> object : (List<List<Map<String, Object>>>) requirementData.get("data")) {
                        openRequirementsList.add(buildRequirements(object, (Boolean) requirementData.getOrDefault("chargeable", true)));
                    }
                }
                ScriptFormSimple simple = new ScriptFormSimple(config, simpleResponseExecuteDataList, new SoundData("", 1f, 0f, true), openRequirementsList);
                if (config.containsKey("open_sound")) {
                    Map<String, Object> openSoundMap = (Map<String, Object>) config.get("open_sound");
                    simple.setOpenSound(new SoundData((String) openSoundMap.get("name"), Float.parseFloat(openSoundMap.getOrDefault("volume", 1f).toString()), Float.parseFloat(openSoundMap.getOrDefault("pitch", 0f).toString()), (Boolean) openSoundMap.getOrDefault("personal", true)));
                }
                simple.setStartDate(stringToDate(config.getOrDefault("start_time", "").toString()));
                simple.setExpiredDate(stringToDate(config.getOrDefault("expire_time", "").toString()));
                if (simple.getWindow() != null) {
                    return simple;
                }
                break;
            case 1:
                List<ResponseExecuteData> out = new ArrayList<>();
                //custom
                for (Map<String, Object> component : (List<Map<String, Object>>) config.getOrDefault("components", new ArrayList<>())) {
                    String type = (String) component.getOrDefault("type", "");
                    switch (type) {
                        case "StepSlider":
                        case "Dropdown":
                            List<SimpleResponseExecuteData> data = new ArrayList<>();
                            List<Map<String, Object>> maps = (List<Map<String, Object>>) component.getOrDefault("responses", new ArrayList<>());
                            List<ConfigModification> configModifications = new ArrayList<>();
                            // todo 看看能否在某些地方能够运用到requirements
                            if (component.containsKey("config")) {
                                for (Map<String, Object> configEntry : (List<Map<String, Object>>) component.getOrDefault("configs", new ArrayList<>())) {
                                    int configType = (int) configEntry.get("type");
                                    String config_name = configEntry.get("key_name").toString();
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
                                    ConfigModification modification = new ConfigModification(configType, config_name, configEntry.get("deal_value"), modificationType);
                                    configModifications.add(modification);
                                }
                            }
                            for (Map<String, Object> map : maps) {
                                SimpleResponseExecuteData simpleResponseExecuteData = new SimpleResponseExecuteData((List<String>) map.getOrDefault("commands", new ArrayList<>()), (List<String>) map.getOrDefault("messages", new ArrayList<>()), new ArrayList<>(), new ArrayList<>(), new ArrayList<>(), configModifications);
                                simpleResponseExecuteData.setStartDate(stringToDate((String) map.getOrDefault("start_time", "")));
                                simpleResponseExecuteData.setExpiredDate(stringToDate((String) map.getOrDefault("expire_time", "")));
                                data.add(simpleResponseExecuteData);
                            }
                            out.add(new StepResponseExecuteData(data));
                            break;
                        case "PlayerListDropdown":
                            DropdownPlayerListResponse dropdownPlayerListResponse = new DropdownPlayerListResponse((List<String>) component.getOrDefault("commands", new ArrayList<>()), (List<String>) component.getOrDefault("messages", new ArrayList<>()), new ArrayList<>(), new ArrayList<>(), new ArrayList<>(), new ArrayList<>());
                            dropdownPlayerListResponse.setStartDate(stringToDate((String) component.getOrDefault("start_time", "")));
                            dropdownPlayerListResponse.setExpiredDate(stringToDate((String) component.getOrDefault("expire_time", "")));
                            if (component.containsKey("requirements")) {
                                List<Requirements> requirementsList = new ArrayList<>();
                                Map<String, Object> requirementData = (Map<String, Object>) component.get("requirements");
                                for (List<Map<String, Object>> object : (List<List<Map<String, Object>>>) requirementData.get("data")) {
                                    requirementsList.add(buildRequirements(object, (Boolean) requirementData.getOrDefault("chargeable", true)));
                                }
                                dropdownPlayerListResponse.setRequirements(requirementsList);
                            }
                            List<ConfigModification> configModificationsForPlayerListDropDown = new ArrayList<>();
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
                                    configModificationsForPlayerListDropDown.add(modification);
                                }
                            }
                            dropdownPlayerListResponse.setConfigModifications(configModificationsForPlayerListDropDown);
                            out.add(dropdownPlayerListResponse);
                            break;
                        case "Toggle":
                            Map<String, Object> toggleMap = (Map<String, Object>) component.getOrDefault("responses", new LinkedHashMap<>());
                            ToggleResponseExecuteData toggleResponseExecuteData = new ToggleResponseExecuteData((List<String>) toggleMap.getOrDefault("true_commands", new ArrayList<>()), (List<String>) toggleMap.getOrDefault("true_messages", new ArrayList<>()), (List<String>) toggleMap.getOrDefault("false_commands", new ArrayList<>()), (List<String>) toggleMap.getOrDefault("false_messages", new ArrayList<>()));
                            toggleResponseExecuteData.setStartDate(stringToDate((String) toggleMap.getOrDefault("start_time", "")));
                            toggleResponseExecuteData.setExpiredDate(stringToDate((String) toggleMap.getOrDefault("expire_time", "")));
                            out.add(toggleResponseExecuteData);
                            break;
                        case "Input":
                            configModificationsForPlayerListDropDown = new ArrayList<>();
                            for (Map<String, Object> configEntry : (List<Map<String, Object>>) component.getOrDefault("configs", new ArrayList<>())) {
                                int configType = (int) configEntry.get("type");
                                String config_name = configEntry.get("key_name").toString();
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
                                ConfigModification modification = new ConfigModification(configType, config_name, configEntry.get("deal_value"), modificationType);
                                configModificationsForPlayerListDropDown.add(modification);
                            }
                            SimpleResponseExecuteData simpleResponseExecuteData = new SimpleResponseExecuteData((List<String>) component.getOrDefault("commands", new ArrayList<>()), (List<String>) component.getOrDefault("messages", new ArrayList<>()), (List<String>) component.getOrDefault("failed_commands", new ArrayList<>()), (List<String>) component.getOrDefault("failed_messages", new ArrayList<>()), new ArrayList<>(), configModificationsForPlayerListDropDown);
                            simpleResponseExecuteData.setStartDate(stringToDate((String) component.getOrDefault("start_time", "")));
                            simpleResponseExecuteData.setExpiredDate(stringToDate((String) component.getOrDefault("expire_time", "")));
                            if (component.containsKey("requirements")) {
                                List<Requirements> requirements = new ArrayList<>();
                                Map<String, Object> requirementData = (Map<String, Object>) component.get("requirements");
                                for (List<Map<String, Object>> object : (List<List<Map<String, Object>>>) requirementData.get("data")) {
                                    requirements.add(buildRequirements(object, (Boolean) requirementData.getOrDefault("chargeable", true)));
                                }
                                simpleResponseExecuteData.setRequirements(requirements);
                            }
                            out.add(simpleResponseExecuteData);
                            break;
                        default:
                            out.add(null);
                            break;
                    }
                }
                openRequirementsList = new ArrayList<>();
                if (config.containsKey("open_requirements")) {
                    Map<String, Object> requirementData = (Map<String, Object>) config.get("open_requirements");
                    for (List<Map<String, Object>> object : (List<List<Map<String, Object>>>) requirementData.get("data")) {
                        openRequirementsList.add(buildRequirements(object, (Boolean) requirementData.getOrDefault("chargeable", true)));
                    }
                }
                ScriptFormCustom custom = new ScriptFormCustom(config, out, new SoundData("", 1f, 0f, true), openRequirementsList);
                if (config.containsKey("open_sound")) {
                    Map<String, Object> openSoundMap = (Map<String, Object>) config.get("open_sound");
                    custom.setOpenSound(new SoundData((String) openSoundMap.get("name"), Float.parseFloat(openSoundMap.getOrDefault("volume", 1f).toString()), Float.parseFloat(openSoundMap.getOrDefault("pitch", 0f).toString()), (Boolean) openSoundMap.getOrDefault("personal", true)));
                }
                custom.setStartDate(stringToDate(config.getOrDefault("start_time", "").toString()));
                custom.setExpiredDate(stringToDate(config.getOrDefault("expire_time", "").toString()));
                if (custom.getWindow() != null) {
                    return custom;
                }
                break;
            case 2:
                //modal
                simpleResponseExecuteDataList = new ArrayList<>();
                for (Map<String, Object> component : (List<Map<String, Object>>) config.getOrDefault("components", new ArrayList<>())) {
                    SimpleResponseExecuteData data = new SimpleResponseExecuteData((List<String>) component.getOrDefault("commands", new ArrayList<>()), (List<String>) component.getOrDefault("messages", new ArrayList<>()), new ArrayList<>(), new ArrayList<>(), new ArrayList<>(), new ArrayList<>());
                    if (component.containsKey("requirements")) {
                        List<Requirements> requirementsList = new ArrayList<>();
                        Map<String, Object> requirementData = (Map<String, Object>) component.get("requirements");
                        for (List<Map<String, Object>> object : (List<List<Map<String, Object>>>) requirementData.get("data")) {
                            requirementsList.add(buildRequirements(object, (Boolean) requirementData.getOrDefault("chargeable", true)));
                        }
                        data.setRequirements(requirementsList);
                    }
                    data.setStartDate(stringToDate((String) component.getOrDefault("start_time", "")));
                    data.setExpiredDate(stringToDate((String) component.getOrDefault("expire_time", "")));
                    simpleResponseExecuteDataList.add(data);
                }
                openRequirementsList = new ArrayList<>();
                if (config.containsKey("open_requirements")) {
                    Map<String, Object> requirementData = (Map<String, Object>) config.get("open_requirements");
                    for (List<Map<String, Object>> object : (List<List<Map<String, Object>>>) requirementData.get("data")) {
                        openRequirementsList.add(buildRequirements(object, (Boolean) requirementData.getOrDefault("chargeable", true)));
                    }
                }
                ScriptFormModal modal = new ScriptFormModal(config, simpleResponseExecuteDataList, new SoundData("", 1f, 0f, true), openRequirementsList);
                if (config.containsKey("open_sound")) {
                    Map<String, Object> openSoundMap = (Map<String, Object>) config.get("open_sound");
                    modal.setOpenSound(new SoundData((String) openSoundMap.get("name"), Float.parseFloat(openSoundMap.getOrDefault("volume", 1f).toString()), Float.parseFloat(openSoundMap.getOrDefault("pitch", 0f).toString()), (Boolean) openSoundMap.getOrDefault("personal", true)));
                }
                modal.setStartDate(stringToDate(config.getOrDefault("start_time", "").toString()));
                modal.setExpiredDate(stringToDate(config.getOrDefault("expire_time", "").toString()));
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

        private FormWindow formWindow;

        public WindowInfo(FormType type, String script, FormWindow formWindow) {
            this.type = type;
            this.script = script;
            this.formWindow = formWindow;
        }

        public WindowInfo(FormType type, String script, ScriptForm customizedScriptForm, FormWindow formWindow) {
            this.type = type;
            this.script = script;
            this.customizedScriptForm = customizedScriptForm;
            this.formWindow = formWindow;
        }

    }
}
