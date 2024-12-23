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
        this.setImmobile(true);
    }

    @Override
    public void dropItem() {
        // no-do
    }

    @Override
    public void kill() {
        // no-do
    }
}
