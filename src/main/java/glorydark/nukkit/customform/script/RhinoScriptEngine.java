package glorydark.nukkit.customform.script;

import cn.nukkit.Server;
import cn.nukkit.event.Event;
import cn.nukkit.event.EventPriority;
import cn.nukkit.plugin.EventExecutor;
import cn.nukkit.plugin.Plugin;
import glorydark.nukkit.customform.CustomFormMain;
import org.mozilla.javascript.*;

import javax.script.*;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class RhinoScriptEngine implements ScriptEngine {

    public static final Map<Scriptable, ScriptPluginData> PLUGINS = new LinkedHashMap<>();

    private static final Map<Scriptable, List<Function>> onDisableHandlers = new LinkedHashMap<>();

    public Scriptable getPlugin(String name) {
        for (Map.Entry<Scriptable, ScriptPluginData> entry : PLUGINS.entrySet()) {
            Scriptable scope = entry.getKey();
            ScriptPluginData pluginData = entry.getValue();
            if (pluginData.name().equals(name)) {
                return scope;
            }
        }
        return null;
    }

    public Plugin getPlugin(Scriptable scriptable) {
        ScriptPluginData pluginData = PLUGINS.get(scriptable);
        if (pluginData == null) {
            return CustomFormMain.getFakeScriptPlugin();
        } else {
            return CustomFormMain.getFakeScriptPlugin(pluginData.name());
        }
    }

    public Scriptable createFreshScope(Context context, ScriptPluginData pluginData) {
        Scriptable scope = new ImporterTopLevel(context);
        RhinoScriptEngine.PLUGINS.put(scope, pluginData);
        Object oldFn = ScriptableObject.getProperty(scope, "importClass");

        ScriptableObject.putProperty(scope, "regPluginInfo", new BaseFunction() {
            @Override
            public Object call(Context cx, Scriptable sc, Scriptable thisObj, Object[] args) {
                return null;
            }
        });

        ScriptableObject.putProperty(scope, "importClass", new BaseFunction() {
            @Override
            public Object call(Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
                if (args.length == 0) {
                    return Context.getUndefinedValue();
                }

                // 取类的简单名
                Object clazz = args[0];
                String simpleName = null;
                if (clazz instanceof NativeJavaClass) {
                    simpleName = ((NativeJavaClass) clazz).getClassObject().getSimpleName();
                } else if (clazz instanceof Class<?>) {
                    simpleName = ((Class<?>) clazz).getSimpleName();
                }

                if (simpleName != null && !ScriptableObject.hasProperty(scope, simpleName)) {
                    if (oldFn instanceof Function) {
                        ((Function) oldFn).call(cx, scope, thisObj, args);
                    }
                }
                return Context.getUndefinedValue();
            }
        });

        // 然后在scope中添加两个函数
        ScriptableObject.putProperty(scope, "onEvent", new BaseFunction() {
            @Override
            public Object call(Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
                if (args.length < 2) {
                    throw new IllegalArgumentException("onEvent() requires at least 2 arguments: eventClass and handler");
                }

                // 解析事件类
                Object eventArg = args[0];
                Class<? extends Event> eventClass = resolveEventClass(eventArg);

                if (eventClass == null) {
                    throw new IllegalArgumentException("Could not resolve event class from argument: " + eventArg);
                }

                // 验证处理器
                if (!(args[1] instanceof Function handler)) {
                    throw new IllegalArgumentException("Second argument must be a function");
                }

                // 解析优先级
                String priority = "NORMAL";
                if (args.length > 2 && args[2] instanceof String) {
                    String priorityStr = args[2].toString().toUpperCase();
                    try {
                        EventPriority.valueOf(priorityStr);
                        priority = priorityStr;
                    } catch (IllegalArgumentException e) {
                        CustomFormMain.plugin.getLogger().warning("Invalid priority: " + priorityStr + ", using NORMAL instead");
                    }
                }

                // 注册事件
                registerSingleEvent(scope, eventClass, handler, priority);

                CustomFormMain.plugin.getLogger().debug("Registered single handler for event " +
                        eventClass.getSimpleName() + " with priority " + priority);

                return true; // 返回成功标志
            }
        });

        ScriptableObject.putProperty(scope, "onEvents", new BaseFunction() {
            @Override
            public Object call(Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
                if (args.length < 2) {
                    throw new IllegalArgumentException("onEvents() requires at least 2 arguments: eventHandlers and eventClass(es)");
                }

                if (!(args[0] instanceof Scriptable handlersObj)) {
                    throw new IllegalArgumentException("First argument must be an object with event handlers");
                }

                String defaultPriority = "NORMAL";

                // 确定优先级参数的位置
                int eventArgsStart = 1;
                int eventArgsEnd = args.length;

                // 检查最后一个参数是否是字符串优先级
                if (args[args.length - 1] instanceof String) {
                    String lastArg = args[args.length - 1].toString().toUpperCase();
                    try {
                        EventPriority.valueOf(lastArg); // 验证是否是有效优先级
                        defaultPriority = lastArg;
                        eventArgsEnd = args.length - 1;
                    } catch (IllegalArgumentException e) {
                        // 不是优先级，保持原样
                    }
                }

                // 遍历所有事件处理器
                Object[] handlerIds = handlersObj.getIds();
                for (Object handlerId : handlerIds) {
                    if (!(handlerId instanceof String handlerName)) {
                        continue;
                    }

                    Object handlerObj = handlersObj.get(handlerName, handlersObj);

                    if (!(handlerObj instanceof Function handler)) {
                        continue;
                    }

                    // 为每个事件类注册这个处理器
                    for (int i = eventArgsStart; i < eventArgsEnd; i++) {
                        try {
                            Class<? extends Event> eventClass = resolveEventClass(args[i]);

                            if (eventClass == null) {
                                CustomFormMain.plugin.getLogger().warning("Could not resolve event class from argument " + i + ": " + args[i]);
                                continue;
                            }

                            registerSingleEvent(scope, eventClass, handler, defaultPriority);

                            if (CustomFormMain.debug) {
                                CustomFormMain.plugin.getLogger().info("Registered event: " + eventClass.getName() +
                                        " with handler '" + handlerName +
                                        "' and priority: " + defaultPriority);
                            }

                        } catch (Exception e) {
                            CustomFormMain.plugin.getLogger().error("Error registering event for handler " + handlerName, e);
                        }
                    }
                }

                return null;
            }
        });

        ScriptableObject.putProperty(scope, "onDisable", new BaseFunction() {
            @Override
            public Object call(Context cx, Scriptable scr, Scriptable thisObj, Object[] args) {
                if (args.length == 1 && args[0] instanceof Function) {
                    onDisableHandlers.computeIfAbsent(scr, o->new ArrayList<>()).add((Function) args[0]);
                }
                return Undefined.instance;
            }
        });

        return scope;
    }

    public void onDisablePlugins() {
        for (Map.Entry<Scriptable, List<Function>> entry : onDisableHandlers.entrySet()) {
            Scriptable scope = entry.getKey();
            for (Function function : entry.getValue()) {
                Context cx = Context.enter();
                try {
                    function.call(cx, scope, scope, new Object[]{});
                } finally {
                    Context.exit();
                }
            }
        }
        onDisableHandlers.clear();
    }

    public void onDisablePlugin(String pluginName) {
        Scriptable scope = getPlugin(pluginName);
        if (scope == null) {
            CustomFormMain.getFakeScriptPlugin().getLogger().warning("Cannot disable plugin " + pluginName + ": plugin not found!");
            return;
        }
        for (Function function : onDisableHandlers.getOrDefault(scope, new ArrayList<>())) {
            Context cx = Context.enter();
            try {
                function.call(cx, scope, scope, new Object[]{});
            } finally {
                Context.exit();
            }
        }
        onDisableHandlers.remove(scope);
    }

    private Class<? extends Event> resolveEventClass(Object eventArg) {
        if (eventArg instanceof NativeJavaClass) {
            Class<?> clazz = ((NativeJavaClass) eventArg).getClassObject();
            if (Event.class.isAssignableFrom(clazz)) {
                @SuppressWarnings("unchecked")
                Class<? extends Event> eventClass = (Class<? extends Event>) clazz;
                return eventClass;
            }
        } else if (eventArg instanceof Class<?> clazz) {
            if (Event.class.isAssignableFrom(clazz)) {
                @SuppressWarnings("unchecked")
                Class<? extends Event> eventClass = (Class<? extends Event>) clazz;
                return eventClass;
            }
        }
        return null;
    }

    private Object registerSingleEvent(Scriptable scope, Object eventClass, Function handler, String priority) {
        // 创建EventExecutor
        EventExecutor executor = (listener, event) -> {
            Context cx1 = Context.enter();
            try {
                handler.call(cx1, scope, scope, new Object[]{event});
            } catch (Exception e) {
                CustomFormMain.getFakeScriptPlugin().getLogger().error("Event handler error", e);
            } finally {
                Context.exit();
            }
        };

        // 注册事件
        EventPriority ep = EventPriority.valueOf(priority.toUpperCase());
        Server.getInstance().getPluginManager().registerEvent(
                (Class<? extends Event>) eventClass,
                JsListener.LISTENER,
                ep,
                executor,
                getPlugin(scope),
                false
        );

        if (CustomFormMain.debug) {
            CustomFormMain.plugin.getLogger().info("Registered event: " + eventClass.getClass().getSimpleName());
        }
        return null;
    }
    @Override
    public Object eval(String script)  {
        return new UnsupportedOperationException("Unsupported operation, use evalScriptable(String script) instead");
    }

    public void put(Scriptable scope, String key, Object value) {
        Object jsValue = Context.javaToJS(value, scope);
        ScriptableObject.putProperty(scope, key, jsValue);
    }

    public Object get(Scriptable scope, String key) {
        return ScriptableObject.getProperty(scope, key);
    }

    @Override
    public void put(String key, Object value) {
        throw new UnsupportedOperationException(
                "Per-script scope: use eval(script, bindings) instead of put()");
    }

    @Override
    public Object get(String key) {
        throw new UnsupportedOperationException(
                "Per-script scope: use eval(script, bindings)");
    }

    @Override
    public Object eval(Reader reader) throws ScriptException {
        StringBuilder sb = new StringBuilder();
        try {
            int ch;
            while ((ch = reader.read()) != -1) {
                sb.append((char) ch);
            }
            return eval(sb.toString());
        } catch (IOException e) {
            throw new ScriptException(e.getMessage());
        }
    }

    @Override
    public Object eval(String script, ScriptContext context) throws ScriptException {
        return eval(script);
    }

    @Override
    public Object eval(Reader reader, ScriptContext context) throws ScriptException {
        return eval(reader);
    }

    @Override
    public Object eval(String script, Bindings bindings) throws ScriptException {
        if (bindings != null) {
            for (String key : bindings.keySet()) {
                put(key, bindings.get(key));
            }
        }
        return eval(script);
    }

    @Override
    public Object eval(Reader reader, Bindings bindings) throws ScriptException {
        if (bindings != null) {
            for (String key : bindings.keySet()) {
                put(key, bindings.get(key));
            }
        }
        return eval(reader);
    }

    @Override
    public Bindings getBindings(int scopeType) { return null; }

    @Override
    public void setBindings(Bindings bindings, int scopeType) { }

    @Override
    public Bindings createBindings() { return new SimpleBindings(); }

    @Override
    public ScriptContext getContext() { return null; }

    @Override
    public void setContext(ScriptContext context) { }

    @Override
    public ScriptEngineFactory getFactory() { return null; }
}