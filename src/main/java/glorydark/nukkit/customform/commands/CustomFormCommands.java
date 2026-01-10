package glorydark.nukkit.customform.commands;

import cn.nukkit.Player;
import cn.nukkit.Server;
import cn.nukkit.command.Command;
import cn.nukkit.command.CommandSender;
import cn.nukkit.form.element.*;
import cn.nukkit.item.Item;
import cn.nukkit.lang.TranslationContainer;
import cn.nukkit.level.Location;
import cn.nukkit.scheduler.Task;
import cn.nukkit.utils.Config;
import cn.nukkit.utils.ConfigSection;
import cn.nukkit.utils.TextFormat;
import gameapi.form.AdvancedFormWindowCustom;
import glorydark.nukkit.customform.CustomFormMain;
import glorydark.nukkit.customform.Language;
import glorydark.nukkit.customform.chestForm.ChestFormMain;
import glorydark.nukkit.customform.factory.FormCreator;
import glorydark.nukkit.customform.hopperform.HopperFormMain;
import glorydark.nukkit.customform.minecartChestMenu.MinecartChestMenuMain;
import glorydark.nukkit.customform.script.CustomFormScriptManager;
import glorydark.nukkit.customform.scriptForms.form.ScriptForm;
import glorydark.nukkit.customform.utils.InventoryUtils;

