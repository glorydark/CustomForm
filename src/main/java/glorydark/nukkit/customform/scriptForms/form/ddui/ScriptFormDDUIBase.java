package glorydark.nukkit.customform.scriptForms.form.ddui;

import cn.nukkit.Player;
import cn.nukkit.ddui.Observable;
import cn.nukkit.form.response.FormResponse;
import cn.nukkit.form.window.FormWindow;
import glorydark.nukkit.customform.CustomFormMain;
import glorydark.nukkit.customform.factory.FormCreator;
import glorydark.nukkit.customform.factory.FormType;
import glorydark.nukkit.customform.scriptForms.data.SoundData;
import glorydark.nukkit.customform.scriptForms.data.execute_data.config.ConfigModification;
import glorydark.nukkit.customform.scriptForms.data.execute_data.config.ConfigModificationType;
import glorydark.nukkit.customform.scriptForms.data.requirement.Requirements;
import glorydark.nukkit.customform.scriptForms.form.PermissionEnum;
import glorydark.nukkit.customform.scriptForms.form.ScriptForm;
import glorydark.nukkit.customform.utils.ReplaceStringUtils;
import lombok.Data;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Data
public abstract class ScriptFormDDUIBase implements ScriptForm {

    protected static final Pattern VARIABLE_PATTERN = Pattern.compile("\\{([a-zA-Z_][a-zA-Z0-9_]*)\\}");

    protected Map<String, Object> config;
    protected String title;
    protected String fallbackCommand;
    protected SoundData openSound;
    protected Date startDate = new Date(-1);
    protected Date expiredDate = new Date(-1);
    protected List<Requirements> openRequirements;
    protected List<PermissionEnum> openPermissions;
    protected List<String> openPermissionWhitelist;
    protected Map<String, Object> variableConfig;

    protected final Map<Player, Map<String, Observable<?>>> playerVariables = new HashMap<>();
    protected static final Map<Player, ScriptFormDDUIBase> ACTIVE_FORMS = new HashMap<>();

    protected ScriptFormDDUIBase(Map<String, Object> config) {
        this.config = config;
        this.title = (String) config.getOrDefault("title", "DDUI Form");
        this.fallbackCommand = (String) config.get("fallback_command");
        this.openRequirements = new ArrayList<>();
        this.openPermissions = new ArrayList<>();
        this.openPermissionWhitelist = new ArrayList<>();
        this.variableConfig = (Map<String, Object>) config.getOrDefault("variables", new HashMap<>());

        parsePermissions();
        parseOpenRequirements();
    }

    // ============ Abstract ============

    protected abstract void closeUI(Player player);

    // ============ Per-player variables ============

    @SuppressWarnings("unchecked")
    protected Map<String, Observable<?>> getPlayerVariables(Player player) {
        return playerVariables.computeIfAbsent(player, k -> {
            Map<String, Observable<?>> vars = new HashMap<>();
            for (Map.Entry<String, Object> entry : variableConfig.entrySet()) {
                String varName = entry.getKey();
                Map<String, Object> varDef = (Map<String, Object>) entry.getValue();
                String type = (String) varDef.getOrDefault("type", "string");
                Object defaultValue = varDef.get("default");
                switch (type.toLowerCase()) {
                    case "string" -> vars.put(varName, new Observable<>(defaultValue != null ? defaultValue.toString() : ""));
                    case "boolean" -> vars.put(varName, new Observable<>(defaultValue != null && (Boolean) defaultValue));
                    case "long", "int" -> vars.put(varName, new Observable<>(defaultValue != null ? Long.parseLong(defaultValue.toString()) : 0L));
                }
            }
            return vars;
        });
    }

    public void cleanupPlayerVariables(Player player) {
        playerVariables.remove(player);
    }

    public static void closeForm(Player player) {
        ScriptFormDDUIBase form = ACTIVE_FORMS.remove(player);
        if (form != null) {
            form.closeUI(player);
            form.cleanupPlayerVariables(player);
        }
    }

    // ============ Variable replacement ============

    protected Observable<String> resolveTextWithVariables(String text, Player player, Map<String, Observable<?>> vars) {
        if (text == null || text.isEmpty()) {
            return new Observable<>("");
        }

        text = ReplaceStringUtils.replace(text, player);

        Matcher matcher = VARIABLE_PATTERN.matcher(text);
        List<String> foundVars = new ArrayList<>();
        while (matcher.find()) {
            foundVars.add(matcher.group(1));
        }

        if (foundVars.isEmpty()) {
            return new Observable<>(text);
        }

        Observable<String> result = new Observable<>(replaceVariables(text, vars));

        for (String varName : foundVars) {
            Observable<?> var = vars.get(varName);
            if (var != null) {
                String finalText = text;
                var.subscribe(value -> {
                    String newValue = replaceVariables(ReplaceStringUtils.replace(finalText, player), vars);
                    if (!result.getValue().equals(newValue)) {
                        result.setValue(newValue);
                    }
                    return null;
                });
            }
        }

        return result;
    }

