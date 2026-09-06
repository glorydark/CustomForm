package glorydark.nukkit.customform.scriptForms.form.ddui;

import cn.nukkit.Player;
import cn.nukkit.ddui.Observable;
import cn.nukkit.ddui.element.DropdownElement;
import cn.nukkit.ddui.element.options.*;
import cn.nukkit.ddui.properties.ObjectProperty;
import glorydark.nukkit.customform.factory.FormType;
import lombok.Data;

import java.util.*;

public class ScriptFormDDUICustom extends ScriptFormDDUIBase {

    private final List<DDUIComponent> components = new ArrayList<>();
    private final Map<Player, DDUICustomForm> uiWindows = new HashMap<>();

    public ScriptFormDDUICustom(Map<String, Object> config) {
        super(config);
        parseComponents();
    }

    @SuppressWarnings("unchecked")
    private void parseComponents() {
        List<Map<String, Object>> componentsConfig = (List<Map<String, Object>>) config.getOrDefault("components", new ArrayList<>());
        for (Map<String, Object> comp : componentsConfig) {
            DDUIComponent component = new DDUIComponent();
            component.setType((String) comp.getOrDefault("type", ""));
            component.setConfig(comp);
            component.setRequirementsConfig(parseRequirementsConfig(comp));
            component.setCloseMenu((Boolean) comp.getOrDefault("close_menu", false));
            components.add(component);
        }
    }

    @Override
    protected void closeUI(Player player) {
        DDUICustomForm form = uiWindows.remove(player);
        if (form != null) form.close(player);
    }

