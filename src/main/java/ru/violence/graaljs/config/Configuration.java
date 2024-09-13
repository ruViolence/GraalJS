package ru.violence.graaljs.config;

import org.bukkit.configuration.MemoryConfiguration;

public class Configuration {
    public final boolean runtimeExecutingEnabled;
    public final boolean runtimeExecutingOnlyConsole;
    public final long runtimeExecutingContextLifetimeMillis;

    public final boolean runtimeExecutingHoverTooltipEnabled;
    public final int runtimeExecutingHoverTooltipMaxMembers;

    public final String placeholderApiArgumentSeparator;

    public Configuration(MemoryConfiguration config) {
        runtimeExecutingEnabled = config.getBoolean("runtime-executing.enabled");
        runtimeExecutingOnlyConsole = config.getBoolean("runtime-executing.only-console");
        runtimeExecutingContextLifetimeMillis = config.getLong("runtime-executing.context-lifetime-millis");

        runtimeExecutingHoverTooltipEnabled = config.getBoolean("runtime-executing.hover-tooltip.enabled");
        runtimeExecutingHoverTooltipMaxMembers = config.getInt("runtime-executing.hover-tooltip.max-members");

        placeholderApiArgumentSeparator = config.getString("placeholderapi.argument-separator");
    }
}