    protected String replaceVariables(String text, Map<String, Observable<?>> vars) {
        if (text == null) return "";

        Matcher matcher = VARIABLE_PATTERN.matcher(text);
        StringBuffer sb = new StringBuffer();
        while (matcher.find()) {
            String varName = matcher.group(1);
            Observable<?> var = vars.get(varName);
            String replacement = var != null ? String.valueOf(var.getValue()) : "{" + varName + "}";
            matcher.appendReplacement(sb, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(sb);
        return sb.toString();
    }

    // ============ Visibility ============

    protected Observable<Boolean> resolveVisible(Object visibleConfig, Player player, Map<String, Observable<?>> vars) {
        if (visibleConfig == null) return new Observable<>(true);

        String strValue = ReplaceStringUtils.replace(visibleConfig.toString(), player);

        Matcher matcher = VARIABLE_PATTERN.matcher(strValue);
        if (matcher.matches()) {
            String varName = matcher.group(1);
            Observable<?> var = vars.get(varName);
            if (var != null && var.getValue() instanceof Boolean) {
                return (Observable<Boolean>) var;
            }
        }
        return new Observable<>(Boolean.parseBoolean(strValue));
    }

    // ============ set_variables ============

    @SuppressWarnings("unchecked")
    protected void setVariables(Map<String, Object> actionConfig, Map<String, Observable<?>> vars) {
        if (!actionConfig.containsKey("set_variables")) return;

        Map<String, Object> setVars = (Map<String, Object>) actionConfig.get("set_variables");

        for (Map.Entry<String, Object> entry : setVars.entrySet()) {
            String varName = entry.getKey();
            Object value = entry.getValue();

            if (!vars.containsKey(varName)) continue;

            Observable<?> obs = vars.get(varName);
            Map<String, Object> varDef = (Map<String, Object>) variableConfig.getOrDefault(varName, new HashMap<>());
            String type = (String) varDef.getOrDefault("type", "string");

            switch (type.toLowerCase()) {
                case "string" -> ((Observable<String>) obs).setValue(value != null ? value.toString() : "");
                case "boolean" -> ((Observable<Boolean>) obs).setValue(Boolean.parseBoolean(value.toString()));
                case "long", "int" -> ((Observable<Long>) obs).setValue(Long.parseLong(value.toString()));
            }
        }
    }

    // ============ Execute commands/messages ============

    protected void executeActions(Player player, List<String> commands, List<String> messages, Map<String, Observable<?>> vars) {
        Map<String, String> replacements = new HashMap<>();
        for (Map.Entry<String, Observable<?>> entry : vars.entrySet()) {
            Object value = entry.getValue().getValue();
            replacements.put(entry.getKey(), value != null ? value.toString() : "");
        }

        for (String cmd : commands) {
            String processedCmd = ReplaceStringUtils.replace(cmd, player);
            processedCmd = replaceVariables(processedCmd, vars);
            for (Map.Entry<String, String> rep : replacements.entrySet()) {
                processedCmd = processedCmd.replace("%" + rep.getKey() + "%", rep.getValue());
            }
            CustomFormMain.plugin.getServer().dispatchCommand(
                    CustomFormMain.plugin.getServer().getConsoleSender(),
                    processedCmd
            );
        }

        for (String msg : messages) {
            String processedMsg = ReplaceStringUtils.replace(msg, player);
            processedMsg = replaceVariables(processedMsg, vars);
            for (Map.Entry<String, String> rep : replacements.entrySet()) {
                processedMsg = processedMsg.replace("%" + rep.getKey() + "%", rep.getValue());
            }
            player.sendMessage(processedMsg);
        }
    }

    // ============ Parse config modifications ============

    @SuppressWarnings("unchecked")
    protected List<ConfigModification> parseConfigModifications(List<Map<String, Object>> configsList) {
        List<ConfigModification> modifications = new ArrayList<>();
        for (Map<String, Object> entry : configsList) {
            int configType = (int) entry.get("type");
            String extraData = configType == 0 ? (String) entry.get("key_name") : (String) entry.get("config_name");
            String dealType = (String) entry.get("deal_type");
            ConfigModificationType modificationType = switch (dealType) {
                case "add" -> ConfigModificationType.ADD;
                case "deduct" -> ConfigModificationType.DEDUCT;
                case "set" -> ConfigModificationType.SET;
                case "remove" -> ConfigModificationType.REMOVE;
                default -> null;
            };
            if (modificationType != null) {
                modifications.add(new ConfigModification(configType, extraData, entry.get("deal_value"), modificationType));
            }
        }
        return modifications;
    }

    // ============ Parse requirements from config ============

    @SuppressWarnings("unchecked")
    protected List<Requirements> parseRequirements(Map<String, Object> parentConfig) {
        List<Requirements> result = new ArrayList<>();
        if (parentConfig.containsKey("requirements")) {
            Map<String, Object> reqData = (Map<String, Object>) parentConfig.get("requirements");
            List<List<Map<String, Object>>> reqList = (List<List<Map<String, Object>>>) reqData.get("data");
            if (reqList != null) {
                for (List<Map<String, Object>> req : reqList) {
                    result.add(FormCreator.buildRequirements("ddui_form", req,
                            (Boolean) reqData.getOrDefault("chargeable", true)));
                }
            }
        }
        return result;
    }

    // ============ Handle action response ============

    @SuppressWarnings("unchecked")
    protected void handleAction(Player player, Map<String, Object> actionConfig, Map<String, Observable<?>> vars) {
        if (actionConfig == null) return;

        // 1. Requirements
        List<Requirements> reqs = parseRequirements(actionConfig);
        if (!reqs.isEmpty()) {
            boolean qualified = false;
            for (Requirements req : reqs) {
                if (req.isAllQualified(player)) {
                    req.reduceAllCosts(player, 1);
                    qualified = true;
                    break;
                }
            }
            if (!qualified) return;
        }

        // 2. set_variables
        setVariables(actionConfig, vars);

        // 3. commands & messages
        List<String> commands = (List<String>) actionConfig.getOrDefault("commands", new ArrayList<>());
        List<String> messages = (List<String>) actionConfig.getOrDefault("messages", new ArrayList<>());
        executeActions(player, commands, messages, vars);

        // 4. config modifications
        if (actionConfig.containsKey("configs")) {
            List<ConfigModification> mods = parseConfigModifications((List<Map<String, Object>>) actionConfig.get("configs"));
            for (ConfigModification mod : mods) {
                mod.execute(player);
            }
        }
    }

    // ============ Parse permissions ============

    @SuppressWarnings("unchecked")
    private void parsePermissions() {
        Map<String, Object> permissionConfig = (Map<String, Object>) config.getOrDefault("open_permissions", new HashMap<>());
        List<String> types = (List<String>) permissionConfig.getOrDefault("types", new ArrayList<>());
        for (String type : types) {
            switch (type) {
                case "op" -> openPermissions.add(PermissionEnum.OP);
                case "console" -> openPermissions.add(PermissionEnum.CONSOLE);
                case "user-only" -> openPermissions.add(PermissionEnum.ONLY_USER);
            }
        }
        if (openPermissions.isEmpty()) {
            openPermissions.add(PermissionEnum.DEFAULT);
        }
        openPermissionWhitelist.addAll((List<String>) permissionConfig.getOrDefault("whitelist", new ArrayList<>()));
    }

    @SuppressWarnings("unchecked")
    private void parseOpenRequirements() {
        if (config.containsKey("open_requirements")) {
            Map<String, Object> reqData = (Map<String, Object>) config.get("open_requirements");
            List<List<Map<String, Object>>> reqList = (List<List<Map<String, Object>>>) reqData.get("data");
            if (reqList != null) {
                for (List<Map<String, Object>> req : reqList) {
                    openRequirements.add(FormCreator.buildRequirements("ddui_form", req,
                            (Boolean) reqData.getOrDefault("chargeable", true)));
                }
            }
        }
    }

    // ============ ScriptForm interface defaults ============

    @Deprecated
    @Override
    public void execute(Player player, FormWindow respondWindow, FormResponse response, Object... params) {
    }

    @Override
    public FormWindow getWindow(Player player) {
        return null;
    }

    @Override
    public List<Requirements> getOpenRequirements() { return openRequirements; }

    @Override
    public SoundData getOpenSound() { return openSound; }

    @Override
    public Date getStartDate() { return startDate; }

    @Override
    public Date getExpiredDate() { return expiredDate; }

    @Override
    public List<PermissionEnum> getOpenPermissions() { return openPermissions; }

    @Override
    public List<String> getOpenPermissionWhitelist() { return openPermissionWhitelist; }
}
