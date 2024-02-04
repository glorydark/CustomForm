package glorydark.customform.scriptForms.data.requirement.item;

import cn.nukkit.Player;
import cn.nukkit.item.Item;
import cn.nukkit.nbt.tag.CompoundTag;
import glorydark.customform.CustomFormMain;
import glorydark.customform.utils.NukkitTypeUtils;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Data
public class ItemRequirementData {

    private boolean reduce;

    private List<NeedItem> needItems = new ArrayList<>();

    private boolean checkTag;

    private boolean checkCustomName;

    private boolean checkDamage;

    public ItemRequirementData(boolean reduce, boolean checkTag, boolean checkCustomName, boolean checkDamage) {
        this.reduce = reduce;
        this.checkTag = checkTag;
        this.checkCustomName = checkCustomName;
        this.checkDamage = checkDamage;
    }

    public boolean checkItemIsPossess(Player player, boolean reducing, int multiply) {
        if (needItems.size() > 0) {
            List<NeedItem> costItems = new ArrayList<>();

            for (NeedItem s : needItems) {
                boolean b = false;
                Item avail = getAvailableItem(player, s.getItem());
                if (avail != null) {
                    if (avail.getCount() >= s.getItem().getCount() * multiply) {
                        b = true;
                        s.setHasItem(avail);
                        s.setFinalComparedItem(s.getItem());
                        costItems.add(s);
                    }
                } else {
                    for (Item alternative : s.getAlternatives()) {
                        avail = getAvailableItem(player, alternative);
                        if (avail != null) {
                            if (avail.getCount() >= alternative.getCount() * multiply) {
                                b = true;
                                s.setHasItem(avail);
                                s.setFinalComparedItem(s.getItem());
                                costItems.add(s);
                            }
                        }
                    }
                }
                if (!b) {
                    StringBuilder builder = new StringBuilder();
                    builder.append(s.getItem().getName()).append("*").append(s.getItem().getCount());
                    for (Item alternative : s.getAlternatives()) {
                        builder.append("/").append(alternative.getName()).append("*").append(s.getItem().getCount());
                    }
                    player.sendMessage(CustomFormMain.language.translateString(player, "requirements_item_not_qualified", builder.toString()));
                    return false;
                }
            }
            if (reducing && reduce) {
                for (NeedItem cost : costItems) {
                    int balance = cost.getHasItem().getCount() - cost.getItem().getCount();
                    player.getInventory().removeItem(cost.getHasItem());
                    Item giveBalance = cost.getHasItem().clone();
                    giveBalance.setCount(balance);
                    player.getInventory().addItem(giveBalance);
                }
            }
        }
        return true;
    }

    public Item getAvailableItem(Player player, Item item) {
        if (item == null) {
            return null;
        }
        Item output = Item.get(item.getId(), item.getDamage(), item.getCount());
        output.setCompoundTag(item.getCompoundTag());
        output.setCount(0);
        for (Map.Entry<Integer, Item> mapEntry : player.getInventory().getContents().entrySet()) {
            Item entryValue = mapEntry.getValue();
            if (checkTag) {
                switch (NukkitTypeUtils.getNukkitType()) {
                    case POWER_NUKKIT_X:
                    case POWER_NUKKIT_X_2:
                    case MOT:
                        CompoundTag c1 = entryValue.getNamedTag();
                        CompoundTag c2 = item.getNamedTag();
                        boolean tagEqual = (c1 != null && c1.equals(c2)) || (c1 == null && c2 == null);
                        if (checkDamage && entryValue.getDamage() != item.getDamage()) {
                            continue;
                        }
                        if (entryValue.getNamespaceId().equals(item.getNamespaceId()) && tagEqual) {
                            output.setCount(output.getCount() + entryValue.getCount());
                        }
                        break;
                    default:
                        c1 = entryValue.getNamedTag();
                        c2 = item.getNamedTag();
                        tagEqual = (c1 != null && c1.equals(c2)) || (c1 == null && c2 == null);
                        if (checkDamage && entryValue.getDamage() != item.getDamage()) {
                            continue;
                        }
                        if (entryValue.getId() == item.getId() && tagEqual) {
                            output.setCount(output.getCount() + entryValue.getCount());
                        }
                        break;
                }
            } else {
                if (checkCustomName && !entryValue.getCustomName().equals(item.getCustomName())) {
                    continue;
                }
                switch (NukkitTypeUtils.getNukkitType()) {
                    case POWER_NUKKIT_X:
                    case POWER_NUKKIT_X_2:
                    case MOT:
                        if (entryValue.getNamespaceId().equals(item.getNamespaceId()) && entryValue.getDamage() == item.getDamage()) {
                            output.setCount(output.getCount() + entryValue.getCount());
                        }
                        break;
                    default:
                        if (entryValue.getId() == item.getId() && entryValue.getDamage() == item.getDamage()) {
                            output.setCount(output.getCount() + entryValue.getCount());
                        }
                        break;
                }
            }
        }
        return output;
    }
}
