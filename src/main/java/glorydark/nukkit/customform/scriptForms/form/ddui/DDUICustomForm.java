package glorydark.nukkit.customform.scriptForms.form.ddui;

import cn.nukkit.ddui.CustomForm;
import cn.nukkit.ddui.Observable;
import cn.nukkit.ddui.element.DropdownElement;
import cn.nukkit.ddui.element.SliderElement;
import cn.nukkit.ddui.element.TextFieldElement;
import cn.nukkit.ddui.element.ToggleElement;
import cn.nukkit.ddui.element.options.DropdownOptions;
import cn.nukkit.ddui.element.options.SliderElementOptions;
import cn.nukkit.ddui.element.options.TextFieldOptions;
import cn.nukkit.ddui.element.options.ToggleOptions;

import java.util.List;

/**
 * Extended CustomForm that can access protected layout field
 * to support Observable label for toggle/slider/dropdown/textfield
 */
public class DDUICustomForm extends CustomForm {

    public DDUICustomForm() {
        super();
    }

    public DDUICustomForm(String title) {
        super(title);
    }

    public DDUICustomForm(Observable<String> title) {
        super(title);
    }

    /**
     * TextField with Observable label
     */
    public DDUICustomForm textFieldWithObservableLabel(Observable<String> label,
                                                        Observable<String> value,
                                                        TextFieldOptions options) {
        TextFieldElement element = new TextFieldElement(label.getValue(), value, options, layout);
        element.setLabel(label);
        layout.setProperty(element);
        return this;
    }

    /**
     * Toggle with Observable label
     */
    public DDUICustomForm toggleWithObservableLabel(Observable<String> label, 
                                                     Observable<Boolean> toggled, 
                                                     ToggleOptions options) {
        ToggleElement element = new ToggleElement(label.getValue(), toggled, options, layout);
        element.setLabel(label);
        layout.setProperty(element);
        return this;
    }

    /**
     * Slider with Observable label
     */
    public DDUICustomForm sliderWithObservableLabel(Observable<String> label,
                                                     long min, long max,
                                                     Observable<Long> value,
                                                     SliderElementOptions options) {
        SliderElement element = new SliderElement(label.getValue(), value, min, max, options, layout);
        element.setLabel(label);
        layout.setProperty(element);
        return this;
    }

    /**
     * Dropdown with Observable label
     */
    public DDUICustomForm dropdownWithObservableLabel(Observable<String> label,
                                                       List<DropdownElement.Item> items,
                                                       Observable<Long> selected,
                                                       DropdownOptions options) {
        DropdownElement element = new DropdownElement(label.getValue(), items, selected, options, layout);
        element.setLabel(label);
        layout.setProperty(element);
        return this;
    }
}
