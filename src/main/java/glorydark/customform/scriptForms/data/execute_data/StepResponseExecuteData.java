package glorydark.customform.scriptForms.data.execute_data;

import cn.nukkit.Player;
import cn.nukkit.Server;
import lombok.Data;
import tip.utils.Api;

import java.util.Date;
import java.util.List;

// StepSlider, Dropdown
@Data
public class StepResponseExecuteData implements ResponseExecuteData {

    public List<SimpleResponseExecuteData> responses;

    Date startDate = new Date(-1);

    Date expiredDate = new Date(-1);

    public StepResponseExecuteData(List<SimpleResponseExecuteData> responses) {
        this.responses = responses;
    }

    public void execute(Player player, int responseId, Object... params) {
        if (!this.isInStartDate(player)) {
            return;
        }
        if (responseId >= responses.size()) {
            return;
        }
        for (String command : responses.get(responseId).getCommands()) {
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
        for (String message : responses.get(responseId).getMessages()) {
            player.sendMessage(replace(message, player, false, params[0]));
        }
    }

    public String replace(String text, Player player, boolean addQuotationMark, Object... params) {
        if (addQuotationMark) {
            if (params.length < 1) {
                return Api.strReplace(text.replace("%player%", "\"" + player.getName() + "\"").replace("%level%", "\"" + player.getLevel().getName() + "\"").replaceFirst("console#", "").replaceFirst("op#", ""), player);
            } else {
                String ready = text.replace("%player%", "\"" + player.getName() + "\"").replace("%level%", player.getLevel().getName());
                return Api.strReplace(ready.replace("%content%", String.valueOf(params[1])).replace("%contentId%", String.valueOf(params[0])).replaceFirst("console#", "").replaceFirst("op#", ""), player);
            }
        } else {
            if (params.length < 1) {
                return Api.strReplace(text.replace("%player%", player.getName()).replace("%level%", player.getLevel().getName()).replaceFirst("console#", "").replaceFirst("op#", ""), player);
            } else {
                String ready = text.replace("%player%", player.getName()).replace("%level%", player.getLevel().getName());
                return Api.strReplace(ready.replace("%content%", String.valueOf(params[1])).replace("%contentId%", String.valueOf(params[0])).replaceFirst("console#", "").replaceFirst("op#", ""), player);
            }
        }
    }
}
