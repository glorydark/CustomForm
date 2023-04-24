package glorydark.customform.scriptForms.form;

import cn.nukkit.Player;
import cn.nukkit.Server;
import cn.nukkit.form.element.*;
import cn.nukkit.form.response.FormResponse;
import cn.nukkit.form.response.FormResponseCustom;
import cn.nukkit.form.response.FormResponseData;
import cn.nukkit.form.window.FormWindowCustom;
import glorydark.customform.CustomFormMain;
import glorydark.customform.scriptForms.data.SoundData;
import glorydark.customform.scriptForms.data.execute_data.ResponseExecuteData;
import lombok.Data;
import tip.utils.Api;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Data
public class ScriptFormCustom implements ScriptForm {

    private List<ResponseExecuteData> data;

    private Map<String, Object> config;

    private FormWindowCustom window;

    private SoundData openSound;

    private List<String> globalCommands = new ArrayList<>();

    private List<String> globalMessages = new ArrayList<>();

    public ScriptFormCustom(Map<String, Object> config, List<ResponseExecuteData> data, SoundData openSound){
        this.config = config;
        this.data = data;
        this.window = initWindow();
        this.openSound = openSound;
        if(config.containsKey("global_responses")){
            Map<String, List<String>> globalResponses = (Map<String, List<String>>) config.get("global_responses");
            globalCommands = globalResponses.get("commands");
            globalMessages = globalResponses.get("messages");
        }
    }

    public void execute(Player player, FormResponse response, Object... params){
        FormResponseCustom responseCustom = (FormResponseCustom) response;
        Map<Integer, Object> responsesMap = responseCustom.getResponses();
        globalMessages.forEach(message->{
            for(int i= 0; i < responsesMap.size(); i++){
                message = message.replace("%"+i+"%", responsesMap.get(i).toString());
            }
            player.sendMessage(message);
        });
        globalCommands.forEach(command->{
            for(int i= 0; i < responsesMap.size(); i++){
                command = command.replace("%"+i+"%", responsesMap.get(i).toString());
            }
            if(command.startsWith("console#")){
                Server.getInstance().dispatchCommand(Server.getInstance().getConsoleSender(), command.replace("console#", ""));
            }else{
                Server.getInstance().dispatchCommand(player, command);
            }
        });
        responsesMap.forEach((key1, value) -> {
            int key = key1;
            if (window.getElements().get(key) instanceof ElementDropdown) {
                FormResponseData formResponseData = responseCustom.getDropdownResponse(key);
                if(formResponseData != null) {
                    data.get(key).execute(player, formResponseData.getElementID(), formResponseData.getElementContent());
                }
            } else {
                if(window.getElements().get(0) instanceof ElementStepSlider){
                    FormResponseData formResponseData = responseCustom.getStepSliderResponse(key);
                    data.get(key).execute(player, formResponseData.getElementID(), formResponseData.getElementContent());
                }else{
                    if(responseCustom.getResponse(key) != null) {
                        data.get(key).execute(player, 0, responseCustom.getResponse(key));
                    }
                }
            }
        });
    }

    public FormWindowCustom getWindow(Player player){
        if(CustomFormMain.enableTips){
            FormWindowCustom custom = this.getModifiableWindow();
            int elementId = 0;
            //test
            custom.setTitle(Api.strReplace(custom.getTitle(), player));
            for(Element element: custom.getElements()){
                if(element instanceof ElementLabel){
                    ((ElementLabel) element).setText(Api.strReplace(((ElementLabel) element).getText(), player));
                }else if(element instanceof ElementInput){
                    ElementInput input =  ((ElementInput) element);
                    input.setDefaultText(Api.strReplace(input.getDefaultText(), player));
                    input.setText(Api.strReplace(input.getDefaultText(), player));
                    input.setPlaceHolder(Api.strReplace(input.getDefaultText(), player));
                }else if(element instanceof ElementDropdown){
                    ElementDropdown dropdown = ((ElementDropdown) element);
                    dropdown.setText(Api.strReplace(dropdown.getText(), player));
                    dropdown.getOptions().replaceAll(string -> Api.strReplace(string, player));
                }else if(element instanceof ElementToggle){
                    ((ElementToggle) element).setText(Api.strReplace(((ElementToggle) element).getText(), player));
                }else if(element instanceof ElementSlider){
                    ((ElementSlider) element).setText(Api.strReplace(((ElementSlider) element).getText(), player));
                }else if(element instanceof ElementStepSlider){
                    ElementStepSlider stepSlider = ((ElementStepSlider) element);
                    stepSlider.setText(Api.strReplace(stepSlider.getText(), player));
                    stepSlider.getSteps().replaceAll(string -> Api.strReplace(string, player));
                    window.getElements().set(elementId, stepSlider);
                }
                elementId++;
            }
            return custom;
        }
        return this.getWindow();
    }

    public FormWindowCustom getModifiableWindow(){
        return new FormWindowCustom(window.getTitle(), window.getElements());
    }

    @Override
    public SoundData getOpenSound() {
        return openSound;
    }

    public FormWindowCustom initWindow(){
        FormWindowCustom custom;
        custom = new FormWindowCustom(replace((String) config.getOrDefault("title", "")));
        for(Map<String, Object> component: (List<Map<String, Object>>) config.getOrDefault("components", new ArrayList<>())) {
            switch ((String) component.getOrDefault("type", "")){
                case "Input":
                    custom.addElement(new ElementInput((String) component.getOrDefault("text", ""), (String) component.getOrDefault("placeholder", ""), (String) component.getOrDefault("default", "")));
                    break;
                case "Label":
                    custom.addElement(new ElementLabel((String) component.getOrDefault("text", "")));
                    break;
                case "Toggle":
                    custom.addElement(new ElementToggle((String) component.getOrDefault("text", ""), (boolean) component.getOrDefault("default", "")));
                    break;
                case "Slider":
                    custom.addElement(new ElementSlider((String) component.getOrDefault("text", ""), (int) component.getOrDefault("min", 0), (int) component.getOrDefault("max", 0), (int) component.getOrDefault("step", 0), Float.parseFloat(component.getOrDefault("default", 0f).toString())));
                    break;
                case "StepSlider":
                    custom.addElement(new ElementStepSlider((String) component.getOrDefault("text", ""), (List<String>) component.getOrDefault("steps", new ArrayList<>()), (int) component.getOrDefault("default", 0)));
                    break;
                case "Dropdown":
                    custom.addElement(new ElementDropdown((String) component.getOrDefault("text", ""), (List<String>) component.getOrDefault("options", new ArrayList<>()), (int) component.getOrDefault("default", 0)));
                    break;
            }
        }
        return custom;
    }

    public String replace(String string){
        return string.replace("\\n", "\n");
    }
}
