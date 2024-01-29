package glorydark.customform.utils;

import cn.nukkit.Player;
import cn.nukkit.Server;
import cn.nukkit.math.BlockFace;
import cn.nukkit.math.Vector3f;
import cn.nukkit.network.protocol.CameraInstructionPacket;
import cn.nukkit.network.protocol.types.camera.CameraEase;
import cn.nukkit.network.protocol.types.camera.CameraPreset;
import cn.nukkit.network.protocol.types.camera.CameraSetInstruction;
import cn.nukkit.utils.CameraPresetManager;
import glorydark.customform.CustomFormMain;
import org.cloudburstmc.protocol.common.util.OptionalBoolean;

import java.util.HashSet;

/**
 * @author LT_Name
 */
public class CameraUtils {

    public static HashSet<Player> players = new HashSet<>();

    public static void sendFormOpen(Player player) {
        if (players.contains(player)) {
            return;
        }
        players.add(player);
        CameraPreset preset = CameraPresetManager.getPreset("minecraft:free");

        CameraSetInstruction setInstruction = new CameraSetInstruction();
        setInstruction.setPreset(preset);
        setInstruction.setEase(new CameraSetInstruction.EaseData(CameraEase.EASE_IN_OUT_CIRC, 1));
        Vector3f vector3f = player.getSide(player.getDirection(), 3).asVector3f();
        vector3f.setY(player.getFloorY() + 2f);
        setInstruction.setPos(vector3f);
        setInstruction.setFacing(player.getSide(offsetDirection(player), 1).asVector3f().setY(player.getFloorY() + 1.5f));

        CameraInstructionPacket pk = new CameraInstructionPacket();
        pk.setSetInstruction(setInstruction);
        player.dataPacket(pk);
    }

    public static void sendFormClose(Player player) {
        if (!players.contains(player)) {
            return;
        }
        CameraPreset preset = CameraPresetManager.getPreset("minecraft:free");

        CameraSetInstruction setInstruction = new CameraSetInstruction();
        setInstruction.setPreset(preset);
        setInstruction.setEase(new CameraSetInstruction.EaseData(CameraEase.EASE_IN_OUT_CIRC, 1));
        Vector3f vector3f = player.getSide(player.getDirection().getOpposite(), 3).asVector3f();
        vector3f.setY(player.getFloorY() + 1f);
        setInstruction.setPos(vector3f);
        setInstruction.setFacing(player.asVector3f().setY(player.getFloorY() + 1.5f));

        CameraInstructionPacket pk = new CameraInstructionPacket();
        pk.setSetInstruction(setInstruction);
        player.dataPacket(pk);

        Server.getInstance().getScheduler().scheduleDelayedTask(CustomFormMain.plugin, () -> {
            CameraInstructionPacket pk2 = new CameraInstructionPacket();
            pk2.setClear(OptionalBoolean.of(true));
            player.dataPacket(pk2);
            players.remove(player);
        }, 22);
    }

    private static BlockFace offsetDirection(Player player) {//偏移方向
        double yaw = player.yaw - 55;
        double rotation = yaw % 360.0;
        if (rotation < 0.0) {
            rotation += 360.0;
        }

        if ((!(0.0 <= rotation) || !(rotation < 45.0)) && (!(315.0 <= rotation) || !(rotation < 360.0))) {
            if (45.0 <= rotation && rotation < 135.0) {
                return BlockFace.WEST;
            } else if (135.0 <= rotation && rotation < 225.0) {
                return BlockFace.NORTH;
            } else {
                return 225.0 <= rotation && rotation < 315.0 ? BlockFace.EAST : null;
            }
        } else {
            return BlockFace.SOUTH;
        }
    }

}