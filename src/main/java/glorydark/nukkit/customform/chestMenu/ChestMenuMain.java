package glorydark.nukkit.customform.chestMenu;

import cn.nukkit.Player;
import cn.nukkit.entity.item.EntityMinecartChest;
import glorydark.nukkit.customform.entity.FormEntityMinecartChest;
import glorydark.nukkit.customform.forms.FormCreator;
import glorydark.nukkit.customform.scriptForms.data.requirement.Requirements;
import lombok.Data;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ChestMenuMain {

    public static HashMap<String, MinecartChestMenu> chestMenus = new HashMap<>();

    public static HashMap<Player, PlayerMinecartChestTempData> mineCartChests = new HashMap<>();

    public static void closeDoubleChestInventory(Player player) {
        if (mineCartChests.containsKey(player)) {
            EntityMinecartChest chest = mineCartChests.get(player).getEntityMinecartChest();
            chest.kill();
            chest.close();
            mineCartChests.remove(player);
        }
    }

    public static void showMinecartChestMenu(Player player, String identifier) {
        MinecartChestMenu menu = chestMenus.get(identifier);
        if (menu != null) {
            menu.show(player, 1);
        }
    }

    public static boolean registerMinecartChestMenu(String identifier, Map<String, Object> config) {
        if (!chestMenus.containsKey(identifier)) {
            MinecartChestMenu menu = new MinecartChestMenu(identifier, (String) config.get("title"));
            List<Map<String, Object>> peComponents = (List<Map<String, Object>>) config.getOrDefault("pe_components", new ArrayList<>());
            for (Map<String, Object> component : peComponents) {
                ChestMenuComponent chestMenuComponent = new ChestMenuComponent((String) component.get("name"), (List<String>) component.get("descriptions"), (String) component.get("item"), (Boolean) component.get("isEnchanted"));
                chestMenuComponent.setFailedMessages((List<String>) component.getOrDefault("failed_messages", new ArrayList<>()));
                chestMenuComponent.setFailedCommands((List<String>) component.getOrDefault("failed_commands", new ArrayList<>()));
                chestMenuComponent.setSuccessMessages((List<String>) component.getOrDefault("messages", new ArrayList<>()));
                chestMenuComponent.setSuccessCommands((List<String>) component.getOrDefault("commands", new ArrayList<>()));
                if (component.containsKey("requirements")) {
                    List<Requirements> requirements = new ArrayList<>();
                    Map<String, Object> requirementData = (Map<String, Object>) component.get("requirements");
                    for (List<Map<String, Object>> object : (List<List<Map<String, Object>>>) requirementData.get("data")) {
                        requirements.add(FormCreator.buildRequirements(object, (Boolean) requirementData.getOrDefault("chargeable", true)));
                    }
                    chestMenuComponent.setRequirements(requirements);
                }
                menu.addComponent((Integer) component.get("slot"), chestMenuComponent);
            }
            List<Map<String, Object>> pcComponents = (List<Map<String, Object>>) config.getOrDefault("pc_components", new ArrayList<>());
            for (Map<String, Object> component : pcComponents) {
                ChestMenuComponent chestMenuComponent = new ChestMenuComponent((String) component.get("name"), (List<String>) component.get("descriptions"), (String) component.get("item"), (Boolean) component.get("isEnchanted"));
                chestMenuComponent.setFailedMessages((List<String>) component.getOrDefault("failed_messages", new ArrayList<>()));
                chestMenuComponent.setFailedCommands((List<String>) component.getOrDefault("failed_commands", new ArrayList<>()));
                chestMenuComponent.setSuccessMessages((List<String>) component.getOrDefault("messages", new ArrayList<>()));
                chestMenuComponent.setSuccessCommands((List<String>) component.getOrDefault("commands", new ArrayList<>()));
                if (component.containsKey("requirements")) {
                    List<Requirements> requirements = new ArrayList<>();
                    Map<String, Object> requirementData = (Map<String, Object>) component.get("requirements");
                    for (List<Map<String, Object>> object : (List<List<Map<String, Object>>>) requirementData.get("data")) {
                        requirements.add(FormCreator.buildRequirements(object, (Boolean) requirementData.getOrDefault("chargeable", true)));
                    }
                    chestMenuComponent.setRequirements(requirements);
                }
                menu.addPEComponent((Integer) component.get("slot"), chestMenuComponent);
            }
            chestMenus.put(identifier, menu);
            return true;
        }
        return false;
    }

    @Data
    public static class PlayerMinecartChestTempData {
        private FormEntityMinecartChest entityMinecartChest;
        private MinecartChestMenu menu;

        private int lastClickId = -1;

        private int doubleCheckComponentId = -1;

        public PlayerMinecartChestTempData(FormEntityMinecartChest entityMinecartChest, MinecartChestMenu menu) {
            this.entityMinecartChest = entityMinecartChest;
            this.menu = menu;
        }
    }
}
