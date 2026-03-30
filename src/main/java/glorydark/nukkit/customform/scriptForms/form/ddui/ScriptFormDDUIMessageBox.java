package glorydark.nukkit.customform.scriptForms.form.ddui;

import cn.nukkit.Player;
import cn.nukkit.ddui.MessageBox;
import cn.nukkit.ddui.Observable;
import glorydark.nukkit.customform.factory.FormType;
import glorydark.nukkit.customform.utils.ReplaceStringUtils;
import lombok.Data;

import java.util.*;

public class ScriptFormDDUIMessageBox extends ScriptFormDDUIBase {

    private final String body;
    private final MessageBoxButton button1;
    private final MessageBoxButton button2;
    private final Map<Player, MessageBox> uiWindows = new HashMap<>();

    public ScriptFormDDUIMessageBox(Map<String, Object> config) {
        super(config);
        this.body = (String) config.getOrDefault("body", "");
        this.button1 = parseButton("button1");
        this.button2 = parseButton("button2");
    }

    @SuppressWarnings("unchecked")
    private MessageBoxButton parseButton(String key) {
        if (!config.containsKey(key)) return null;
        Map<String, Object> c = (Map<String, Object>) config.get(key);
        MessageBoxButton btn = new MessageBoxButton();
        btn.setLabel((String) c.getOrDefault("label", "OK"));
        btn.setTooltip((String) c.getOrDefault("tooltip", ""));
        btn.setConfig(c);
        btn.setCloseMenu((Boolean) c.getOrDefault("close_menu", false));
        return btn;
    }

    @Override
    protected void closeUI(Player player) {
        MessageBox box = uiWindows.remove(player);
        if (box != null) box.close(player);
    }

    public MessageBox createDDUIForm(Player player) {
        Map<String, Observable<?>> vars = getPlayerVariables(player);

        MessageBox messageBox = new MessageBox(resolveTextWithVariables(title, player, vars));
        messageBox.body(resolveTextWithVariables(body, player, vars));

        if (button1 != null) {
            Map<String, Object> c = button1.getConfig();
            boolean close1 = button1.isCloseMenu();
            messageBox.button1(
                    resolveTextWithVariables(button1.getLabel(), player, vars).getValue(),
                    ReplaceStringUtils.replace(button1.getTooltip(), player),
                    p -> {
                        handleAction(p, c, vars);
                        if (close1) closeForm(p);
                    }
            );
        }

        if (button2 != null) {
            Map<String, Object> c = button2.getConfig();
            boolean close2 = button2.isCloseMenu();
            messageBox.button2(
                    resolveTextWithVariables(button2.getLabel(), player, vars).getValue(),
                    ReplaceStringUtils.replace(button2.getTooltip(), player),
                    p -> {
                        handleAction(p, c, vars);
                        if (close2) closeForm(p);
                    }
            );
        }

        return messageBox;
    }

    @Override
    public void showToPlayer(Player player, FormType formType, String identifier) {
        playerVariables.remove(player);
        MessageBox messageBox = createDDUIForm(player);
        uiWindows.put(player, messageBox);
        ACTIVE_FORMS.put(player, this);
        messageBox.show(player);
    }

    @Data
    public static class MessageBoxButton {
        private String label;
        private String tooltip;
        private Map<String, Object> config;
        private boolean closeMenu;
    }
}
