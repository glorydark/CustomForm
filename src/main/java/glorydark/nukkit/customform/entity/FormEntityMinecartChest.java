package glorydark.nukkit.customform.entity;

import cn.nukkit.entity.item.EntityMinecartChest;
import cn.nukkit.level.format.FullChunk;
import cn.nukkit.nbt.tag.CompoundTag;

/**
 * @author glorydark
 */
public class FormEntityMinecartChest extends EntityMinecartChest {

    public FormEntityMinecartChest(FullChunk chunk, CompoundTag nbt) {
        super(chunk, nbt);
    }

    @Override
    public void dropItem() {
        // no-do
    }
}
