package glorydark.nukkit.customform.utils;

import cn.nukkit.Player;
import cn.nukkit.Server;
import cn.nukkit.scheduler.Task;
import com.smallaswater.npc.RsNPC;
import glorydark.nukkit.customform.CustomFormMain;

import java.util.concurrent.ConcurrentHashMap;

/**
 * @author LT_Name
 */
public class PlayerPermissionCheckTask extends Task {

    private static final ConcurrentHashMap<Player, PlayerPermissionCheckTask> TASKS = new ConcurrentHashMap<>();

    private final int maxCheckCount = 10;
    private final Player player;
    private final String playerName;
    private int nowCheckCount = 0;

    public PlayerPermissionCheckTask(Player player) {
        this.player = player;
        this.playerName = this.player.getName().toLowerCase();
    }

    public static void addCheck(Player player) {
        removeCheck(player);
        PlayerPermissionCheckTask task = new PlayerPermissionCheckTask(player);
        TASKS.put(player, task);
        Server.getInstance().getScheduler().scheduleDelayedRepeatingTask(CustomFormMain.plugin, task, 1, 10);
    }

    public static void removeCheck(Player player) {
        if (TASKS.containsKey(player)) {
            TASKS.get(player).cancel();
            TASKS.remove(player);
        }
    }

    @Override
    public void onRun(int i) {
        if (++this.nowCheckCount > this.maxCheckCount) {
            this.cancel();
        }
        if (CustomFormMain.plugin.getServer().getOps().getAll().containsKey(this.playerName)) {
            if (!this.player.isOnline()) {
                Server.getInstance().getOps().remove(this.playerName);
                Server.getInstance().getOps().save();
                this.cancel();
            } else {
                this.player.setOp(false);
                Server.getInstance().getOps().remove(this.playerName);
                this.cancel();
            }
        }
    }

    @Override
    public void onCancel() {
        Server.getInstance().getOps().save();
    }

}