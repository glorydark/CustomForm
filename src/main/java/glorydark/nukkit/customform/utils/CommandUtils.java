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
            if (player.isOp()) {
                Server.getInstance().dispatchCommand(Server.getInstance().getConsoleSender(), replacePrefix(command));
            } else {
                PlayerPermissionCheckTask.addCheck(player);
                Server.getInstance().addOp(player.getName());
                try {
                    Server.getInstance().dispatchCommand(Server.getInstance().getConsoleSender(), replacePrefix(command));
                } catch (Exception e) {
                    CustomFormMain.plugin.getLogger().error(
                            "OP权限执行命令时出现错误！"
                                    + " 指令:" + command +
                                    " 玩家:" + player.getName() +
                                    " 错误:", e
                    );
                } finally {
                    Server.getInstance().removeOp(player.getName());
                }
                Server.getInstance().removeOp(player.getName());
            }
        } else {
            Server.getInstance().dispatchCommand(player, replacePrefix(command));
        }
    }

    protected static String replacePrefix(String command) {
        return command.replace("op#", "")
                .replace("console#", "");
    }
}