    @SuppressWarnings("unchecked")
    public DDUICustomForm createDDUIForm(Player player) {
        Map<String, Observable<?>> vars = getPlayerVariables(player);

        Observable<String> titleObs = resolveTextWithVariables(title, player, vars);
        DDUICustomForm form = new DDUICustomForm(titleObs);

        for (DDUIComponent component : components) {
            Map<String, Object> c = component.getConfig();
            String type = component.getType();
            Observable<Boolean> visible = resolveVisible(c.get("visible"), player, vars);
            Observable<Boolean> disabled = resolveDisabled(c.get("disabled"), player, vars);

            ObjectProperty element;
            switch (type.toLowerCase()) {
                case "header" -> {
                    element = form.header(
                            resolveTextWithVariables((String) c.getOrDefault("text", ""), player, vars),
                            HeaderOptions.builder()
                                    .visible(visible)
                                    .build());
                }
                case "label" -> {
                    element = form.label(
                            resolveTextWithVariables((String) c.getOrDefault("text", ""), player, vars),
                            LabelOptions.builder()
                                    .visible(visible)
                                    .build());
                }
                case "spacer" -> {
                    element = form.spacer(SpacerOptions.builder()
                            .visible(visible)
                            .build());
                }
                case "divider" -> {
                    element = form.divider(DividerOptions.builder()
                            .visible(visible)
                            .build());
                }
                case "textfield" -> {
                    Observable<String> tfLabel = resolveTextWithVariables((String) c.getOrDefault("label", ""), player, vars);
                    String varName = (String) c.get("variable");
                    Observable<String> tfValue = varName != null ? (Observable<String>) vars.get(varName) : null;
                    if (tfValue == null) tfValue = new Observable<>("");
                    element = form.textFieldWithObservableLabel(tfLabel, tfValue, TextFieldOptions.builder()
                            .visible(visible)
                            .disabled(disabled)
                            .build());

                    if (c.containsKey("on_change")) {
                        Map<String, Object> onChange = (Map<String, Object>) c.get("on_change");
                        tfValue.subscribe(value -> {
                            Map<String, Object> actionWithInput = new HashMap<>(onChange);
                            actionWithInput.put("input", value);
                            actionWithInput.put("variable", varName);
                            handleActionWithInput(player, actionWithInput, vars, value);
                            return null;
                        });
                    }
                }
                case "toggle" -> {
                    Observable<String> tLabel = resolveTextWithVariables((String) c.getOrDefault("label", ""), player, vars);
                    String varName = (String) c.get("variable");
                    Observable<Boolean> tValue = varName != null ? (Observable<Boolean>) vars.get(varName) : null;
                    if (tValue == null) tValue = new Observable<>(false);
                    element = form.toggleWithObservableLabel(tLabel, tValue, ToggleOptions.builder()
                            .visible(visible)
                            .disabled(disabled)
                            .build());

                    tValue.subscribe(value -> {
                        Object action = value ? c.get("on_true") : c.get("on_false");
                        handleAction(player, action instanceof Map ? (Map<String, Object>) action : null, vars);
                        return null;
                    });
                }
                case "slider" -> {
                    Observable<String> sLabel = resolveTextWithVariables((String) c.getOrDefault("label", ""), player, vars);
                    long min = Long.parseLong(c.getOrDefault("min", 0).toString());
                    long max = Long.parseLong(c.getOrDefault("max", 100).toString());
                    long step = Long.parseLong(c.getOrDefault("step", 1).toString());
                    String varName = (String) c.get("variable");
                    final String sliderVarName = varName;
                    Observable<Long> sValue = varName != null ? (Observable<Long>) vars.get(varName) : null;
                    if (sValue == null) sValue = new Observable<>(min);
                    SliderElementOptions opts = SliderElementOptions.builder()
                            .visible(visible)
                            .disabled(disabled)
                            .step(step)
                            .build();
                    
                    element = form.sliderWithObservableLabel(sLabel, min, max, sValue, opts);

                    if (c.containsKey("on_change")) {
                        // Store the on_change config for this slider
                        final Map<String, Object> onChangeConfig = (Map<String, Object>) c.get("on_change");
                        
                        // Create new listener
                        Observable.Listener<Long> listener = value -> {
                            Map<String, Object> actionWithInput = new HashMap<>(onChangeConfig);
                            actionWithInput.put("input", String.valueOf(value));
                            actionWithInput.put("value", value);
                            actionWithInput.put("variable", sliderVarName);
                            if (!actionWithInput.containsKey("property") && !actionWithInput.containsKey("properties")) {
                                actionWithInput.put("property", "selected_item");
                            }
                            
                            // Use fresh vars for evaluation, but don't trigger listeners
                            Map<String, Observable<?>> evalVars = getPlayerVariables(player);
                            handleActionWithInput(player, actionWithInput, evalVars, String.valueOf(value));
                            return null;
                        };
                        
                        // Remove old listener if exists
                        String listenerKey = "slider_" + sliderVarName;
                        Map<String, Observable.Listener<?>> playerListeners = playerChangeListeners.computeIfAbsent(player, k -> new HashMap<>());
                        Observable.Listener<?> oldListener = playerListeners.remove(listenerKey);
                        if (oldListener != null) {
                            try {
                                sValue.unsubscribe((Observable.Listener<Long>) oldListener);
                            } catch (Exception ignored) {}
                        }
                        
                        sValue.subscribe(listener);
                        playerListeners.put(listenerKey, listener);
                    }
                }

                case "dropdown" -> {
                    Observable<String> dLabel = resolveTextWithVariables((String) c.getOrDefault("label", ""), player, vars);
                    
                    // Parse items with inline properties_binding
                    List<Map<String, Object>> itemsData = (List<Map<String, Object>>) c.getOrDefault("items", new ArrayList<>());
                    List<String> displayItems = new ArrayList<>();
                    List<String> bindingKeys = new ArrayList<>();
                    
                    for (Map<String, Object> item : itemsData) {
                        String name = (String) item.getOrDefault("name", "");
                        String binding = (String) item.getOrDefault("properties_binding", "");
                        displayItems.add(name);
                        bindingKeys.add(binding);
                    }
                    
                    String varName = (String) c.get("variable");
                    
                    // Initialize binding info with current selection
                    Observable<Long> dValue = varName != null ? (Observable<Long>) vars.get(varName) : null;
                    if (dValue == null) dValue = new Observable<>(0L);
                    
                    // Store initial selection
                    if (varName != null && !bindingKeys.isEmpty()) {
                        long selectedIdx = dValue.getValue();
                        Map<String, Object> bindingInfo = new HashMap<>();
                        bindingInfo.put("keys", bindingKeys);
                        bindingInfo.put("selectedIndex", selectedIdx);
                        String key = selectedIdx < bindingKeys.size() ? bindingKeys.get((int) selectedIdx) : null;
                        bindingInfo.put("key", key);
                        storeSelectedOption(player, varName, bindingInfo);
                    }
                    
                    List<DropdownElement.Item> ddItems = new ArrayList<>();
                    for (String item : displayItems) {
                        ddItems.add(DropdownElement.Item.builder().label(item).build());
                    }
                    
                    boolean hasOnChange = c.containsKey("on_change");
                    
                    DropdownOptions opts;
                    if (hasOnChange) {
                        opts = DropdownOptions.builder()
                                .visible(visible)
                                .disabled(disabled)
                                .build();
                    } else {
                        opts = DropdownOptions.builder()
                                .visible(visible)
                                .disabled(disabled)
                                .build();
                    }
                    
                    element = form.dropdownWithObservableLabel(dLabel, ddItems, dValue, opts);
                    
                    // Subscribe to value changes for on_change
                    if (hasOnChange) {
                        // Remove old listener if exists
                        Map<String, Observable.Listener<?>> playerListeners = playerChangeListeners.computeIfAbsent(player, k -> new HashMap<>());
                        String listenerKey = "dropdown_" + varName;
                        if (playerListeners.containsKey(listenerKey)) {
                            dValue.unsubscribe((Observable.Listener<Long>) playerListeners.remove(listenerKey));
                        }
                        
                        // Create and add new listener
                        final List<String> finalBindingKeys = bindingKeys;
                        final List<String> finalDisplayItems = displayItems;
                        Observable.Listener<Long> listener = value -> {
                            if (value < finalBindingKeys.size()) {
                                Map<String, Object> bindingInfo = new HashMap<>();
                                bindingInfo.put("keys", finalBindingKeys);
                                bindingInfo.put("selectedIndex", value);
                                String key = finalBindingKeys.get(value.intValue());
                                bindingInfo.put("key", key);
                                storeSelectedOption(player, varName, bindingInfo);
                            }
                            return null;
                        };
                        dValue.subscribe(listener);
                        playerListeners.put(listenerKey, listener);
                    }
                }

                case "button" -> {
                    Observable<String> bLabel = resolveTextWithVariables(
                            (String) c.getOrDefault("label", c.getOrDefault("text", "")), player, vars);
                    boolean closeMenu = component.isCloseMenu();
                    
                    Observable<String> tooltip = resolveTextWithVariables(
                            (String) c.getOrDefault("tooltip", ""), player, vars);

                    element = form.button(bLabel, p -> {
                        handleAction(p, c, vars);
                        if (closeMenu) closeForm(p);
                    }, ButtonOptions.builder()
                            .visible(visible)
                            .disabled(disabled)
                            .tooltip(tooltip)
                            .build());
                }

                case "close_button" -> {
                    String closeLabel = (String) c.getOrDefault("label", "Close");
                    element = form.closeButton(
                            CloseButtonOptions.builder()
                                    .label(closeLabel)
                                    .visible(visible)
                                    .build());
                }

                default -> element = null;
            }

            // Remove setProperty calls since we now use Options builder
        }

        return form;
    }

