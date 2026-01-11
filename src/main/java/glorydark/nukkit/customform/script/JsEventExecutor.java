package glorydark.nukkit.customform.script;

import cn.nukkit.event.Event;
import cn.nukkit.event.Listener;
import cn.nukkit.plugin.EventExecutor;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Function;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;

public class JsEventExecutor implements EventExecutor {

    private final Scriptable jsObject;
    private final Function executeFunction;

    public JsEventExecutor(Scriptable jsObject) {
        this.jsObject = jsObject;
        Object executeProp = ScriptableObject.getProperty(jsObject, "execute");
        if (executeProp instanceof Function) {
            this.executeFunction = (Function) executeProp;
        } else {
            throw new IllegalArgumentException("JavaScript object must have an 'execute' function");
        }
    }

    @Override
    public void execute(Listener listener, Event event) {
        Context cx = Context.enter();
        try {
            cx.setLanguageVersion(Context.VERSION_ES6);
            cx.setOptimizationLevel(-1);

            Scriptable scope = executeFunction.getParentScope();
            if (scope == null) {
                scope = cx.initStandardObjects();
            }

            executeFunction.call(cx, scope, jsObject, new Object[]{listener, event});
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            Context.exit();
        }
    }
}