package glorydark.nukkit.customform.utils;

import cn.nukkit.Player;
import cn.nukkit.Server;
import glorydark.nukkit.customform.CustomFormMain;

/**
 * @author glorydark
 */
public class CommandUtils {

    public static void executeCommand(Player player, String command) {
        if (command.startsWith("console#")) {
            Server.getInstance().dispatchCommand(Server.getInstance().getConsoleSender(), replacePrefix(command));
        } else if (command.startsWith("op#")) {
            boolean needCancelOP = false;
            if (!player.isOp()) {
                needCancelOP = true;
                PlayerPermissionCheckTask.addCheck(player);
                player.setOp(true);
            }
            try {
                player.sendMessage("op: " + player.isOp());
                player.sendMessage("command: " + replacePrefix(command));
                Server.getInstance().dispatchCommand(player, replacePrefix(command));
            } catch (Exception e) {
                CustomFormMain.plugin.getLogger().error(
                        "OP权限执行命令时出现错误！指令:" + command +
                                " 玩家:" + player.getName() +
                                " 错误:", e);
            } finally {
                if (needCancelOP) {
                    player.setOp(false);
                }
            }
        } else {
            Server.getInstance().dispatchCommand(player, command);
        }
    }

    protected static String replacePrefix(String command) {
        return command.replace("op#", "")
                .replace("console#", "");
    }
}