    @Override
    public void showToPlayer(Player player, FormType formType, String identifier) {
        playerVariables.remove(player);
        playerSelectedOptions.remove(player);
        
        // Initialize selected options for all dropdowns with items
        Map<String, Observable<?>> vars = getPlayerVariables(player);
        for (DDUIComponent comp : components) {
            Map<String, Object> c = comp.getConfig();
            if ("dropdown".equals(c.get("type")) && c.containsKey("items")) {
                String varName = (String) c.get("variable");
                if (varName != null) {
                    List<Map<String, Object>> itemsData = (List<Map<String, Object>>) c.get("items");
                    Observable<?> obs = vars.get(varName);
                    if (obs != null && itemsData != null && !itemsData.isEmpty()) {
                        long index = 0;
                        if (obs.getValue() instanceof Number) {
                            index = ((Number) obs.getValue()).longValue();
                        }
                        if (index < itemsData.size()) {
                            storeSelectedOption(player, varName, itemsData.get((int) index));
                        }
                    }
                }
            }
        }
        
        DDUICustomForm form = createDDUIForm(player);
        uiWindows.put(player, form);
        ACTIVE_FORMS.put(player, this);
        form.show(player);
    }

    private void handleActionWithInput(Player player, Map<String, Object> actionConfig, Map<String, Observable<?>> vars, String inputValue) {
        if (actionConfig == null) return;

        Map<String, Object> newConfig = new HashMap<>(actionConfig);

        // Auto-set variable if component has variable binding
        String varName = (String) actionConfig.get("variable");
        if (varName != null && vars.containsKey(varName)) {
            // Add the variable to set_variables if not present
            if (!newConfig.containsKey("set_variables")) {
                newConfig.put("set_variables", new ArrayList<Map<String, Object>>());
            }
            List<Map<String, Object>> setVarsList = (List<Map<String, Object>>) newConfig.get("set_variables");
            Map<String, Object> varConfig = new HashMap<>();
            varConfig.put("variable", varName);
            varConfig.put("value", inputValue);
            setVarsList.add(varConfig);
        }
        
        // For dropdown/slider on_change, add property to get property data for expressions
        if (varName != null && newConfig.containsKey("set_variables")) {
            if (!newConfig.containsKey("property")) {
                newConfig.put("property", varName);
            }
        }

        // Check conditions and execute matching action
        if (actionConfig.containsKey("conditions")) {
            // Get property data for condition evaluation
            Map<String, Object> propertyData = new HashMap<>();
            if (actionConfig.containsKey("properties")) {
                List<String> propVarsList = (List<String>) actionConfig.get("properties");
                for (String propVar : propVarsList) {
                    propertyData.putAll(getOptionPropertiesForVariable(player, propVar));
                }
            }
            
            List<Map<String, Object>> conditions = (List<Map<String, Object>>) actionConfig.get("conditions");
            boolean matched = false;
            for (Map<String, Object> cond : conditions) {
                if (checkCondition(cond, inputValue, propertyData)) {
                    if (cond.containsKey("then")) {
                        Map<String, Object> then = (Map<String, Object>) cond.get("then");
                        mergeAction(newConfig, then, inputValue);
                    }
                    matched = true;
                    break;
                }
            }
            if (!matched && actionConfig.containsKey("then")) {
                Map<String, Object> then = (Map<String, Object>) actionConfig.get("then");
                mergeAction(newConfig, then, inputValue);
            }
        } else {
            // Simple commands/messages with {input} {value} replacement
            replaceInputPlaceholders(newConfig, inputValue);
        }

        handleAction(player, newConfig, vars);
    }

