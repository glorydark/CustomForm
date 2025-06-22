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
        Map<String, Object> playerConfMap = CustomFormMain.playerConfCaches.getOrDefault(this.player.getName(), new LinkedHashMap<>());
        for (String keyName : CustomFormMain.playerCacheVariableList.keySet()) {
            this.addStrReplaceString("{form_player_cache_" + keyName + "}", playerConfMap.getOrDefault(keyName, String.valueOf(CustomFormMain.playerCacheVariableList.getOrDefault(keyName, "null"))).toString());
        }

        for (String keyName : CustomFormMain.specificCacheVariableList.keySet()) {
            Map<String, Object> specificConfMap = CustomFormMain.specificConfCaches.getOrDefault(keyName, new LinkedHashMap<>());
            this.addStrReplaceString("{form_specific_cache_" + keyName + "}", specificConfMap.getOrDefault(player.getName(), String.valueOf(CustomFormMain.specificCacheVariableList.getOrDefault(keyName, "null"))).toString());
        }
    }
}
