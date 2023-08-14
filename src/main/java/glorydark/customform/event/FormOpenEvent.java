package glorydark.customform.event;

import cn.nukkit.Player;
import cn.nukkit.event.HandlerList;
import cn.nukkit.event.player.PlayerEvent;
import glorydark.customform.scriptForms.form.ScriptForm;

public class FormOpenEvent extends PlayerEvent {

    private static final HandlerList handlers = new HandlerList();
    ScriptForm scriptForm;

    Player player;

    public FormOpenEvent(ScriptForm scriptForm, Player player) {
        this.scriptForm = scriptForm;
        this.player = player;
    }

    public static HandlerList getHandlers() {
        return handlers;
    }

    @Override
    public Player getPlayer() {
        return player;
    }

    public ScriptForm getScriptForm() {
        return scriptForm;
    }
}
