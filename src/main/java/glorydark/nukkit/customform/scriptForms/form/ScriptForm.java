package glorydark.nukkit.customform.scriptForms.form;

import cn.nukkit.Player;
import cn.nukkit.form.response.FormResponse;
import cn.nukkit.form.window.FormWindow;
import glorydark.nukkit.customform.CustomFormMain;
import glorydark.nukkit.customform.forms.FormCreator;
import glorydark.nukkit.customform.scriptForms.data.SoundData;
import glorydark.nukkit.customform.scriptForms.data.requirement.Requirements;

import java.util.Date;
import java.util.List;

public interface ScriptForm {

    List<Requirements> getOpenRequirements();

    void execute(Player player, FormWindow respondWindow, FormResponse response, Object... params);

    FormWindow getWindow(Player player);

    SoundData getOpenSound();

    Date getStartDate();

    Date getExpiredDate();

    List<PermissionEnum> getOpenPermissions();

    List<String> getOpenPermissionWhitelist();

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