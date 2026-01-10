package glorydark.nukkit.customform.script;

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
        }
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