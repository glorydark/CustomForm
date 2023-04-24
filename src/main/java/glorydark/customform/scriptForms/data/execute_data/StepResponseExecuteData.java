package glorydark.customform.scriptForms.data.execute_data;

import cn.nukkit.Player;
import cn.nukkit.Server;
import lombok.Data;
import tip.utils.Api;

import java.util.List;

// StepSlider, Dropdown
@Data
public class StepResponseExecuteData implements ResponseExecuteData {

    public List<SimpleResponseExecuteData> responses;

    public StepResponseExecuteData(List<SimpleResponseExecuteData> responses){
        this.responses = responses;
    }

    public void execute(Player player, int responseId, Object... params){
        if(responseId >= responses.size()){
            return;
        }
        for(String command: responses.get(responseId).getCommands()){
            if(command.startsWith("console#")){
                Server.getInstance().dispatchCommand(Server.getInstance().getConsoleSender(), replace(command, player, responseId, params[0]));
            } else if(command.startsWith("op#")) {
                Server.getInstance().addOp(player.getName());
                Server.getInstance().dispatchCommand(Server.getInstance().getConsoleSender(), replace(command, player, responseId, params[0]));
                Server.getInstance().removeOp(player.getName());
            } else{
                Server.getInstance().dispatchCommand(player, replace(command, player, responseId, params[0]));
            }
        }
        for(String message: responses.get(responseId).getMessages()){
            player.sendMessage(replace(message, player, responseId, params[0]));
        }
    }

    public String replace(String text, Player player, Object... params){
        if(params.length < 1) {
            return Api.strReplace(text.replace("%player%", player.getName()).replace("%level%", player.getLevel().getName()).replaceFirst("console#", ""), player);
        }else{
            String ready = text.replace("%player%", player.getName()).replace("%level%", player.getLevel().getName());
            return Api.strReplace(ready.replace("%content%", String.valueOf(params[1])).replace("%contentId%", String.valueOf(params[0])).replaceFirst("console#", ""), player);
        }
    }
}
