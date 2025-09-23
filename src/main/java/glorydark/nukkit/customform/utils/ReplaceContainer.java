package glorydark.nukkit.customform.utils;

import java.util.*;

/**
 * @author glorydark
 */
public class ReplaceContainer extends HashMap<String, String> {

    public static final ReplaceContainer EMPTY_CONTAINER = new ReplaceContainer();

    public ReplaceContainer() {

    }

    public ReplaceContainer(ReplacePair<String, String>... replacePairs) {
        if (replacePairs != null) {
            for (ReplacePair<String, String> replacePair : replacePairs) {
                this.put(replacePair.getKey(), replacePair.getValue());
            }
        }
    }

    @SafeVarargs
    public static ReplaceContainer of(ReplacePair<String, String>... replacePairs) {
        ReplaceContainer container = new ReplaceContainer();
        if (replacePairs != null) {
            for (ReplacePair<String, String> replacePair : replacePairs) {
                container.put(replacePair.getKey(), replacePair.getValue());
            }
        }
        return container;
    }

    public String replaceString(String s) {
        for (Entry<String, String> entry : this.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();
            s = s.replace(key, value);
        }
        return s;
    }
}
