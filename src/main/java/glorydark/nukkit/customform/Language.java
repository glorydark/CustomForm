package glorydark.nukkit.customform;

import cn.nukkit.Player;
import cn.nukkit.utils.Config;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class Language {

    public Map<String, Map<String, Object>> lang = new HashMap<>();

    public String defaultLang;

    public Language(String defaultLang, String langPath) {
        this.defaultLang = defaultLang;
        File files = new File(langPath);
        for (File file : Objects.requireNonNull(files.listFiles())) {
            if (file.getName().endsWith(".properties")) {
                lang.put(file.getName().replace(".properties", ""), new Config(file, Config.PROPERTIES).getAll());
            }
        }
    }

    public String translateString(Player player, String key, Object... params) {
        String originText = (String) lang.getOrDefault(getLang(player), new HashMap<>()).getOrDefault(key, "Key not found!");
        for (int i = 1; i <= params.length; i++) {
            originText = originText.replaceAll("%" + i + "%", params[i - 1].toString());
        }
        return originText;
    }

    private String getLang(Player player) {
        if (player == null) {
            return defaultLang;
        }
        String languageCode = player.getLoginChainData().getLanguageCode();
        return lang.containsKey(languageCode) ? languageCode : defaultLang;
    }
}
