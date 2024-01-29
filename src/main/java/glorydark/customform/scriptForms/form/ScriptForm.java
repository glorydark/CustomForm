package glorydark.customform.scriptForms.form;

import cn.nukkit.Player;
import cn.nukkit.form.response.FormResponse;
import cn.nukkit.form.window.FormWindow;
import glorydark.customform.scriptForms.data.SoundData;

public interface ScriptForm {
    void execute(Player player, FormWindow respondWindow, FormResponse response, Object... params);

    FormWindow getWindow(Player player);

    SoundData getOpenSound();

    long getStartMillis();

    long getExpiredMillis();
}