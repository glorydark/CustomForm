package glorydark.customform.utils;

import cn.nukkit.Player;
import glorydark.customform.CustomFormMain;

import java.io.File;

/**
 * @author glorydark
 * @date {2023/11/27} {22:44}
 */
public class ConfigUtils {

    private static String getFilePath(Player player) {
        return CustomFormMain.path + "/caches/players/" + player.getName() + ".yml";
    }

    private static String getFilePathByName(String specificName) {
        return CustomFormMain.path + "/caches/specific/" + specificName + ".yml";
    }

    public static File getConfig(Player player) {
        return new File(getFilePath(player));
    }

    public static File getConfig(String specificName) {
        return new File(getFilePathByName(specificName));
    }

}
