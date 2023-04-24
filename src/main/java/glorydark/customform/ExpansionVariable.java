package glorydark.customform;

import cn.nukkit.Player;
import cn.nukkit.item.Item;
import glorydark.customform.utils.Inventory;
import tip.utils.variables.BaseVariable;

public class ExpansionVariable extends BaseVariable {
    public ExpansionVariable(Player player) {
        super(player);
    }

    @Override
    public void strReplace() {
        Item item = player.getInventory().getItemInHand();
        if(item != null) {
            this.addStrReplaceString("{item_nbt_tag}", Inventory.bytesToHexString(item.getCompoundTag()));
            this.addStrReplaceString("{item_durability}", String.valueOf(item.getDamage()));
            this.addStrReplaceString("{item_name}", item.getName());
        }
        this.addStrReplaceString("{player_experience_amount}", String.valueOf(player.getExperience()));
        this.addStrReplaceString("{player_experience_level}", String.valueOf(player.getExperienceLevel()));
    }
}
