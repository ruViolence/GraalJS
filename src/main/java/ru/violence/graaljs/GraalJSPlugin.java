package ru.violence.graaljs;

import dev.jorel.commandapi.CommandAPI;
import dev.jorel.commandapi.CommandAPIBukkitConfig;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.arguments.ArgumentSuggestions;
import dev.jorel.commandapi.arguments.GreedyStringArgument;
import dev.jorel.commandapi.arguments.StringArgument;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.plugin.java.JavaPlugin;
import org.graalvm.polyglot.Value;
import org.jetbrains.annotations.NotNull;
import ru.violence.graaljs.config.Configuration;
import ru.violence.graaljs.hook.papi.PlaceholderAPIHook;
import ru.violence.graaljs.model.context.RuntimeExecutor;
import ru.violence.graaljs.model.engine.ExecutionResult;
import ru.violence.graaljs.model.engine.ScriptExecutor;
import ru.violence.graaljs.model.engine.js.JSExecutor;
import ru.violence.graaljs.util.Utils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;

import static net.kyori.adventure.text.Component.text;
import static net.kyori.adventure.text.minimessage.MiniMessage.miniMessage;

public class GraalJSPlugin extends JavaPlugin {
    public static final String JS_LANG_ID = "js";

    private File scriptsFolder;
    private Configuration configuration;

    private ScriptExecutor scriptExecutor;
    private RuntimeExecutor runtimeExecutor;

    private PlaceholderAPIHook placeholderAPIHook;

    @Override
    public void onLoad() {
        CommandAPI.onLoad(new CommandAPIBukkitConfig(this));
    }