    @SuppressWarnings("unchecked")
    private void replaceInputPlaceholders(Map<String, Object> config, String inputValue) {
        if (config.containsKey("commands")) {
            List<String> newCommands = new ArrayList<>();
            Object cmdObj = config.get("commands");
            if (cmdObj instanceof List) {
                for (Object cmd : (List<?>) cmdObj) {
                    newCommands.add(cmd.toString().replace("{input}", inputValue).replace("{value}", inputValue));
                }
            } else {
                newCommands.add(cmdObj.toString().replace("{input}", inputValue).replace("{value}", inputValue));
            }
            config.put("commands", newCommands);
        }
        if (config.containsKey("messages")) {
            List<String> newMessages = new ArrayList<>();
            Object msgObj = config.get("messages");
            if (msgObj instanceof List) {
                for (Object msg : (List<?>) msgObj) {
                    newMessages.add(msg.toString().replace("{input}", inputValue).replace("{value}", inputValue));
                }
            } else {
                newMessages.add(msgObj.toString().replace("{input}", inputValue).replace("{value}", inputValue));
            }
            config.put("messages", newMessages);
        }
    }

    @SuppressWarnings("unchecked")
    private void mergeAction(Map<String, Object> target, Map<String, Object> source, String inputValue) {
        if (source.containsKey("commands")) {
            List<String> newCommands = new ArrayList<>();
            Object cmdObj = source.get("commands");
            if (cmdObj instanceof List) {
                for (Object cmd : (List<?>) cmdObj) {
                    newCommands.add(cmd.toString().replace("{input}", inputValue).replace("{value}", inputValue));
                }
            } else {
                newCommands.add(cmdObj.toString().replace("{input}", inputValue).replace("{value}", inputValue));
            }
            target.put("commands", newCommands);
        }
        if (source.containsKey("messages")) {
            List<String> newMessages = new ArrayList<>();
            Object msgObj = source.get("messages");
            if (msgObj instanceof List) {
                for (Object msg : (List<?>) msgObj) {
                    newMessages.add(msg.toString().replace("{input}", inputValue).replace("{value}", inputValue));
                }
            } else {
                newMessages.add(msgObj.toString().replace("{input}", inputValue).replace("{value}", inputValue));
            }
            target.put("messages", newMessages);
        }
        if (source.containsKey("set_variables")) {
            target.put("set_variables", source.get("set_variables"));
        }
        if (source.containsKey("configs")) {
            target.put("configs", source.get("configs"));
        }
    }

