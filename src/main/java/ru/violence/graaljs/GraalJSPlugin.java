package ru.violence.graaljs;

import com.mojang.brigadier.arguments.StringArgumentType;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.plugin.lifecycle.event.LifecycleEventManager;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.plugin.Plugin;
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
    public void onEnable() {
        saveDefaultConfig();
        configuration = new Configuration(this.getConfig());

        this.scriptsFolder = new File(getDataFolder(), "scripts");

        this.scriptExecutor = new JSExecutor(this, getClassLoader());
        this.runtimeExecutor = new RuntimeExecutor(this);

        if (Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            this.placeholderAPIHook = new PlaceholderAPIHook(this);
        }

        registerCommand();

        Bukkit.getScheduler().runTask(this, () -> {
            if (placeholderAPIHook != null) {
                placeholderAPIHook.getScriptRegistry().loadScripts(this);
            }
        });
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

    @SuppressWarnings("UnstableApiUsage")
    private void registerCommand() {
        LifecycleEventManager<@NotNull Plugin> manager = this.getLifecycleManager();
        manager.registerEventHandler(LifecycleEvents.COMMANDS, event -> event.registrar().register(
                Commands.literal("graaljs")
                        .requires(s -> s.getSender().hasPermission("graaljs.admin"))
                        .then(Commands.literal("reload")
                                .executes(ctx -> {
                                    reloadConfig();
                                    configuration = new Configuration(this.getConfig());

                                    if (placeholderAPIHook != null) {
                                        placeholderAPIHook.getScriptRegistry().loadScripts(this);
                                    }

                                    ctx.getSource().getSender().sendMessage("Plugin reloaded!");
                                    return 0;
                                })
                        )
                        .then(Commands.literal("runtime")
                                .then(Commands.literal("execute")
                                        .then(Commands.argument("script", StringArgumentType.greedyString())
                                                .executes(ctx -> {
                                                    CommandSender sender = ctx.getSource().getSender();

                                                    if (!configuration.runtimeExecutingEnabled) {
                                                        sender.sendMessage(miniMessage().deserialize("<red>Runtime executing is disabled!"));
                                                        return 0;
                                                    }

                                                    String script = ctx.getArgument("script", String.class);

                                                    if (configuration.runtimeExecutingOnlyConsole && !(sender instanceof ConsoleCommandSender)) {
                                                        sender.sendMessage(miniMessage().deserialize("<red>Only console can execute scripts!"));
                                                        return 0;
                                                    }

                                                    ExecutionResult result = runtimeExecutor.execute(sender, script);

                                                    TextComponent hover = null;

                                                    hover:
                                                    if (configuration.runtimeExecutingHoverTooltipEnabled) {
                                                        Value value = result.getValue();
                                                        if (value == null || !value.isHostObject())
                                                            break hover;

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

                                                    return 0;
                                                }))
                                )
                                .then(Commands.literal("script")
                                        .then(Commands.argument("file", StringArgumentType.string())
                                                .suggests((ctx, builder) -> {
                                                    getScriptFileNames().stream()
                                                            .filter(entry -> entry.toLowerCase().startsWith(builder.getRemainingLowerCase()))
                                                            .forEach(builder::suggest);

                                                    return builder.buildFuture();
                                                })
                                                .executes(ctx -> {
                                                    CommandSender sender = ctx.getSource().getSender();

                                                    if (!configuration.runtimeExecutingEnabled) {
                                                        sender.sendMessage(miniMessage().deserialize("<red>Runtime executing is disabled!"));
                                                        return 0;
                                                    }

                                                    String fileName = ctx.getArgument("file", String.class);

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
                                                        return 0;
                                                    }

                                                    if (configuration.runtimeExecutingOnlyConsole && !(sender instanceof ConsoleCommandSender)) {
                                                        sender.sendMessage(miniMessage().deserialize("<red>Only console can execute scripts!"));
                                                        return 0;
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
                                                    return 0;
                                                }))
                                )
                                .then(Commands.literal("clear")
                                        .executes(ctx -> {
                                            CommandSender sender = ctx.getSource().getSender();
                                            runtimeExecutor.remove(sender);
                                            sender.sendMessage("Context cleared!");
                                            return 0;
                                        }))
                        )
                        .build()));
    }

    private @NotNull List<String> getScriptFileNames() {
        List<String> fileNames = new ArrayList<>();

        Path scriptsFolderPath = scriptsFolder.toPath();
        try (Stream<Path> pathStream = Files.walk(scriptsFolderPath, 1)
                .skip(1)
                .filter(path -> {
                    if (!Files.isRegularFile(path)) return false;
                    String fileName = path.getFileName().toString();
                    return fileName.length() > 3 && fileName.endsWith(".js");
                })) {
            pathStream.forEach(path -> fileNames.add(path.getFileName().toString()));
        } catch (IOException ignored) {}

        return fileNames;
    }
}
