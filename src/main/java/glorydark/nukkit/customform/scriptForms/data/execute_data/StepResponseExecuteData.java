package glorydark.nukkit.customform.scriptForms.data.execute_data;

import cn.nukkit.Player;
import glorydark.nukkit.customform.utils.CommandUtils;
import glorydark.nukkit.customform.utils.ReplaceContainer;
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

    public void execute(Player player, int responseId, ReplaceContainer replaceContainer, Object... params) {
        if (!this.isInStartDate(player)) {
            return;
        }
        if (responseId >= responses.size()) {
            return;
        }
        for (String command : responses.get(responseId).getCommands()) {
            CommandUtils.executeCommand(player, replaceContainer.replaceString(replace(command, player, true, responseId, params[0])));
        }
        for (String message : responses.get(responseId).getMessages()) {
            player.sendMessage(replace(message, player, false, responseId, params[0]));
        }
    }

    public String replace(String text, Player player, boolean addQuotationMark, Object... params) {
        if (addQuotationMark) {
            if (params.length < 1) {
                return Api.strReplace(text.replace("%player%", "\"" + player.getName() + "\"")
                        .replace("{player}", "\"" + player.getName() + "\"")
                        .replace("%level%", "\"" + player.getLevel().getName() + "\""), player);
            } else {
                String ready = text.replace("%player%", "\"" + player.getName() + "\"")
                        .replace("{player}", "\"" + player.getName() + "\"")
                        .replace("%level%", player.getLevel().getName());
                return Api.strReplace(ready.replace("%content%", String.valueOf(params[1]))
                        .replace("%contentId%", String.valueOf(params[0])), player);
            }
        } else {
            if (params.length < 1) {
                return Api.strReplace(text.replace("%player%", player.getName())
                        .replace("{player}", player.getName())
                        .replace("%level%", player.getLevel().getName()), player);
            } else {
                String ready = text.replace("%player%", player.getName())
                        .replace("{player}", player.getName())
                        .replace("%level%", player.getLevel().getName());
                return Api.strReplace(ready.replace("%content%", String.valueOf(params[1]))
                        .replace("%contentId%", String.valueOf(params[0])), player);
            }
        }
    }
}
