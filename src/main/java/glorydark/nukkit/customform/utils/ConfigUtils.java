package glorydark.nukkit.customform.utils;

import cn.nukkit.Player;
import glorydark.nukkit.customform.CustomFormMain;

import java.io.File;

/**
 * @author glorydark
 * @date {2023/11/27} {22:44}
 */
public class ConfigUtils {

    private static String getPlayerConfigCachePath(Player player) {
        return CustomFormMain.path + "/caches/players/" + player.getName() + ".yml";
    }

    private static String getSpecificConfigCachePath(String specificName) {
        return CustomFormMain.path + "/caches/specific/" + specificName + ".yml";
    }

    public static File getPlayerConfigCacheFile(Player player) {
        return new File(getPlayerConfigCachePath(player));
    }

    public static File getSpecificConfigCacheFile(String specificName) {
        return new File(getSpecificConfigCachePath(specificName));
    }

}
