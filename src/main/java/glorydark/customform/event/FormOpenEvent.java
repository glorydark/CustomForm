package glorydark.customform.event;

import cn.nukkit.Player;
import cn.nukkit.event.player.PlayerEvent;
import glorydark.customform.scriptForms.form.ScriptForm;

public class FormOpenEvent extends PlayerEvent {

    ScriptForm scriptForm;

    Player player;

    public FormOpenEvent(ScriptForm scriptForm, Player player){
        this.scriptForm = scriptForm;
        this.player = player;
    }

    @Override
    public Player getPlayer() {
        return player;
    }

    public ScriptForm getScriptForm() {
        return scriptForm;
    }
}
