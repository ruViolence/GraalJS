package ru.violence.graaljs.hook.papi;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import me.clip.placeholderapi.expansion.Relational;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import ru.violence.graaljs.GraalJSPlugin;
import ru.violence.graaljs.model.script.Placeholder;
import ru.violence.graaljs.model.script.ScriptRegistry;
import ru.violence.graaljs.util.Utils;

import java.util.logging.Level;

public class PlaceholderAPIHook extends PlaceholderExpansion implements Relational {
    private final GraalJSPlugin plugin;
    private final ScriptRegistry scriptRegistry;

    public PlaceholderAPIHook(@NotNull GraalJSPlugin plugin) {
        this.plugin = plugin;
        this.scriptRegistry = new ScriptRegistry();

        scriptRegistry.loadScripts(plugin);

        register();
    }

    @Override
    public @Nullable String onPlaceholderRequest(@Nullable Player player, @NotNull String identifier) {
        Placeholder placeholder = getJSPlaceholder(identifier);
        if (placeholder == null) return "";

        try {
            String[] args = Utils.parseArgs(identifier, placeholder, plugin.getConfiguration().placeholderApiArgumentSeparator);
            return args != null
                    ? placeholder.getScript().onPlaceholderRequest(player, args)
                    : placeholder.getScript().onPlaceholderRequest(player);
        } catch (Exception e) {
            log(Level.SEVERE, "An error occurred while executing a script \"" + identifier + "\"", e);
            return "Script error (see the console)";
        }
    }

    @Override
    public String onPlaceholderRequest(Player first, Player second, String identifier) {
        Placeholder placeholder = getJSPlaceholder(identifier);
        if (placeholder == null) return "";

        try {
            String[] args = Utils.parseArgs(identifier, placeholder, plugin.getConfiguration().placeholderApiArgumentSeparator);
            return args != null
                    ? placeholder.getScript().onRelPlaceholderRequest(first, second, args)
                    : placeholder.getScript().onRelPlaceholderRequest(first, second);
        } catch (Exception e) {
            log(Level.SEVERE, "An error occurred while executing a script \"" + identifier + "\"", e);
            return "Script error (see the console)";
        }
    }

    private @Nullable Placeholder getJSPlaceholder(String identifier) {
        Placeholder placeholder = this.scriptRegistry.get(identifier);
        if (placeholder != null) return placeholder;

        int separatorIndex = identifier.indexOf('#');
        String scriptKey = separatorIndex != -1 ? identifier.substring(0, separatorIndex) : identifier;
        return this.scriptRegistry.get(scriptKey);
    }

    @Override
    public @NotNull String getIdentifier() {
        return "graaljs";
    }

    @Override
    public @NotNull String getAuthor() {
        return plugin.getDescription().getAuthors().get(0);
    }

    @Override
    public @NotNull String getVersion() {
        return plugin.getDescription().getVersion();
    }

    @Override
    public boolean persist() {
        return true;
    }

    public @NotNull ScriptRegistry getScriptRegistry() {
        return scriptRegistry;
    }
}
