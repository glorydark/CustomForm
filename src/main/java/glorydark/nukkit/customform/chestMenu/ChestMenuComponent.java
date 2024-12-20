package glorydark.nukkit.customform.chestMenu;

import cn.nukkit.Player;
import cn.nukkit.item.Item;
import cn.nukkit.item.enchantment.Enchantment;
import glorydark.nukkit.LanguageMain;
import glorydark.nukkit.customform.CustomFormMain;
import glorydark.nukkit.customform.scriptForms.data.requirement.Requirements;
import glorydark.nukkit.customform.utils.CommandUtils;
import glorydark.nukkit.customform.utils.Tools;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class ChestMenuComponent {

    protected List<String> successCommands = new ArrayList<>();
    protected List<String> successMessages = new ArrayList<>();
    protected List<String> failedCommands = new ArrayList<>();
    protected List<String> failedMessages = new ArrayList<>();
    private String name;
    private String description;
    private Item item;
    private boolean isEnchanted;
    private List<Requirements> requirements = new ArrayList<>();

    public ChestMenuComponent(String name, List<String> descriptions, String item, boolean isEnchanted) {
        this.name = name;
        this.description = Tools.toString(descriptions);
        this.isEnchanted = isEnchanted;
        /* Deal with item String */
        String[] strings = item.split(":");
        switch (strings.length) {
            case 1:
                this.item = Item.get(Integer.parseInt(strings[0]));
                break;
            case 2:
                this.item = Item.get(Integer.parseInt(strings[0]), Integer.parseInt(strings[1]));
                break;
            default:
                this.item = Item.get(1);
                break;
        }
        this.item.setLore(description.replace("\\n", "\n"));
        this.item.setCustomName(name);
        if (isEnchanted) {
            this.item.addEnchantment(Enchantment.get(Enchantment.ID_DURABILITY).setLevel(1));
        }
    }

    public void setSuccessMessages(List<String> successMessages) {
        this.successMessages = successMessages;
    }

    public void setSuccessCommands(List<String> successCommands) {
        this.successCommands = successCommands;
    }

    public void setFailedMessages(List<String> failedMessages) {
        this.failedMessages = failedMessages;
    }

    public void setFailedCommands(List<String> failedCommands) {
        this.failedCommands = failedCommands;
    }

    public void execute(Player player) {
        // To check whether player is qualified or not
        boolean success;
        Requirements successRequire = null;
        if (requirements.size() > 0) {
            success = false;
            for (Requirements require : requirements) {
                if (require.isAllQualified(player)) {
                    successRequire = require;
                    success = true;
                }
            }
        } else {
            success = true;
        }

        // Execute corresponding commands and messages
        if (success) {
            if (successRequire != null) {
                if (successRequire.isAllQualified(player)) {
                    successRequire.reduceAllCosts(player, 1);
                }
            }
            for (String successCommand : successCommands) {
                CommandUtils.executeCommand(player, replace(successCommand, player));
            }

            for (String successMessage : successMessages) {
                player.sendMessage(successMessage);
            }
        } else {
            for (String failedCommand : failedCommands) {
                CommandUtils.executeCommand(player, replace(failedCommand, player));
            }

            for (String failedMessage : failedMessages) {
                player.sendMessage(failedMessage);
            }
        }
    }

    public String replace(String text, Player player) {
        if (CustomFormMain.enableLanguageAPI) {
            text = LanguageMain.getInstance().getTranslation(CustomFormMain.plugin, player, text);
        }
        return text.replace("%player%", player.getName())
                .replace("{player}", player.getName())
                .replace("%level%", player.getLevel().getName());
    }
}
