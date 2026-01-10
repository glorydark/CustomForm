package glorydark.nukkit.customform.scriptForms.data.requirement;

import cn.nukkit.Player;
import glorydark.dcurrency.CurrencyAPI;
import glorydark.nukkit.customform.CustomFormMain;
import glorydark.nukkit.customform.scriptForms.data.requirement.config.ConfigRequirementData;
import glorydark.nukkit.customform.scriptForms.data.requirement.custom.ReducibleRequirementData;
import glorydark.nukkit.customform.scriptForms.data.requirement.custom.RequirementData;
import glorydark.nukkit.customform.scriptForms.data.requirement.economy.EconomyRequirementData;
import glorydark.nukkit.customform.scriptForms.data.requirement.item.ItemRequirementData;
import glorydark.nukkit.customform.scriptForms.data.requirement.tips.TipsRequirementData;
import glorydark.nukkit.customform.utils.CommandUtils;
import lombok.ToString;
import me.onebone.economyapi.EconomyAPI;
import net.player.api.Point;
import tip.utils.Api;

import java.math.BigDecimal;
import java.util.List;

@ToString
public class Requirements {

    // First, register new requirementData here
    List<EconomyRequirementData> economyRequirementData;

    List<TipsRequirementData> tipsRequirementData;

    List<ItemRequirementData> itemRequirementData;

    List<ConfigRequirementData> configRequirementData;

    List<RequirementData> customRequirementData;

    List<String> messages;

    List<String> commands;

    List<String> failedMessages;

    List<String> failedCommands;

    boolean chargeable;

    public Requirements(List<EconomyRequirementData> economyRequirementData, List<TipsRequirementData> tipsRequirementData, List<ItemRequirementData> itemRequirementData, List<ConfigRequirementData> configRequirementData, List<RequirementData> customRequirementData, List<String> commands, List<String> messages, List<String> failedCommands, List<String> failedMessages, boolean chargeable) {
        this.economyRequirementData = economyRequirementData;
        this.tipsRequirementData = tipsRequirementData;
        this.itemRequirementData = itemRequirementData;
        this.configRequirementData = configRequirementData;
        this.customRequirementData = customRequirementData;
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
        return this.isAllQualifiedWithOutputMessage(player, true, params);
    }

    public boolean isAllQualifiedWithOutputMessage(Player player, boolean output, Object... params) {
        int multiply = params.length == 1 ? (int) params[0] : 1;

        // Deal with different kinds of requirementData
        int i = 0;
        for (EconomyRequirementData datum : this.economyRequirementData) {
            i++;
            BigDecimal difference;
            if (!datum.isQualified(player, multiply)) {
                switch (datum.getType()) {
                    case Points:
                        difference = BigDecimal.valueOf(datum.getAmount()).subtract(BigDecimal.valueOf(Point.getPoint(player.getUniqueId())).multiply(new BigDecimal(multiply)));
                        if (output) {
                            player.sendMessage(CustomFormMain.language.translateString(player, "requirements.points.not_qualified", i, difference));
                        }
                        break;
                    case DCurrency:
                        difference = BigDecimal.valueOf(datum.getAmount()).subtract(BigDecimal.valueOf(CurrencyAPI.getCurrencyBalance(player.getName(), (String) datum.getExtraData()[0], 0d))).multiply(new BigDecimal(multiply));
                        if (output) {
                            player.sendMessage(CustomFormMain.language.translateString(player, "requirements.currencyapi.not_qualified", i, datum.getExtraData()[0], difference));
                        }
                        break;
                    case EconomyAPI:
                        difference = BigDecimal.valueOf(datum.getAmount()).subtract(BigDecimal.valueOf(EconomyAPI.getInstance().myMoney(player))).multiply(new BigDecimal(multiply));
                        if (output) {
                            player.sendMessage(CustomFormMain.language.translateString(player, "requirements.economyapi.not_qualified", i, difference));
                        }
                        break;
                }
                return false;
            }
        }

        for (TipsRequirementData datum : this.tipsRequirementData) {
            if (!datum.isQualified(player)) {
                if (output) {
                    datum.sendFailedMsg(player, (datum.getComparedValue() instanceof Double || datum.getComparedValue() instanceof Integer), params[0]);
                }
                return false;
            }
        }

        for (ItemRequirementData datum : this.itemRequirementData) {
            if (!datum.checkItemIsPossess(player, false, multiply, output)) {
                return false;
            }
        }

        for (ConfigRequirementData datum : this.configRequirementData) {
            if (!datum.isQualified(player)) {
                if (output) {
                    datum.sendFailedMsg(player);
                }
                return false;
            }
        }

        for (RequirementData customRequirementDatum : this.customRequirementData) {
            if (customRequirementDatum instanceof ReducibleRequirementData reducibleRequirementData) {
                if (!reducibleRequirementData.isQualified(player, multiply)) {
                    if (output) {
                        reducibleRequirementData.sendFailedMessage(player);
                    }
                    return false;
                }
            }
        }
        return true;
    }

    public void reduceAllCosts(Player player, int multiply) {
        for (EconomyRequirementData datum : this.economyRequirementData) {
            if (datum.isChargeable()) {
                datum.reduceCost(player, multiply);
            }
        }

        for (ItemRequirementData datum : this.itemRequirementData) {
            if (datum.isReduce()) {
                datum.checkItemIsPossess(player, true, multiply);
            }
        }

        for (RequirementData customRequirementDatum : this.customRequirementData) {
            if (customRequirementDatum instanceof ReducibleRequirementData) {
                ReducibleRequirementData reducibleRequirementData = (ReducibleRequirementData) customRequirementDatum;
                if (reducibleRequirementData.isReduce()) {
                    reducibleRequirementData.reduceCost(player, multiply);
                }
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

    public void addCustomRequirementData(RequirementData data) {
        this.customRequirementData.add(data);
    }

    public void executeSuccessCommand(Player player) {
        for (String command : commands) {
            CommandUtils.executeCommand(player, replace(command, player, true));
        }
        for (String message : messages) {
            player.sendMessage(replace(message, player, false));
        }
    }

    public void executeFailedCommand(Player player) {
        for (String command : failedCommands) {
            CommandUtils.executeCommand(player, replace(command, player, true));
        }
        for (String message : failedMessages) {
            player.sendMessage(replace(message, player, false));
        }
    }

    public String replace(String text, Player player, boolean quotationMark) {
        String out = text
                .replace("%level%", player.getLevel().getName());
        if (quotationMark) {
            out = out.replace("%player%", "\"" + player.getName() + "\"");
        } else {
            out = out.replace("%player%", player.getName());
        }
        return Api.strReplace(out, player);
    }

    public static boolean isAnyRequirementMet(List<Requirements> requirements, Player player, int multiply, boolean output) {
        for (Requirements one : requirements) {
            if (one.isAllQualifiedWithOutputMessage(player, output, multiply)) {
                return true;
            }
        }
        return false;
    }
}