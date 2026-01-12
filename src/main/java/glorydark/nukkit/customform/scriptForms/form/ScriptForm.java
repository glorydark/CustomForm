package glorydark.nukkit.customform.scriptForms.form;

import cn.nukkit.Player;
import cn.nukkit.form.response.FormResponse;
import cn.nukkit.form.window.FormWindow;
import cn.nukkit.network.protocol.ModalFormRequestPacket;
import glorydark.nukkit.customform.CustomFormMain;
import glorydark.nukkit.customform.factory.FormCreator;
import glorydark.nukkit.customform.factory.FormType;
import glorydark.nukkit.customform.scriptForms.data.SoundData;
import glorydark.nukkit.customform.scriptForms.data.requirement.Requirements;
import glorydark.nukkit.customform.utils.NukkitTypeUtils;

import java.util.Date;
import java.util.List;

public interface ScriptForm {

    List<Requirements> getOpenRequirements();

    @Deprecated
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
            player.sendMessage(CustomFormMain.language.translateString(player, "default.not_in_opening_hours", FormCreator.dateToString(player, this.getStartDate()), FormCreator.dateToString(player, this.getExpiredDate())));
            return false;
        }
        if (expireMillis > 0 && System.currentTimeMillis() > expireMillis) {
            player.sendMessage(CustomFormMain.language.translateString(player, "default.not_in_opening_hours", FormCreator.dateToString(player, this.getStartDate()), FormCreator.dateToString(player, this.getExpiredDate())));
            return false;
        }
        return true;
    }

    default void showToPlayer(Player player, FormType formType, String identifier) {
        FormWindow window = this.getWindow(player);
        ModalFormRequestPacket packet = new ModalFormRequestPacket();
        packet.formId = FormCreator.formId;
        if (NukkitTypeUtils.getNukkitType() == NukkitTypeUtils.NukkitType.MOT) {
            packet.data = window.getJSONData(player.getGameVersion());
        } else {
            packet.data = window.getJSONData(player.protocol);
        }
        player.dataPacket(packet);
        player.namedTag.putLong("lastFormRequestMillis", System.currentTimeMillis());
        FormCreator.UI_CACHE.put(player.getName(), new FormCreator.WindowInfo(formType, identifier, this, window));
    }
}