    private boolean checkCondition(Map<String, Object> condition, String inputValue, Map<String, Object> propertyData) {
        String type = ((String) condition.getOrDefault("type", "string")).toLowerCase();
        Object targetValue = condition.get("value");
        String operator = (String) condition.getOrDefault("operator", "==");
        
        // If field is specified, get value from propertyData instead of inputValue
        String field = (String) condition.get("field");
        String compareValue = inputValue;
        if (field != null && propertyData != null && propertyData.containsKey(field)) {
            compareValue = propertyData.get(field).toString();
        }

        try {
            switch (type) {
                case "int":
                case "long": {
                    long input = Long.parseLong(compareValue);
                    long target = Long.parseLong(targetValue.toString());
                    return compareLong(input, target, operator);
                }
                case "double": {
                    double input = Double.parseDouble(compareValue);
                    double target = Double.parseDouble(targetValue.toString());
                    return compareDouble(input, target, operator);
                }
                case "string": {
                    String input = compareValue;
                    String target = targetValue.toString();
                    return compareString(input, target, operator);
                }
                default: {
                    return compareValue.equals(targetValue.toString());
                }
            }
        } catch (Exception e) {
            return false;
        }
    }

    private boolean compareLong(long input, long target, String op) {
        return switch (op) {
            case ">" -> input > target;
            case ">=" -> input >= target;
            case "<" -> input < target;
            case "<=" -> input <= target;
            case "==" -> input == target;
            case "!=" -> input != target;
            default -> false;
        };
    }

    private boolean compareDouble(double input, double target, String op) {
        return switch (op) {
            case ">" -> input > target;
            case ">=" -> input >= target;
            case "<" -> input < target;
            case "<=" -> input <= target;
            case "==" -> input == target;
            case "!=" -> input != target;
            default -> false;
        };
    }

    private boolean compareString(String input, String target, String op) {
        return switch (op) {
            case "==", "equals" -> input.equals(target);
            case "!=" -> !input.equals(target);
            case "contains" -> input.contains(target);
            case "startsWith" -> input.startsWith(target);
            case "endsWith" -> input.endsWith(target);
            default -> false;
        };
    }

    @Data
    public static class DDUIComponent {
        private String type;
        private Map<String, Object> config;
        private List<Map<String, Object>> requirementsConfig;
        private boolean closeMenu;
    }
}
