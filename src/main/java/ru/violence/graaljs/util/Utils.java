package ru.violence.graaljs.util;

import org.jetbrains.annotations.NotNull;
import ru.violence.graaljs.model.script.Placeholder;

import java.io.File;

public final class Utils {
    private Utils() {}

    public static boolean isFileInsideFolder(@NotNull File baseFolder, @NotNull File file) {
        File parentFile = file.getParentFile();

        while (parentFile != null) {
            if (parentFile.equals(baseFolder)) {
                return true;
            }
            parentFile = parentFile.getParentFile();
        }

        return false;
    }

    public static String[] parseArgs(@NotNull String identifier, @NotNull Placeholder placeholder, @NotNull String separator) {
        // Has no arguments
        if (identifier.length() <= placeholder.getIdentifier().length()) return null;

        String rawArgs = identifier.substring(placeholder.getIdentifier().length() + 1); // Trim leading "identifier + '#'"
        return rawArgs.split(separator);
    }
}
