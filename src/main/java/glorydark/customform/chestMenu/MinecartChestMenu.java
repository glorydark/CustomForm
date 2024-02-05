package glorydark.customform.chestMenu;

import cn.nukkit.Player;
import cn.nukkit.block.BlockAir;
import cn.nukkit.block.BlockGlassStained;
import cn.nukkit.block.BlockWool;
import cn.nukkit.entity.item.EntityMinecartChest;
import cn.nukkit.item.Item;
import cn.nukkit.item.ItemBookEnchanted;
import cn.nukkit.nbt.tag.ListTag;
import cn.nukkit.utils.DyeColor;
import glorydark.customform.CustomFormMain;
import glorydark.customform.entity.FormEntityMinecartChest;
import lombok.Data;

import java.util.HashMap;
import java.util.Map;

@Data
public class MinecartChestMenu {

    private String identifier;

    private String title;

    private HashMap<Integer, ChestMenuComponent> chestMenuComponents = new HashMap<>();

    private HashMap<Integer, ChestMenuComponent> chestMenuPEComponents = new HashMap<>();

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
        if (isPC(player)) {
            Map<Integer, Item> items = new HashMap<>();
            int base = (page - 1) * 18;
            for (int i = base; i < 18 * page; i++) {
                ChestMenuComponent component = chestMenuComponents.getOrDefault(i, null);
                int relativeIndex = i - base;
                if (component != null) {
                    items.put(relativeIndex, component.getItem());
                } else {
                    items.put(relativeIndex, new BlockAir().toItem());
                }
            }
            if (page > 1) {
                Item previousPageButton = new BlockGlassStained(4).toItem();
                previousPageButton.setCustomName(CustomFormMain.language.translateString(player, "item_previous_page_name"));
                items.put(18, previousPageButton);
            }
            Item info = new BlockGlassStained(4).toItem();
            info.setCustomName(CustomFormMain.language.translateString(player, "item_info_name", page, getMaxPage()));
            items.put(22, info);
            if (page < this.getMaxPage()) {
                Item nextPageButton = new BlockGlassStained(4).toItem();
                nextPageButton.setCustomName(CustomFormMain.language.translateString(player, "item_next_page_name"));
                items.put(26, nextPageButton);
            }
            return items;
        } else {
            Map<Integer, Item> items = new HashMap<>();
            int base = (page - 1) * 23;
            for (int i = base; i < 23 * page; i++) {
                ChestMenuComponent component = chestMenuPEComponents.getOrDefault(i, null);
                int relativeIndex = i - base;
                if (component != null) {
                    items.put(relativeIndex, component.getItem());
                } else {
                    items.put(relativeIndex, new BlockAir().toItem());
                }
            }
            if (page > 1) {
                Item previousPageButton = new BlockGlassStained(4).toItem();
                previousPageButton.setCustomName(CustomFormMain.language.translateString(player, "item_previous_page_name"));
                items.put(24, previousPageButton);
            }
            Item info = new BlockGlassStained(4).toItem();
            info.setCustomName(CustomFormMain.language.translateString(player, "item_info_name", page, getMaxPage()));
            items.put(25, info);
            if (page < this.getMaxPage()) {
                Item nextPageButton = new BlockGlassStained(4).toItem();
                nextPageButton.setCustomName(CustomFormMain.language.translateString(player, "item_next_page_name"));
                items.put(26, nextPageButton);
            }
            return items;
        }
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
        chest.setImmobile(true);
        chest.spawnTo(player);
        player.addWindow(chest.getInventory());
        ChestMenuMain.mineCartChests.put(player, new ChestMenuMain.PlayerMinecartChestTempData(chest, this));
    }

    public Map<Integer, Item> getDoubleCheckItem(Player player, int checkComponentIndex) {
        if (isPC(player)) {
            Map<Integer, Item> items = new HashMap<>();
            Item cancelButton = new BlockWool(DyeColor.RED).toItem();
            cancelButton.setCustomName(CustomFormMain.language.translateString(player, "item_selected_cancel"));
            items.put(11, cancelButton);

            Item ConfirmButton = new BlockWool(DyeColor.GREEN).toItem();
            ConfirmButton.setCustomName(CustomFormMain.language.translateString(player, "item_selected_confirm"));
            items.put(15, ConfirmButton);

            Item selectedInfo = new ItemBookEnchanted();
            selectedInfo.setCustomName(CustomFormMain.language.translateString(player, "item_selected_info", chestMenuComponents.get(checkComponentIndex).getName()));
            items.put(22, selectedInfo);
            return items;
        } else {
            Map<Integer, Item> items = new HashMap<>();
            Item cancelButton = new BlockWool(DyeColor.RED).toItem();
            cancelButton.setCustomName(CustomFormMain.language.translateString(player, "item_selected_cancel"));
            items.put(24, cancelButton);

            Item ConfirmButton = new BlockWool(DyeColor.GREEN).toItem();
            ConfirmButton.setCustomName(CustomFormMain.language.translateString(player, "item_selected_confirm"));
            items.put(25, ConfirmButton);

            Item selectedInfo = new ItemBookEnchanted();
            selectedInfo.setCustomName(CustomFormMain.language.translateString(player, "item_selected_info", chestMenuComponents.get(checkComponentIndex).getName()));
            items.put(26, selectedInfo);
            return items;
        }
    }

    public void addComponent(int slot, ChestMenuComponent component) {
        chestMenuComponents.put(slot, component);
    }

    public void addPEComponent(int slot, ChestMenuComponent component) {
        chestMenuPEComponents.put(slot, component);
    }

    public int getMaxPage() {
        int maxIndex = 0;
        for (int i : this.getChestMenuComponents().keySet()) {
            if (i > maxIndex) {
                maxIndex = i;
            }
        }
        int maxPage = maxIndex / 17;
        if (maxPage < 1) {
            maxPage = 1;
        } else {
            if (maxIndex % 17 > 0) {
                maxPage++;
            }
        }
        return maxPage;
    }
}
