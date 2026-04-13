package ru.totemguard.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Configuration for TotemGuard mod.
 * Loaded from config/totemguard.json on startup.
 */
public class TotemConfig {
    private static final String CONFIG_FILE = "totemguard.json";
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static TotemConfig INSTANCE;

    // Region settings
    public int base_radius = 16;
    public long drain_interval_ticks = 1200; // 60 seconds = 1200 ticks
    public int guest_grace_period_seconds = 30;
    public int guest_karma_warning_seconds = 60;
    public int guest_karma_drain_per_minute = 1;
    public int boundary_view_radius = 64;

    // Trade multipliers
    public double copper_multiplier = 1.0;
    public double silver_multiplier = 1.5;
    public double gold_multiplier = 2.5;

    public static TotemConfig get() {
        if (INSTANCE == null) {
            load();
        }
        return INSTANCE;
    }

    public static void load() {
        Path configPath = Path.of("config", CONFIG_FILE);
        try {
            if (!Files.exists(configPath)) {
                INSTANCE = new TotemConfig();
                save();
            } else {
                try (Reader reader = Files.newBufferedReader(configPath)) {
                    INSTANCE = GSON.fromJson(reader, TotemConfig.class);
                }
            }
        } catch (IOException e) {
            INSTANCE = new TotemConfig();
        }
    }

    public static void save() {
        Path configPath = Path.of("config", CONFIG_FILE);
        try {
            Files.createDirectories(configPath.getParent());
            try (Writer writer = Files.newBufferedWriter(configPath)) {
                GSON.toJson(INSTANCE, writer);
            }
        } catch (IOException e) {
            // Silent
        }
    }
}
