package glorydark.nukkit.customform.commands;

import cn.nukkit.Player;
import cn.nukkit.Server;
import cn.nukkit.command.Command;
import cn.nukkit.command.CommandSender;
import cn.nukkit.lang.TranslationContainer;
import cn.nukkit.level.Location;
import cn.nukkit.scheduler.Task;
import cn.nukkit.utils.Config;
import cn.nukkit.utils.ConfigSection;
import cn.nukkit.utils.TextFormat;
import glorydark.nukkit.customform.CustomFormMain;
import glorydark.nukkit.customform.chestForm.ChestFormMain;
import glorydark.nukkit.customform.minecartChestMenu.MinecartChestMenuMain;
import glorydark.nukkit.customform.forms.FormCreator;
import glorydark.nukkit.customform.scriptForms.form.ScriptForm;
import glorydark.nukkit.customform.utils.InventoryUtils;

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
                    CustomFormMain.plugin.loadAll();
                    commandSender.sendMessage(CustomFormMain.language.translateString(commandSender.isPlayer() ? (Player) commandSender : null, "plugin_reloaded"));
                } else {
                    commandSender.sendMessage(new TranslationContainer(TextFormat.RED + "%commands.generic.unknown", s));
                }
                break;
            case "whitelist": // whitelist add 菜单名 玩家名
                if (strings.length == 4) {
                    String formId = strings[2];
                    String playerName = strings[3];
                    switch (strings[1]) {
                        case "add":
                            ScriptForm scriptForm = FormCreator.formScripts.get(formId);
                            if (scriptForm == null) {
                                commandSender.sendMessage(CustomFormMain.language.translateString(null, "command.form.not_found", formId));
                            } else {
                                if (!scriptForm.getOpenPermissionWhitelist().contains(playerName)) {
                                    scriptForm.getOpenPermissionWhitelist().add(playerName);
                                }
                                Config config = null;
                                File file = new File(CustomFormMain.path + "/forms/" + formId + ".json");
                                if (file.exists()) {
                                    config = new Config(file, Config.JSON);
                                } else {
                                    file = new File(CustomFormMain.path + "/forms/" + formId + ".yml");
                                    if (file.exists()) {
                                        config = new Config(file, Config.YAML);
                                    }
                                }
                                if (config != null) {
                                    ConfigSection section = config.getSection("open_permissions");
                                    section.set("whitelist", scriptForm.getOpenPermissionWhitelist());
                                    config.set("open_permissions", section);
                                    config.save();
                                    commandSender.sendMessage(CustomFormMain.language.translateString(null, "command.whitelist.add.success", playerName, formId));
                                } else {
                                    commandSender.sendMessage(TextFormat.RED + "Found an exception in finding file: " + formId);
                                }
                            }
                            break;
                        case "remove":
                            scriptForm = FormCreator.formScripts.get(formId);
                            if (scriptForm == null) {
                                commandSender.sendMessage(CustomFormMain.language.translateString(null, "command.form.not_found", formId));
                            } else {
                                scriptForm.getOpenPermissionWhitelist().remove(playerName);
                                Config config = null;
                                File file = new File(CustomFormMain.path + "/forms/" + formId + ".json");
                                if (file.exists()) {
                                    config = new Config(file, Config.JSON);
                                } else {
                                    file = new File(CustomFormMain.path + "/forms/" + formId + ".yml");
                                    if (file.exists()) {
                                        config = new Config(file, Config.YAML);
                                    }
                                }
                                if (config != null) {
                                    ConfigSection section = config.getSection("open_permissions");
                                    section.set("whitelist", scriptForm.getOpenPermissionWhitelist());
                                    config.set("open_permissions", section);
                                    config.save();
                                    commandSender.sendMessage(CustomFormMain.language.translateString(null, "command.whitelist.remove.success", playerName, formId));
                                } else {
                                    commandSender.sendMessage(TextFormat.RED + "Found an exception in finding file: " + formId);
                                }
                            }
                            break;
                    }
                }
                break;
            case "show":
                if (!CustomFormMain.ready) {
                    return false;
                }
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
                    } else {
                        commandSender.sendMessage(CustomFormMain.language.translateString(null, "command_use_in_game"));
                    }
                }
                break;
            case "showchestform":
                if (!CustomFormMain.ready) {
                    return false;
                }
                if (commandSender.isPlayer()) {
                    if (strings.length == 2) {
                        Server.getInstance().getScheduler().scheduleDelayedTask(CustomFormMain.plugin, new Task() {
                            @Override
                            public void onRun(int i) {
                                ChestFormMain.showToPlayer((Player) commandSender, strings[1]);
                            }
                        }, 10);
                    } else {
                        commandSender.sendMessage(TextFormat.RED + "Unable to open form: " + strings[1]);
                    }
                } else {
                    commandSender.sendMessage(CustomFormMain.language.translateString(null, "command_use_in_game", strings[1]));
                }
                break;
            case "showminecartmenu":
                if (!CustomFormMain.ready) {
                    return false;
                }
                if (commandSender.isPlayer()) {
                    if (strings.length == 2) {
                        MinecartChestMenuMain.showMinecartChestMenu((Player) commandSender, strings[1]);
                    }
                } else {
                    commandSender.sendMessage(CustomFormMain.language.translateString(null, "command_use_in_game", strings[1]));
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
                if (strings.length == 4) { // form executewithdelay console xxx
                    CommandSender sender;
                    if ("console".equals(strings[1])) {
                        sender = Server.getInstance().getConsoleSender();
                    } else {
                        sender = Server.getInstance().getPlayer(strings[1]);
                    }
                    Server.getInstance().getScheduler().scheduleDelayedTask(CustomFormMain.plugin, () -> Server.getInstance().dispatchCommand(sender, strings[2]), Integer.parseInt(strings[3]));
                }
                break;
            case "broadcastmsg":
                if (commandSender.isPlayer() && !commandSender.isOp()) {
                    return false;
                }
                if (strings.length == 2) {
                    for (Player player : Server.getInstance().getOnlinePlayers().values()) {
                        player.sendMessage(strings[1].replace("{空格}", " "));
                    }
                    CustomFormMain.plugin.getLogger().info(strings[1].replace("{空格}", " "));
                }
                break;
            case "savenbt":
                if (strings.length == 2) {
                    if (commandSender.isPlayer() && commandSender.isOp()) {
                        Config config = new Config(CustomFormMain.path + "/save_nbt_cache.yml", Config.YAML);
                        config.set(strings[1], InventoryUtils.saveItemToString(((Player) commandSender).getInventory().getItemInHand()));
                        config.save();
                        commandSender.sendMessage("Save item string successfully!");
                    }
                }
                break;
            case "tp": // tp test 0 1 0 world 2 2 2
                if (commandSender.isPlayer() && !commandSender.isOp()) {
                    return false;
                }
                Player player = Server.getInstance().getPlayer(strings[1]);
                if (player == null) {
                    commandSender.sendMessage("Can not find player!");
                    return false;
                }
                if (strings.length >= 5) {
                    switch (strings.length) {
                        case 5:
                            player.teleport(new Location(Double.parseDouble(strings[2]), Double.parseDouble(strings[3]), Double.parseDouble(strings[4]), player.getLevel()));
                            break;
                        case 6:
                            player.teleport(new Location(Double.parseDouble(strings[2]), Double.parseDouble(strings[3]), Double.parseDouble(strings[4]), Server.getInstance().getLevelByName(strings[5])));
                            break;
                        case 9:
                            player.teleport(new Location(Double.parseDouble(strings[2]), Double.parseDouble(strings[3]), Double.parseDouble(strings[4]), Double.parseDouble(strings[5]), Double.parseDouble(strings[6]), Double.parseDouble(strings[7]), Server.getInstance().getLevelByName(strings[8])));
                            break;
                    }
                }
                break;
        }
        return true;
    }
}
