package glorydark.customform.scriptForms.data.requirement.item;

import cn.nukkit.Player;
import cn.nukkit.item.Item;
import cn.nukkit.nbt.tag.CompoundTag;
import glorydark.customform.CustomFormMain;
import glorydark.customform.utils.NukkitTypeUtils;
import lombok.Data;
import lombok.ToString;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Data
@ToString
public class ItemRequirementData {

    private boolean reduce;

    private List<NeedItem> needItems = new ArrayList<>();

    public ItemRequirementData(boolean reduce) {
        this.reduce = reduce;
    }

    public boolean checkItemIsPossess(Player player, boolean reducing, int multiply) {
        if (needItems.size() > 0) {
            List<NeedItem> costItems = new ArrayList<>();
            for (NeedItem s : needItems) {
                if (s.getItem().getId() != 0) {
                    s.setHasItems(new ArrayList<>());
                    boolean b = false;
                    List<Item> avail = getAvailableItem(player, s);
                    if (avail != null) {
                        int count = 0;
                        for (Item item : avail) {
                            count += item.getCount();
                        }
                        if (count >= s.getItem().getCount() * multiply) {
                            b = true;
                            s.setHasItems(avail);
                            s.setFinalComparedItem(s.getItem());
                            costItems.add(s);
                        }
                    } else {
                        for (Item alternative : s.getAlternatives()) {
                            if (alternative.getId() != 0) {
                                avail = getAvailableItem(player, s);
                                if (avail != null) {
                                    int count = 0;
                                    for (Item item : avail) {
                                        count += item.getCount();
                                    }
                                    if (count >= alternative.getCount() * multiply) {
                                        b = true;
                                        s.setHasItems(avail);
                                        s.setFinalComparedItem(s.getItem());
                                        costItems.add(s);
                                    }
                                }
                            } else {
                                CustomFormMain.plugin.getLogger().error("Find an air item in " + this);
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
                } else {
                    CustomFormMain.plugin.getLogger().error("Find an air item in " + this);
                    return false;
                }
            }
            if (reducing && reduce) {
                for (NeedItem cost : costItems) {
                    int costNeed = cost.getFinalComparedItem().getCount();
                    for (Item hasItem : cost.getHasItems()) {
                        player.getInventory().removeItem(hasItem);
                        costNeed -= hasItem.getCount();
                        if (costNeed <= 0) {
                            int surplus = Math.abs(costNeed);
                            Item newItem = Item.fromString(hasItem.getNamespaceId() + ":" + hasItem.getDamage());
                            newItem.setCount(surplus);
                            newItem.setCompoundTag(hasItem.getCompoundTag());
                            player.getInventory().addItem(newItem);
                            break;
                        }
                    }
                }
            }
        }
        return true;
    }

    public List<Item> getAvailableItem(Player player, NeedItem needItem) {
        Item item = needItem.getItem();
        if (item == null) {
            return null;
        }
        List<Item> hasItems = new ArrayList<>();
        for (Map.Entry<Integer, Item> mapEntry : player.getInventory().getContents().entrySet()) {
            Item entryValue = mapEntry.getValue();
            if (needItem.isCheckTag()) {
                switch (NukkitTypeUtils.getNukkitType()) {
                    case POWER_NUKKIT_X:
                    case POWER_NUKKIT_X_2:
                    case MOT:
                        CompoundTag c1 = entryValue.getNamedTag();
                        CompoundTag c2 = item.getNamedTag();
                        boolean tagEqual = (c1 != null && c1.equals(c2)) || (c1 == null && c2 == null);
                        if (needItem.isCheckDamage() && entryValue.getDamage() != item.getDamage()) {
                            continue;
                        }
                        if (entryValue.getNamespaceId().equals(item.getNamespaceId()) && tagEqual) {
                            hasItems.add(entryValue);
                        }
                        break;
                    default:
                        c1 = entryValue.getNamedTag();
                        c2 = item.getNamedTag();
                        tagEqual = (c1 != null && c1.equals(c2)) || (c1 == null && c2 == null);
                        if (needItem.isCheckDamage() && entryValue.getDamage() != item.getDamage()) {
                            continue;
                        }
                        if (entryValue.getId() == item.getId() && tagEqual) {
                            hasItems.add(entryValue);
                        }
                        break;
                }
            } else {
                if (needItem.isCheckCustomName() && !entryValue.getCustomName().equals(item.getCustomName())) {
                    continue;
                }
                switch (NukkitTypeUtils.getNukkitType()) {
                    case POWER_NUKKIT_X:
                    case POWER_NUKKIT_X_2:
                    case MOT:
                        if (needItem.isCheckDamage() && entryValue.getDamage() != item.getDamage()) {
                            continue;
                        }
                        if (entryValue.getNamespaceId().equals(item.getNamespaceId()) && entryValue.getDamage() == item.getDamage()) {
                            hasItems.add(entryValue);
                        }
                        break;
                    default:
                        if (needItem.isCheckDamage() && entryValue.getDamage() != item.getDamage()) {
                            continue;
                        }
                        if (entryValue.getId() == item.getId() && entryValue.getDamage() == item.getDamage()) {
                            hasItems.add(entryValue);
                        }
                        break;
                }
            }
        }
        return hasItems;
    }
}
