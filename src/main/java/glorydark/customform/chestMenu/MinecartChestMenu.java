package glorydark.customform.chestMenu;

import cn.nukkit.Player;
import cn.nukkit.block.BlockAir;
import cn.nukkit.block.BlockGlassStained;
import cn.nukkit.entity.item.EntityMinecartChest;
import cn.nukkit.item.Item;
import cn.nukkit.nbt.tag.ListTag;
import cn.nukkit.potion.Effect;
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
        int base = (page-1)*18;
        for(int i=base; i<18*page; i++){
            ChestMenuComponent component = chestMenuComponents.getOrDefault(i, null);
            int relativeIndex = i - base;
            if(component != null) {
                items.put(relativeIndex, component.getItem());
            }else{
                items.put(relativeIndex, new BlockAir().toItem());
            }
        }
        if(page > 1){
            items.put(18, new BlockGlassStained(0).toItem());
        }
        Item info = new BlockGlassStained(4).toItem();
        info.setCustomName("Page: §6§l"+page +"§f / "+getMaxPage());
        items.put(22, info);
        if(page < this.getMaxPage()){
            items.put(26, new BlockGlassStained(4).toItem());
        }
        return items;
    }

    public void show(Player player, int page){
        EntityMinecartChest chest = new EntityMinecartChest(player.getChunk(), EntityMinecartChest.getDefaultNBT(player.getPosition()));
        chest.namedTag.putBoolean("custom_form_entity", true);
        chest.namedTag.putInt("page", 1);
        chest.namedTag.putList(new ListTag("Items"));
        chest.namedTag.putByte("Slot", 27);
        chest.namedTag.putBoolean("Invulnerable", true);
        chest.namedTag.putBoolean("CustomDisplayTile", false);
        chest.setNameTag(this.title);
        chest.getInventory().setContents(getItems(page));
        chest.setNameTagVisible(false);
        chest.setNameTagAlwaysVisible(false);
        chest.setImmobile(true);
        chest.spawnTo(player);
        chest.addEffect(Effect.getEffect(Effect.INVISIBILITY).setDuration(999999).setVisible(false));
        player.addWindow(chest.getInventory());
        ChestMenuMain.mineCartChests.put(player, new ChestMenuMain.PlayerMinecartChestTempData(chest, this));
    }

    public void addComponent(int slot, ChestMenuComponent component){
        chestMenuComponents.put(slot, component);
    }

    public int getMaxPage(){
        int maxIndex = 0;
        for(int i: this.getChestMenuComponents().keySet()){
            if(i > maxIndex){
                maxIndex = i;
            }
        }
        int maxPage = maxIndex / 17;
        if(maxPage < 1){
            maxPage = 1;
        }else{
            if(maxIndex % 17 > 0){
                maxPage++;
            }
        }
        return maxPage;
    }
}
