package glorydark.nukkit.customform.hopperform;

import cn.nukkit.item.Item;
import glorydark.nukkit.customform.CustomFormMain;
import glorydark.nukkit.customform.factory.FormCreator;
import glorydark.nukkit.customform.scriptForms.data.requirement.Requirements;
import glorydark.nukkit.customform.utils.InventoryUtils;
import lombok.Data;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * @author glorydark
 */
@Data
public class HopperFormData {

    private final String title;

    private final Map<Integer, HopperFormItemInfo> itemInfos;

    public HopperFormData(String title, Map<Integer, HopperFormItemInfo> itemInfos) {
        this.title = title;
        this.itemInfos = itemInfos;
    }

    public static HopperFormData parse(Map<String, Object> map) {
        Map<Integer, HopperFormItemInfo> itemInfos = new LinkedHashMap<>();
        for (Map<String, Object> components : (List<Map<String, Object>>) map.getOrDefault("components", new ArrayList<Map<String, Object>>())) {
            int slot = (int) components.getOrDefault("slot", -1);
            if (slot == -1) {
                CustomFormMain.plugin.getLogger().warning("Found an suspicious slot in file, details: " + map);
                continue;
            }
            Item item = InventoryUtils.toItem((String) components.getOrDefault("item", ""));
            if (item == null) {
                item = Item.AIR_ITEM;
            }
            Object o = components.getOrDefault("description", "");
            List<String> descriptions = new ArrayList<>();
            if (o instanceof List) {
                List<String> o1 = (List<String>) o;
                descriptions.addAll(o1);
            } else {
                descriptions.add(o.toString());
            }

            List<Requirements> requirements = new ArrayList<>();
            if (components.containsKey("requirements")) {
                Map<String, Object> requirementData = (Map<String, Object>) components.get("requirements");
                for (List<Map<String, Object>> object : (List<List<Map<String, Object>>>) requirementData.get("data")) {
                    requirements.add(FormCreator.buildRequirements(object, (Boolean) requirementData.getOrDefault("chargeable", true)));
                }

            }
            itemInfos.put(slot, new HopperFormItemInfo(
                    (String) components.getOrDefault("name", ""),
                    descriptions.toArray(new String[0]),
                    item,
                    requirements,
                    (List<String>) components.getOrDefault("success_commands", new ArrayList<>()),
                    (List<String>) components.getOrDefault("success_messages", new ArrayList<>()),
                    (List<String>) components.getOrDefault("fail_commands", new ArrayList<>()),
                    (List<String>) components.getOrDefault("fail_messages", new ArrayList<>()),
                    (Boolean) components.getOrDefault("close_form", false)
            ));
        }
        return new HopperFormData((String) map.getOrDefault("title", ""), itemInfos);
    }
}
