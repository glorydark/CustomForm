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

    private List<AlternativeItem> alternatives = new ArrayList<>();

    private Item finalComparedItem = null;

    private List<Item> hasItems = new ArrayList<>();

    private boolean checkTag = true;

    private boolean checkCustomName = false;

    private boolean checkDamage = true;

    private Map<String, Object> mustHaveTag;

    public NeedItem(String item, List<Map<String, Object>> alternatives, Map<String, Object> mustHaveTag) {
        this.item = InventoryUtils.toItem(item);
        for (Map<String, Object> map : alternatives) {
            this.alternatives.add(new AlternativeItem((String) map.get("item"), (Map<String, Object>) map.getOrDefault("must_have_tag", new LinkedHashMap<>())));
        }
        this.mustHaveTag = mustHaveTag;
    }
}
