package glorydark.nukkit.customform.scriptForms.form.ddui;

import cn.nukkit.Player;
import cn.nukkit.ddui.Observable;
import cn.nukkit.ddui.ObservableOptions;
import cn.nukkit.form.response.FormResponse;
import cn.nukkit.form.window.FormWindow;
import glorydark.nukkit.customform.CustomFormMain;
import glorydark.nukkit.customform.factory.FormCreator;
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
    protected Map<String, Object> properties;
    protected SoundData openSound;
    protected Date startDate = new Date(-1);
    protected Date expiredDate = new Date(-1);
    protected List<Requirements> openRequirements;
    protected List<PermissionEnum> openPermissions;
    protected List<String> openPermissionWhitelist;
    protected Map<String, Object> variableConfig;

    protected final Map<Player, Map<String, Observable<?>>> playerVariables = new HashMap<>();
    protected static final Map<Player, ScriptFormDDUIBase> ACTIVE_FORMS = new HashMap<>();
    
    // Per-player selected option data: variableName -> selected option data (includes key to look up properties)
    protected final Map<Player, Map<String, Map<String, Object>>> playerSelectedOptions = new HashMap<>();
    
    // Per-player onChange listeners for each variable, so we can clean up before re-adding
    protected final Map<Player, Map<String, Observable.Listener<?>>> playerChangeListeners = new HashMap<>();
    
    // Processing flag to prevent recursion
    protected final Set<Player> processingAction = new HashSet<>();

    protected ScriptFormDDUIBase(Map<String, Object> config) {
        this.config = config;
        this.title = (String) config.getOrDefault("title", "DDUI Form");
        this.fallbackCommand = (String) config.get("fallback_command");
        this.properties = (Map<String, Object>) config.getOrDefault("properties", new HashMap<>());
        this.variableConfig = (Map<String, Object>) config.getOrDefault("variables", new HashMap<>());
        this.openPermissions = new ArrayList<>();
        this.openPermissionWhitelist = new ArrayList<>();
        
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
                ObservableOptions opts = ObservableOptions.builder().clientWritable(true).build();
                switch (type.toLowerCase()) {
                    case "string" -> vars.put(varName, new Observable<>(defaultValue != null ? defaultValue.toString() : "", opts));
                    case "boolean" -> vars.put(varName, new Observable<>(defaultValue != null && (Boolean) defaultValue, opts));
                    case "long", "int" -> vars.put(varName, new Observable<>(defaultValue != null ? Long.parseLong(defaultValue.toString()) : 0L, opts));
                }
            }
            return vars;
        });
    }

    public void cleanupPlayerVariables(Player player) {
        playerVariables.remove(player);
        playerSelectedOptions.remove(player);
        playerChangeListeners.remove(player);
        processingAction.remove(player);
    }

    // ============ Math expression evaluation ============
    // Can be used as cal("{a} * {b}") or directly in config
    
    public double calculate(String expr, Player player, Map<String, Observable<?>> vars) {
        // Build eval context
        Map<String, Object> evalContext = new HashMap<>();
        
        // Add variable values
        if (vars != null) {
            for (Map.Entry<String, Observable<?>> entry : vars.entrySet()) {
                try {
                    Object value = entry.getValue();
                    if (value instanceof Observable) {
                        java.lang.reflect.Method getValue = value.getClass().getMethod("getValue");
                        value = getValue.invoke(value);
                    }
                    evalContext.put(entry.getKey(), value);
                } catch (Exception ignored) {}
            }
        }
        
        // Resolve placeholders and evaluate
        String resolved = resolveStringWithPropertyData(expr, evalContext);
        return evaluateMathExpression(resolved);
    }
    
    // Convenience method for YAML: cal("{a} * {b}")
    public String calculateString(String expr, Player player, Map<String, Observable<?>> vars) {
        double result = calculate(expr, player, vars);
        if (result == (long) result) {
            return String.valueOf((long) result);
        }
        return String.valueOf(result);
    }

    // ============ Get properties for selected option ============
    // Uses properties_binding to look up from global properties

    @SuppressWarnings("unchecked")
    protected Map<String, Object> getOptionPropertiesForVariable(Player player, String varName) {
        Map<String, Observable<?>> vars = playerVariables.get(player);
        if (vars == null || varName == null) return new HashMap<>();
        
        // Get stored binding info
        Map<String, Map<String, Object>> selectedMap = playerSelectedOptions.get(player);
        if (selectedMap == null || !selectedMap.containsKey(varName)) return new HashMap<>();
        
        Map<String, Object> bindingInfo = selectedMap.get(varName);
        
        // Return binding info (includes selectedIndex, key, keys)
        Map<String, Object> result = new HashMap<>(bindingInfo);
        
        // Then, if there's a "key", look up global properties and merge
        String key = (String) bindingInfo.get("key");
        if (key != null && this.properties != null) {
            Object globalProps = this.properties.get(key);
            if (globalProps instanceof Map) {
                result.putAll((Map<String, Object>) globalProps);
            }
        }
        
        return result;
    }

    // Store selected option data when dropdown changes
    protected void storeSelectedOption(Player player, String varName, Map<String, Object> optionData) {
        playerSelectedOptions.computeIfAbsent(player, k -> new HashMap<>()).put(varName, optionData);
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

        // First do simple placeholder replacement (player name only)
        String initialText = text.replace("{player}", player.getName()).replace("%player%", player.getName());
        
        // If no DDUI variables in text, return simple Observable
        if (!VARIABLE_PATTERN.matcher(initialText).find()) {
            return new Observable<>(initialText);
        }
        
        String initialValue = replaceVariables(initialText, vars);
        Observable<String> result = new Observable<>(initialValue);

        for (String varName : foundVars) {
            Observable<?> var = vars.get(varName);
            if (var != null) {
                String finalText = initialText;
                result.subscribe(value -> {
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
        boolean negated = false;

        if (strValue.startsWith("{!") && strValue.endsWith("}")) {
            negated = true;
            strValue = "{" + strValue.substring(2, strValue.length() - 1) + "}";
        }

        if (strValue.startsWith("{") && strValue.endsWith("}")) {
            String varName = strValue.substring(1, strValue.length() - 1);
            Observable<?> var = vars.get(varName);
            if (var != null) {
                if (negated) {
                    return negateObservable(var);
                }
                return convertToBooleanObservable(var);
            }
        }
        return new Observable<>(Boolean.parseBoolean(strValue));
    }

    @SuppressWarnings("unchecked")
    private Observable<Boolean> negateObservable(Observable<?> var) {
        final Observable<Boolean> result = new Observable<>(false);
        try {
            java.lang.reflect.Method getValue = var.getClass().getMethod("getValue");
            Object current = getValue.invoke(var);
            result.setValue(current instanceof Boolean && !((Boolean) current));
            var.subscribe(value -> {
                if (value instanceof Boolean) {
                    result.setValue(!((Boolean) value));
                } else if (value instanceof Number) {
                    result.setValue(((Number) value).longValue() == 0);
                }
                return null;
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    private Observable<Boolean> convertToBooleanObservable(Observable<?> var) {
        if (var instanceof Observable) {
            try {
                java.lang.reflect.Method getValue = var.getClass().getMethod("getValue");
                Object value = getValue.invoke(var);
                if (value instanceof Boolean) {
                    return (Observable<Boolean>) var;
                }
            } catch (Exception ignored) {}
        }
        final Observable<Boolean> result = new Observable<>(false);
        try {
            var.subscribe(value -> {
                if (value instanceof Boolean) {
                    result.setValue((Boolean) value);
                } else if (value instanceof Number) {
                    result.setValue(((Number) value).longValue() != 0);
                } else if (value != null) {
                    result.setValue(Boolean.parseBoolean(value.toString()));
                }
                return null;
            });
        } catch (Exception ignored) {}
        return result;
    }

    protected Observable<Boolean> resolveDisabled(Object disabledConfig, Player player, Map<String, Observable<?>> vars) {
        if (disabledConfig == null) return new Observable<>(false);

        String strValue = ReplaceStringUtils.replace(disabledConfig.toString(), player);

        if (strValue.startsWith("{") && strValue.endsWith("}")) {
            String varName = strValue.substring(1, strValue.length() - 1);
            Observable<?> var = vars.get(varName);
            if (var != null) {
                return convertToBooleanObservable(var);
            }
        }
        return new Observable<>(Boolean.parseBoolean(strValue));
    }

    // ============ Get property data from dropdown selection ============

    @SuppressWarnings("unchecked")
    protected Map<String, Object> getSelectedOptionProperties(List<Map<String, Object>> options, long index) {
        if (options == null || index < 0 || index >= options.size()) {
            return new HashMap<>();
        }
        return options.get((int) index);
    }

    // ============ set_variables ============

    @SuppressWarnings("unchecked")
    protected void setVariables(Map<String, Object> actionConfig, Map<String, Observable<?>> vars, Player player) {
        if (!actionConfig.containsKey("set_variables")) return;

        List<Map<String, Object>> setVarsList = (List<Map<String, Object>>) actionConfig.get("set_variables");

        // Get property data for expression evaluation
        Map<String, Object> propertyData = new HashMap<>();
        String sourceVariable = null;
        if (actionConfig.containsKey("properties")) {
            List<String> propVars = (List<String>) actionConfig.get("properties");
            for (String propVar : propVars) {
                propertyData.putAll(getOptionPropertiesForVariable(player, propVar));
            }
        } else if (actionConfig.containsKey("property")) {
            propertyData = getOptionPropertiesForVariable(player, actionConfig.get("property").toString());
        } else if (actionConfig.containsKey("variable")) {
            sourceVariable = actionConfig.get("variable").toString();
            propertyData = getOptionPropertiesForVariable(player, sourceVariable);
        }

        for (Map<String, Object> varConfig : setVarsList) {
            String varName = (String) varConfig.get("variable");
            Object value = varConfig.get("value");

            if (varName == null || !vars.containsKey(varName)) continue;

            // Skip setting the source variable to prevent infinite loops
            if (varName.equals(sourceVariable)) continue;

            // If value contains expression, evaluate it
            if (value instanceof String) {
                String valueStr = value.toString();
                if (valueStr.contains("{") || valueStr.contains("property.")) {
                    // Build eval context
                    Map<String, Object> evalContext = new HashMap<>(propertyData);
                    if (vars != null) {
                        for (Map.Entry<String, Observable<?>> entry : vars.entrySet()) {
                            try {
                                Object varValue = entry.getValue();
                                if (varValue instanceof Observable) {
                                    java.lang.reflect.Method getValue = varValue.getClass().getMethod("getValue");
                                    varValue = getValue.invoke(varValue);
                                }
                                evalContext.put(entry.getKey(), varValue);
                            } catch (Exception ignored) {}
                        }
                    }
                    String resolved = resolveStringWithPropertyData(valueStr, evalContext);
                    Object evaluated = evaluateMathExpression(resolved);
                    value = evaluated;
                }
            }

            Observable<?> obs = vars.get(varName);
            Map<String, Object> varDef = (Map<String, Object>) variableConfig.getOrDefault(varName, new HashMap<>());
            String type = (String) varDef.getOrDefault("type", "string");

            switch (type.toLowerCase()) {
                case "string" -> {
                    String newVal = value != null ? value.toString() : "";
                    String oldVal = ((Observable<String>) obs).getValue();
                    if (!newVal.equals(oldVal)) {
                        ((Observable<String>) obs).setValue(newVal);
                    }
                }
                case "boolean" -> {
                    boolean newVal = Boolean.parseBoolean(value.toString());
                    boolean oldVal = ((Observable<Boolean>) obs).getValue();
                    if (newVal != oldVal) {
                        ((Observable<Boolean>) obs).setValue(newVal);
                    }
                }
                case "long", "int" -> {
                    long newVal = value instanceof Number ? ((Number) value).longValue() : Long.parseLong(value.toString());
                    long oldVal = ((Observable<Long>) obs).getValue();
                    if (newVal != oldVal) {
                        ((Observable<Long>) obs).setValue(newVal);
                    }
                }
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
    protected List<Map<String, Object>> parseRequirementsConfig(Map<String, Object> parentConfig) {
        List<Map<String, Object>> result = new ArrayList<>();
        if (parentConfig.containsKey("requirements")) {
            Map<String, Object> reqData = (Map<String, Object>) parentConfig.get("requirements");
            result.add(reqData);
        }
        return result;
    }

    // Build and evaluate requirements at runtime with current property data
    @SuppressWarnings("unchecked")
    private boolean evaluateRequirements(Player player, Map<String, Object> reqData, Map<String, Object> propertyData, Map<String, Observable<?>> vars) {
        if (reqData == null) return true;
        
        List<List<Map<String, Object>>> reqList = (List<List<Map<String, Object>>>) reqData.get("data");
        if (reqList == null) return true;
        
        boolean chargeable = (Boolean) reqData.getOrDefault("chargeable", true);
        
        for (List<Map<String, Object>> req : reqList) {
            // Process cost expressions with property data and variables
            List<Map<String, Object>> processedReq = processCostExpressions(req, propertyData, vars);
            
            Requirements requirements = FormCreator.buildRequirements("ddui_action", processedReq, chargeable);
            if (requirements.isAllQualified(player)) {
                if (chargeable) {
                    requirements.reduceAllCosts(player, 1);
                }
                return true;
            }
        }
        return false;
    }

    // Process cost expressions: replace {property.X} and {X} with actual values, then evaluate math
    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> processCostExpressions(List<Map<String, Object>> req, Map<String, Object> propertyData, Map<String, Observable<?>> vars) {
        // Build evaluation context: merge propertyData with regular variables
        Map<String, Object> evalContext = new HashMap<>();
        if (propertyData != null) {
            evalContext.putAll(propertyData);
            this.getClass(); // debug
        }
        System.out.println("[DDUI] processCostExpressions vars: " + (vars != null ? vars.keySet() : "null"));
        if (vars != null) {
            for (Map.Entry<String, Observable<?>> entry : vars.entrySet()) {
                try {
                    Object value = entry.getValue();
                    System.out.println("[DDUI] raw value for " + entry.getKey() + " = " + value + " (type: " + (value != null ? value.getClass().getName() : "null") + ")");
                    if (value instanceof Observable) {
                        java.lang.reflect.Method getMethod = value.getClass().getMethod("getValue");
                        value = getMethod.invoke(value);
                    }
                    if (value instanceof Number) {
                        evalContext.put(entry.getKey(), value);
                    }
                } catch (Exception ignored) {}
            }
        }
        
        List<Map<String, Object>> result = new ArrayList<>();
        for (Map<String, Object> r : req) {
            Map<String, Object> processed = new HashMap<>(r);
            if (processed.containsKey("cost")) {
                Object cost = processed.get("cost");
                if (cost instanceof String) {
                    String costStr = cost.toString();
                    if (costStr.contains("{") || costStr.contains("property.")) {
                        String resolved = resolveStringWithPropertyData(costStr, evalContext);
                        Object evaluated = evaluateMathExpression(resolved);
                        if (evaluated instanceof Number) {
                            processed.put("cost", ((Number) evaluated).doubleValue());
                        }
                    }
                }
            }
            result.add(processed);
        }
        return result;
    }

    // Resolve {property.X} and {X} placeholders using evalContext
    private String resolveStringWithPropertyData(String template, Map<String, Object> evalContext) {
        String result = template;
        
        // First, replace {property.X} and {X} placeholders (with braces)
        // Sort keys by length descending to avoid partial replacements
        List<String> keys = new ArrayList<>(evalContext.keySet());
        keys.sort((a, b) -> b.length() - a.length());
        
        for (String key : keys) {
            Object value = evalContext.get(key);
            if (value != null) {
                String strValue = value.toString();
                result = result.replace("{property." + key + "}", strValue);
                result = result.replace("{" + key + "}", strValue);
                result = result.replace("%property_" + key + "%", strValue);
                result = result.replace("%" + key + "%", strValue);
            }
        }
        
        // Then, replace property.X without braces (e.g., property.price -> 10)
        // This should happen AFTER brace replacements
        for (Map.Entry<String, Object> entry : evalContext.entrySet()) {
            Object value = entry.getValue();
            if (value != null && value instanceof Number) {
                String strValue = value.toString();
                result = result.replace("property." + entry.getKey(), strValue);
                result = result.replace(entry.getKey(), strValue);
            }
        }
        
        return result;
    }

    // Evaluate simple math expression
    private double evaluateMathExpression(String expr) {
        expr = expr.trim();
        try {
            return gameapi.tools.StringFormulaCalculator.evaluate(expr);
        } catch (Exception e) {
            return 0.0;
        }
    }

    // ============ Handle action response ============

    @SuppressWarnings("unchecked")
    protected void handleAction(Player player, Map<String, Object> actionConfig, Map<String, Observable<?>> vars) {
        if (actionConfig == null) return;
        
        // Prevent recursive processing
        if (!processingAction.add(player)) {
            return; // Already processing an action for this player
        }
        
        try {
            // 0. Process properties - get data from variables specified in "properties" field (can be multiple)
            Map<String, Object> mergedPropertyData = new HashMap<>();
            if (actionConfig.containsKey("properties")) {
                List<String> propVars = (List<String>) actionConfig.get("properties");
                for (String varName : propVars) {
                    Map<String, Object> data = getOptionPropertiesForVariable(player, varName);
                    mergedPropertyData.putAll(data);
                }
        } else if (actionConfig.containsKey("property")) {
            // Backward compatibility: single property
            String fromVar = actionConfig.get("property").toString();
            mergedPropertyData = getOptionPropertiesForVariable(player, fromVar);
        }

        // 1. Requirements - process mergedPropertyData for cost (supports expressions like property.price * quantity)
        if (actionConfig.containsKey("requirements")) {
            Map<String, Object> reqData = (Map<String, Object>) actionConfig.get("requirements");
            if (!evaluateRequirements(player, reqData, mergedPropertyData, vars)) {
                return; // Requirements not met
            }
        }

        // 2. set_variables
        setVariables(actionConfig, vars, player);

        // 3. commands & messages - process mergedPropertyData and variable placeholders
        List<String> commands = (List<String>) actionConfig.getOrDefault("commands", new ArrayList<>());
        List<String> messages = (List<String>) actionConfig.getOrDefault("messages", new ArrayList<>());

        commands = processCommandsWithPropertyDataAndVars(commands, mergedPropertyData, vars);
        messages = processMessagesWithPropertyDataAndVars(messages, mergedPropertyData, vars);

        executeActions(player, commands, messages, vars);

        // 4. config modifications
        if (actionConfig.containsKey("configs")) {
            List<ConfigModification> mods = parseConfigModifications((List<Map<String, Object>>) actionConfig.get("configs"));
            for (ConfigModification mod : mods) {
                mod.execute(player);
            }
        }
        } finally {
            processingAction.remove(player);
        }
    }

    private Object evalSimpleMath(String expr) {
        expr = expr.trim();
        try {
            // Use JavaScript engine for safe evaluation
            javax.script.ScriptEngine engine = new javax.script.ScriptEngineManager().getEngineByName("JavaScript");
            Object result = engine.eval(expr);
            if (result instanceof Number) {
                return ((Number) result).longValue(); // Return as long for integer results
            }
            return result;
        } catch (Exception e) {
            return 0;
        }
    }

    private List<String> processCommandsWithPropertyData(List<String> commands, Map<String, Object> propertyData) {
        List<String> result = new ArrayList<>();
        for (String cmd : commands) {
            String processed = cmd;
            for (Map.Entry<String, Object> entry : propertyData.entrySet()) {
                processed = processed.replace("{property." + entry.getKey() + "}", entry.getValue().toString());
                processed = processed.replace("%property_" + entry.getKey() + "%", entry.getValue().toString());
            }
            result.add(processed);
        }
        return result;
    }

    private List<String> processMessagesWithPropertyData(List<String> messages, Map<String, Object> propertyData) {
        List<String> result = new ArrayList<>();
        for (String msg : messages) {
            String processed = msg;
            for (Map.Entry<String, Object> entry : propertyData.entrySet()) {
                processed = processed.replace("{property." + entry.getKey() + "}", entry.getValue().toString());
                processed = processed.replace("%property_" + entry.getKey() + "%", entry.getValue().toString());
            }
            result.add(processed);
        }
        return result;
    }

    private List<String> processCommandsWithPropertyDataAndVars(List<String> commands, Map<String, Object> propertyData, Map<String, Observable<?>> vars) {
        List<String> result = new ArrayList<>();
        Map<String, Object> allData = new HashMap<>();
        if (propertyData != null) allData.putAll(propertyData);
        if (vars != null) {
            for (Map.Entry<String, Observable<?>> entry : vars.entrySet()) {
                try {
                    Object value = entry.getValue();
                    java.lang.reflect.Method getMethod = value.getClass().getMethod("get");
                    value = getMethod.invoke(value);
                    allData.put(entry.getKey(), value);
                } catch (Exception ignored) {}
            }
        }
        
        for (String cmd : commands) {
            String processed = cmd;
            // Sort keys by length descending to replace longer patterns first
            List<String> keys = new ArrayList<>(allData.keySet());
            keys.sort((a, b) -> b.length() - a.length());
            
            for (String key : keys) {
                Object value = allData.get(key);
                if (value == null) continue;
                String strValue = value.toString();
                // Replace {property.key} and {key}
                processed = processed.replace("{property." + key + "}", strValue);
                processed = processed.replace("{" + key + "}", strValue);
                processed = processed.replace("%property_" + key + "%", strValue);
                processed = processed.replace("%" + key + "%", strValue);
            }
            result.add(processed);
        }
        return result;
    }

    private List<String> processMessagesWithPropertyDataAndVars(List<String> messages, Map<String, Object> propertyData, Map<String, Observable<?>> vars) {
        List<String> result = new ArrayList<>();
        Map<String, Object> allData = new HashMap<>();
        if (propertyData != null) allData.putAll(propertyData);
        if (vars != null) {
            for (Map.Entry<String, Observable<?>> entry : vars.entrySet()) {
                try {
                    Object value = entry.getValue();
                    java.lang.reflect.Method getMethod = value.getClass().getMethod("get");
                    value = getMethod.invoke(value);
                    allData.put(entry.getKey(), value);
                } catch (Exception ignored) {}
            }
        }
        
        for (String msg : messages) {
            String processed = msg;
            // Sort keys by length descending to replace longer patterns first
            List<String> keys = new ArrayList<>(allData.keySet());
            keys.sort((a, b) -> b.length() - a.length());
            
            for (String key : keys) {
                Object value = allData.get(key);
                if (value == null) continue;
                String strValue = value.toString();
                // Replace {property.key} and {key}
                processed = processed.replace("{property." + key + "}", strValue);
                processed = processed.replace("{" + key + "}", strValue);
                processed = processed.replace("%property_" + key + "%", strValue);
                processed = processed.replace("%" + key + "%", strValue);
            }
            result.add(processed);
        }
        return result;
    }

    // ============ Parse permissions ============

    @SuppressWarnings("unchecked")
    private void parsePermissions() {
        if (!config.containsKey("open_permissions")) {
            openPermissions.add(PermissionEnum.DEFAULT);
            return;
        }
        
        Map<String, Object> permissionConfig = (Map<String, Object>) config.get("open_permissions");
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
        if (!config.containsKey("open_requirements")) {
            return;
        }
        Map<String, Object> reqData = (Map<String, Object>) config.get("open_requirements");
        List<List<Map<String, Object>>> reqList = (List<List<Map<String, Object>>>) reqData.get("data");
        if (reqList != null) {
            for (List<Map<String, Object>> req : reqList) {
                openRequirements.add(FormCreator.buildRequirements("ddui_form", req,
                        (Boolean) reqData.getOrDefault("chargeable", true)));
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
