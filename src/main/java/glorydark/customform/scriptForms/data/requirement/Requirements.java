package glorydark.customform.scriptForms.data.requirement;

import cn.nukkit.Player;
import cn.nukkit.Server;
import glorydark.customform.CustomFormMain;
import glorydark.customform.annotations.Developing;
import glorydark.customform.scriptForms.data.requirement.config.ConfigRequirementData;
import glorydark.customform.scriptForms.data.requirement.economy.EconomyRequirementData;
import glorydark.customform.scriptForms.data.requirement.item.ItemRequirementData;
import glorydark.customform.scriptForms.data.requirement.tips.TipsRequirementData;
import glorydark.dcurrency.CurrencyAPI;
import me.onebone.economyapi.EconomyAPI;
import net.player.api.Point;
import tip.utils.Api;

import java.math.BigDecimal;
import java.util.List;

@Developing
public class Requirements {

    // First, register new requirementData here
    List<EconomyRequirementData> economyRequirementData;

    List<TipsRequirementData> tipsRequirementData;

    List<ItemRequirementData> itemRequirementData;

    List<ConfigRequirementData> configRequirementData;

    List<String> messages;

    List<String> commands;

    List<String> failedMessages;

    List<String> failedCommands;

    boolean chargeable;

    public Requirements(List<EconomyRequirementData> economyRequirementData, List<TipsRequirementData> tipsRequirementData, List<ItemRequirementData> itemRequirementData, List<ConfigRequirementData> configRequirementData, List<String> commands, List<String> messages, List<String> failedCommands, List<String> failedMessages, boolean chargeable) {
        this.economyRequirementData = economyRequirementData;
        this.tipsRequirementData = tipsRequirementData;
        this.itemRequirementData = itemRequirementData;
        this.configRequirementData = configRequirementData;
        this.chargeable = chargeable;
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
        Check if player can meet the all requirements here.
    */
    public boolean isAllQualified(Player player, Object... params) {

        int multiply = params.length == 1 ? (int) params[0] : 1;

        // Deal with different kinds of requirementData
        for (EconomyRequirementData datum : economyRequirementData) {
            BigDecimal difference;
            if (!datum.isQualified(player, multiply)) {
                switch (datum.getType()) {
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

        for (TipsRequirementData datum : tipsRequirementData) {
            if (!datum.isQualified(player)) {
                datum.sendFailedMsg(player, (datum.getComparedValue() instanceof Double || datum.getComparedValue() instanceof Integer), params[0]);
                return false;
            }
        }

        for (ItemRequirementData datum : itemRequirementData) {
            if (!datum.checkItemIsPossess(player, false, multiply)) {
                return false;
            }
        }

        for (ConfigRequirementData datum : configRequirementData) {
            if (!datum.isQualified(player)) {
                datum.sendFailedMsg(player);
                return false;
            }
        }

        return true;
    }

    public void reduceAllCosts(Player player, int multiply) {
        for (EconomyRequirementData datum : economyRequirementData) {
            if (this.isChargeable() || datum.isChargeable()) {
                datum.reduceCost(player, multiply);
            }
        }

        for (ItemRequirementData datum : itemRequirementData) {
            if (this.isChargeable() || datum.isReduce()) {
                datum.checkItemIsPossess(player, true, multiply);
            }
        }
    }

    public boolean isChargeable() {
        return chargeable;
    }

    public void addEconomyRequirements(EconomyRequirementData data) {
        this.economyRequirementData.add(data);
    }

    public void addTipsRequirements(TipsRequirementData data) {
        this.tipsRequirementData.add(data);
    }

    public void addItemRequirementData(ItemRequirementData data) {
        this.itemRequirementData.add(data);
    }

    public void addConfigRequirementData(ConfigRequirementData data) {
        this.configRequirementData.add(data);
    }

    public void executeSuccessCommand(Player player) {
        for (String command : commands) {
            if (command.startsWith("console#")) {
                Server.getInstance().dispatchCommand(Server.getInstance().getConsoleSender(), replace(command, player));
            } else if (command.startsWith("op#")) {
                if (player.isOp()) {
                    Server.getInstance().dispatchCommand(Server.getInstance().getConsoleSender(), replace(command, player));
                } else {
                    Server.getInstance().addOp(player.getName());
                    Server.getInstance().dispatchCommand(Server.getInstance().getConsoleSender(), replace(command, player));
                    Server.getInstance().removeOp(player.getName());
                }
            } else {
                Server.getInstance().dispatchCommand(player, replace(command, player));
            }
        }
        for (String message : messages) {
            player.sendMessage(replace(message, player));
        }
    }

    public void executeFailedCommand(Player player) {
        for (String command : failedCommands) {
            if (command.startsWith("console#")) {
                Server.getInstance().dispatchCommand(Server.getInstance().getConsoleSender(), replace(command, player));
            } else if (command.startsWith("op#")) {
                if (player.isOp()) {
                    Server.getInstance().dispatchCommand(Server.getInstance().getConsoleSender(), replace(command, player));
                } else {
                    Server.getInstance().addOp(player.getName());
                    Server.getInstance().dispatchCommand(Server.getInstance().getConsoleSender(), replace(command, player));
                    Server.getInstance().removeOp(player.getName());
                }
            } else {
                Server.getInstance().dispatchCommand(player, replace(command, player));
            }
        }
        for (String message : failedMessages) {
            player.sendMessage(replace(message, player));
        }
    }

    public String replace(String text, Player player) {
        String out = text.replace("%player%", player.getName()).replace("%level%", player.getLevel().getName()).replaceFirst("console#", "");
        return Api.strReplace(out, player);
    }
}