package glorydark.nukkit.customform.scriptForms.data.requirement;

import glorydark.nukkit.customform.CustomFormMain;
import glorydark.nukkit.customform.scriptForms.data.requirement.custom.RequirementData;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * @author glorydark
 *
 * Used for custom requirement type registry.
 * This allows more players with no programming basics to design form freely,
 * while saves developers' efforts in working for greater customization.
 *
 */
public class RequirementTypeRegistry {

    protected static Map<String, Function<Map<String, Object>, RequirementData>> customRequirementTypes = new LinkedHashMap<>();

    public static void registerCustomRequirementType(String identifier, Function<Map<String, Object>, RequirementData> function) {
        customRequirementTypes.put(identifier, function);
    }

    /**
     * To use this method, you should add your logics into onLoad() in your main class.
     * @param type requirement type id, identified in "custom_type" in the map returned.
     * @param map the whole config section of a requirement
     * @return a new RequirementData object for auto-parsing.
     */
    public static RequirementData parseCustomRequirementType(String type, Map<String, Object> map) {
        if (customRequirementTypes.containsKey(type)) {
            Function<Map<String, Object>, RequirementData> find = customRequirementTypes.get(type);
            try {
                if (find != null) {
                    return find.apply(map);
                }
            } catch (Throwable t) {
                CustomFormMain.plugin.getLogger().info("识别自定义条件类型失败。数据类型: " + type + "，数据内容: " + map.toString());
                t.printStackTrace();
            }
        }
        return null;
    }
}
