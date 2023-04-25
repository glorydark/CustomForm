package glorydark.customform.scriptForms.form;

import cn.nukkit.Player;
import cn.nukkit.form.element.ElementButton;
import cn.nukkit.form.element.ElementButtonImageData;
import cn.nukkit.form.response.FormResponse;
import cn.nukkit.form.response.FormResponseSimple;
import cn.nukkit.form.window.FormWindowSimple;
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

    public ScriptFormSimple(Map<String, Object> config, List<SimpleResponseExecuteData> data, SoundData openSound){
        this.config = config;
        this.data = data;
        this.window = initWindow();
        this.openSound = openSound;
    }

    public void execute(Player player, FormResponse response, Object... params){
        FormResponseSimple responseSimple = (FormResponseSimple) response;
        if(data.size() <= responseSimple.getClickedButtonId()){
            return;
        }
        data.get(responseSimple.getClickedButtonId()).execute(player, 0, params);
    }

    public String replace(String string){
        return string.replace("\\n", "\n");
    }

    public FormWindowSimple getWindow(Player player){
        if(CustomFormMain.enableTips){
            FormWindowSimple simple_temp = this.getModifiableWindow();
            int elementId = 0;
            for(ElementButton button: new ArrayList<>(simple_temp.getButtons())){
                button.setText(Api.strReplace(button.getText(), player));
                simple_temp.getButtons().set(elementId, button);
                elementId++;
            }
            simple_temp.setContent(Api.strReplace(simple_temp.getContent(), player));
            simple_temp.setTitle(Api.strReplace(simple_temp.getTitle(), player));
            return simple_temp;
        }
        return this.getModifiableWindow();
    }

    public FormWindowSimple getModifiableWindow(){
        return new FormWindowSimple(window.getTitle(), window.getContent(), window.getButtons());
    }

    @Override
    public SoundData getOpenSound() {
        return openSound;
    }

    public FormWindowSimple initWindow(){
        FormWindowSimple simple;
        String content = replace((String) config.getOrDefault("content", ""));
        if(content.equals("")) {
            simple = new FormWindowSimple(replace((String) config.getOrDefault("title", "")), "");
        }else{
            simple = new FormWindowSimple(replace((String) config.getOrDefault("title", "")), content);
        }
        for(Map<String, Object> component: (List<Map<String, Object>>) config.getOrDefault("components", new ArrayList<>())) {
            enableTipsVariableReplacement.add((Boolean) component.getOrDefault("enable_tips_variable", true));
            String picPath = (String) component.getOrDefault("pic", "");
            if (picPath.equals("")) {
                simple.addButton(new ElementButton(replace((String) component.getOrDefault("text", ""))));
            } else {
                if (picPath.startsWith("path#")) {
                    simple.addButton(new ElementButton(replace((String) component.getOrDefault("text", "")), new ElementButtonImageData("path", picPath.replaceFirst("path#", ""))));
                }else {
                    if (picPath.startsWith("url#")) {
                        simple.addButton(new ElementButton(replace((String) component.getOrDefault("text", "")), new ElementButtonImageData("url", picPath.replaceFirst("url#", ""))));
                    }else{
                        simple.addButton(new ElementButton(replace((String) component.getOrDefault("text", "")))); //格式都不对则取消
                    }
                }
            }
        }
        return simple;
    }
}
