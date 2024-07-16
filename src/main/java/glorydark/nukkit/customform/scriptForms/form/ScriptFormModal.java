package glorydark.nukkit.customform.scriptForms.form;

import cn.nukkit.Player;
import cn.nukkit.Server;
import cn.nukkit.form.response.FormResponse;
import cn.nukkit.form.response.FormResponseModal;
import cn.nukkit.form.window.FormWindow;
import cn.nukkit.form.window.FormWindowModal;
import cn.nukkit.utils.Config;
import cn.nukkit.utils.ConfigSection;
import com.creeperface.nukkit.placeholderapi.api.PlaceholderAPI;
import com.smallaswater.npc.data.RsNpcConfig;
import com.smallaswater.npc.utils.exception.RsNpcConfigLoadException;
import com.smallaswater.npc.utils.exception.RsNpcLoadException;
import com.smallaswater.npc.variable.VariableManage;
import glorydark.nukkit.LanguageMain;
import glorydark.nukkit.customform.CustomFormMain;
import glorydark.nukkit.customform.scriptForms.data.SoundData;
import glorydark.nukkit.customform.scriptForms.data.execute_data.SimpleResponseExecuteData;
import glorydark.nukkit.customform.scriptForms.data.requirement.Requirements;
import lombok.Data;
import tip.utils.Api;

import java.util.*;

@Data
public class ScriptFormModal implements ScriptForm {

    private SoundData openSound;
    private List<SimpleResponseExecuteData> data;

    private Map<String, Object> config;

    private FormWindowModal window;

    private Date startDate = new Date(-1);

    private Date expiredDate = new Date(-1);

    private List<Requirements> openRequirements;

    private List<PermissionEnum> openPermissions;

    private List<String> openPermissionWhitelist;

    public ScriptFormModal(Map<String, Object> config, List<SimpleResponseExecuteData> data, SoundData openSound, List<Requirements> openRequirements) {
        this.config = config;
        this.data = data;
        this.window = initWindow();
        this.openSound = openSound;
        this.openRequirements = openRequirements;
        this.openPermissions = new ArrayList<>();
        this.openPermissionWhitelist = new ArrayList<>();
        if (config.containsKey("open_permissions")) {
            Map<String, Object> openPermissionMap = (Map<String, Object>) config.getOrDefault("open_permissions", new HashMap<>());
            List<String> permissions = (List<String>) openPermissionMap.getOrDefault("types", new ArrayList<>());
            for (String permission : permissions) {
                switch (permission) {
                    case "op":
                        this.openPermissions.add(PermissionEnum.OP);
                        break;
                    case "console":
                        this.openPermissions.add(PermissionEnum.CONSOLE);
                        break;
                    case "user-only":
                        this.openPermissions.add(PermissionEnum.ONLY_USER);
                        break;
                }
            }
            if (this.openPermissions.size() == 0) {
                this.openPermissions.add(PermissionEnum.DEFAULT);
            }
            this.openPermissionWhitelist.addAll((List<String>) openPermissionMap.getOrDefault("whitelist", new ArrayList<>()));
        } else {
            openPermissions.add(PermissionEnum.DEFAULT);
        }
    }

    public void execute(Player player, FormWindow respondWindow, FormResponse response, Object... params) {
        FormResponseModal responseModal = (FormResponseModal) response;
        if (data.size() <= (responseModal.getClickedButtonId())) {
            return;
        }
        data.get(responseModal.getClickedButtonId()).execute(player, 0, params);
    }

    public FormWindowModal initWindow() {
        FormWindowModal modal;
        Object object = config.getOrDefault("content", "");
        String content = "";
        if (!object.equals("")) {
            if (object instanceof String) {
                content = (String) object;
            } else if (object instanceof ArrayList) {
                StringBuilder tempStr = new StringBuilder();
                List<String> stringListTemp = (List<String>) object;
                if (stringListTemp.size() > 0) {
                    for (int i = 0; i < stringListTemp.size(); i++) {
                        tempStr.append(stringListTemp.get(i));
                        if (i < stringListTemp.size() - 1) {
                            tempStr.append("\n");
                        }
                    }
                }
                content = tempStr.toString();
            }
        }
        List<Map<String, Object>> buttons = (List<Map<String, Object>>) config.getOrDefault("components", new ArrayList<>());
        if (buttons.size() != 2) {
            return null;
        }
        if (content.equals("")) {
            modal = new FormWindowModal((String) config.getOrDefault("title", ""), "", (String) buttons.get(0).get("text"), (String) buttons.get(1).get("text"));
        } else {
            modal = new FormWindowModal((String) config.getOrDefault("title", ""), content, (String) buttons.get(0).get("text"), (String) buttons.get(1).get("text"));
        }

        return modal;
    }

    public FormWindowModal getWindow(Player player) {
        if (CustomFormMain.enableTips || CustomFormMain.enableRsNPCX) {
            FormWindowModal modal = this.getModifiableWindow();
            modal.setContent(replace(modal.getContent(), player, true));
            modal.setTitle(replace(modal.getTitle(), player));
            return modal;
        }
        return this.getModifiableWindow();
    }

    public FormWindowModal getModifiableWindow() {
        return new FormWindowModal(window.getTitle(), window.getContent(), window.getButton1(), window.getButton2());
    }

    @Override
    public SoundData getOpenSound() {
        return openSound;
    }

    public String replaceBreak(String string) {
        return string.replace("\\n", "\n");
    }

    public String replace(String string, Player player) {
        return replace(string, player, false);
    }

    /**
     * Refracted in order to expand the usages easily.
     */
    public String replace(String string, Player player, boolean replaceBreak) {
        if (CustomFormMain.enableLanguageAPI) {
            string = LanguageMain.getInstance().getTranslation(CustomFormMain.plugin, player, string);
        }
        string = string.replace("{player}", player.getName())
                .replace("%player%", player.getName());
        if (CustomFormMain.enableTips) {
            string = Api.strReplace(string, player);
        }
        if (CustomFormMain.enableRsNPCX) {
            string = VariableManage.stringReplace(player, string, CustomFormMain.rsNpcConfig);
        }
        if (CustomFormMain.enablePlaceHolderAPI) {
            string = PlaceholderAPI.getInstance().translateString(string);
        }
        if (replaceBreak) {
            string = replaceBreak(string);
        }
        return string;
    }
}
