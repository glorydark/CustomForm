package glorydark.nukkit.customform.scriptForms.data.requirement.item;

import cn.nukkit.item.Item;
import glorydark.nukkit.customform.utils.InventoryUtils;
import lombok.Data;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Data
public class NeedItem {

    private Item item;

    // todo:
    //  alternative should be instanceof NeedItem soon
    //  this will allow users' higher-level customization
    private List<Item> alternatives = new ArrayList<>();

    private Item finalComparedItem = null;

    private List<Item> hasItems = new ArrayList<>();

    private boolean checkTag = true;

    private boolean checkCustomName = false;

    private boolean checkDamage = true;

    private Map<String, Object> mustHaveTag;

    public NeedItem(String item, List<String> alternatives, Map<String, Object> mustHaveTag) {
        this.item = InventoryUtils.toItem(item);
        for (String s : alternatives) {
            this.alternatives.add(InventoryUtils.toItem(s));
        }
        this.mustHaveTag = mustHaveTag;
    }
}
