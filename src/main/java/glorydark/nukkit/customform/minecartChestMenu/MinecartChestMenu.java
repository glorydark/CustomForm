package glorydark.nukkit.customform.minecartChestMenu;

import cn.nukkit.Player;
import cn.nukkit.Server;
import cn.nukkit.block.BlockAir;
import cn.nukkit.block.BlockGlassStained;
import cn.nukkit.block.BlockWool;
import cn.nukkit.entity.item.EntityMinecartChest;
import cn.nukkit.item.Item;
import cn.nukkit.item.ItemBookEnchanted;
import cn.nukkit.nbt.tag.ListTag;
import cn.nukkit.scheduler.Task;
import cn.nukkit.utils.DyeColor;
import glorydark.nukkit.customform.CustomFormMain;
import glorydark.nukkit.customform.entity.FormEntityMinecartChest;
import lombok.Data;

import java.util.HashMap;
import java.util.Map;

@Data
public class MinecartChestMenu {

    private String identifier;

    private String title;

    public static final int MAX_ITEM_SLOTS_PE = 23;
    public static final int MAX_ITEM_SLOTS_PC = 18;

    public static final int SLOT_ITEM_CANCEL_PC = 11;
    public static final int SLOT_ITEM_CONFIRM_PC = 15;
    public static final int SLOT_ITEM_INFO_PC = 22;
    public static final int SLOT_ITEM_CANCEL_PE = 7;
    public static final int SLOT_ITEM_CONFIRM_PE = 10;
    public static final int SLOT_ITEM_INFO_PE = 25;

    public static final int SLOT_ITEM_PREV_PAGE_PC = 18;
    public static final int SLOT_ITEM_CURRENT_PAGE_INFO_PC = 22;
    public static final int SLOT_ITEM_NEXT_PAGE_PC = 26;

    public static final int SLOT_ITEM_PREV_PAGE_PE = 24;
    public static final int SLOT_ITEM_CURRENT_PAGE_INFO_PE = 25;
    public static final int SLOT_ITEM_NEXT_PAGE_PE = 26;

    private boolean invalid = false;

    private HashMap<Integer, MinecartChestMenuComponent> chestMenuPCComponents = new HashMap<>();

    private HashMap<Integer, MinecartChestMenuComponent> chestMenuPEComponents = new HashMap<>();

    public MinecartChestMenu(String identifier, String title) {
        this.identifier = identifier;
        this.title = title;
    }

    public static boolean isPC(Player player) {
        int deviceOs = player.getLoginChainData().getDeviceOS();
        switch (deviceOs) {
            case 7:
            case 8:
                return true;
            default:
                return false;
        }
    }

    public Map<Integer, Item> getItems(Player player, int page) {
        boolean pc = isPC(player);
        Map<Integer, Item> items = new HashMap<>();
        int base;
        if (pc) {
            base = (page - 1) * MAX_ITEM_SLOTS_PE;
            for (int i = base; i < MAX_ITEM_SLOTS_PE * page; i++) {
                MinecartChestMenuComponent component = chestMenuPCComponents.getOrDefault(i, null);
                int relativeIndex = i - base;
                if (component != null) {
                    items.put(relativeIndex, component.getItem(player));
                } else {
                    items.put(relativeIndex, new BlockAir().toItem());
                }
            }
            if (page > 1) {
                Item previousPageButton = new BlockGlassStained(4).toItem();
                previousPageButton.setCustomName(CustomFormMain.language.translateString(player, "item_previous_page_name"));
                items.put(SLOT_ITEM_PREV_PAGE_PC, previousPageButton);
            }
            Item info = new BlockGlassStained(4).toItem();
            info.setCustomName(CustomFormMain.language.translateString(player, "item_info_name", page, getMaxPage(true)));
            items.put(SLOT_ITEM_CURRENT_PAGE_INFO_PC, info);
            if (page < this.getMaxPage(true)) {
                Item nextPageButton = new BlockGlassStained(4).toItem();
                nextPageButton.setCustomName(CustomFormMain.language.translateString(player, "item_next_page_name"));
                items.put(SLOT_ITEM_NEXT_PAGE_PC, nextPageButton);
            }
        } else {
            base = (page - 1) * MAX_ITEM_SLOTS_PC;
            for (int i = base; i < MAX_ITEM_SLOTS_PC * page; i++) {
                MinecartChestMenuComponent component = chestMenuPEComponents.getOrDefault(i, null);
                int relativeIndex = i - base;
                if (component != null) {
                    items.put(relativeIndex, component.getItem(player));
                } else {
                    items.put(relativeIndex, new BlockAir().toItem());
                }
            }
            if (page > 1) {
                Item previousPageButton = new BlockGlassStained(4).toItem();
                previousPageButton.setCustomName(CustomFormMain.language.translateString(player, "item_previous_page_name"));
                items.put(SLOT_ITEM_PREV_PAGE_PE, previousPageButton);
            }
            Item info = new BlockGlassStained(4).toItem();
            info.setCustomName(CustomFormMain.language.translateString(player, "item_info_name", page, getMaxPage(false)));
            items.put(SLOT_ITEM_CURRENT_PAGE_INFO_PE, info);
            if (page < this.getMaxPage(false)) {
                Item nextPageButton = new BlockGlassStained(4).toItem();
                nextPageButton.setCustomName(CustomFormMain.language.translateString(player, "item_next_page_name"));
                items.put(SLOT_ITEM_NEXT_PAGE_PE, nextPageButton);
            }
        }
        return items;
    }

