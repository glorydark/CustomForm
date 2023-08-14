package glorydark.customform.scriptForms.data.requirement.item;

import cn.nukkit.Player;
import cn.nukkit.item.Item;
import lombok.Data;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

@Data
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
                boolean b = false;
                Item avail = getAvailableItem(player, s.getItem());
                if(avail != null) {
                    if(avail.getCount() >= s.getItem().getCount() * multiply) {
                        b = true;
                        s.setHasItem(avail);
                        s.setFinalComparedItem(s.getItem());
                        costItems.add(s);
                    }
                }else{
                    for(Item alternative : s.getAlternatives()) {
                        avail = getAvailableItem(player, alternative);
                        if(avail != null) {
                            if(avail.getCount() >= alternative.getCount() * multiply) {
                                b = true;
                                s.setHasItem(avail);
                                s.setFinalComparedItem(s.getItem());
                                costItems.add(s);
                            }
                        }
                    }
                }
                if(!b) {
                    return false;
                }
            }
            if(reducing && reduce) {
                for(NeedItem cost: costItems) {
                    int balance = cost.getHasItem().getCount() - cost.getFinalComparedItem().getCount();
                    player.getInventory().remove(cost.getHasItem());
                    Item giveBalance = cost.getHasItem().clone();
                    giveBalance.setCount(balance);
                    player.getInventory().addItem(giveBalance);
                }
            }
        }
        return true;
    }

    public Item getAvailableItem(Player player, Item item) {
        if(player.getInventory().contains(item)) {
            Item output = item.clone();
            output.setCount(0);
            for(Map.Entry<Integer, Item> mapEntry : player.getInventory().getContents().entrySet()) {
                Item entryValue = mapEntry.getValue();
                if(entryValue.getId() == item.getId() && entryValue.getDamage() == item.getDamage() && Arrays.equals(entryValue.getCompoundTag(), item.getCompoundTag())) {
                    output.setCount(output.getCount()+entryValue.getCount());
                }
            }
            return output;
        }else{
            return null;
        }
    }

}
