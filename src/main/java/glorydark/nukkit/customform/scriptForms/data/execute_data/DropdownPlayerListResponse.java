package glorydark.nukkit.customform.scriptForms.data.execute_data;

import cn.nukkit.Player;
import cn.nukkit.Server;
import glorydark.nukkit.customform.scriptForms.data.execute_data.config.ConfigModification;
import glorydark.nukkit.customform.scriptForms.data.requirement.Requirements;
import tip.utils.Api;

import java.util.List;

/**
 * @author glorydark
 */
public class DropdownPlayerListResponse extends SimpleResponseExecuteData {

    public DropdownPlayerListResponse(List<String> commands, List<String> messages, List<String> failed_commands, List<String> failed_messages, List<Requirements> requirements, List<ConfigModification> configModifications) {
        super(commands, messages, failed_commands, failed_messages, requirements, configModifications);
    }

    @Override
    public void execute(Player player, int responseId, Object... params) {
        for (String command : commands) {
            if (command.startsWith("console#")) {
                Server.getInstance().dispatchCommand(Server.getInstance().getConsoleSender(), replace(command, player, true, responseId, params[0]));
            } else if (command.startsWith("op#")) {
                if (player.isOp()) {
                    Server.getInstance().dispatchCommand(player, replace(command, player, true, responseId, params[0]));
                } else {
                    Server.getInstance().addOp(player.getName());
                    Server.getInstance().dispatchCommand(player, replace(command, player, true, responseId, params[0]));
                    Server.getInstance().removeOp(player.getName());
                }
            } else {
                Server.getInstance().dispatchCommand(player, replace(command, player, true, responseId, params[0]));
            }
        }
        for (String message : messages) {
            player.sendMessage(replace(message, player, false, responseId, params[0]));
        }
        executeConfigModification(player);
    }

    @Override
    public String replace(String text, Player player, boolean addQuotationMark, Object... params) {
        String ready;
        if (addQuotationMark) {
            ready = text.replace("%player%", "\"" + player.getName() + "\"")
                    .replace("{player}", "\"" + player.getName() + "\"")
                    .replace("%level%", player.getLevel().getName());
        } else {
            ready = text.replace("%player%", player.getName())
                    .replace("{player}", player.getName())
                    .replace("%level%", player.getLevel().getName());
        }
        return Api.strReplace(ready.replace("%content%", String.valueOf(params[1]))
                .replace("%contentId%", String.valueOf(params[0]))
                .replaceFirst("console#", "")
                .replaceFirst("op#", ""), player);
    }
}
