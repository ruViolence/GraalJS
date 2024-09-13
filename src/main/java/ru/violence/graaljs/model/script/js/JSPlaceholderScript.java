package ru.violence.graaljs.model.script.js;

import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Source;
import org.graalvm.polyglot.Value;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import ru.violence.graaljs.GraalJSPlugin;
import ru.violence.graaljs.model.script.PlaceholderScript;

public class JSPlaceholderScript implements PlaceholderScript {
    private final Context context;
    private final Value bindings;
    private final Value onPlaceholderRequest;
    private final Value onRelPlaceholderRequest;

    public JSPlaceholderScript(Context context, String script) {
        this.context = context;
        this.bindings = context.getBindings(GraalJSPlugin.JS_LANG_ID);
        context.eval(Source.create(GraalJSPlugin.JS_LANG_ID, script)); // First initialize
        Value onInitialize = this.bindings.getMember("onInitialize");
        if (onInitialize != null) onInitialize.executeVoid();
        this.onPlaceholderRequest = this.bindings.getMember("onPlaceholderRequest");
        this.onRelPlaceholderRequest = this.bindings.getMember("onRelPlaceholderRequest");
        if (this.onPlaceholderRequest == null && this.onRelPlaceholderRequest == null) {
            throw new RuntimeException("Script does not contain any onPlaceholder function");
        }
    }

    @Override
    public @Nullable String onPlaceholderRequest(Object... params) {
        synchronized (this) {
            return this.onPlaceholderRequest.execute(params).toString();
        }
    }

    @Override
    public @NotNull String onRelPlaceholderRequest(Object... params) {
        synchronized (this) {
            return this.onRelPlaceholderRequest.execute(params).toString();
        }
    }

    @Override
    public void terminate() {
        synchronized (this) {
            Value onTerminate = this.bindings.getMember("onTerminate");
            if (onTerminate != null) onTerminate.executeVoid();
            this.context.close(true);
        }
    }
}
