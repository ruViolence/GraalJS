package ru.violence.graaljs.model.script;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import ru.violence.graaljs.GraalJSPlugin;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.stream.Stream;

public class ScriptRegistry {
    private final Map<String, Placeholder> scriptMap = new HashMap<>();

    public void loadScripts(@NotNull GraalJSPlugin plugin) {
        unregisterAll();

        Path scriptDirectoryPath = plugin.getDataFolder().toPath().resolve("placeholders");
        try {
            Files.createDirectories(scriptDirectoryPath);
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "An error occurred while creating a directory for scripts", e);
        }

        try (Stream<Path> pathStream = Files.walk(scriptDirectoryPath, 1)
                .skip(1)
                .filter(path -> {
                    if (!Files.isRegularFile(path)) return false;
                    String fileName = path.getFileName().toString();
                    return fileName.length() > 3 && fileName.endsWith(".js");
                })) {

            Iterator<Path> iterator = pathStream.iterator();
            while (iterator.hasNext()) {
                Path path = iterator.next();
                String fileName = path.getFileName().toString();
                String identifier = fileName.substring(0, fileName.length() - 3); // Strip ".js"

                if (identifier.isEmpty()) {
                    plugin.getLogger().log(Level.SEVERE, "Illegal script identifier: " + fileName);
                    continue;
                }

                try {
                    String script = Files.readString(path);
                    register(new Placeholder(plugin.getJsExecutor().createContext(null), identifier, script));
                } catch (Exception e) {
                    plugin.getLogger().log(Level.SEVERE, "An error occurred while parsing a script \"" + fileName + "\"", e);
                }
            }

            plugin.getLogger().log(Level.INFO, scriptMap.size() + " PlaceholderAPI scripts loaded");
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "An error occurred while loading scripts", e);
        }
    }

    public void register(@NotNull Placeholder placeholder) {
        this.scriptMap.put(placeholder.getIdentifier(), placeholder);
    }

    public @Nullable Placeholder get(String scriptKey) {
        return this.scriptMap.get(scriptKey);
    }

    public void unregister(@NotNull Placeholder placeholder) {
        this.scriptMap.remove(placeholder.getIdentifier());
        placeholder.getScript().terminate();
    }

    public void unregisterAll() {
        List<Placeholder> placeholders = new ArrayList<>(this.scriptMap.values());
        this.scriptMap.clear();
        for (Placeholder placeholder : placeholders) {
            placeholder.getScript().terminate();
        }
    }

    public @NotNull Map<String, Placeholder> getScriptMap() {
        return new HashMap<>(this.scriptMap);
    }
}
