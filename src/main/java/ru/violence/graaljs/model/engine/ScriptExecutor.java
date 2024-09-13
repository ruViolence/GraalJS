package ru.violence.graaljs.model.engine;

import org.graalvm.polyglot.Context;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.Map;

public interface ScriptExecutor {
    @NotNull ExecutionResult execute(@NotNull String script, @Nullable Map<String, Object> bindings);

    @NotNull ExecutionResult execute(@NotNull Context context, @NotNull String script);

    @NotNull ExecutionResult execute(@NotNull Context context, @NotNull File file);

    @NotNull Context createContext(@Nullable Map<String, Object> bindings);
}
