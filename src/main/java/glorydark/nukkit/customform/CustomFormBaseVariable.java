package glorydark.nukkit.customform;

import cn.nukkit.Player;
import tip.utils.variables.BaseVariable;

import java.util.LinkedHashMap;
import java.util.Map;

public class CustomFormBaseVariable extends BaseVariable {

    public CustomFormBaseVariable(Player player) {
        super(player);
    }

    @Override
    public void strReplace() {
        for (Map.Entry<String, Map<String, Object>> entry : CustomFormMain.specificConfCaches.entrySet()) {
            String fileName = entry.getKey();
            this.addStrReplaceString("{specific_cache_" + fileName + "}", entry.getValue().getOrDefault(this.player.getName(), 0).toString());
        }

        Map<String, Object> playerConfMap = CustomFormMain.playerConfCaches.getOrDefault(player.getName(), new LinkedHashMap<>());
        for (Map.Entry<String, Object> entry1 : playerConfMap.entrySet()) {
            String keyName = entry1.getKey();
            Object value = entry1.getValue();
            this.addStrReplaceString("{player_cache_" + keyName + "}", value.toString());
        }
    }
}
