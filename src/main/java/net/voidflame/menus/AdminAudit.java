package net.voidflame.menus;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

final class AdminAudit {
    private final Path file;
    private final ExecutorService writer;

    AdminAudit(VoidFlameMenusPlugin plugin) {
        Path dir = plugin.getDataFolder().toPath().resolve("logs");
        this.file = dir.resolve("admin.log");
        this.writer = Executors.newSingleThreadExecutor(r -> {
            Thread thread = new Thread(r, "VoidFlame-Admin-LogWriter");
            thread.setDaemon(true);
            return thread;
        });
        try {
            Files.createDirectories(dir);
        } catch (IOException e) {
            plugin.getLogger().warning("Could not create Admin log directory: " + e.getMessage());
        }
    }

    void log(String actor, String action) {
        String line = Instant.now() + " | actor=" + safe(actor) + " | action=" + safe(action)
                + System.lineSeparator();
        writer.execute(() -> {
            try {
                Files.writeString(file, line, StandardCharsets.UTF_8,
                        StandardOpenOption.CREATE, StandardOpenOption.WRITE, StandardOpenOption.APPEND);
            } catch (IOException ignored) {
                // Logging must never interrupt an administrative action.
            }
        });
    }

    void shutdown() {
        writer.shutdown();
        try {
            if (!writer.awaitTermination(2, TimeUnit.SECONDS)) writer.shutdownNow();
        } catch (InterruptedException interrupted) {
            writer.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    private static String safe(String value) {
        return value == null ? "" : value.replace("\\", "/")
                .replace("\r", " ").replace("\n", " ").replace("|", "/");
    }
}
