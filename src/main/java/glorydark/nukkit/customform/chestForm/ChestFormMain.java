package glorydark.nukkit.customform.chestForm;

import cn.nukkit.Player;
import cn.nukkit.utils.TextFormat;
import gameapi.form.AdvancedDoubleChestForm;
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
public class ChestFormMain {

    protected static Map<String, ChestData> chestForms = new LinkedHashMap<>();

    public static void loadAll() {
        if (!CustomFormMain.enableGameAPI) {
            return;
        }
        chestForms.clear();
        File file = new File(CustomFormMain.path + "/chest_forms/");
        file.mkdirs();
        for (File listFile : Objects.requireNonNull(file.listFiles())) {
            Map<String, Object> map = FormCreator.convertConfigToMap(listFile);
            String name = listFile.getName().substring(0, listFile.getName().lastIndexOf("."));
            chestForms.put(name, ChestData.parse(map));
            CustomFormMain.plugin.getLogger().info(TextFormat.GREEN + "成功加载箱子菜单: " + name);
        }
    }

    public static boolean showToPlayer(Player player, String identifier) {
        if (chestForms.containsKey(identifier)) {
            ChestData chestData = chestForms.get(identifier);
            if (chestData != null) {
                AdvancedDoubleChestForm chestForm = new AdvancedDoubleChestForm(chestData.getTitle());
                for (Map.Entry<Integer, ChestItemInfo> entry : chestData.getItemInfos().entrySet()) {
                    int id = entry.getKey();
                    ChestItemInfo itemInfo = entry.getValue();
                    chestForm.addItem(id,
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
                }
                chestForm.showToPlayer(player);
                return true;
            }
        }
        return false;
    }
}
