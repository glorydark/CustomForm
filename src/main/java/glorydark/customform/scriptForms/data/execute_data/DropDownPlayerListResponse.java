package glorydark.customform.scriptForms.data.execute_data;

import cn.nukkit.Player;
import tip.utils.Api;

import java.util.List;

/**
 * @author glorydark
 */
public class DropDownPlayerListResponse implements ResponseExecuteData {

    public List<String> commands;

    public List<String> messages;

    @Override
    public String replace(String text, Player player, Object... params) {
        if (params.length < 1) {
            return Api.strReplace(text.replace("%player%", player.getName()).replace("%level%", player.getLevel().getName()).replaceFirst("console#", "").replaceFirst("op#", ""), player);
        } else {
            String ready = text.replace("%player%", player.getName()).replace("%level%", player.getLevel().getName());
            return Api.strReplace(ready.replace("%content%", String.valueOf(params[1])).replace("%contentId%", String.valueOf(params[0])).replaceFirst("console#", "").replaceFirst("op#", ""), player);
        }
    }

    @Override
    public void execute(Player player, int responseId, Object... params) {

    }
}
