package ru.violence.graaljs.model.context;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.graalvm.polyglot.Context;
import org.jetbrains.annotations.NotNull;
import ru.violence.graaljs.GraalJSPlugin;
import ru.violence.graaljs.model.engine.ExecutionResult;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class RuntimeExecutor {
    private static final UUID CONSOLE_UUID = UUID.fromString("00000000-0000-0000-0000-000000000000");

    private final GraalJSPlugin plugin;
    private final Map<UUID, ContextInfo> contexts = new HashMap<>();

    public RuntimeExecutor(GraalJSPlugin plugin) {
        this.plugin = plugin;

        plugin.getServer().getScheduler().runTaskTimer(plugin, () -> {
            for (ContextInfo info : new HashMap<>(contexts).values()) {
                if (System.currentTimeMillis() - info.getLastAccessTime() > plugin.getConfiguration().runtimeExecutingContextLifetimeMillis) {
                    remove(info.getExecutorUniqueId());
                }
            }
        }, 0, 20 * 60 /* 60 seconds */);
    }

    public @NotNull ExecutionResult execute(@NotNull CommandSender executor, @NotNull String script) {
        Context context = getOrCreate(executor).getContext();
        return plugin.getJsExecutor().execute(context, script);
    }

    public @NotNull ExecutionResult execute(@NotNull CommandSender executor, @NotNull File file) {
        Context context = getOrCreate(executor).getContext();
        return plugin.getJsExecutor().execute(context, file);
    }

    public @NotNull ContextInfo getOrCreate(@NotNull CommandSender executor) {
        UUID uniqueId = resolveUniqueId(executor);
        ContextInfo info = contexts.get(uniqueId);
        if (info == null) {
            info = new ContextInfo(uniqueId, plugin.getJsExecutor().createContext(Map.of(
                    "bukkitClassLoader", plugin.getClass().getClassLoader(),
                    "sender", executor)
            ));
            contexts.put(uniqueId, info);
        } else {
            info.updateLastAccessTime();
        }
        return info;
    }

    public void remove(@NotNull CommandSender executor) {
        remove(resolveUniqueId(executor));
    }

    public void remove(@NotNull UUID executorUniqueId) {
        ContextInfo info = contexts.remove(executorUniqueId);
        if (info != null) info.context.close();
    }

    private @NotNull UUID resolveUniqueId(@NotNull CommandSender executor) {
        if (executor instanceof Player player) {
            return player.getUniqueId();
        } else {
            return CONSOLE_UUID;
        }
    }

    public static class ContextInfo {
        private final @NotNull UUID executorUniqueId;
        private final @NotNull Context context;

        private long lastAccessTime;

        public ContextInfo(@NotNull UUID executorUniqueId, @NotNull Context context) {
            this.executorUniqueId = executorUniqueId;
            this.context = context;
            updateLastAccessTime();
        }

        public @NotNull UUID getExecutorUniqueId() {
            return executorUniqueId;
        }

        public @NotNull Context getContext() {
            return context;
        }

        public long getLastAccessTime() {
            return lastAccessTime;
        }

        public void updateLastAccessTime() {
            this.lastAccessTime = System.currentTimeMillis();
        }
    }
}
