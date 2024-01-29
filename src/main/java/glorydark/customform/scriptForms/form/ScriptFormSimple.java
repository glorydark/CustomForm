package glorydark.customform.scriptForms.form;

import cn.nukkit.Player;
import cn.nukkit.form.element.ElementButton;
import cn.nukkit.form.element.ElementButtonImageData;
import cn.nukkit.form.response.FormResponse;
import cn.nukkit.form.response.FormResponseSimple;
import cn.nukkit.form.window.FormWindow;
import cn.nukkit.form.window.FormWindowSimple;
import com.creeperface.nukkit.placeholderapi.api.PlaceholderAPI;
import com.smallaswater.npc.variable.VariableManage;
import glorydark.customform.CustomFormMain;
import glorydark.customform.scriptForms.data.SoundData;
import glorydark.customform.scriptForms.data.execute_data.SimpleResponseExecuteData;
import lombok.Data;
import tip.utils.Api;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Data
public class ScriptFormSimple implements ScriptForm {

    private List<SimpleResponseExecuteData> data;

    private Map<String, Object> config;

    private FormWindowSimple window;

    private SoundData openSound;

    private List<Boolean> enableTipsVariableReplacement = new ArrayList<>();

    private List<Boolean> enableRsNPCXVariableReplacement = new ArrayList<>();

    private long startMillis = -1L;

    private long expiredMillis = -1L;

    public ScriptFormSimple(Map<String, Object> config, List<SimpleResponseExecuteData> data, SoundData openSound) {
        this.config = config;
        this.data = data;
        this.window = initWindow();
        this.openSound = openSound;
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
            boolean tipsEnabled = enableTipsVariableReplacement.get(elementId);
            boolean rsNPCXEnabled = enableRsNPCXVariableReplacement.get(elementId);
            button.setText(replace(button.getText(), player, true, rsNPCXEnabled, tipsEnabled));
            simple_temp.getButtons().set(elementId, button);
            elementId++;
        }
        simple_temp.setContent(replace(simple_temp.getContent(), player, true));
        simple_temp.setTitle(replace(simple_temp.getTitle(), player));
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
            enableTipsVariableReplacement.add((Boolean) component.getOrDefault("enable_tips_variable", true));
            enableRsNPCXVariableReplacement.add((Boolean) component.getOrDefault("enable_rsNPCX_variable", true));
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

    public String replaceBreak(String string) {
        return string.replace("\\n", "\n");
    }

    public String replace(String string, Player player) {
        return this.replace(string, player, false);
    }

    /**
     * Refracted in order to expand the usages easily.
     */

    public String replace(String string, Player player, boolean replaceBreak) {
        return this.replace(string, player, replaceBreak, true, true);
    }

    public String replace(String string, Player player, boolean replaceBreak, boolean enableRsNPCX, boolean enableTips) {
        if (CustomFormMain.enableTips && enableTips) {
            string = Api.strReplace(string, player);
        }
        if (CustomFormMain.enableRsNPCX && enableRsNPCX) {
            string = VariableManage.stringReplace(player, string, null);
        }
        if (CustomFormMain.enablePlaceHolderAPI) {
            string = PlaceholderAPI.getInstance().translateString(string);
        }
        if (replaceBreak) {
            string = replaceBreak(string);
        }
        return string;
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
