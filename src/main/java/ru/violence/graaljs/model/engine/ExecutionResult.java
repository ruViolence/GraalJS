package ru.violence.graaljs.model.engine;

import org.graalvm.polyglot.Value;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

public class ExecutionResult {
    private final @NotNull Result result;
    private final @NotNull String output;
    private final @Nullable Value value;

    public ExecutionResult(@NotNull Result result, @NotNull String output, @Nullable Value value) {
        this.result = result;
        this.output = output;
        this.value = value;
    }

    public @NotNull Result getResult() {
        return result;
    }

    public @NotNull String getOutput() {
        return output;
    }

    public @Nullable Value getValue() {
        return value;
    }

    @Override
    public final boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ExecutionResult that)) return false;

        return result == that.result && Objects.equals(output, that.output) && Objects.equals(value, that.value);
    }

    @Override
    public int hashCode() {
        int result1 = result.hashCode();
        result1 = 31 * result1 + Objects.hashCode(output);
        result1 = 31 * result1 + Objects.hashCode(value);
        return result1;
    }

    public enum Result {
        SUCCESS,
        EXECUTION_ERROR,
        IO_ERROR,
        NOT_GRAALVM
    }
}
