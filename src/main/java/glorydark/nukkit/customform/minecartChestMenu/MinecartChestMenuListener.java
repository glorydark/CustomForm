package glorydark.nukkit.customform.minecartChestMenu;

import cn.nukkit.Player;
import cn.nukkit.entity.Entity;
import cn.nukkit.entity.item.EntityMinecartChest;
import cn.nukkit.event.EventHandler;
import cn.nukkit.event.Listener;
import cn.nukkit.event.entity.EntityDamageByBlockEvent;
import cn.nukkit.event.entity.EntityDamageByChildEntityEvent;
import cn.nukkit.event.entity.EntityDamageByEntityEvent;
import cn.nukkit.event.entity.EntityDamageEvent;
import cn.nukkit.event.inventory.*;
import cn.nukkit.event.player.PlayerInteractEntityEvent;
import cn.nukkit.event.player.PlayerQuitEvent;
import cn.nukkit.inventory.Inventory;
import cn.nukkit.inventory.MinecartChestInventory;
import glorydark.nukkit.customform.CustomFormMain;
import glorydark.nukkit.customform.chestForm.ChestFormMain;
import glorydark.nukkit.customform.hopperform.HopperFormMain;

public class MinecartChestMenuListener implements Listener {

    @EventHandler
    public void InventoryPickupItemEvent(InventoryPickupItemEvent event) {
        if (isCustomFormInventory(event.getInventory())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void InventoryMoveItemEvent(InventoryMoveItemEvent event) {
        if (isCustomFormInventory(event.getInventory())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void InventoryTransactionEvent(InventoryTransactionEvent event) {
        for (Inventory inventory : event.getTransaction().getInventories()) {
            if (isCustomFormInventory(inventory)) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void InventoryClickEvent(InventoryClickEvent event) {
        Player player = event.getPlayer();
        if (isCustomFormInventory(event.getInventory())) {
            event.setCancelled(true);
            if (event.getInventory() instanceof MinecartChestInventory) {
                EntityMinecartChest entity = ((MinecartChestInventory) event.getInventory()).getHolder();
                MinecartChestMenuMain.PlayerMinecartChestTempData data = MinecartChestMenuMain.mineCartChests.get(player);
                if (data == null) {
                    return;
                }
                int page = getPage(entity);
                boolean pc = MinecartChestMenu.isPC(player);
                if (pc) {
                    if (data.getDoubleCheckComponentId() == -1) {
                        switch (event.getSlot()) {
                            case MinecartChestMenu.SLOT_ITEM_PREV_PAGE_PC:
                                if (page > 1) {
                                    setPage(player, entity, page - 1);
                                }
                                break;
                            case MinecartChestMenu.SLOT_ITEM_NEXT_PAGE_PC:
                                if (page < data.getMenu().getMaxPage(true)) {
                                    setPage(player, entity, page + 1);
                                }
                                break;
                            default:
                                if (event.getSlot() <= MinecartChestMenu.MAX_ITEM_SLOTS_PC) {
                                    if (CustomFormMain.enableDoubleCheckMenu) {
                                        int clickId = event.getSlot() + (page - 1) * MinecartChestMenu.MAX_ITEM_SLOTS_PC;
                                        showConfirmPage(player, entity, clickId);
                                        event.setCancelled(true);
                                        return;
                                    } else {
                                        if (data.getLastClickId() == -1) {
                                            data.setLastClickId(event.getSlot());
                                            event.setCancelled(true);
                                            return;
                                        }
                                        if (data.getLastClickId() != event.getSlot()) {
                                            data.setLastClickId(event.getSlot());
                                            event.setCancelled(true);
                                            return;
                                        }
                                    }
                                    int clickId = event.getSlot() + (page - 1) * MinecartChestMenu.MAX_ITEM_SLOTS_PC;
                                    MinecartChestMenuComponent component = data.getMenu().getChestMenuPCComponents().get(clickId);
                                    component.execute(player);
                                    MinecartChestMenuMain.closeDoubleChestInventory(player);
                                }
                                break;
                        }
                    } else {
                        switch (event.getSlot()) {
                            case MinecartChestMenu.SLOT_ITEM_CANCEL_PC:
                                setPage(player, entity, page);
                                data.setDoubleCheckComponentId(-1);
                                break;
                            case MinecartChestMenu.SLOT_ITEM_CONFIRM_PC:
                                int clickId = data.getDoubleCheckComponentId();
                                MinecartChestMenuComponent component = data.getMenu().getChestMenuPCComponents().get(clickId);
                                component.execute(player);
                                MinecartChestMenuMain.closeDoubleChestInventory(player);
                                break;
                        }
                    }
                } else {
                    if (data.getDoubleCheckComponentId() == -1) {
                        switch (event.getSlot()) {
                            case MinecartChestMenu.SLOT_ITEM_PREV_PAGE_PE:
                                if (page > 1) {
                                    setPage(player, entity, page - 1);
                                }
                                break;
                            case MinecartChestMenu.SLOT_ITEM_NEXT_PAGE_PE:
                                if (page < data.getMenu().getMaxPage(false)) {
                                    setPage(player, entity, page + 1);
                                }
                                break;
                            default:
                                if (event.getSlot() <= MinecartChestMenu.MAX_ITEM_SLOTS_PE) {
                                    if (CustomFormMain.enableDoubleCheckMenu) {
                                        int clickId = event.getSlot() + (page - 1) * MinecartChestMenu.MAX_ITEM_SLOTS_PE;
                                        showConfirmPage(player, entity, clickId);
                                        event.setCancelled(true);
                                        return;
                                    } else {
                                        if (data.getLastClickId() == -1) {
                                            data.setLastClickId(event.getSlot());
                                            event.setCancelled(true);
                                            return;
                                        }
                                        if (data.getLastClickId() != event.getSlot()) {
                                            data.setLastClickId(event.getSlot());
                                            event.setCancelled(true);
                                            return;
                                        }
                                    }
                                    int clickId = event.getSlot() + (page - 1) * MinecartChestMenu.MAX_ITEM_SLOTS_PE;
                                    MinecartChestMenuComponent component = data.getMenu().getChestMenuPEComponents().get(clickId);
                                    component.execute(player);
                                    MinecartChestMenuMain.closeDoubleChestInventory(player);
                                }
                                break;
                        }
                    } else {
                        switch (event.getSlot()) {
                            case MinecartChestMenu.SLOT_ITEM_CANCEL_PE:
                                setPage(player, entity, page);
                                data.setDoubleCheckComponentId(-1);
                                break;
                            case MinecartChestMenu.SLOT_ITEM_CONFIRM_PE:
                                int clickId = data.getDoubleCheckComponentId();
                                MinecartChestMenuComponent component = data.getMenu().getChestMenuPEComponents().get(clickId);
                                component.execute(player);
                                MinecartChestMenuMain.closeDoubleChestInventory(player);
                                break;
                        }
                    }
                }
            }
        }
    }

    @EventHandler
    public void PlayerQuitEvent(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        if (MinecartChestMenuMain.mineCartChests.containsKey(player)) {
            MinecartChestMenuMain.closeDoubleChestInventory(player);
        }
        ChestFormMain.players.remove(player);
        HopperFormMain.players.remove(player);
    }

    @EventHandler
    public void InventoryCloseEvent(InventoryCloseEvent event) {
        if (MinecartChestMenuMain.mineCartChests.containsKey(event.getPlayer())) {
            MinecartChestMenuMain.closeDoubleChestInventory(event.getPlayer());
        }
    }

    @EventHandler
    public void EntityDamageEvent(EntityDamageEvent event) {
        if (isCustomFormEntity(event.getEntity())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void EntityDamageByEntityEvent(EntityDamageByEntityEvent event) {
        if (isCustomFormEntity(event.getEntity())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void EntityDamageByBlockEvent(EntityDamageByBlockEvent event) {
        if (isCustomFormEntity(event.getEntity())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void EntityDamageByChildEntityEvent(EntityDamageByChildEntityEvent event) {
        if (isCustomFormEntity(event.getEntity())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void PlayerInteractEntityEvent(PlayerInteractEntityEvent event) {
        if (isCustomFormEntity(event.getEntity())) {
            event.setCancelled(true);
        }
    }

    public boolean isCustomFormInventory(Inventory inventory) {
        if (inventory instanceof MinecartChestInventory) {
            EntityMinecartChest e = ((MinecartChestInventory) inventory).getHolder();
            return e.namedTag.contains("custom_form_entity");
        }
        return false;
    }

    public boolean isCustomFormEntity(Entity entity) {
        if (entity instanceof EntityMinecartChest) {
            EntityMinecartChest e = (EntityMinecartChest) entity;
            return e.namedTag.contains("custom_form_entity");
        }
        return false;
    }

    public int getPage(Entity entity) {
        if (entity.namedTag != null) {
            if (entity.namedTag.contains("page")) {
                return entity.namedTag.getInt("page");
            }
        }
        return 0;
    }

    public void showConfirmPage(Player player, Entity entity, int clickId) {
        if (entity instanceof EntityMinecartChest) {
            if (entity.namedTag != null) {
                if (entity.namedTag.contains("page")) {
                    MinecartChestMenuMain.PlayerMinecartChestTempData tempData = MinecartChestMenuMain.mineCartChests.get(player);
                    tempData.setDoubleCheckComponentId(clickId);
                    ((EntityMinecartChest) entity).getInventory().setContents(tempData.getMenu().getDoubleCheckItem(player, clickId));
                }
            }
        }
    }

    public void setPage(Player player, Entity entity, int page) {
        if (entity instanceof EntityMinecartChest) {
            if (entity.namedTag != null) {
                if (entity.namedTag.contains("page")) {
                    entity.namedTag.putInt("page", page);
                    MinecartChestMenuMain.PlayerMinecartChestTempData tempData = MinecartChestMenuMain.mineCartChests.get(player);
                    ((EntityMinecartChest) entity).getInventory().setContents(tempData.getMenu().getItems(player, page));
                }
            }
        }
    }
}
