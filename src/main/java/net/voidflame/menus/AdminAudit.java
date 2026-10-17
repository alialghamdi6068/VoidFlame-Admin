package net.voidflame.menus;

import org.bukkit.Bukkit;
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
        if (writeToCore(actor, action)) return;
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

    private boolean writeToCore(String actor, String action) {
        try {
            Class<?> type = Class.forName("net.voidflame.core.api.AuditLogService");
            var registration = Bukkit.getServicesManager().getRegistration(type);
            if (registration == null || registration.getProvider() == null) return false;
            Object provider = registration.getProvider();
            var method = type.getMethod("log", String.class, String.class, String.class, String.class);
            method.invoke(provider, actor, action, "VoidFlame-Admin", "source=VoidFlame-Admin");
            return true;
        } catch (ReflectiveOperationException | LinkageError ignored) {
            return false;
        }
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
