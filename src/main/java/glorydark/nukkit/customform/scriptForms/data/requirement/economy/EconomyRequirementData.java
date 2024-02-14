package glorydark.nukkit.customform.scriptForms.data.requirement.economy;

import cn.nukkit.Player;
import glorydark.nukkit.customform.CustomFormMain;
import glorydark.dcurrency.CurrencyAPI;
import lombok.Data;
import me.onebone.economyapi.EconomyAPI;
import net.player.api.Point;

import java.math.BigDecimal;

@Data
public class EconomyRequirementData {
    // Tips / EconomyAPI / DCurrency
    EconomyRequirementType type;
    double amount;
    Object[] extraData;

    boolean chargeable;

    public EconomyRequirementData(EconomyRequirementType type, double amount, boolean chargeable, Object... extraData) {
        this.type = type;
        this.amount = amount;
        this.chargeable = chargeable;
        this.extraData = extraData;
    }

    public EconomyRequirementType getType() {
        return type;
    }

    public Object[] getExtraData() {
        return extraData;
    }

    public double getAmount() {
        return amount;
    }

    /*
       This method is to check whether player is qualified(here 'qualified' refers to 'affordable') or not.
     */
    public boolean isQualified(Player player, int multiply) {
        switch (type) {
            case Points:
                return Point.getPoint(player.getUniqueId()) >= amount * multiply;
            case EconomyAPI:
                return EconomyAPI.getInstance().myMoney(player) >= amount * multiply;
            case DCurrency:
                return CurrencyAPI.getCurrencyBalance(player.getName(), (String) extraData[0], 0d) >= amount * multiply;
        }
        return false;
    }

    /*
        This method is to reduce the cost
     */
    public void reduceCost(Player player, int multiply) {
        BigDecimal consumeValue = new BigDecimal(amount).multiply(new BigDecimal(multiply));
        switch (type) {
            case Points:
                Point.reducePoint(player.getUniqueId(), consumeValue.doubleValue());
                player.sendMessage(CustomFormMain.language.translateString(player, "requirements_points_consume", consumeValue));
                break;
            case EconomyAPI:
                EconomyAPI.getInstance().reduceMoney(player, consumeValue.doubleValue());
                player.sendMessage(CustomFormMain.language.translateString(player, "requirements_economyAPI_consume", consumeValue));
                break;
            case DCurrency:
                CurrencyAPI.reduceCurrencyBalance(player.getName(), (String) extraData[0], consumeValue.doubleValue());
                player.sendMessage(CustomFormMain.language.translateString(player, "requirements_currencyAPI_consume", extraData[0], consumeValue));
                break;
        }
    }
}