    @Override
    public void onEnable() {
        CommandAPI.onEnable();

        saveDefaultConfig();
        configuration = new Configuration(this.getConfig());

        this.scriptsFolder = new File(getDataFolder(), "scripts");

        this.scriptExecutor = new JSExecutor(this, getClassLoader());
        this.runtimeExecutor = new RuntimeExecutor(this);

        if (Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            this.placeholderAPIHook = new PlaceholderAPIHook(this);
        }

        new CommandAPICommand("graaljs")
                .withPermission("graaljs.admin")
                .withSubcommand(new CommandAPICommand("reload")
                        .executes((sender, args) -> {
                            reloadConfig();
                            configuration = new Configuration(this.getConfig());

                            if (placeholderAPIHook != null) {
                                placeholderAPIHook.getScriptRegistry().loadScripts(this);
                            }

                            sender.sendMessage("Plugin reloaded!");
                        })
                )
                .withSubcommand(new CommandAPICommand("runtime")
                        .withSubcommand(new CommandAPICommand("execute")
                                .withArguments(new GreedyStringArgument("script"))
                                .executes((sender, args) -> {
                                    if (!configuration.runtimeExecutingEnabled) {
                                        sender.sendMessage(miniMessage().deserialize("<red>Runtime executing is disabled!"));
                                        return;
                                    }

                                    String script = (String) args.get("script");

                                    if (configuration.runtimeExecutingOnlyConsole && !(sender instanceof ConsoleCommandSender)) {
                                        sender.sendMessage(miniMessage().deserialize("<red>Only console can execute scripts!"));
                                        return;
                                    }

                                    ExecutionResult result = runtimeExecutor.execute(sender, script);

                                    TextComponent hover = null;

                                    hover:
                                    if (configuration.runtimeExecutingHoverTooltipEnabled) {
                                        Value value = result.getValue();
                                        if (value == null || !value.isHostObject()) break hover;

                                        Object valueObject = value.asHostObject();
                                        if (valueObject == null) break hover;

                                        TextComponent.Builder builder = text();

                                        builder.append(miniMessage().deserialize("<yellow>Object: "));
                                        builder.append(text(Objects.toIdentityString(valueObject), NamedTextColor.GRAY));

                                        int maxLines = configuration.runtimeExecutingHoverTooltipMaxMembers;
                                        if (maxLines > 0) {
                                            int lines = 0;
                                            for (String memberKey : value.getMemberKeys().stream().distinct().toList()) {
                                                if (++lines > maxLines) {
                                                    builder.append(miniMessage().deserialize("<br><gray>..."));
                                                    break;
                                                }
                                                builder.append(miniMessage().deserialize("<br> - <green><gray>" + memberKey));
                                            }
                                        }

                                        hover = builder.build();
                                    }

                                    sender.sendMessage(text()
                                            .append(miniMessage().deserialize("<#00FF00>Script executed:<br>"))
                                            .append(result.getOutput().isEmpty()
                                                    ? miniMessage().deserialize("<gray>(empty)")
                                                    : text().append(text(result.getOutput())).hoverEvent(hover)
                                            )
                                            .build());
                                })
                        )
                        .withSubcommand(new CommandAPICommand("script")
                                .withArguments(new StringArgument("file")
                                        .replaceSuggestions(ArgumentSuggestions.stringsAsync(info -> CompletableFuture.supplyAsync(() -> {
                                            String currentArg = info.currentArg();
                                            List<String> fileNames = new ArrayList<>();

                                            Path scriptsFolderPath = scriptsFolder.toPath();
                                            try (Stream<Path> pathStream = Files.walk(scriptsFolderPath, 1)
                                                    .skip(1)
                                                    .filter(path -> {
                                                        if (!Files.isRegularFile(path)) return false;
                                                        String fileName = path.getFileName().toString();
                                                        return fileName.length() > 3 && fileName.endsWith(".js");
                                                    })) {

                                                pathStream.filter(path -> path.getFileName().toString().toLowerCase().startsWith(currentArg.toLowerCase()))
                                                        .forEach(path -> fileNames.add(path.getFileName().toString()));

                                            } catch (IOException ignored) {}

                                            return fileNames.toArray(new String[0]);
                                        }))))
                                .executes((sender, args) -> {
                                    if (!configuration.runtimeExecutingEnabled) {
                                        sender.sendMessage(miniMessage().deserialize("<red>Runtime executing is disabled!"));
                                        return;
                                    }

                                    String fileName = (String) args.get("file");

                                    File scriptFile = null;

                                    if (fileName.endsWith(".js")) {
                                        scriptFile = new File(scriptsFolder, fileName);

                                        if (Utils.isFileInsideFolder(scriptsFolder, scriptFile)) {
                                            if (!scriptFile.exists()) {
                                                scriptFile = null;
                                            }
                                        } else {
                                            scriptFile = null;
                                        }
                                    }

                                    if (scriptFile == null) {
                                        sender.sendMessage(miniMessage().deserialize("<red>Script not found!"));
                                        return;
                                    }

                                    if (configuration.runtimeExecutingOnlyConsole && !(sender instanceof ConsoleCommandSender)) {
                                        sender.sendMessage(miniMessage().deserialize("<red>Only console can execute scripts!"));
                                        return;
                                    }

                                    ExecutionResult result = runtimeExecutor.execute(sender, scriptFile);

                                    TextComponent hover = null;

                                    hover:
                                    if (configuration.runtimeExecutingHoverTooltipEnabled) {
                                        Value value = result.getValue();
                                        if (value == null || !value.isHostObject()) break hover;

                                        Object valueObject = value.asHostObject();
                                        if (valueObject == null) break hover;

                                        TextComponent.Builder builder = text();

                                        builder.append(miniMessage().deserialize("<yellow>Object: "));
                                        builder.append(text(Objects.toIdentityString(valueObject), NamedTextColor.GRAY));

                                        int maxLines = configuration.runtimeExecutingHoverTooltipMaxMembers;
                                        if (maxLines > 0) {
                                            int lines = 0;
                                            for (String memberKey : value.getMemberKeys().stream().distinct().toList()) {
                                                if (++lines > maxLines) {
                                                    builder.append(miniMessage().deserialize("<br><gray>..."));
                                                    break;
                                                }
                                                builder.append(miniMessage().deserialize("<br> - <green><gray>" + memberKey));
                                            }
                                        }

                                        hover = builder.build();
                                    }

                                    sender.sendMessage(text()
                                            .append(miniMessage().deserialize("<#00FF00>Script executed:<br>"))
                                            .append(result.getOutput().isEmpty()
                                                    ? miniMessage().deserialize("<gray>(empty)")
                                                    : text().append(text(result.getOutput())).hoverEvent(hover)
                                            )
                                            .build());
                                })
                        )
                        .withSubcommand(new CommandAPICommand("clear")
                                .executes((sender, args) -> {
                                    runtimeExecutor.remove(sender);
                                    sender.sendMessage("Context cleared!");
                                })
                        )
                )
                .register();
    }

    @Override
    public void onDisable() {
        CommandAPI.onDisable();
    }

    public @NotNull Configuration getConfiguration() {
        return configuration;
    }

    public @NotNull File getScriptsFolder() {
        return scriptsFolder;
    }

    public @NotNull ScriptExecutor getJsExecutor() {
        return scriptExecutor;
    }
}
