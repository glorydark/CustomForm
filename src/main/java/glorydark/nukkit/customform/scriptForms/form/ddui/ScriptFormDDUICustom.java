package glorydark.nukkit.customform.scriptForms.form.ddui;

import cn.nukkit.Player;
import cn.nukkit.ddui.Observable;
import cn.nukkit.ddui.element.DropdownElement;
import cn.nukkit.ddui.element.options.*;
import glorydark.nukkit.customform.factory.FormType;
import glorydark.nukkit.customform.scriptForms.data.requirement.Requirements;
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
            component.setRequirements(parseRequirements(comp));
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

            switch (type.toLowerCase()) {
                case "header" -> form.header(
                        resolveTextWithVariables((String) c.getOrDefault("text", ""), player, vars),
                        HeaderOptions.builder().visible(visible).build());

                case "label" -> form.label(
                        resolveTextWithVariables((String) c.getOrDefault("text", ""), player, vars),
                        LabelOptions.builder().visible(visible).build());

                case "spacer" -> form.spacer(SpacerOptions.builder().visible(visible).build());
                case "divider" -> form.divider(DividerOptions.builder().visible(visible).build());

                case "textfield" -> {
                    Observable<String> tfLabel = resolveTextWithVariables((String) c.getOrDefault("label", ""), player, vars);
                    String varName = (String) c.get("variable");
                    Observable<String> tfValue = varName != null ? (Observable<String>) vars.get(varName) : null;
                    if (tfValue == null) tfValue = new Observable<>("");
                    form.textFieldWithObservableLabel(tfLabel, tfValue, TextFieldOptions.builder().visible(visible).build());
                }

                case "toggle" -> {
                    Observable<String> tLabel = resolveTextWithVariables((String) c.getOrDefault("label", ""), player, vars);
                    String varName = (String) c.get("variable");
                    Observable<Boolean> tValue = varName != null ? (Observable<Boolean>) vars.get(varName) : null;
                    if (tValue == null) tValue = new Observable<>(false);
                    form.toggleWithObservableLabel(tLabel, tValue, ToggleOptions.builder().visible(visible).build());

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
                    String varName = (String) c.get("variable");
                    Observable<Long> sValue = varName != null ? (Observable<Long>) vars.get(varName) : null;
                    if (sValue == null) sValue = new Observable<>(min);
                    form.sliderWithObservableLabel(sLabel, min, max, sValue, SliderElementOptions.builder().visible(visible).build());
                }

                case "dropdown" -> {
                    Observable<String> dLabel = resolveTextWithVariables((String) c.getOrDefault("label", ""), player, vars);
                    List<String> items = (List<String>) c.getOrDefault("items", new ArrayList<>());
                    List<DropdownElement.Item> ddItems = new ArrayList<>();
                    for (String item : items) {
                        ddItems.add(DropdownElement.Item.builder().label(item).build());
                    }
                    String varName = (String) c.get("variable");
                    Observable<Long> dValue = varName != null ? (Observable<Long>) vars.get(varName) : null;
                    if (dValue == null) dValue = new Observable<>(0L);
                    form.dropdownWithObservableLabel(dLabel, ddItems, dValue, DropdownOptions.builder().visible(visible).build());
                }

                case "button" -> {
                    Observable<String> bLabel = resolveTextWithVariables(
                            (String) c.getOrDefault("label", c.getOrDefault("text", "")), player, vars);
                    boolean closeMenu = component.isCloseMenu();

                    form.button(bLabel, p -> {
                        handleAction(p, c, vars);
                        if (closeMenu) closeForm(p);
                    }, ButtonOptions.builder().visible(visible).build());
                }

                case "close_button" -> form.closeButton(
                        CloseButtonOptions.builder().label((String) c.getOrDefault("label", "Close")).build());
            }
        }

        return form;
    }

    @Override
    public void showToPlayer(Player player, FormType formType, String identifier) {
        playerVariables.remove(player);
        DDUICustomForm form = createDDUIForm(player);
        uiWindows.put(player, form);
        ACTIVE_FORMS.put(player, this);
        form.show(player);
    }

    @Data
    public static class DDUIComponent {
        private String type;
        private Map<String, Object> config;
        private List<Requirements> requirements;
        private boolean closeMenu;
    }
}
