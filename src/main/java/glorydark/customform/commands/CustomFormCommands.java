package glorydark.customform.commands;

import cn.nukkit.Player;
import cn.nukkit.Server;
import cn.nukkit.command.Command;
import cn.nukkit.command.CommandSender;
import cn.nukkit.lang.TranslationContainer;
import cn.nukkit.utils.Config;
import cn.nukkit.utils.TextFormat;
import glorydark.customform.CustomFormMain;
import glorydark.customform.chestMenu.ChestMenuMain;
import glorydark.customform.forms.FormCreator;
import glorydark.customform.utils.InventoryUtils;

import java.io.File;

/**
 * @author glorydark
 */
public class CustomFormCommands extends Command {
    public CustomFormCommands() {
        super("form");
    }

    @Override
    public boolean execute(CommandSender commandSender, String s, String[] strings) {
        if (strings.length == 0) {
            return false;
        }
        switch (strings[0].toLowerCase()) {
            case "reload":
                if (commandSender.isOp() || !commandSender.isPlayer()) {
                    CustomFormMain.plugin.loadScriptWindows(new File(CustomFormMain.path + "/forms/"));
                    CustomFormMain.plugin.loadScriptMineCartWindows(new File(CustomFormMain.path + "/minecart_chest_windows/"));
                    commandSender.sendMessage(CustomFormMain.language.translateString(commandSender.isPlayer() ? (Player) commandSender : null, "plugin_reloaded"));
                } else {
                    commandSender.sendMessage(new TranslationContainer(TextFormat.RED + "%commands.generic.unknown", s));
                }
                break;
            case "show":
                if (commandSender.isPlayer()) {
                    if (strings.length == 2) {
                        FormCreator.showScriptForm((Player) commandSender, strings[1], false);
                    }
                } else {
                    if (strings.length == 3) {
                        Player player = Server.getInstance().getPlayer(strings[1]);
                        if (player != null) {
                            FormCreator.showScriptForm(player, strings[2], true);
                        } else {
                            commandSender.sendMessage(CustomFormMain.language.translateString(null, "command_player_not_found", strings[1]));
                        }
                    }
                }
                break;
            case "showminecartmenu":
                if (commandSender.isPlayer()) {
                    if (strings.length == 2) {
                        ChestMenuMain.showMinecartChestMenu((Player) commandSender, strings[1]);
                    }
                } else {
                    commandSender.sendMessage(CustomFormMain.language.translateString(null, "command_player_not_found", strings[1]));
                }
                break;
            case "list":
                commandSender.sendMessage(FormCreator.formScripts.keySet().size() + " form scripts loaded: ");
                for (String string : FormCreator.formScripts.keySet()) {
                    commandSender.sendMessage("- " + string);
                }
                break;
            case "executewithdelay":
                if (commandSender.isPlayer() && !commandSender.isOp()) {
                    return false;
                }
                if (strings.length == 4) {
                    CommandSender sender;
                    if ("console".equals(strings[1])) {
                        sender = Server.getInstance().getConsoleSender();
                    } else {
                        sender = Server.getInstance().getPlayer(strings[1]);
                    }
                    Server.getInstance().getScheduler().scheduleDelayedTask(CustomFormMain.plugin, () -> Server.getInstance().dispatchCommand(sender, strings[2]), Integer.parseInt(strings[3]));
                }
                break;
            case "savenbt":
                if (commandSender.isPlayer()) {
                    Config config = new Config(CustomFormMain.path + "/save_nbt_cache.yml", Config.YAML);
                    config.set(strings[1], InventoryUtils.saveItemToString(((Player) commandSender).getInventory().getItemInHand()));
                }
        }
        return true;
    }
}
