package glorydark.customform.scriptForms.data.execute_data.element;

import cn.nukkit.Player;
import cn.nukkit.Server;
import cn.nukkit.form.element.ElementDropdown;

import java.util.ArrayList;

/**
 * @author glorydark
 */
public class ElementPlayerListDropdown extends ElementDropdown {

    public ElementPlayerListDropdown(String text) {
        super(text);
    }

    public ElementPlayerListDropdown copyNew() {
        ElementPlayerListDropdown dropdown = new ElementPlayerListDropdown(this.getText());
        dropdown.setDefaultOptionIndex(this.getDefaultOptionIndex());
        for (String option : new ArrayList<>(dropdown.getOptions())) {
            dropdown.getOptions().remove(option);
        }
        for (Player player : Server.getInstance().getOnlinePlayers().values()) {
            dropdown.addOption(player.getName());
        }
        return dropdown;
    }
}
