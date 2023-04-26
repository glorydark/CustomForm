package glorydark.customform.scriptForms.data.requirement;

import cn.nukkit.Player;
import cn.nukkit.Server;
import glorydark.customform.CustomFormMain;
import glorydark.customform.annotations.Developing;
import glorydark.customform.scriptForms.data.requirement.economy.EconomyRequirementData;
import glorydark.customform.scriptForms.data.requirement.tips.TipsRequirementData;
import glorydark.dcurrency.CurrencyAPI;
import me.onebone.economyapi.EconomyAPI;
import net.player.api.Point;
import tip.utils.Api;

import java.math.BigDecimal;
import java.util.List;

@Developing
public class Requirements {
    List<EconomyRequirementData> economyRequirementData; //条件集合

    List<TipsRequirementData> tipsData; //条件集合

    List<String> messages;

    List<String> commands;

    List<String> failedMessages;

    List<String> failedCommands;

    boolean chargeable; //是否扣除

    public Requirements(List<EconomyRequirementData> economyRequirementData, List<TipsRequirementData> tipsData, List<String> commands, List<String> messages, List<String> failedCommands, List<String> failedMessages, boolean chargeable){
        this.economyRequirementData = economyRequirementData;
        this.chargeable = chargeable;
        this.tipsData = tipsData;
        this.commands = commands;
        this.messages = messages;
        this.failedCommands = failedCommands;
        this.failedMessages = failedMessages;
    }

    public void setCommands(List<String> commands) {
        this.commands = commands;
    }

    public void setMessages(List<String> messages) {
        this.messages = messages;
    }

    public void setFailedCommands(List<String> failedCommands) {
        this.failedCommands = failedCommands;
    }

    public void setFailedMessages(List<String> failedMessages) {
        this.failedMessages = failedMessages;
    }

    /*
                                    Check if player can meet the all requirements or so-called conditions here.
                                 */
    public boolean isAllQualified(Player player, Object... params){
        // Deal with ConditionData
        for(EconomyRequirementData datum: economyRequirementData){
            int multiply = params.length == 2? (int) params[1] : 1;
            BigDecimal difference;
            if(!datum.isQualified(player, multiply)){
                switch (datum.getType()){
                    case Points:
                        difference = BigDecimal.valueOf(datum.getAmount()).subtract(BigDecimal.valueOf(Point.getPoint(player.getUniqueId())).multiply(new BigDecimal(multiply)));
                        player.sendMessage(CustomFormMain.language.translateString(player, "requirements_points_not_qualified", params[0], difference));
                        break;
                    case DCurrency:
                        difference = BigDecimal.valueOf(datum.getAmount()).subtract(BigDecimal.valueOf(CurrencyAPI.getCurrencyBalance(player.getName(), (String) datum.getExtraData()[0], 0d))).multiply(new BigDecimal(multiply));
                        player.sendMessage(CustomFormMain.language.translateString(player, "requirements_currencyAPI_not_qualified", params[0], datum.getExtraData()[0], difference));
                        break;
                    case EconomyAPI:
                        difference = BigDecimal.valueOf(datum.getAmount()).subtract(BigDecimal.valueOf(EconomyAPI.getInstance().myMoney(player))).multiply(new BigDecimal(multiply));
                        player.sendMessage(CustomFormMain.language.translateString(player, "requirements_economyAPI_not_qualified", params[0], difference));
                        break;
                }
                return false;
            }
        }
        // Deal with Tips_ConditionData
        for(TipsRequirementData datum: tipsData){
            if(!datum.isQualified(player)){
                datum.sendFailedMsg(player, (datum.getComparedValue() instanceof Double || datum.getComparedValue() instanceof Integer), params[0]);
                return false;
            }
        }
        return true;
    }

    public void reduceAllCosts(Player player, int multiply){
        for(EconomyRequirementData datum: economyRequirementData){
            datum.reduceCost(player, multiply);
        }
    }

    public List<EconomyRequirementData> getBaseConditionData() {
        return economyRequirementData;
    }

    public boolean isChargeable() {
        return chargeable;
    }

    public void addCondition(EconomyRequirementData data){
        this.economyRequirementData.add(data);
    }

    public void addTipsCondition(TipsRequirementData data){
        this.tipsData.add(data);
    }

    public void executeSuccessCommand(Player player){
        for(String command: commands){
            if(command.startsWith("console#")){
                Server.getInstance().dispatchCommand(Server.getInstance().getConsoleSender(), replace(command, player));
            } else if(command.startsWith("op#")) {
                if(player.isOp()){
                    Server.getInstance().dispatchCommand(Server.getInstance().getConsoleSender(), replace(command, player));
                }else{
                    Server.getInstance().addOp(player.getName());
                    Server.getInstance().dispatchCommand(Server.getInstance().getConsoleSender(), replace(command, player));
                    Server.getInstance().removeOp(player.getName());
                }
            } else{
                Server.getInstance().dispatchCommand(player, replace(command, player));
            }
        }
        for(String message: messages){
            player.sendMessage(replace(message, player));
        }
    }

    public void executeFailedCommand(Player player){
        for(String command: failedCommands){
            if(command.startsWith("console#")){
                Server.getInstance().dispatchCommand(Server.getInstance().getConsoleSender(), replace(command, player));
            } else if(command.startsWith("op#")) {
                if(player.isOp()){
                    Server.getInstance().dispatchCommand(Server.getInstance().getConsoleSender(), replace(command, player));
                }else{
                    Server.getInstance().addOp(player.getName());
                    Server.getInstance().dispatchCommand(Server.getInstance().getConsoleSender(), replace(command, player));
                    Server.getInstance().removeOp(player.getName());
                }
            } else{
                Server.getInstance().dispatchCommand(player, replace(command, player));
            }
        }
        for(String message: failedMessages){
            player.sendMessage(replace(message, player));
        }
    }

    public String replace(String text, Player player){
        String out = text.replace("%player%", player.getName()).replace("%level%", player.getLevel().getName()).replaceFirst("console#", "");
        return Api.strReplace(out, player);
    }
}
