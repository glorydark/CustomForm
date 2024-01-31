package glorydark.customform.scriptForms.form;

import cn.nukkit.Player;
import cn.nukkit.form.response.FormResponse;
import cn.nukkit.form.window.FormWindow;
import glorydark.customform.scriptForms.data.SoundData;
import glorydark.customform.scriptForms.data.requirement.Requirements;

import java.util.List;

public interface ScriptForm {

    List<Requirements> getOpenRequirements();

    void execute(Player player, FormWindow respondWindow, FormResponse response, Object... params);

    FormWindow getWindow(Player player);

    SoundData getOpenSound();

    long getStartMillis();

    long getExpiredMillis();

    List<PermissionEnum> getOpenPermissions();

    List<String> getOpenPermissionWhitelist();
}