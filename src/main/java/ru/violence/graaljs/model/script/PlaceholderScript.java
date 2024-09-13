package ru.violence.graaljs.model.script;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface PlaceholderScript {
    @Nullable String onPlaceholderRequest(Object... params);

    @NotNull String onRelPlaceholderRequest(Object... params);

    void terminate();
}
