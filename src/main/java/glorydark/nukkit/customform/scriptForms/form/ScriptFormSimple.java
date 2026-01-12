package glorydark.nukkit.customform.scriptForms.form;

import cn.nukkit.Player;
import cn.nukkit.form.element.*;
import cn.nukkit.form.response.FormResponse;
import cn.nukkit.form.window.FormWindow;
import cn.nukkit.form.window.FormWindowSimple;
import gameapi.form.AdvancedFormWindowSimple;
import gameapi.form.element.ResponsiveElementButton;
import glorydark.nukkit.customform.CustomFormMain;
import glorydark.nukkit.customform.factory.FormType;
import glorydark.nukkit.customform.scriptForms.data.SoundData;
import glorydark.nukkit.customform.scriptForms.data.execute_data.SimpleResponseExecuteData;
import glorydark.nukkit.customform.scriptForms.data.requirement.Requirements;
import glorydark.nukkit.customform.utils.CameraUtils;
import glorydark.nukkit.customform.utils.ReplaceContainer;
import glorydark.nukkit.customform.utils.ReplaceStringUtils;
import lombok.Data;

import java.util.*;

@Data
public class ScriptFormSimple implements ScriptForm {

    private Map<Integer, SimpleResponseExecuteData> data;

    private Map<String, Object> config;

    private FormWindowSimple window;

    private SoundData openSound;

    private Date startDate = new Date(-1);

    private Date expiredDate = new Date(-1);

    private List<Requirements> openRequirements;

    private List<PermissionEnum> openPermissions;

    private List<String> openPermissionWhitelist;

    public ScriptFormSimple(Map<String, Object> config, Map<Integer, SimpleResponseExecuteData> data, SoundData openSound, List<Requirements> openRequirements) {
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
            if (this.openPermissions.isEmpty()) {
                this.openPermissions.add(PermissionEnum.DEFAULT);
            }
            this.openPermissionWhitelist.addAll((List<String>) openPermissionMap.getOrDefault("whitelist", new ArrayList<>()));
        } else {
            this.openPermissions.add(PermissionEnum.DEFAULT);
        }
    }

    @Deprecated
    @Override
    public void execute(Player player, FormWindow respondWindow, FormResponse response, Object... params) {

    }

    @Override
    public void showToPlayer(Player player, FormType formType, String identifier) {
        ((AdvancedFormWindowSimple) this.getWindow()).showToPlayer(player);
    }

    public FormWindowSimple getWindow(Player player) {
        // copy elements
        AdvancedFormWindowSimple formWindowSimple = new AdvancedFormWindowSimple(
                this.window.getTitle(),
                this.window.getContent());
        for (SimpleElement element : cloneElements(player, this.window.getElements())) {
            formWindowSimple.addElement(element);
        }

        // replace text with variables
        for (int i = 0; i < formWindowSimple.getElements().size(); i++) {
            SimpleElement element = formWindowSimple.getElements().get(i);
            if (element instanceof ElementButton button) {
                button.setText(ReplaceStringUtils.replace(button.getText(), player));
            }
        }
        formWindowSimple.setContent(ReplaceStringUtils.replace(formWindowSimple.getContent(), player));
        formWindowSimple.setTitle(ReplaceStringUtils.replace(formWindowSimple.getTitle(), player));

        formWindowSimple.onClose(player1 -> {
            if (CustomFormMain.enableCameraAnimation) {
                CameraUtils.sendFormClose(player1);
            }
        });
        return formWindowSimple;
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
                if (!stringListTemp.isEmpty()) {
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
        if (content.isEmpty()) {
            simple = new FormWindowSimple((String) config.getOrDefault("title", ""), "");
        } else {
            simple = new FormWindowSimple((String) config.getOrDefault("title", ""), content);
        }
        for (Map<String, Object> component : (List<Map<String, Object>>) config.getOrDefault("components", new ArrayList<>())) {
            String type = (String) component.getOrDefault("type", "");
            switch (type.toLowerCase(Locale.ROOT)) {
                case "header":
                    simple.addElement(new ElementHeader((String) component.getOrDefault("text", "")));
                    break;
                case "divider":
                    simple.addElement(new ElementDivider());
                    break;
                case "button":
                default:
                    String picPath = (String) component.getOrDefault("pic", "");
                    if (picPath.isEmpty()) {
                        simple.addElement(new ElementButton((String) component.getOrDefault("text", "")));
                    } else {
                        if (picPath.startsWith("path#")) {
                            simple.addElement(new ElementButton((String) component.getOrDefault("text", ""), new ElementButtonImageData("path", picPath.replaceFirst("path#", ""))));
                        } else {
                            if (picPath.startsWith("url#")) {
                                simple.addElement(new ElementButton((String) component.getOrDefault("text", ""), new ElementButtonImageData("url", picPath.replaceFirst("url#", ""))));
                            } else {
                                simple.addElement(new ElementButton((String) component.getOrDefault("text", ""))); //格式都不对则取消
                            }
                        }
                    }
                    break;
            }
        }
        return simple;
    }

    public List<SimpleElement> cloneElements(Player player, List<SimpleElement> elements) {
        List<SimpleElement> out = new ArrayList<>();

        int i = 0;
        for (SimpleElement element : elements) {
            if (element instanceof ElementHeader header) {
                out.add(new ElementHeader(header.getText()));
            } else if (element instanceof ElementDivider) {
                out.add(new ElementDivider());
            } else if (element instanceof ElementButton elementButton) {
                SimpleResponseExecuteData executeData = data.get(i);
                if (!executeData.isVisible(player)) {
                    i++;
                    continue;
                }
                if (elementButton.getImage() == null) {
                    out.add(
                            new ResponsiveElementButton(elementButton.getText())
                                    .onRespond(player1 -> {
                                        executeData.execute(player1, 0, ReplaceContainer.EMPTY_CONTAINER);
                                        if (CustomFormMain.enableCameraAnimation) {
                                            CameraUtils.sendFormClose(player1);
                                        }
                                    })
                    );
                } else {
                    out.add(
                            new ResponsiveElementButton(elementButton.getText(),
                                    new ElementButtonImageData(elementButton.getImage().getType(), elementButton.getImage().getData())
                            ).onRespond(player1 -> {
                                executeData.execute(player1, 0, ReplaceContainer.EMPTY_CONTAINER);
                                if (CustomFormMain.enableCameraAnimation) {
                                    CameraUtils.sendFormClose(player1);
                                }
                            })
                    );
                }
            }
            i++;
        }
        return out;
    }
}
