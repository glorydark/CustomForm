package glorydark.nukkit.customform.script;

import cn.nukkit.Player;
import cn.nukkit.command.ConsoleCommandSender;
import cn.nukkit.network.protocol.PlaySoundPacket;
import glorydark.dcurrency.CurrencyAPI;
import glorydark.nukkit.customform.CustomFormMain;
import me.onebone.economyapi.EconomyAPI;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

public class ScriptPlayerAPI {

    private static final Map<String, Object> globalVariables = new LinkedHashMap<>();

    private static final Map<String, Map<String, Object>> playerVariables = new LinkedHashMap<>();

    public ScriptPlayerAPI() {

    }

    public static void resetVariables() {
        globalVariables.clear();
        playerVariables.clear();
    }

    public static <T> T getGlobalVariable(String key, T value) {
        return (T) globalVariables.getOrDefault(key, value);
    }

    public static void setGlobalVariable(String key, Object value) {
        globalVariables.put(key, value);
    }

    public static void removeGlobalVariable(String key) {
        globalVariables.remove(key);
    }

    public static <T> T getPlayerVariable(Player player, String key, T value) {
        return (T) playerVariables.getOrDefault(player.getName(), new LinkedHashMap<>()).getOrDefault(key, value);
    }

    public static void setPlayerVariable(Player player, String key, Object value) {
        playerVariables.computeIfAbsent(player.getName(), (s) -> new LinkedHashMap<>()).put(key, value);
    }

    public static void removePlayerVariable(Player player, String key) {
        playerVariables.getOrDefault(player.getName(), new LinkedHashMap<>()).remove(key);
    }

    public static String[] newStringArray(int size) {
        return new String[size];
    }
}