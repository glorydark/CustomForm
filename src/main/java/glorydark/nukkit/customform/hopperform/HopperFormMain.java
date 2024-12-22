package glorydark.nukkit.customform.hopperform;

import cn.nukkit.Player;
import cn.nukkit.utils.TextFormat;
import gameapi.form.AdvancedHopperForm;
import gameapi.form.element.ResponsiveElementSlotItem;
import glorydark.nukkit.customform.CustomFormMain;
import glorydark.nukkit.customform.forms.FormCreator;
import glorydark.nukkit.customform.scriptForms.data.requirement.Requirements;
import glorydark.nukkit.customform.utils.CommandUtils;
import glorydark.nukkit.customform.utils.ReplaceStringUtils;

import java.io.File;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * @author glorydark
 */
public class HopperFormMain {

    public static Map<Player, AdvancedHopperForm> players = new LinkedHashMap<>();

    protected static Map<String, HopperFormData> forms = new LinkedHashMap<>();

    public static void loadAll() {
        if (!CustomFormMain.enableGameAPI) {
            return;
        }
        forms.clear();
        File file = new File(CustomFormMain.path + "/hopper_forms/");
        file.mkdirs();
        for (File listFile : Objects.requireNonNull(file.listFiles())) {
            Map<String, Object> map = FormCreator.convertConfigToMap(listFile);
            String name = listFile.getName().substring(0, listFile.getName().lastIndexOf("."));
            forms.put(name, HopperFormData.parse(map));
            CustomFormMain.plugin.getLogger().info(TextFormat.GREEN + "成功加载箱子菜单: " + name);
        }
    }

    public static boolean showToPlayer(Player player, String identifier) {
        if (players.containsKey(player) && !players.get(player).destroyed) {
            player.sendMessage(TextFormat.RED + "您点的太快了，请稍后再试！");
            return false;
        }
        if (forms.containsKey(identifier)) {
            HopperFormData hopperFormData = forms.get(identifier);
            if (hopperFormData != null) {
                AdvancedHopperForm hopperForm = new AdvancedHopperForm(hopperFormData.getTitle());
                players.put(player, hopperForm);
                for (Map.Entry<Integer, HopperFormItemInfo> entry : hopperFormData.getItemInfos().entrySet()) {
                    int id = entry.getKey();
                    HopperFormItemInfo itemInfo = entry.getValue();
                    hopperForm.addItem(id,
                            new ResponsiveElementSlotItem(itemInfo.getItem(player))
                                    .onRespond((player1, blockInventoryResponse) -> {
                                        if (itemInfo.isCloseForm()) {
                                            blockInventoryResponse.getInventory().closeForPlayer(player1);
                                        }
                                        if (itemInfo.getRequirements().isEmpty()) {
                                            for (String successCommand : itemInfo.getSuccessCommands()) {
                                                CommandUtils.executeCommand(player1, ReplaceStringUtils.replace(successCommand, player1, true, true));
                                            }
                                            for (String successMessage : itemInfo.getSuccessMessages()) {
                                                player1.sendMessage(ReplaceStringUtils.replace(successMessage, player1));
                                            }
                                        } else {
                                            Requirements successRequirements = null;
                                            for (Requirements requirement : itemInfo.getRequirements()) {
                                                if (requirement != null && requirement.isAllQualified(player1)) {
                                                    successRequirements = requirement;
                                                    break;
                                                }
                                            }
                                            if (successRequirements != null) {
                                                successRequirements.reduceAllCosts(player, 1);
                                                for (String successCommand : itemInfo.getSuccessCommands()) {
                                                    CommandUtils.executeCommand(player1, ReplaceStringUtils.replace(successCommand, player1, true, true));
                                                }
                                                for (String successMessage : itemInfo.getSuccessMessages()) {
                                                    player1.sendMessage(ReplaceStringUtils.replace(successMessage, player1));
                                                }
                                            } else {
                                                for (String failCommands : itemInfo.getFailCommands()) {
                                                    CommandUtils.executeCommand(player1, ReplaceStringUtils.replace(failCommands, player1, true, true));
                                                }
                                                for (String failMessages : itemInfo.getFailMessages()) {
                                                    player1.sendMessage(ReplaceStringUtils.replace(failMessages, player1));
                                                }
                                            }
                                        }
                                    })
                    );
                    hopperForm.onClose(player1 -> players.remove(player1));
                }
                hopperForm.showToPlayer(player);
                return true;
            }
        }
        return false;
    }
}