    public void show(Player player, int page) {
        FormEntityMinecartChest chest = new FormEntityMinecartChest(player.getChunk(), EntityMinecartChest.getDefaultNBT(player.getPosition()));
        chest.namedTag.putBoolean("custom_form_entity", true);
        chest.namedTag.putInt("page", 1);
        chest.namedTag.putList(new ListTag("Items"));
        chest.namedTag.putByte("Slot", 27);
        chest.namedTag.putBoolean("Invulnerable", true);
        chest.namedTag.putBoolean("CustomDisplayTile", false);
        chest.setNameTag(this.title);
        chest.getInventory().setContents(getItems(player, page));
        chest.setNameTagVisible(false);
        chest.setNameTagAlwaysVisible(false);
        chest.spawnTo(player);
        MinecartChestMenuMain.mineCartChests.put(player, new MinecartChestMenuMain.PlayerMinecartChestTempData(chest, this));
        Server.getInstance().getScheduler().scheduleDelayedTask(CustomFormMain.plugin, new Task() {
            @Override
            public void onRun(int i) {
                player.addWindow(chest.getInventory());
            }
        }, 10);
    }

    public Map<Integer, Item> getDoubleCheckItem(Player player, int checkComponentIndex) {
        if (isPC(player)) {
            Map<Integer, Item> items = new HashMap<>();
            Item cancelButton = new BlockWool(DyeColor.RED).toItem();
            cancelButton.setCustomName(CustomFormMain.language.translateString(player, "item_selected_cancel"));
            items.put(SLOT_ITEM_CANCEL_PC, cancelButton);

            Item ConfirmButton = new BlockWool(DyeColor.GREEN).toItem();
            ConfirmButton.setCustomName(CustomFormMain.language.translateString(player, "item_selected_confirm"));
            items.put(SLOT_ITEM_CONFIRM_PC, ConfirmButton);

            Item selectedInfo = new ItemBookEnchanted();
            selectedInfo.setCustomName(CustomFormMain.language.translateString(player, "item_selected_info", chestMenuPCComponents.get(checkComponentIndex).getName()));
            items.put(SLOT_ITEM_INFO_PC, selectedInfo);
            return items;
        } else {
            Map<Integer, Item> items = new HashMap<>();
            Item cancelButton = new BlockWool(DyeColor.RED).toItem();
            cancelButton.setCustomName(CustomFormMain.language.translateString(player, "item_selected_cancel"));
            items.put(SLOT_ITEM_CANCEL_PE, cancelButton);

            Item ConfirmButton = new BlockWool(DyeColor.GREEN).toItem();
            ConfirmButton.setCustomName(CustomFormMain.language.translateString(player, "item_selected_confirm"));
            items.put(SLOT_ITEM_CONFIRM_PE, ConfirmButton);

            Item selectedInfo = new ItemBookEnchanted();
            selectedInfo.setCustomName(CustomFormMain.language.translateString(player, "item_selected_info", chestMenuPEComponents.get(checkComponentIndex).getName()));
            items.put(SLOT_ITEM_INFO_PE, selectedInfo);
            return items;
        }
    }

    public void addPCComponent(int slot, MinecartChestMenuComponent component) {
        chestMenuPCComponents.put(slot, component);
    }

    public void addPEComponent(int slot, MinecartChestMenuComponent component) {
        chestMenuPEComponents.put(slot, component);
    }

    public int getMaxPage(boolean pc) {
        int maxIndex = 0;
        int maxPage;
        if (pc) {
            for (int i : this.getChestMenuPCComponents().keySet()) {
                if (i > maxIndex) {
                    maxIndex = i;
                }
            }
            maxPage = maxIndex / (MAX_ITEM_SLOTS_PE - 1);
            if (maxPage < 1) {
                maxPage = 1;
            } else {
                if (maxIndex % (MAX_ITEM_SLOTS_PE - 1) > 0) {
                    maxPage++;
                }
            }
        } else {
            for (int i : this.getChestMenuPEComponents().keySet()) {
                if (i > maxIndex) {
                    maxIndex = i;
                }
            }
            maxPage = maxIndex / (MAX_ITEM_SLOTS_PC - 1);
            if (maxPage < 1) {
                maxPage = 1;
            } else {
                if (maxIndex % (MAX_ITEM_SLOTS_PC - 1) > 0) {
                    maxPage++;
                }
            }
        }
        return maxPage;
    }
}
