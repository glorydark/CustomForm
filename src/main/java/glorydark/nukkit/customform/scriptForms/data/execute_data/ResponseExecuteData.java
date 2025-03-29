package glorydark.nukkit.customform.scriptForms.data.execute_data;

import cn.nukkit.Player;
import glorydark.nukkit.customform.CustomFormMain;
import glorydark.nukkit.customform.factory.FormCreator;

import java.util.Date;

public interface ResponseExecuteData {

    Date getStartDate();

    Date getExpiredDate();

    String replace(String text, Player player, boolean addQuotationMark, Object... params);

    void execute(Player player, int responseId, Object... params);

    default boolean isInStartDate(Player player) {
        long startMillis = this.getStartDate().getTime();
        long expireMillis = this.getExpiredDate().getTime();
        if (startMillis > 0 && System.currentTimeMillis() < startMillis) {
            player.sendMessage(CustomFormMain.language.translateString(player, "form_not_in_opening_hours", FormCreator.dateToString(player, this.getStartDate()), FormCreator.dateToString(player, this.getExpiredDate())));
            return false;
        }
        if (expireMillis > 0 && System.currentTimeMillis() > expireMillis) {
            player.sendMessage(CustomFormMain.language.translateString(player, "form_not_in_opening_hours", FormCreator.dateToString(player, this.getStartDate()), FormCreator.dateToString(player, this.getExpiredDate())));
            return false;
        }
        return true;
    }
}