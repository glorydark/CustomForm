package glorydark.customform.chestMenu;

import cn.nukkit.Player;
import cn.nukkit.entity.Entity;
import cn.nukkit.entity.item.EntityMinecartChest;
import cn.nukkit.event.EventHandler;
import cn.nukkit.event.Listener;
import cn.nukkit.event.entity.EntityDamageByBlockEvent;
import cn.nukkit.event.entity.EntityDamageByChildEntityEvent;
import cn.nukkit.event.entity.EntityDamageByEntityEvent;
import cn.nukkit.event.entity.EntityDamageEvent;
import cn.nukkit.event.inventory.InventoryClickEvent;
import cn.nukkit.event.inventory.InventoryCloseEvent;
import cn.nukkit.event.inventory.InventoryMoveItemEvent;
import cn.nukkit.event.inventory.InventoryPickupItemEvent;
import cn.nukkit.event.player.PlayerInteractEntityEvent;
import cn.nukkit.event.player.PlayerQuitEvent;
import cn.nukkit.inventory.Inventory;
import cn.nukkit.inventory.MinecartChestInventory;

public class ChestMenuListener implements Listener {

    @EventHandler
    public void InventoryPickupItemEvent(InventoryPickupItemEvent event){
        if(isCustomFormInventory(event.getInventory())){
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void InventoryClickEvent(InventoryClickEvent event){
        if(isCustomFormInventory(event.getInventory())){
            for (Player viewer : event.getViewers()) {
                viewer.sendMessage(String.valueOf(event.getSlot()));
                if(event.getInventory() instanceof MinecartChestInventory){
                    EntityMinecartChest entity = ((MinecartChestInventory) event.getInventory()).getHolder();
                    int page = getPage(entity);
                    switch (event.getSlot()){
                        case 18:
                            if(page > 1){
                                setPage(event.getPlayer(), entity, page-1);
                            }
                            break;
                        case 26:
                            setPage(event.getPlayer(), entity, page+1);
                            break;
                        default:
                            if(event.getSlot() < 18) {
                                int clickId = event.getSlot() + (getPage(entity) - 1) * 18;
                                ChestMenuMain.PlayerMinecartChestTempData data = ChestMenuMain.mineCartChests.get(event.getPlayer());
                                ChestMenuComponent component = data.getMenu().getChestMenuComponents().get(clickId);
                            }
                            break;
                    }
                }
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void InventoryMoveItemEvent(InventoryMoveItemEvent event){
        if(isCustomFormInventory(event.getInventory())){
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void InventoryCloseEvent(InventoryCloseEvent event){
        if(ChestMenuMain.mineCartChests.containsKey(event.getPlayer())){
            EntityMinecartChest chest = ChestMenuMain.mineCartChests.get(event.getPlayer()).getEntityMinecartChest();
            if(event.getInventory().getHolder().equals(chest)){
                ChestMenuMain.closeDoubleChestInventory(event.getPlayer());
            }
        }
    }

    @EventHandler
    public void PlayerQuitEvent(PlayerQuitEvent event){
        if(ChestMenuMain.mineCartChests.containsKey(event.getPlayer())){
            ChestMenuMain.closeDoubleChestInventory(event.getPlayer());
        }
    }

    @EventHandler
    public void EntityDamageEvent(EntityDamageEvent event){
        if(isCustomFormEntity(event.getEntity())){
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void EntityDamageByEntityEvent(EntityDamageByEntityEvent event){
        if(isCustomFormEntity(event.getEntity())){
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void EntityDamageByBlockEvent(EntityDamageByBlockEvent event){
        if(isCustomFormEntity(event.getEntity())){
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void EntityDamageByChildEntityEvent(EntityDamageByChildEntityEvent event){
        if(isCustomFormEntity(event.getEntity())){
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void PlayerInteractEntityEvent(PlayerInteractEntityEvent event){
        if(isCustomFormEntity(event.getEntity())){
            event.setCancelled(true);
        }
    }

    public boolean isCustomFormInventory(Inventory inventory){
        if(inventory instanceof MinecartChestInventory) {
            EntityMinecartChest e = ((MinecartChestInventory) inventory).getHolder();
            return e.namedTag.contains("custom_form_entity");
        }
        return false;
    }

    public boolean isCustomFormEntity(Entity entity){
        if(entity instanceof EntityMinecartChest) {
            EntityMinecartChest e = (EntityMinecartChest) entity;
            return e.namedTag.contains("custom_form_entity");
        }
        return false;
    }

    public int getPage(Entity entity){
        if(entity.namedTag != null){
            if(entity.namedTag.contains("page")){
                return entity.namedTag.getInt("page");
            }
        }
        return 0;
    }

    public void setPage(Player player, Entity entity, int page){
        if(entity.namedTag != null){
            if(entity.namedTag.contains("page")){
                entity.namedTag.putInt("page", page);
                if(entity instanceof EntityMinecartChest) {
                    ChestMenuMain.PlayerMinecartChestTempData tempData = ChestMenuMain.mineCartChests.get(player);
                    ((EntityMinecartChest) entity).getInventory().setContents(tempData.getMenu().getItems(page));
                }
            }
        }
    }
}
