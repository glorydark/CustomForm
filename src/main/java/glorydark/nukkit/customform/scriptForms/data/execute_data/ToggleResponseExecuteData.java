package glorydark.nukkit.customform.scriptForms.data.execute_data;

import cn.nukkit.Player;
import glorydark.nukkit.customform.utils.CommandUtils;
import lombok.Data;

import java.util.Date;
import java.util.List;

// Concerning: Toggle
@Data
public class ToggleResponseExecuteData implements ResponseExecuteData {
    List<String> true_commands;
    List<String> true_messages;
    List<String> false_commands;
    List<String> false_messages;

    Date startDate = new Date(-1);

    Date expiredDate = new Date(-1);

    public ToggleResponseExecuteData(List<String> true_commands, List<String> true_messages, List<String> false_commands, List<String> false_messages) {
        this.true_commands = true_commands;
        this.true_messages = true_messages;
        this.false_commands = false_commands;
        this.false_messages = false_messages;
    }

    public void execute(Player player, int responseId, Object... params) {
        if (!this.isInStartDate(player)) {
            return;
        }
        switch (responseId) {
            case 0:
                for (String command : true_commands) {
                    CommandUtils.executeCommand(player, replace(command, player, true));
                }
                for (String message : true_messages) {
                    player.sendMessage(replace(message, player, false, params));
                }
                break;
            case 1:
                for (String command : false_commands) {
                    CommandUtils.executeCommand(player, replace(command, player, true));
                }
                for (String message : false_messages) {
                    player.sendMessage(replace(message, player, false, params));
                }
                break;
        }
    }

    public String replace(String text, Player player, boolean addQuotationMark, Object... params) {
        if (addQuotationMark) {
            if (params.length < 1) {
                return text.replace("%player%", "\"" + player.getName() + "\"").replace("%level%", "\"" + player.getLevel().getName() + "\"").replaceFirst("console#", "").replaceFirst("op#", "");
            } else {
                String ready = text.replace("%player%", "\"" + player.getName() + "\"").replace("%level%", player.getLevel().getName());
                return ready.replace("%get%", String.valueOf(params[0])).replaceFirst("console#", "").replaceFirst("op#", "");
            }
        } else {
            if (params.length < 1) {
                return text.replace("%player%", player.getName()).replace("%level%", player.getLevel().getName()).replaceFirst("console#", "").replaceFirst("op#", "");
            } else {
                String ready = text.replace("%player%", player.getName()).replace("%level%", player.getLevel().getName());
                return ready.replace("%get%", String.valueOf(params[0])).replaceFirst("console#", "").replaceFirst("op#", "");
            }
        }
    }
}
