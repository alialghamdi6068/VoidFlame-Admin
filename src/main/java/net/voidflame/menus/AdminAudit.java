package net.voidflame.menus;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

final class AdminAudit {
    private final Path file;

    AdminAudit(VoidFlameMenusPlugin plugin) {
        Path dir = plugin.getDataFolder().toPath().resolve("logs");
        this.file = dir.resolve("admin.log");
        try { Files.createDirectories(dir); } catch (IOException e) {
            plugin.getLogger().warning("Could not create Admin log directory: " + e.getMessage());
        }
    }

    void log(String actor, String action) {
        String line = java.time.Instant.now() + " | actor=" + actor + " | action=" + action + System.lineSeparator();
        try {
            Files.writeString(file, line, StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.WRITE, StandardOpenOption.APPEND);
        } catch (IOException e) {
            // Never break an administrative action because file logging failed.
        }
    }
}
