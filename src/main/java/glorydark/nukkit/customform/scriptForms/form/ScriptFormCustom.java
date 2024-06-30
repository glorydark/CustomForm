package glorydark.nukkit.customform.scriptForms.form;

import cn.nukkit.Player;
import cn.nukkit.form.element.*;
import cn.nukkit.form.response.FormResponse;
import cn.nukkit.form.response.FormResponseCustom;
import cn.nukkit.form.window.FormWindow;
import cn.nukkit.form.window.FormWindowCustom;
import com.creeperface.nukkit.placeholderapi.api.PlaceholderAPI;
import com.smallaswater.npc.variable.VariableManage;
import glorydark.nukkit.LanguageMain;
import glorydark.nukkit.customform.CustomFormMain;
import glorydark.nukkit.customform.scriptForms.data.SoundData;
import glorydark.nukkit.customform.scriptForms.data.execute_data.ResponseExecuteData;
import glorydark.nukkit.customform.scriptForms.data.execute_data.element.ElementPlayerListDropdown;
import glorydark.nukkit.customform.scriptForms.data.requirement.Requirements;
import glorydark.nukkit.customform.utils.CommandUtils;
import lombok.Data;
import tip.utils.Api;

import java.math.BigDecimal;
import java.util.*;

@Data
public class ScriptFormCustom implements ScriptForm {

    private List<ResponseExecuteData> data;

    private Map<String, Object> config;

    private FormWindowCustom window;

    private SoundData openSound;

    private List<String> globalCommands = new ArrayList<>();

    private List<String> globalMessages = new ArrayList<>();

    private List<Boolean> enableTipsVariableReplacement = new ArrayList<>();

    private List<Boolean> enableRsNPCXVariableReplacement = new ArrayList<>();

    private Date startDate = new Date(-1);

    private Date expiredDate = new Date(-1);

    private List<Requirements> openRequirements;

    private List<PermissionEnum> openPermissions;

    private List<String> openPermissionWhitelist;

