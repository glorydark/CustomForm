package glorydark.nukkit.customform.script;

import cn.nukkit.Server;
import cn.nukkit.event.Event;
import cn.nukkit.event.EventPriority;
import cn.nukkit.event.Listener;
import cn.nukkit.plugin.EventExecutor;
import glorydark.nukkit.customform.CustomFormMain;
import org.mozilla.javascript.*;

import javax.script.*;
import java.io.IOException;
import java.io.Reader;

public class RhinoScriptEngine implements ScriptEngine {

    private Scriptable scope;

    public void initScope(Context context) {
        if (this.scope == null) {
            this.scope = new ImporterTopLevel(context);
            Object oldFn = ScriptableObject.getProperty(scope, "importClass");

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

                    Object eventClass = args[0];
                    Function handler = (Function) args[1];
                    String priority = args.length > 2 ? args[2].toString() : "NORMAL";

                    return registerSingleEvent(scope, eventClass, handler, priority);
                }
            });

            ScriptableObject.putProperty(scope, "onEvents", new BaseFunction() {
                @Override
                public Object call(Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
                    if (args.length < 2) {
                        throw new IllegalArgumentException("onEvents() requires at least 2 arguments: eventHandlers and eventClass(es)");
                    }

                    if (!(args[0] instanceof Scriptable)) {
                        throw new IllegalArgumentException("First argument must be an object with event handlers");
                    }

                    Scriptable handlersObj = (Scriptable) args[0];
                    String defaultPriority = "NORMAL";

                    // 确定优先级参数的位置
                    int eventArgsStart = 1;
                    int eventArgsEnd = args.length;

                    // 检查最后一个参数是否是字符串优先级
                    if (args.length > 1 && args[args.length - 1] instanceof String) {
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
                        if (!(handlerId instanceof String)) {
                            continue;
                        }

                        String handlerName = (String) handlerId;
                        Object handlerObj = handlersObj.get(handlerName, handlersObj);

                        if (!(handlerObj instanceof Function)) {
                            continue;
                        }

                        Function handler = (Function) handlerObj;

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

                private Class<? extends Event> resolveEventClass(Object eventArg) {
                    if (eventArg instanceof NativeJavaClass) {
                        Class<?> clazz = ((NativeJavaClass) eventArg).getClassObject();
                        if (Event.class.isAssignableFrom(clazz)) {
                            @SuppressWarnings("unchecked")
                            Class<? extends Event> eventClass = (Class<? extends Event>) clazz;
                            return eventClass;
                        }
                    } else if (eventArg instanceof Class) {
                        Class<?> clazz = (Class<?>) eventArg;
                        if (Event.class.isAssignableFrom(clazz)) {
                            @SuppressWarnings("unchecked")
                            Class<? extends Event> eventClass = (Class<? extends Event>) clazz;
                            return eventClass;
                        }
                    }
                    return null;
                }
            });
        }
    }

    private Object registerSingleEvent(Scriptable scope, Object eventClass, Function handler, String priority) {
        // 创建EventExecutor
        EventExecutor executor = (listener, event) -> {
            Context cx1 = Context.enter();
            try {
                handler.call(cx1, scope, scope, new Object[]{event});
            } catch (Exception e) {
                CustomFormMain.fakeScriptPlugin.getLogger().error("Event handler error", e);
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
                CustomFormMain.fakeScriptPlugin,
                false
        );

        if (CustomFormMain.debug) {
            CustomFormMain.plugin.getLogger().info("Registered event: " + eventClass.getClass().getSimpleName());
        }
        return null;
    }

    @Override
    public Object eval(String script) throws ScriptException {
        Context ctx = Context.enter();
        try {
            ctx.setLanguageVersion(Context.VERSION_ES6);
            ctx.setOptimizationLevel(-1);

            this.initScope(ctx);

            if (script.contains("return")) {
                script = "(function(){\n" + script + "\n})();";
            }

            return ctx.evaluateString(scope, script, "<script>", 1, null);
        } catch (Exception e) {
            throw new ScriptException(e.getMessage());
        } finally {
            Context.exit();
        }
    }

    @Override
    public void put(String key, Object value) {
        Context ctx = Context.enter();
        try {
            this.initScope(ctx);
            Object jsValue = Context.javaToJS(value, scope);
            ScriptableObject.putProperty(scope, key, jsValue);
        } finally {
            Context.exit();
        }
    }

    @Override
    public Object get(String key) {
        if (scope == null) return null;
        return ScriptableObject.getProperty(scope, key);
    }

    // ========== 以下是接口必须实现的方法，简单处理 ==========

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