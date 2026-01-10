package glorydark.nukkit.customform.script;

import cn.nukkit.Player;
import cn.nukkit.command.ConsoleCommandSender;
import cn.nukkit.network.protocol.PlaySoundPacket;
import glorydark.dcurrency.CurrencyAPI;
import glorydark.nukkit.customform.CustomFormMain;
import me.onebone.economyapi.EconomyAPI;

import java.util.Collection;

public class ScriptPlayerAPI {

    public ScriptPlayerAPI() {

    }

    public double getMoney(Player p) {
        return EconomyAPI.getInstance().myMoney(p);
    }

    public double getCustomCurrency(Player p, String currencyName) {
        return CurrencyAPI.getCurrencyBalance(p, currencyName, 0);
    }

    public void reduceMoney(Player p, double v) {
        EconomyAPI.getInstance().reduceMoney(p, v);
    }

    public void addMoney(Player p, double v) {
        EconomyAPI.getInstance().addMoney(p, v);
    }

    public void reduceCustomCurrency(Player p, String currencyName, double v) {
        CurrencyAPI.reduceCurrencyBalance(p, currencyName, v);
    }

    public void addCustomCurrency(Player p, String currencyName, double v) {
        CurrencyAPI.addCurrencyBalance(p, currencyName, v);
    }

    public void sendMessage(Player p, String msg) {
        p.sendMessage(msg);
    }

    public void sendTip(Player p, String msg) {
        p.sendTip(msg);
    }

    public void sendActionbar(Player p, String msg) {
        p.sendActionBar(msg);
    }

    public void sendActionbar(Player p, String msg, int fadeIn, int duration, int fadeOut) {
        p.sendActionBar(msg, fadeIn, duration, fadeOut);
    }

    public void sendToast(Player p, String title, String content) {
        p.sendToast(title, content);
    }

    public void sendTitle(Player p, String title, String subtitle) {
        p.sendTitle(title, subtitle);
    }

    public void playSound(Player p, String soundIdentifier) {
        this.playSound(p, soundIdentifier, 1f, 1f);
    }

    public void playSound(Player p, String soundIdentifier, float pitch, float volume) {
        PlaySoundPacket pk = new PlaySoundPacket();
        pk.name = soundIdentifier;
        pk.pitch = pitch;
        pk.volume = volume;
        pk.x = p.getFloorX();
        pk.y = p.getFloorY();
        pk.z = p.getFloorZ();
        p.dataPacket(pk);
    }

    public void runCommandByConsole(String command) {
        CustomFormMain.plugin.getServer().dispatchCommand(new ConsoleCommandSender(), command);
    }

    public void runCommandByPlayer(Player player, String command) {
        CustomFormMain.plugin.getServer().dispatchCommand(player, command);
    }

    public static int getOnlineCount() {
        return CustomFormMain.plugin.getServer().getOnlinePlayers().size();
    }

    public static String getPlayerName(Player player) {
        return player.getName();
    }

    public static boolean hasPermission(Player player, String permission) {
        return player.hasPermission(permission);
    }

    public static Collection<Player> getOnlinePlayers() {
        return CustomFormMain.plugin.getServer().getOnlinePlayers().values();
    }

    public static double distance(Player player1, Player player2) {
        return player1.distance(player2);
    }

    public static String getLevelName(Player player) {
        return player.getLevelName();
    }
}