    public ScriptFormCustom(Map<String, Object> config, List<ResponseExecuteData> data, SoundData openSound, List<Requirements> openRequirements) {
        this.config = config;
        this.data = data;
        this.window = initWindow();
        this.openSound = openSound;
        this.openPermissions = new ArrayList<>();
        this.openPermissionWhitelist = new ArrayList<>();
        if (config.containsKey("global_responses")) {
            Map<String, List<String>> globalResponses = (Map<String, List<String>>) config.get("global_responses");
            globalCommands = globalResponses.getOrDefault("commands", new ArrayList<>());
            globalMessages = globalResponses.getOrDefault("messages", new ArrayList<>());
        }
        this.openRequirements = openRequirements;
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
        FormWindowCustom respondCustomWindow = (FormWindowCustom) respondWindow;
        FormResponseCustom responseCustom = (FormResponseCustom) response;
        Map<Integer, Object> responsesMap = responseCustom.getResponses();
        globalMessages.forEach(message -> {
            for (int i = 0; i < respondCustomWindow.getElements().size(); i++) {
                String replacePrefix = "%" + i + "%";
                String replacePrefixToInteger = "%" + i + "_integer%";
                String replacePrefixDropDownId = "%" + i + "_id%";
                String replacePrefixDropDownContent = "%" + i + "_content%";
                Element element = window.getElements().get(i);
                if (element instanceof ElementInput) {
                    message = message.replace(replacePrefix, responseCustom.getInputResponse(i));
                } else if (element instanceof ElementToggle) {
                    message = message.replace(replacePrefix, String.valueOf(responseCustom.getToggleResponse(i)));
                } else if (element instanceof ElementDropdown) {
                    message = message.replace(replacePrefixDropDownId, String.valueOf(responseCustom.getDropdownResponse(i).getElementID()));
                    message = message.replace(replacePrefixDropDownContent, responseCustom.getDropdownResponse(i).getElementContent());
                } else if (element instanceof ElementSlider) {
                    message = message.replace(replacePrefix, String.valueOf(responseCustom.getSliderResponse(i)));
                    message = message.replace(replacePrefixToInteger, String.valueOf(new BigDecimal(String.valueOf(responseCustom.getSliderResponse(i))).intValue()));
                } else if (element instanceof ElementStepSlider) {
                    message = message.replace(replacePrefixDropDownId, String.valueOf(responseCustom.getStepSliderResponse(i).getElementID()));
                    message = message.replace(replacePrefixDropDownContent, responseCustom.getStepSliderResponse(i).getElementContent());
                }
            }
            player.sendMessage(message);
        });
        globalCommands.forEach(command -> {
            for (int i = 0; i < respondCustomWindow.getElements().size(); i++) {
                String replacePrefix = "%" + i + "%";
                String replacePrefixDropDownId = "%" + i + "_id%";
                String replacePrefixDropDownContent = "%" + i + "_content%";
                Element element = window.getElements().get(i);
                if (element instanceof ElementInput) {
                    command = command.replace(replacePrefix, responseCustom.getInputResponse(i));
                } else if (element instanceof ElementToggle) {
                    command = command.replace(replacePrefix, String.valueOf(responseCustom.getToggleResponse(i)));
                } else if (element instanceof ElementDropdown) {
                    command = command.replace(replacePrefixDropDownId, String.valueOf(responseCustom.getDropdownResponse(i).getElementID()));
                    command = command.replace(replacePrefixDropDownContent, responseCustom.getDropdownResponse(i).getElementContent());
                } else if (element instanceof ElementSlider) {
                    command = command.replace(replacePrefix, String.valueOf(responseCustom.getSliderResponse(i)));
                } else if (element instanceof ElementStepSlider) {
                    command = command.replace(replacePrefixDropDownId, String.valueOf(responseCustom.getStepSliderResponse(i).getElementID()));
                    command = command.replace(replacePrefixDropDownContent, responseCustom.getStepSliderResponse(i).getElementContent());
                }
            }
            CommandUtils.executeCommand(player, replace(command, player));
        });
        responsesMap.forEach((key, value) -> {
            ResponseExecuteData responseExecuteData = data.get(key);
            if (responseExecuteData != null) {
                Element element = respondCustomWindow.getElements().get(key);
                if (element instanceof ElementDropdown) {
                    int elementDropdownResponseId = Integer.parseInt(((FormResponseCustom) response).getResponse(key).toString());
                    ElementDropdown dropdown = ((ElementDropdown) respondCustomWindow.getElements().get(key));
                    responseExecuteData.execute(player, elementDropdownResponseId, dropdown.getOptions().get(elementDropdownResponseId));
                } else {
                    if (respondCustomWindow.getElements().get(key) instanceof ElementStepSlider) {
                        int stepSliderResponseId = Integer.parseInt(((FormResponseCustom) response).getResponse(key).toString());
                        ElementStepSlider stepSlider = ((ElementStepSlider) respondCustomWindow.getElements().get(key));
                        responseExecuteData.execute(player, stepSliderResponseId, stepSlider.getSteps().get(stepSliderResponseId));
                    } else {
                        if (responseCustom.getResponse(key) != null) {
                            responseExecuteData.execute(player, 0, responseCustom.getResponse(key));
                        }
                    }
                }
            }
        });
    }

