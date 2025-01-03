package glorydark.nukkit.customform.scriptForms.form;

import cn.nukkit.Player;
import cn.nukkit.form.element.ElementButton;
import cn.nukkit.form.element.ElementButtonImageData;
import cn.nukkit.form.response.FormResponse;
import cn.nukkit.form.response.FormResponseSimple;
import cn.nukkit.form.window.FormWindow;
import cn.nukkit.form.window.FormWindowSimple;
import glorydark.nukkit.customform.scriptForms.data.SoundData;
import glorydark.nukkit.customform.scriptForms.data.execute_data.SimpleResponseExecuteData;
import glorydark.nukkit.customform.scriptForms.data.requirement.Requirements;
import glorydark.nukkit.customform.utils.ReplaceStringUtils;
import lombok.Data;

import java.util.*;

@Data
public class ScriptFormSimple implements ScriptForm {

    private List<SimpleResponseExecuteData> data;

    private Map<String, Object> config;

    private FormWindowSimple window;

    private SoundData openSound;

    private Date startDate = new Date(-1);

    private Date expiredDate = new Date(-1);

    private List<Requirements> openRequirements;

    private List<PermissionEnum> openPermissions;

    private List<String> openPermissionWhitelist;

    public ScriptFormSimple(Map<String, Object> config, List<SimpleResponseExecuteData> data, SoundData openSound, List<Requirements> openRequirements) {
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
            this.openPermissions.add(PermissionEnum.DEFAULT);
        }
    }

    public void execute(Player player, FormWindow respondWindow, FormResponse response, Object... params) {
        FormResponseSimple responseSimple = (FormResponseSimple) response;
        if (data.size() <= responseSimple.getClickedButtonId()) {
            return;
        }
        data.get(responseSimple.getClickedButtonId()).execute(player, 0, params);
    }

    public FormWindowSimple getWindow(Player player) {
        FormWindowSimple simple_temp = this.getModifiableWindow();
        int elementId = 0;
        for (ElementButton button : new ArrayList<>(simple_temp.getButtons())) {
            button.setText(ReplaceStringUtils.replace(button.getText(), player));
            simple_temp.getButtons().set(elementId, button);
            elementId++;
        }
        simple_temp.setContent(ReplaceStringUtils.replace(simple_temp.getContent(), player));
        simple_temp.setTitle(ReplaceStringUtils.replace(simple_temp.getTitle(), player));
        return simple_temp;
    }

    public FormWindowSimple getModifiableWindow() {
        return new FormWindowSimple(window.getTitle(), window.getContent(), cloneButtons(window.getButtons()));
    }

    @Override
    public SoundData getOpenSound() {
        return openSound;
    }

    public FormWindowSimple initWindow() {
        FormWindowSimple simple;
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
        if (content.equals("")) {
            simple = new FormWindowSimple((String) config.getOrDefault("title", ""), "");
        } else {
            simple = new FormWindowSimple((String) config.getOrDefault("title", ""), content);
        }
        for (Map<String, Object> component : (List<Map<String, Object>>) config.getOrDefault("components", new ArrayList<>())) {
            String picPath = (String) component.getOrDefault("pic", "");
            if (picPath.equals("")) {
                simple.addButton(new ElementButton((String) component.getOrDefault("text", "")));
            } else {
                if (picPath.startsWith("path#")) {
                    simple.addButton(new ElementButton((String) component.getOrDefault("text", ""), new ElementButtonImageData("path", picPath.replaceFirst("path#", ""))));
                } else {
                    if (picPath.startsWith("url#")) {
                        simple.addButton(new ElementButton((String) component.getOrDefault("text", ""), new ElementButtonImageData("url", picPath.replaceFirst("url#", ""))));
                    } else {
                        simple.addButton(new ElementButton((String) component.getOrDefault("text", ""))); //格式都不对则取消
                    }
                }
            }
        }
        return simple;
    }

    public List<ElementButton> cloneButtons(List<ElementButton> elementButtons) {
        List<ElementButton> out = new ArrayList<>();
        for (ElementButton elementButton : elementButtons) {
            if (elementButton.getImage() == null) {
                out.add(new ElementButton(elementButton.getText()));
            } else {
                out.add(new ElementButton(elementButton.getText(), new ElementButtonImageData(elementButton.getImage().getType(), elementButton.getImage().getData())));
            }
        }
        return out;
    }
}
