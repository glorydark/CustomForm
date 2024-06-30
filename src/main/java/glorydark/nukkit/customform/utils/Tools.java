package glorydark.nukkit.customform.utils;

import cn.nukkit.utils.TextFormat;

import java.util.List;

/**
 * @author glorydark
 */
public class Tools {

    public static String formatTimeDiff(long l1, long l2) {
        long diff = Math.abs(l1 - l2);
        long minute = diff / 60000L;
        long second = (diff - minute * 60000L) / 1000;
        long millis = diff - minute * 60000L - second * 1000L;
        String prefix = "";
        if (millis > 50) {
            if (second > 1) {
                prefix = String.valueOf(TextFormat.DARK_RED);
            } else if (millis > 100) {
                prefix = String.valueOf(TextFormat.RED);
            } else {
                prefix = String.valueOf(TextFormat.YELLOW);
            }
        } else {
            prefix = String.valueOf(TextFormat.GREEN);
        }
        return prefix + minute + "m" + second + "s" + millis + "ms";
    }

    public static String toString(List<String> stringList) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < stringList.size(); i++) {
            builder.append(stringList.get(i));
            if (i != stringList.size() - 1) {
                builder.append("\n");
            }
        }
        return builder.toString();
    }
}
