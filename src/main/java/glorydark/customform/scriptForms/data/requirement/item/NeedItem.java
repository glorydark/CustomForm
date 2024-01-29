package glorydark.customform.scriptForms.data.requirement.item;

import cn.nukkit.item.Item;
import glorydark.customform.utils.InventoryUtils;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class NeedItem {

    private Item item;

    private List<Item> alternatives = new ArrayList<>();

    private Item finalComparedItem = null;

    private Item hasItem;

    public NeedItem(String item, List<String> alternatives) {
        this.item = InventoryUtils.toItem(item);
        for (String s : alternatives) {
            this.alternatives.add(InventoryUtils.toItem(s));
        }
    }
}
