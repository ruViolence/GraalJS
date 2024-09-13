package ru.violence.graaljs.model.script;

import org.graalvm.polyglot.Context;
import org.jetbrains.annotations.NotNull;
import ru.violence.graaljs.model.script.js.JSPlaceholderScript;

public class Placeholder {
    private final @NotNull String identifier;
    private final @NotNull PlaceholderScript script;

    public Placeholder(@NotNull Context context, @NotNull String identifier, @NotNull String script) {
        this.identifier = identifier;
        this.script = new JSPlaceholderScript(context, script);
    }

    public @NotNull String getIdentifier() {
        return identifier;
    }

    public @NotNull PlaceholderScript getScript() {
        return script;
    }
}
