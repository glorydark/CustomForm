package glorydark.customform.chestMenu;

import cn.nukkit.Player;
import cn.nukkit.block.BlockAir;
import cn.nukkit.block.BlockGlassStained;
import cn.nukkit.entity.item.EntityMinecartChest;
import cn.nukkit.item.Item;
import cn.nukkit.nbt.tag.ListTag;
import lombok.Data;

import java.util.HashMap;
import java.util.Map;

@Data
public class MinecartChestMenu {

    private String identifier;

    private String title;

    private HashMap<Integer, ChestMenuComponent> chestMenuComponents = new HashMap<>();

    public MinecartChestMenu(String identifier, String title){
        this.identifier = identifier;
        this.title = title;
    }

    public Map<Integer, Item> getItems(int page){
        Map<Integer, Item> items = new HashMap<>();
        for(int i=(page-1)*18; i<18*(page-1); i++){
            ChestMenuComponent component = chestMenuComponents.getOrDefault(i, null);
            if(component != null) {
                items.put(i, component.getItem());
            }else{
                items.put(i, new BlockAir().toItem());
            }
        }
        if(page > 1){
            items.put(18, new BlockGlassStained(0).toItem());
        }
        Item info = new BlockGlassStained(4).toItem();
        info.setCustomName("当前是第"+page+"页");
        items.put(22, info);
        items.put(26, new BlockGlassStained(4).toItem());
        return items;
    }

    public void show(Player player, int page){
        EntityMinecartChest chest = new EntityMinecartChest(player.getChunk(), EntityMinecartChest.getDefaultNBT(player.getPosition()));
        chest.setNameTag(this.title);
        chest.namedTag.putBoolean("custom_form_entity", true);
        chest.namedTag.putInt("page", 1);
        chest.namedTag.putList(new ListTag("Items"));
        chest.namedTag.putByte("Slot", 27);
        chest.namedTag.putBoolean("IsLotteryBox", true);
        chest.getInventory().setContents(getItems(page));
        chest.setNameTagVisible(false);
        chest.setNameTagAlwaysVisible(false);
        chest.setImmobile(true);
        chest.spawnTo(player);
        player.addWindow(chest.getInventory());
        ChestMenuMain.mineCartChests.put(player, new ChestMenuMain.PlayerMinecartChestTempData(chest, this));
    }

    public void addComponent(int slot, ChestMenuComponent component){
        chestMenuComponents.put(slot, component);
    }
}
