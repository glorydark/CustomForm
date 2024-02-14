package glorydark.nukkit.customform.forms;

import cn.nukkit.Player;
import cn.nukkit.event.EventHandler;
import cn.nukkit.event.Listener;
import cn.nukkit.event.server.DataPacketReceiveEvent;
import cn.nukkit.form.element.*;
import cn.nukkit.form.response.*;
import cn.nukkit.form.window.FormWindow;
import cn.nukkit.form.window.FormWindowCustom;
import cn.nukkit.form.window.FormWindowModal;
import cn.nukkit.form.window.FormWindowSimple;
import cn.nukkit.network.protocol.ModalFormResponsePacket;
import glorydark.nukkit.customform.CustomFormMain;
import glorydark.nukkit.customform.scriptForms.form.ScriptForm;
import glorydark.nukkit.customform.utils.CameraUtils;

import java.util.HashMap;
import java.util.LinkedHashMap;

public class FormListener implements Listener {

    @EventHandler
    public void DataPacketReceiveEvent(DataPacketReceiveEvent event) {
        Player player = event.getPlayer();
        if (event.getPacket() instanceof ModalFormResponsePacket) {
            ModalFormResponsePacket pk = (ModalFormResponsePacket) event.getPacket();
            if (pk.formId == FormCreator.formId) {
                dealResponse(player, FormCreator.UI_CACHE.get(player.getName()).getFormWindow(), pk.data);
            }
        }
    }

    // Give corresponding response to the player.
    public void dealResponse(Player p, FormWindow respondWindow, String response) {
        if (p == null) {
            return;
        }
        if (CustomFormMain.enableCameraAnimation) {
            CameraUtils.sendFormClose(p);
        }
        if (response == null) {
            return;
        }
        String pName = p.getName();
        FormType formType = FormCreator.UI_CACHE.containsKey(pName) ? FormCreator.UI_CACHE.get(pName).getType() : null;
        if (formType == null) {
            FormCreator.UI_CACHE.remove(pName);
            return;
        }
        String script = FormCreator.UI_CACHE.get(pName).getScript();
        ScriptForm form = FormCreator.formScripts.get(script);
        if (form == null) {
            form = FormCreator.UI_CACHE.get(pName).getCustomizedScriptForm();
        }
        if (translateResponse(response.trim(), form.getWindow(p)) == null) {
            return;
        }
        FormCreator.UI_CACHE.remove(pName);
        form.execute(p, respondWindow, translateResponse(response.trim(), form.getWindow(p)));
    }

    // Deal with empty response
    public FormResponse translateResponse(String string, FormWindow window) {
        if (window instanceof FormWindowSimple) {
            if (string.equals("null")) {
                return null;
            } else {
                int id = Integer.parseInt(string);
                return new FormResponseSimple(id, ((FormWindowSimple) window).getButtons().get(id));
            }
        }
        if (window instanceof FormWindowCustom) {
            FormWindowCustom custom = (FormWindowCustom) window;
            HashMap<Integer, FormResponseData> dropdownResponses = new LinkedHashMap<>();
            HashMap<Integer, String> inputResponses = new LinkedHashMap<>();
            HashMap<Integer, Float> sliderResponses = new LinkedHashMap<>();
            HashMap<Integer, FormResponseData> stepSliderResponses = new LinkedHashMap<>();
            HashMap<Integer, Boolean> toggleResponses = new LinkedHashMap<>();
            HashMap<Integer, String> labelResponses = new LinkedHashMap<>();
            if (string.equals("null") || string.equals("")) {
                return null;
            } else {
                String replaced = string.trim().replace("[", "").replace("]", "");
                String[] split = replaced.split(",");
                HashMap<Integer, Object> hashMap = new HashMap<>();
                for (int i = 0; i < split.length; i++) {
                    String returnValue = split[i];
                    hashMap.put(i, returnValue);
                    Element element = custom.getElements().get(i);
                    if (element instanceof ElementLabel) {
                        labelResponses.put(i, ((ElementLabel) element).getText());
                    } else if (element instanceof ElementInput) {
                        inputResponses.put(i, returnValue);
                    } else if (element instanceof ElementSlider) {
                        sliderResponses.put(i, Float.parseFloat(returnValue));
                    } else if (element instanceof ElementStepSlider) {
                        String selectedItem = ((ElementStepSlider) element).getSteps().get(Integer.parseInt(returnValue));
                        stepSliderResponses.put(i, new FormResponseData(Integer.parseInt(returnValue), selectedItem));
                    } else if (element instanceof ElementToggle) {
                        Boolean selectedItem = Boolean.parseBoolean(returnValue);
                        toggleResponses.put(i, selectedItem);
                    } else if (element instanceof ElementDropdown) {
                        String selectedItem = ((ElementDropdown) element).getOptions().get(Integer.parseInt(returnValue));
                        dropdownResponses.put(i, new FormResponseData(Integer.parseInt(returnValue), selectedItem));
                    }
                }
                return new FormResponseCustom(hashMap, dropdownResponses, inputResponses, sliderResponses, stepSliderResponses, toggleResponses, labelResponses);
            }
        }
        if (window instanceof FormWindowModal) {
            if (string.equals("null")) {
                return null;
            } else {
                int id = string.equals("true") ? 0 : 1;
                return new FormResponseModal(id, id == 0 ? ((FormWindowModal) window).getButton1() : ((FormWindowModal) window).getButton2());
            }
        }
        return null;
    }
}
