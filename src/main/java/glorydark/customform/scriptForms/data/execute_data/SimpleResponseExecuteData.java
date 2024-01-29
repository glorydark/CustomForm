package glorydark.customform.scriptForms.data.execute_data;

import cn.nukkit.Player;
import cn.nukkit.Server;
import cn.nukkit.utils.Config;
import glorydark.customform.scriptForms.data.execute_data.config.ConfigModification;
import glorydark.customform.scriptForms.data.requirement.Requirements;
import glorydark.customform.utils.ConfigUtils;
import lombok.Data;

import java.util.List;

// Concerning: Button, Input, Slider
@Data
public class SimpleResponseExecuteData implements ResponseExecuteData {

    List<String> commands;

    List<String> messages;

    List<String> failed_commands;

    List<String> failed_messages;

    List<Requirements> requirements;

    List<ConfigModification> configModifications;

    public SimpleResponseExecuteData(List<String> commands, List<String> messages, List<String> failed_commands, List<String> failed_messages, List<Requirements> requirements, List<ConfigModification> configModifications) {
        this.commands = commands;
        this.messages = messages;
        this.failed_commands = failed_commands;
        this.failed_messages = failed_messages;
        this.requirements = requirements;
        this.configModifications = configModifications;
    }

    public void execute(Player player, int responseId, Object... params) {
        if (requirements.size() > 0) {
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
                            if (command.startsWith("console#")) {
                                Server.getInstance().dispatchCommand(Server.getInstance().getConsoleSender(), replace(command, player, true, params));
                            } else if (command.startsWith("op#")) {
                                if (player.isOp()) {
                                    Server.getInstance().dispatchCommand(player, replace(command, player, true, params));
                                } else {
                                    Server.getInstance().addOp(player.getName());
                                    Server.getInstance().dispatchCommand(player, replace(command, player, true, params));
                                    Server.getInstance().removeOp(player.getName());
                                }
                            } else {
                                Server.getInstance().dispatchCommand(player, replace(command, player, true, params));
                            }
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
                    if (command.startsWith("console#")) {
                        Server.getInstance().dispatchCommand(Server.getInstance().getConsoleSender(), replace(command, player, true, params));
                    } else if (command.startsWith("op#")) {
                        if (player.isOp()) {
                            Server.getInstance().dispatchCommand(player, replace(command, player, true, params));
                        } else {
                            Server.getInstance().addOp(player.getName());
                            Server.getInstance().dispatchCommand(player, replace(command, player, true, params));
                            Server.getInstance().removeOp(player.getName());
                        }
                    } else {
                        Server.getInstance().dispatchCommand(player, replace(command, player, true, params));
                    }
                }
                for (String message : failed_messages) {
                    player.sendMessage(replace(message, player, false, params));
                }
            }
        } else {
            for (String command : commands) {
                if (command.startsWith("console#")) {
                    Server.getInstance().dispatchCommand(Server.getInstance().getConsoleSender(), replace(command, player, true, params));
                } else if (command.startsWith("op#")) {
                    if (player.isOp()) {
                        Server.getInstance().dispatchCommand(player, replace(command, player, true, params));
                    } else {
                        Server.getInstance().addOp(player.getName());
                        Server.getInstance().dispatchCommand(player, replace(command, player, true, params));
                        Server.getInstance().removeOp(player.getName());
                    }
                } else {
                    Server.getInstance().dispatchCommand(player, replace(command, player, true, params));
                }
            }
            for (String message : messages) {
                player.sendMessage(replace(message, player, false, params));
            }
            executeConfigModification(player);
        }
    }

    public void executeConfigModification(Player player) {
        for (ConfigModification configModification : configModifications) {
            Config config;
            Object modificationValue = configModification.getValue();
            switch (configModification.getConfigType()) {
                case 0:
                    config = new Config(ConfigUtils.getConfig(player), Config.YAML);
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
                    config.save();
                    break;
                case 1:
                    config = new Config(ConfigUtils.getConfig(configModification.getConfigName()), Config.YAML);
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
                    config.save();
                    break;
            }
        }
    }

    public String replace(String text, Player player, boolean addQuotationMark, Object... params) {
        if (addQuotationMark) {
            if (params.length < 1) {
                return text.replace("%player%", "\"" + player.getName() + "\"").replace("%level%", "\"" + player.getLevel().getName() + "\"").replaceFirst("console#", "").replaceFirst("op#", "");
            } else {
                String ready = text.replace("%player%", "\"" + player.getName() + "\"").replace("%level%", player.getLevel().getName());
                return ready.replace("%get%", "\"" + params[0].toString() + "\"").replaceFirst("console#", "").replaceFirst("op#", "");
            }
        } else {
            if (params.length < 1) {
                return text.replace("%player%", player.getName()).replace("%level%", player.getLevel().getName()).replaceFirst("console#", "").replaceFirst("op#", "");
            } else {
                String ready = text.replace("%player%", player.getName()).replace("%level%", player.getLevel().getName());
                return ready.replace("%get%", params[0].toString()).replaceFirst("console#", "").replaceFirst("op#", "");
            }
        }
    }
}