    public FormWindowCustom getWindow(Player player) {
        FormWindowCustom custom_temp = this.getModifiableWindow();
        int elementId = 0;
        custom_temp.setTitle(replace(custom_temp.getTitle(), player));
        for (Element element : new ArrayList<>(custom_temp.getElements())) {
            if (element instanceof ElementLabel) {
                ((ElementLabel) element).setText(replace(((ElementLabel) element).getText(), player));
                custom_temp.getElements().set(elementId, element);
            } else if (element instanceof ElementInput) {
                ElementInput input = ((ElementInput) element);
                input.setDefaultText(replace(input.getDefaultText(), player));
                input.setText(replace(input.getText(), player));
                input.setPlaceHolder(replace(input.getPlaceHolder(), player));
                custom_temp.getElements().set(elementId, input);
            } else if (element instanceof ElementDropdown) {
                if (element instanceof ElementPlayerListDropdown) {
                    ElementPlayerListDropdown playerListDropdown = ((ElementPlayerListDropdown) element).copyNew();
                    playerListDropdown.setText(replace(playerListDropdown.getText(), player));
                    custom_temp.getElements().set(elementId, playerListDropdown);
                } else {
                    ElementDropdown dropdown = ((ElementDropdown) element);
                    dropdown.setText(replace(dropdown.getText(), player));
                    dropdown.getOptions().replaceAll(string -> replace(string, player));
                    custom_temp.getElements().set(elementId, dropdown);
                }
            } else if (element instanceof ElementToggle) {
                ((ElementToggle) element).setText(replace(((ElementToggle) element).getText(), player));
                custom_temp.getElements().set(elementId, element);
            } else if (element instanceof ElementSlider) {
                ((ElementSlider) element).setText(replace(((ElementSlider) element).getText(), player));
                custom_temp.getElements().set(elementId, element);
            } else if (element instanceof ElementStepSlider) {
                ElementStepSlider stepSlider = ((ElementStepSlider) element);
                stepSlider.setText(replace(stepSlider.getText(), player));
                stepSlider.getSteps().replaceAll(string -> replace(string, player));
                custom_temp.getElements().set(elementId, stepSlider);
            }
            elementId++;
        }
        return custom_temp;
    }

    public FormWindowCustom getModifiableWindow() {
        return new FormWindowCustom(window.getTitle(), cloneElements(window.getElements()));
    }

    public List<Element> cloneElements(List<Element> elements) {
        List<Element> out = new ArrayList<>();
        for (Element element : elements) {
            if (element instanceof ElementDropdown) {
                if (element instanceof ElementPlayerListDropdown) {
                    out.add(element); // 丢到getWindow处理，和tips等变量替换一起
                } else {
                    ElementDropdown elementDropdown = (ElementDropdown) element;
                    out.add(new ElementDropdown(elementDropdown.getText(), new ArrayList<>(elementDropdown.getOptions()), elementDropdown.getDefaultOptionIndex()));
                }
            } else if (element instanceof ElementInput) {
                ElementInput input = (ElementInput) element;
                out.add(new ElementInput(input.getText(), input.getPlaceHolder(), input.getDefaultText()));
            } else if (element instanceof ElementLabel) {
                ElementLabel label = (ElementLabel) element;
                out.add(new ElementLabel(label.getText()));
            } else if (element instanceof ElementToggle) {
                ElementToggle elementToggle = (ElementToggle) element;
                out.add(new ElementToggle(elementToggle.getText(), elementToggle.isDefaultValue()));
            } else if (element instanceof ElementSlider) {
                ElementSlider slider = (ElementSlider) element;
                out.add(new ElementSlider(slider.getText(), slider.getMin(), slider.getMax(), slider.getStep(), slider.getDefaultValue()));
            } else if (element instanceof ElementStepSlider) {
                ElementStepSlider stepSlider = (ElementStepSlider) element;
                out.add(new ElementStepSlider(stepSlider.getText(), new ArrayList<>(stepSlider.getSteps()), stepSlider.getDefaultStepIndex()));
            }
        }
        return out;
    }

    @Override
    public SoundData getOpenSound() {
        return openSound;
    }

    public FormWindowCustom initWindow() {
        FormWindowCustom custom;
        custom = new FormWindowCustom((String) config.getOrDefault("title", ""));
        for (Map<String, Object> component : (List<Map<String, Object>>) config.getOrDefault("components", new ArrayList<>())) {
            enableTipsVariableReplacement.add((Boolean) component.getOrDefault("enable_tips_variable", true));
            enableRsNPCXVariableReplacement.add((Boolean) component.getOrDefault("enable_rsNPCX_variable", true));
            switch ((String) component.getOrDefault("type", "")) {
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
                case "PlayerListDropdown":
                    custom.addElement(new ElementPlayerListDropdown((String) component.getOrDefault("text", "")));
                    break;
            }
        }
        return custom;
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
        string = string.replace("{player}", player.getName());
        if (CustomFormMain.enableTips) {
            string = Api.strReplace(string, player);
        }
        if (CustomFormMain.enableRsNPCX) {
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
}
