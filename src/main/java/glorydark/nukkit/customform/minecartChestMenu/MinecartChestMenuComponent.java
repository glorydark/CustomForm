package glorydark.nukkit.customform.minecartChestMenu;

import cn.nukkit.Player;
import cn.nukkit.item.Item;
import cn.nukkit.item.enchantment.Enchantment;
import glorydark.nukkit.customform.scriptForms.data.requirement.Requirements;
import glorydark.nukkit.customform.utils.CommandUtils;
import glorydark.nukkit.customform.utils.ReplaceStringUtils;
import glorydark.nukkit.customform.utils.Tools;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class MinecartChestMenuComponent {

    protected List<String> successCommands = new ArrayList<>();
    protected List<String> successMessages = new ArrayList<>();
    protected List<String> failedCommands = new ArrayList<>();
    protected List<String> failedMessages = new ArrayList<>();
    private final String name;
    private final String description;
    private final Item item;
    private final boolean isEnchanted;
    private List<Requirements> requirements = new ArrayList<>();

    public MinecartChestMenuComponent(String name, List<String> descriptions, String item, boolean isEnchanted) {
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
        this.item.setLore(this.description);
        this.item.setCustomName(this.name);
        if (isEnchanted) {
            this.item.addEnchantment(Enchantment.get(Enchantment.ID_DURABILITY).setLevel(1));
        }
    }

    public Item getItem(Player player) {
        Item item1 = this.item.clone();
        item1.setCustomName(ReplaceStringUtils.replace(item1.getCustomName(), player));
        String[] lore = item1.getLore().clone();
        for (int i = 0; i < item1.getLore().length; i++) {
            lore[i] = ReplaceStringUtils.replace(lore[i], player);
        }
        item1.setLore(lore);
        return item1;
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
        Requirements successRequire = null;
        if (!requirements.isEmpty()) {
            for (Requirements require : requirements) {
                if (require != null && require.isAllQualified(player)) {
                    successRequire = require;
                }
            }
            // Execute corresponding commands and messages
            if (successRequire != null) {
                successRequire.reduceAllCosts(player, 1);
                for (String successCommand : successCommands) {
                    CommandUtils.executeCommand(player, ReplaceStringUtils.replace(successCommand, player, true, true));
                }

                for (String successMessage : successMessages) {
                    player.sendMessage(ReplaceStringUtils.replace(successMessage, player));
                }
            } else {
                for (String failedCommand : failedCommands) {
                    CommandUtils.executeCommand(player, ReplaceStringUtils.replace(failedCommand, player, true, true));
                }

                for (String failedMessage : failedMessages) {
                    player.sendMessage(ReplaceStringUtils.replace(failedMessage, player));
                }
            }
        } else {
            for (String successCommand : successCommands) {
                CommandUtils.executeCommand(player, ReplaceStringUtils.replace(successCommand, player, true, true));
            }

            for (String successMessage : successMessages) {
                player.sendMessage(ReplaceStringUtils.replace(successMessage, player));
            }
        }

    }
}