import java.io.File;
import java.util.LinkedHashMap;

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
        Player player = commandSender.asPlayer();
        switch (strings[0].toLowerCase()) {
            case "test":
                if (commandSender.isOp()) {
                    AdvancedFormWindowCustom custom = new AdvancedFormWindowCustom("Test Form");
                    custom.addElement(new ElementInput("Input"));
                    custom.addElement(new ElementToggle("Toggle"));
                    custom.addElement(new ElementDropdown("DropDown"));
                    custom.addElement(new ElementSlider("Slider", 1, 2));
                    custom.addElement(new ElementStepSlider("Step Slider"));
                    custom.showToPlayer(commandSender.asPlayer());
                }
                break;
            case "giveitem":
                if (!commandSender.isOp()) {
                    return false;
                }
                if (strings.length != 4) {
                    return false;
                }
                String giveToPlayerName = strings[1];
                String itemId = strings[2];
                int count = Integer.parseInt(strings[3]);
                Item item = InventoryUtils.toItem(itemId);
                if (item.getId() == 0) {
                    commandSender.sendMessage("Cannot build item from nbt string/save id: " + itemId);
                    return false;
                }
                item.setCount(count);
                Player player1 = Server.getInstance().getPlayer(giveToPlayerName);
                if (player1 == null) {
                    commandSender.sendMessage(TextFormat.RED + "Fail to find player: " + giveToPlayerName);
                    return false;
                }
                player1.getInventory().addItem(item);
                commandSender.sendMessage(TextFormat.GREEN + "Give {" + item.getName() + "} to {" + player + "}, item nbt/save id {" + itemId + "}");
                break;
            case "reload":
                if (commandSender.isOp() || !commandSender.isPlayer()) {
                    Config config = new Config(CustomFormMain.path + "/config.yml", Config.YAML);
                    CustomFormMain.debug = config.getBoolean("debug", false); // todo
                    CustomFormMain.enableDoubleCheckMenu = config.getBoolean("enable_doubleCheckMenu", true);
                    CustomFormMain.enableCameraAnimation = config.getBoolean("enable_cameraAnimation", false);
                    CustomFormMain.coolDownMillis = config.getLong("coolDown", 200L);
                    CustomFormMain.language = new Language(config.getString("default_lang", "zh_cn"), CustomFormMain.path + "/languages/");
                    CustomFormMain.playerCacheVariableList = config.get("player_cache_variables", new LinkedHashMap<>());
                    CustomFormMain.specificCacheVariableList = config.get("specific_cache_variables", new LinkedHashMap<>());
                    CustomFormMain.plugin.loadAll();
                    commandSender.sendMessage(CustomFormMain.language.translateString(commandSender.isPlayer() ? (Player) commandSender : null, "plugin.configuration.reload"));
                } else {
                    commandSender.sendMessage(new TranslationContainer(TextFormat.RED + "%commands.generic.unknown", s));
                }
                break;
            case "whitelist": // whitelist add 菜单名 玩家名
                if (!commandSender.isOp()) {
                    return false;
                }
                if (strings.length == 4) {
                    String formId = strings[2];
                    String playerName = strings[3];
                    ScriptForm scriptForm = FormCreator.formScripts.get(formId);
                    if (scriptForm == null) {
                        commandSender.sendMessage(CustomFormMain.language.translateString(null, "command.form.not_found", formId));
                        return false;
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
                    if (config == null) {
                        commandSender.sendMessage(TextFormat.RED + "Unable to find form: " + formId);
                        return false;
                    }
                    switch (strings[1]) {
                        case "add": {
                            if (!scriptForm.getOpenPermissionWhitelist().contains(playerName)) {
                                scriptForm.getOpenPermissionWhitelist().add(playerName);
                            }
                            ConfigSection section = config.getSection("open_permissions");
                            section.set("whitelist", scriptForm.getOpenPermissionWhitelist());
                            config.set("open_permissions", section);
                            config.save();
                            commandSender.sendMessage(CustomFormMain.language.translateString(null, "command.whitelist.add.success", playerName, formId));
                        }
                            break;
                        case "remove":
                            scriptForm.getOpenPermissionWhitelist().remove(playerName);
                            ConfigSection section = config.getSection("open_permissions");
                            section.set("whitelist", scriptForm.getOpenPermissionWhitelist());
                            config.set("open_permissions", section);
                            config.save();
                            commandSender.sendMessage(CustomFormMain.language.translateString(null, "command.whitelist.remove.success", playerName, formId));
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
                        player = Server.getInstance().getPlayer(strings[1]);
                        if (player != null) {
                            FormCreator.showScriptForm(player, strings[2], true);
                        } else {
                            commandSender.sendMessage(CustomFormMain.language.translateString(null, "command.player_not_found", strings[1]));
                        }
                    } else {
                        commandSender.sendMessage(CustomFormMain.language.translateString(null, "command.can_not_use_in_game"));
                    }
                }
                break;
            case "showhopperform":
                if (!CustomFormMain.ready) {
                    return false;
                }
                if (commandSender.isPlayer()) {
                    if (strings.length >= 2) {
                        int delay = strings.length == 3? Integer.parseInt(strings[2]): 0;
                        if (delay == 0) {
                            HopperFormMain.showToPlayer(player, strings[1]);
                        } else {
                            final Player playerCopy = player;
                            Server.getInstance().getScheduler().scheduleDelayedTask(CustomFormMain.plugin, new Task() {
                                @Override
                                public void onRun(int i) {
                                    HopperFormMain.showToPlayer(playerCopy, strings[1]);
                                }
                            }, delay);
                        }
                    } else {
                        commandSender.sendMessage(TextFormat.RED + "Usage: /form showhopperform <formId>");
                    }
                } else {
                    commandSender.sendMessage(CustomFormMain.language.translateString(null, "command.can_not_use_in_game", strings[1]));
                }
                break;
            case "showchestform":
                if (!CustomFormMain.ready) {
                    return false;
                }
                if (commandSender.isPlayer()) {
                    if (strings.length >= 2) {
                        int delay = strings.length == 3? Integer.parseInt(strings[2]): 0;
                        if (delay == 0) {
                            ChestFormMain.showToPlayer(player, strings[1]);
                        } else {
                            final Player playerCopy = player;
                            Server.getInstance().getScheduler().scheduleDelayedTask(CustomFormMain.plugin, new Task() {
                                @Override
                                public void onRun(int i) {
                                    ChestFormMain.showToPlayer(playerCopy, strings[1]);
                                }
                            }, delay);
                        }
                    } else {
                        commandSender.sendMessage(TextFormat.RED + "Usage: /form showchestform <formId>");
                    }
                } else {
                    commandSender.sendMessage(CustomFormMain.language.translateString(null, "command.can_not_use_in_game", strings[1]));
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
                    commandSender.sendMessage(CustomFormMain.language.translateString(null, "command.can_not_use_in_game", strings[1]));
                }
                break;
            case "list":
                commandSender.sendMessage(FormCreator.formScripts.keySet().size() + " form scripts loaded: ");
                for (String string : FormCreator.formScripts.keySet()) {
                    commandSender.sendMessage("- " + string);
                }
                break;
            case "executewithdelay":
                if (!commandSender.isOp()) {
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
                if (!commandSender.isOp()) {
                    return false;
                }
                if (strings.length == 2) {
                    for (Player onlinePlayer : Server.getInstance().getOnlinePlayers().values()) {
                        onlinePlayer.sendMessage(strings[1].replace("{空格}", " "));
                    }
                    CustomFormMain.plugin.getLogger().info(strings[1].replace("{空格}", " "));
                }
                break;
            case "savenbt":
                if (!commandSender.isOp()) {
                    return false;
                }
                if (strings.length == 2) {
                    Config config = new Config(CustomFormMain.path + "/save_nbt_cache.yml", Config.YAML);
                    config.set(strings[1], InventoryUtils.saveItemToString(((Player) commandSender).getInventory().getItemInHand()));
                    config.save();
                    commandSender.sendMessage("Save item string successfully!");
                }
                break;
            case "tp": // tp test 0 1 0 world 2 2 2
                if (!commandSender.isOp()) {
                    return false;
                }
                player = Server.getInstance().getPlayer(strings[1]);
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
            case "script":
                if (strings.length < 2) {
                    return false;
                }
                CustomFormScriptManager.executeLoadedScript(commandSender.asPlayer(), strings[1]);
                break;
        }
        return true;
    }
}
