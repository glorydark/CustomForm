package glorydark.customform.scriptForms.form;

import cn.nukkit.Player;
import cn.nukkit.form.element.ElementButton;
import cn.nukkit.form.element.ElementButtonImageData;
import cn.nukkit.form.response.FormResponse;
import cn.nukkit.form.response.FormResponseSimple;
import cn.nukkit.form.window.FormWindowSimple;
import com.smallaswater.npc.variable.BaseVariable;
import com.smallaswater.npc.variable.BaseVariableV2;
import com.smallaswater.npc.variable.VariableManage;
import glorydark.customform.CustomFormMain;
import glorydark.customform.scriptForms.data.SoundData;
import glorydark.customform.scriptForms.data.execute_data.SimpleResponseExecuteData;
import lombok.Data;
import tip.utils.Api;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

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

    public FormWindowSimple getWindow(Player player){
        if(CustomFormMain.enableTips){
            FormWindowSimple simple_temp = this.getModifiableWindow();
            int elementId = 0;
            for(ElementButton button: new ArrayList<>(simple_temp.getButtons())){
                button.setText(replace(button.getText(), player));
                simple_temp.getButtons().set(elementId, button);
                elementId++;
            }
            simple_temp.setContent(replace(simple_temp.getContent(), player));
            simple_temp.setTitle(replace(simple_temp.getTitle(), player));
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
        Object object = config.getOrDefault("content", "");
        String content = "";
        if(!object.equals("")) {
            if (object instanceof String) {
                content = replaceBreak((String) object);
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
                content = replaceBreak(tempStr.toString());
            }
        }
        if(content.equals("")) {
            simple = new FormWindowSimple(replaceBreak((String) config.getOrDefault("title", "")), "");
        }else{
            simple = new FormWindowSimple(replaceBreak((String) config.getOrDefault("title", "")), content);
        }
        for(Map<String, Object> component: (List<Map<String, Object>>) config.getOrDefault("components", new ArrayList<>())) {
            enableTipsVariableReplacement.add((Boolean) component.getOrDefault("enable_tips_variable", true));
            String picPath = (String) component.getOrDefault("pic", "");
            if (picPath.equals("")) {
                simple.addButton(new ElementButton(replaceBreak((String) component.getOrDefault("text", ""))));
            } else {
                if (picPath.startsWith("path#")) {
                    simple.addButton(new ElementButton(replaceBreak((String) component.getOrDefault("text", "")), new ElementButtonImageData("path", picPath.replaceFirst("path#", ""))));
                }else {
                    if (picPath.startsWith("url#")) {
                        simple.addButton(new ElementButton(replaceBreak((String) component.getOrDefault("text", "")), new ElementButtonImageData("url", picPath.replaceFirst("url#", ""))));
                    }else{
                        simple.addButton(new ElementButton(replaceBreak((String) component.getOrDefault("text", "")))); //格式都不对则取消
                    }
                }
            }
        }
        return simple;
    }

    public String replaceBreak(String string){
        return string.replace("\\n", "\n");
    }

    public String replace(String string, Player player) {
        return replace(string, player, false);
    }

    /**
     * Refracted in order to expand the usages easily.
     */
    public String replace(String string, Player player, boolean replaceBreak){
        if(CustomFormMain.enableTips){
            string = Api.strReplace(string, player);
        }
        if(CustomFormMain.enableRsNPCX){
            try {
                Field field1 = VariableManage.class.getDeclaredField("VARIABLE_CLASS");
                field1.setAccessible(true);
                ConcurrentHashMap<String, BaseVariable> v1_classes = (ConcurrentHashMap<String, BaseVariable>) field1.get(new ConcurrentHashMap<>());
                for(BaseVariable v1: v1_classes.values()){
                    string = v1.stringReplace(player, string);
                }
                Field field2 = VariableManage.class.getDeclaredField("VARIABLE_V2_CLASS");
                field2.setAccessible(true);
                ConcurrentHashMap<String, BaseVariableV2> v2_classes = (ConcurrentHashMap<String, BaseVariableV2>) field2.get(new ConcurrentHashMap<>());
                for(BaseVariableV2 v2: v2_classes.values()){
                    string = v2.stringReplace(string);
                }
            } catch (NoSuchFieldException | IllegalAccessException ignored) {

            }
        }
        if(replaceBreak){
            string = replaceBreak(string);
        }
        return string;
    }
}
