package ru.violence.graaljs.model.engine.js;

import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Source;
import org.graalvm.polyglot.Value;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import ru.violence.graaljs.GraalJSPlugin;
import ru.violence.graaljs.model.engine.ExecutionResult;
import ru.violence.graaljs.model.engine.ScriptExecutor;

import java.io.File;
import java.util.Map;

public class JSExecutor implements ScriptExecutor {
    private final @NotNull ClassLoader pluginClassLoader;

    public JSExecutor(@NotNull GraalJSPlugin plugin, @NotNull ClassLoader classLoader) {
        this.pluginClassLoader = classLoader;
    }

    @Override
    public @NotNull ExecutionResult execute(@NotNull String script, @Nullable Map<String, Object> bindings) {
        if (script.isEmpty()) {
            return new ExecutionResult(ExecutionResult.Result.EXECUTION_ERROR, "Input is empty", null);
        }

        try (Context context = createContext(bindings)) {
            return execute(context, script);
        }
    }

    @Override
    public @NotNull ExecutionResult execute(@NotNull Context context, @NotNull String script) {
        if (script.isEmpty()) {
            return new ExecutionResult(ExecutionResult.Result.EXECUTION_ERROR, "Input is empty", null);
        }

        try {
            Value result = context.eval(GraalJSPlugin.JS_LANG_ID, script);
            return new ExecutionResult(ExecutionResult.Result.SUCCESS, result.toString(), result);
        } catch (Exception ex) {
            return new ExecutionResult(ExecutionResult.Result.EXECUTION_ERROR, ex.getMessage(), null);
        }
    }

    @Override
    public @NotNull ExecutionResult execute(@NotNull Context context, @NotNull File file) {
        if (!file.exists()) {
            return new ExecutionResult(ExecutionResult.Result.EXECUTION_ERROR, "File does not exists", null);
        }

        try {
            return execute(context, Source.newBuilder(GraalJSPlugin.JS_LANG_ID, file).build());
        } catch (Exception ex) {
            return new ExecutionResult(ExecutionResult.Result.EXECUTION_ERROR, ex.getMessage(), null);
        }
    }

    @Override
    public @NotNull Context createContext(@Nullable Map<String, Object> bindings) {
        Context context = Context.newBuilder(GraalJSPlugin.JS_LANG_ID).hostClassLoader(this.pluginClassLoader).allowAllAccess(true).build();

        if (bindings != null) {
            for (Map.Entry<String, Object> entry : bindings.entrySet()) {
                context.getBindings(GraalJSPlugin.JS_LANG_ID).putMember(entry.getKey(), entry.getValue());
            }
        }

        return context;
    }

    public ExecutionResult execute(@NotNull Context context, @NotNull Source source) {
        try {
            Value result = context.eval(source);
            return new ExecutionResult(ExecutionResult.Result.SUCCESS, result.toString(), result);
        } catch (Exception ex) {
            return new ExecutionResult(ExecutionResult.Result.EXECUTION_ERROR, ex.getMessage(), null);
        }
    }
}
