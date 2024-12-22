package glorydark.nukkit.customform.chestForm;

import cn.nukkit.Player;
import cn.nukkit.item.Item;
import glorydark.nukkit.customform.scriptForms.data.requirement.Requirements;
import glorydark.nukkit.customform.utils.ReplaceStringUtils;
import lombok.Data;

import java.util.List;

/**
 * @author glorydark
 */
@Data
public class ChestItemInfo {

    private final String name;

    private final String[] description;

    private final Item item;

    private final List<Requirements> requirements;

    private final List<String> successCommands;

    private final List<String> successMessages;

    private final List<String> failCommands;

    private final List<String> failMessages;

    private final boolean closeForm;

    public ChestItemInfo(String name, String[] description, Item item, List<Requirements> requirements, List<String> successCommands, List<String> successMessages, List<String> failCommands, List<String> failMessages, boolean closeForm) {
        this.name = name;
        this.description = description;
        this.item = item;
        this.requirements = requirements;
        this.successCommands = successCommands;
        this.successMessages = successMessages;
        this.failCommands = failCommands;
        this.failMessages = failMessages;
        this.closeForm = closeForm;
    }

    public Item getItem(Player player) {
        Item item1 = this.item.clone();
        item1.setCustomName(ReplaceStringUtils.replace(this.getName(), player));
        item1.setLore(this.getDescription());
        String[] lore = item1.getLore().clone();
        for (int i = 0; i < item1.getLore().length; i++) {
            lore[i] = ReplaceStringUtils.replace(lore[i], player);
        }
        item1.setLore(lore);
        return item1;
    }
}
