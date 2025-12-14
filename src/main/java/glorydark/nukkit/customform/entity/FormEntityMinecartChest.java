package glorydark.nukkit.customform.entity;

import cn.nukkit.entity.item.EntityMinecartChest;
import cn.nukkit.level.format.FullChunk;
import cn.nukkit.nbt.tag.CompoundTag;

/**
 * @author glorydark
 */
public class FormEntityMinecartChest extends EntityMinecartChest {

    private boolean invalid = false;

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
        if (this.invalid) {
            super.kill();
            this.getInventory().clearAll();
        }
    }

    public void setInvalid(boolean invalid) {
        this.invalid = invalid;
    }

    public boolean isInvalid() {
        return invalid;
    }
}
