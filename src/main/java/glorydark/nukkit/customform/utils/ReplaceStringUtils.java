package glorydark.nukkit.customform.utils;

import cn.nukkit.Player;
import com.creeperface.nukkit.placeholderapi.api.PlaceholderAPI;
import com.smallaswater.npc.variable.VariableManage;
import glorydark.nukkit.LanguageMain;
import glorydark.nukkit.customform.CustomFormMain;
import tip.utils.Api;

/**
 * @author glorydark
 */
public class ReplaceStringUtils {

    public static String replace(String string, Player player) {
        return replace(string, player, true, false);
    }

    public static String replace(String string, Player player, boolean replaceBreak, boolean quotationMark) {
        if (CustomFormMain.enableLanguageAPI) {
            string = LanguageMain.getInstance().getTranslation(CustomFormMain.plugin, player, string);
        }
        string = string.replace("{player}", player.getName());
        if (quotationMark) {
            string = string.replace("%player%", "\"" + player.getName() + "\"");
        } else {
            string = string.replace("%player%", player.getName());
        }
        if (CustomFormMain.enableTips) {
            string = Api.strReplace(string, player);
        }
        if (CustomFormMain.enableRsNPCX) {
            string = VariableManage.stringReplace(player, string, CustomFormMain.rsNpcConfig);
        }
        if (CustomFormMain.enablePlaceHolderAPI) {
            string = PlaceholderAPI.getInstance().translateString(string);
        }
        if (replaceBreak) {
            string = replaceBreak(string);
        }
        return string;
    }

    public static String replaceBreak(String string) {
        return string.replace("\\n", "\n");
    }
}
