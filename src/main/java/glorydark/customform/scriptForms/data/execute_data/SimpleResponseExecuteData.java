package glorydark.customform.scriptForms.data.execute_data;

import cn.nukkit.Player;
import cn.nukkit.Server;
import glorydark.customform.annotations.Developing;
import glorydark.customform.scriptForms.data.requirement.Requirements;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

// Concerning: Button, Input, Slider
@Data
public class SimpleResponseExecuteData implements ResponseExecuteData {
    List<String> commands;
    List<String> messages;
    List<String> failed_commands;
    List<String> failed_messages;
    List<Requirements> requirements;

    @Developing
    public SimpleResponseExecuteData(List<String> commands, List<String> messages, List<String> failed_commands, List<String> failed_messages, List<Requirements> requirements) {
        this.commands = commands;
        this.messages = messages;
        this.failed_commands = failed_commands;
        this.failed_messages = failed_messages;
        this.requirements = requirements;
    }

    public SimpleResponseExecuteData(List<String> commands, List<String> messages, List<String> failed_commands, List<String> failed_messages) {
        this(commands, messages, failed_commands, failed_messages, new ArrayList<>());
    }

    public void execute(Player player, int responseId, Object... params) {
        if(requirements.size() > 0) {
            boolean temp = false;
            int i = 1;
            int multiply = 1;
            try{
                if(params.length > 0) {
                    multiply = -1;
                    String stringInput = ((String) params[0]).replace("\"", "");
                    if(stringInput.contains(".")) {
                        multiply = Float.floatToIntBits(Float.parseFloat(stringInput));
                    }else {
                        multiply = Integer.parseInt(stringInput+".0");
                    }
                }
            }catch (NumberFormatException | ClassCastException ignored) {
            }
            if(multiply <= 0) {
                player.sendMessage("§c填入格式不符：请输入正确的数量！");
                return;
            }
            for(Requirements one: requirements) {
                if(one.isAllQualified(player, i, multiply)) {
                    temp = true;
                    one.reduceAllCosts(player, multiply);
                    for(int time=0; time<multiply; time++) {
                        one.executeSuccessCommand(player);
                        for (String command : commands) {
                            if (command.startsWith("console#")) {
                                Server.getInstance().dispatchCommand(Server.getInstance().getConsoleSender(), replace(command, player, params));
                            } else if(command.startsWith("op#")) {
                                if(player.isOp()) {
                                    Server.getInstance().dispatchCommand(player, replace(command, player, params));
                                }else{
                                    Server.getInstance().addOp(player.getName());
                                    Server.getInstance().dispatchCommand(player, replace(command, player, params));
                                    Server.getInstance().removeOp(player.getName());
                                }
                            } else {
                                Server.getInstance().dispatchCommand(player, replace(command, player, params));
                            }
                        }
                        for (String message : messages) {
                            player.sendMessage(replace(message, player, params));
                        }
                    }
                    break;
                }else{
                    one.executeFailedCommand(player);
                }
                i++;
            }
            if(!temp) {
                for(String command: failed_commands) {
                    if(command.startsWith("console#")) {
                        Server.getInstance().dispatchCommand(Server.getInstance().getConsoleSender(), replace(command, player, params));
                    } else if(command.startsWith("op#")) {
                        if(player.isOp()) {
                            Server.getInstance().dispatchCommand(player, replace(command, player, params));
                        }else{
                            Server.getInstance().addOp(player.getName());
                            Server.getInstance().dispatchCommand(player, replace(command, player, params));
                            Server.getInstance().removeOp(player.getName());
                        }
                    } else{
                        Server.getInstance().dispatchCommand(player, replace(command, player, params));
                    }
                }
                for(String message: failed_messages) {
                    player.sendMessage(replace(message, player, params));
                }
            }
        }else{
            for(String command: commands) {
                if(command.startsWith("console#")) {
                    Server.getInstance().dispatchCommand(Server.getInstance().getConsoleSender(), replace(command, player, params));
                } else if(command.startsWith("op#")) {
                    if(player.isOp()) {
                        Server.getInstance().dispatchCommand(player, replace(command, player, params));
                    }else{
                        Server.getInstance().addOp(player.getName());
                        Server.getInstance().dispatchCommand(player, replace(command, player, params));
                        Server.getInstance().removeOp(player.getName());
                    }
                } else{
                    Server.getInstance().dispatchCommand(player, replace(command, player, params));
                }
            }
            for(String message: messages) {
                player.sendMessage(replace(message, player, params));
            }
        }
    }

    public String replace(String text, Player player, Object... params) {
        if(params.length < 1) {
            return text.replace("%player%", player.getName()).replace("%level%", player.getLevel().getName()).replaceFirst("console#", "").replaceFirst("op#", "");
        }else{
            String ready = text.replace("%player%", player.getName()).replace("%level%", player.getLevel().getName());
            return ready.replace("%get%", String.valueOf(params[0])).replaceFirst("console#", "").replaceFirst("op#", "");
        }
    }
}
