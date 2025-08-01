package glorydark.nukkit.customform.scriptForms.data.execute_data;

import cn.nukkit.Player;
import cn.nukkit.utils.Config;
import glorydark.nukkit.customform.CustomFormMain;
import glorydark.nukkit.customform.scriptForms.data.execute_data.config.ConfigModification;
import glorydark.nukkit.customform.scriptForms.data.requirement.Requirements;
import glorydark.nukkit.customform.utils.CommandUtils;
import glorydark.nukkit.customform.utils.ConfigUtils;
import lombok.Data;

import java.util.Date;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

// Concerning: Button, Input, Slider
@Data
public class SimpleResponseExecuteData implements ResponseExecuteData {

    List<String> commands;

    List<String> messages;

    List<List<String>> randomCommands;

    List<String> failed_commands;

    List<String> failed_messages;

    List<Requirements> requirements;

    List<ConfigModification> configModifications;

    Date startDate = new Date(-1);

    Date expiredDate = new Date(-1);

    public SimpleResponseExecuteData(List<String> commands, List<String> messages, List<String> failed_commands, List<String> failed_messages, List<List<String>> randomCommands, List<Requirements> requirements, List<ConfigModification> configModifications) {
        this.commands = commands;
        this.messages = messages;
        this.failed_commands = failed_commands;
        this.failed_messages = failed_messages;
        this.randomCommands = randomCommands;
        this.requirements = requirements;
        this.configModifications = configModifications;
    }

    public void execute(Player player, int responseId, Object... params) {
        if (!this.isInStartDate(player)) {
            return;
        }
        if (!requirements.isEmpty()) {
            boolean success = false;
            int multiply = 1;
            try {
                if (params.length > 0) {
                    multiply = -1;
                    String stringInput = ((String) params[0]).replace("\"", "");
                    if (stringInput.contains(".")) {
                        multiply = Float.floatToIntBits(Float.parseFloat(stringInput));
                    } else {
                        multiply = Integer.parseInt(stringInput + ".0");
                    }
                }
            } catch (NumberFormatException | ClassCastException ignored) {
            }
            if (multiply <= 0) {
                player.sendMessage("§c填入格式不符：请输入正确的数量！");
                return;
            }
            for (Requirements one : requirements) {
                if (one.isAllQualified(player, multiply)) {
                    success = true;
                    one.reduceAllCosts(player, multiply);
                    for (int time = 0; time < multiply; time++) {
                        one.executeSuccessCommand(player);
                        for (String command : commands) {
                            CommandUtils.executeCommand(player, replace(command, player, true));
                        }
                        for (String message : messages) {
                            player.sendMessage(replace(message, player, false, params));
                        }
                    }
                    executeConfigModification(player);
                    break;
                } else {
                    one.executeFailedCommand(player);
                }
            }
            if (!success) {
                for (String command : failed_commands) {
                    CommandUtils.executeCommand(player, replace(command, player, true));
                }
                for (String message : failed_messages) {
                    player.sendMessage(replace(message, player, false, params));
                }
            }
        } else {
            for (String command : commands) {
                CommandUtils.executeCommand(player, replace(command, player, true));
            }
            for (String message : messages) {
                player.sendMessage(replace(message, player, false, params));
            }
            executeConfigModification(player);
        }
        for (List<String> randomCommand : randomCommands) {
            CommandUtils.executeCommand(player, replace(randomCommand.get(ThreadLocalRandom.current().nextInt(randomCommand.size())), player, true));
        }
    }

    public void executeConfigModification(Player player) {
        for (ConfigModification configModification : configModifications) {
            Config config;
            Object modificationValue = configModification.getValue();
            switch (configModification.getConfigType()) {
                case 0:
                    config = new Config(ConfigUtils.getPlayerConfigCacheFile(player), Config.YAML);
                    String keyName = configModification.getKeyName();
                    switch (configModification.getType()) {
                        case ADD:
                            Object param = config.get(keyName);
                            if (param == null) {
                                param = 0;
                            }
                            if (modificationValue instanceof Double) {
                                double convertedParam = (double) param;
                                config.set(keyName, convertedParam + Double.parseDouble(modificationValue.toString()));
                            } else if (modificationValue instanceof Integer) {
                                int convertedParam = (int) param;
                                config.set(keyName, convertedParam + Integer.parseInt(modificationValue.toString()));
                            }
                            break;
                        case SET:
                            config.set(keyName, config.get(keyName));
                            break;
                        case DEDUCT:
                            param = config.get(player.getName());
                            if (param == null) {
                                param = 0;
                            }
                            if (modificationValue instanceof Double) {
                                double convertedParam = (double) param;
                                config.set(keyName, convertedParam - Double.parseDouble(modificationValue.toString()));
                            } else if (modificationValue instanceof Integer) {
                                int convertedParam = (int) param;
                                config.set(keyName, convertedParam - Integer.parseInt(modificationValue.toString()));
                            }
                            break;
                        case REMOVE:
                            config.remove(keyName);
                            break;
                    }
                    CustomFormMain.playerConfCaches.put(player.getName(), config.getAll());
                    config.save();
                    break;
                case 1:
                    config = new Config(ConfigUtils.getSpecificConfigCacheFile(configModification.getConfigName()), Config.YAML);
                    keyName = player.getName();
                    switch (configModification.getType()) {
                        case ADD:
                            Object param = config.get(keyName);
                            if (param == null) {
                                param = 0;
                            }
                            if (modificationValue instanceof Double) {
                                double convertedParam = (double) param;
                                config.set(keyName, convertedParam + Double.parseDouble(modificationValue.toString()));
                            } else if (modificationValue instanceof Integer) {
                                int convertedParam = (int) param;
                                config.set(keyName, convertedParam + Integer.parseInt(modificationValue.toString()));
                            }
                            break;
                        case SET:
                            config.set(keyName, config.get(keyName));
                            break;
                        case DEDUCT:
                            param = config.get(player.getName());
                            if (param == null) {
                                param = 0;
                            }
                            if (modificationValue instanceof Double) {
                                double convertedParam = (double) param;
                                config.set(keyName, convertedParam - Double.parseDouble(modificationValue.toString()));
                            } else if (modificationValue instanceof Integer) {
                                int convertedParam = (int) param;
                                config.set(keyName, convertedParam - Integer.parseInt(modificationValue.toString()));
                            }
                            break;
                        case REMOVE:
                            config.remove(keyName);
                            break;
                    }
                    CustomFormMain.specificConfCaches.put(configModification.getConfigName(), config.getAll());
                    config.save();
                    break;
            }
        }
    }

    public String replace(String text, Player player, boolean addQuotationMark, Object... params) {
        if (addQuotationMark) {
            if (params.length < 1) {
                return text.replace("%player%", "\"" + player.getName() + "\"")
                        .replace("{player}", "\"" + player.getName() + "\"")
                        .replace("%level%", "\"" + player.getLevel().getName() + "\"");
            } else {
                String ready = text.replace("%player%", "\"" + player.getName() + "\"")
                        .replace("{player}", "\"" + player.getName() + "\"")
                        .replace("%level%", player.getLevel().getName());
                return ready.replace("%get%", String.valueOf(params[0]));
            }
        } else {
            if (params.length < 1) {
                return text.replace("%player%", player.getName())
                        .replace("{player}", player.getName())
                        .replace("%level%", player.getLevel().getName());
            } else {
                String ready = text.replace("%player%", player.getName())
                        .replace("{player}", player.getName())
                        .replace("%level%", player.getLevel().getName());
                return ready.replace("%get%", String.valueOf(params[0]));
            }
        }
    }
}
