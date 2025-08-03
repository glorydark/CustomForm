package glorydark.nukkit.customform.event;

import cn.nukkit.event.Event;
import cn.nukkit.event.HandlerList;

/**
 * @author glorydark
 */
public class FormLoadEvent extends Event {

    private static final HandlerList handlers = new HandlerList();

    public FormLoadEvent() {

    }

    public static HandlerList getHandlers() {
        return handlers;
    }
}
