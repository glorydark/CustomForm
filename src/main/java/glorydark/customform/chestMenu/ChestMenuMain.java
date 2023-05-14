package glorydark.customform.chestMenu;

import cn.nukkit.Player;
import cn.nukkit.entity.item.EntityMinecartChest;
import lombok.Data;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ChestMenuMain {

    public static HashMap<String, MinecartChestMenu> chestMenus = new HashMap<>();

    public static HashMap<Player, PlayerMinecartChestTempData> mineCartChests = new HashMap<>();

    public static void closeDoubleChestInventory(Player player){
        if(mineCartChests.containsKey(player)){
            player.sendMessage("close");
            EntityMinecartChest chest = mineCartChests.get(player).getEntityMinecartChest();
            chest.getInventory().clearAll();
            chest.close();
            mineCartChests.remove(player);
        }
    }

    public static void showMinecartChestMenu(Player player, String identifier){
        MinecartChestMenu menu = chestMenus.get(identifier);
        if(menu != null){
            menu.show(player, 1);

        }
    }

    public static boolean registerMinecartChestMenu(String identifier, Map<String, Object> config){
        if(!chestMenus.containsKey(identifier)){
            MinecartChestMenu menu = new MinecartChestMenu(identifier, (String) config.get("title"));
            List<Map<String, Object>> components = (List<Map<String, Object>>) config.getOrDefault("components", new ArrayList<>());
            for(Map<String, Object> component: components){
                ChestMenuComponent chestMenuComponent = new ChestMenuComponent((String) component.get("name"), (String) component.get("descriptions"), (String) component.get("item"), (Boolean) component.get("isEnchanted"));
                menu.addComponent((Integer) component.get("slot"), chestMenuComponent);
                chestMenuComponent.setFailedMessages((List<String>) component.getOrDefault("failed_messages", new ArrayList<>()));
                chestMenuComponent.setFailedCommands((List<String>) component.getOrDefault("failed_commands", new ArrayList<>()));
                chestMenuComponent.setSuccessMessages((List<String>) component.getOrDefault("success_messages", new ArrayList<>()));
                chestMenuComponent.setSuccessCommands((List<String>) component.getOrDefault("success_commands", new ArrayList<>()));
            }
            chestMenus.put(identifier, menu);
            return true;
        }
        return false;
    }

    @Data
    public static class PlayerMinecartChestTempData {
        private EntityMinecartChest entityMinecartChest;
        private MinecartChestMenu menu;

        public PlayerMinecartChestTempData(EntityMinecartChest entityMinecartChest, MinecartChestMenu menu){
            this.entityMinecartChest = entityMinecartChest;
            this.menu = menu;
        }
    }
}
