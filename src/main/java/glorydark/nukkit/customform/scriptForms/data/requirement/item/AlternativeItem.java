package glorydark.nukkit.customform.scriptForms.data.requirement.item;

import cn.nukkit.item.Item;
import glorydark.nukkit.customform.utils.InventoryUtils;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Data
public class AlternativeItem {

    private Item item;

    private Item finalComparedItem = null;

    private List<Item> hasItems = new ArrayList<>();

    private boolean checkTag = true;

    private boolean checkCustomName = false;

    private boolean checkDamage = true;

    private Map<String, Object> mustHaveTag;

    public AlternativeItem(String item, Map<String, Object> mustHaveTag) {
        this.item = InventoryUtils.toItem(item);
        this.mustHaveTag = mustHaveTag;
    }
}
