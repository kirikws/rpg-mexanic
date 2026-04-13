package ru.kirikws.reputationfabric.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import ru.kirikws.reputationfabric.ReputationFabricMod;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Main configuration class for the Reputation Fabric mod.
 * Loads and saves configuration from/to a JSON file.
 * All values can be reloaded at runtime without restarting the server.
 */
public class ReputationConfig {
    private static final String CONFIG_FILE = "reputation-fabric.json";
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private static ReputationConfig INSTANCE;

    // Karma settings
    public int karma_damage_penalty = 5;
    public int karma_kill_penalty = 50;
    public double karma_kill_bonus_multiplier = 0.5;
    public int karma_kill_bonus_min = 10;

    // Karma thresholds
    public int threshold_bad = -30;
    public int threshold_good = 30;

    // Trade settings
    public double trade_penalty_multiplier = 1.5;
    public double trade_bonus_multiplier = 0.8;
    public int trade_block_threshold = -50;

    // Compass settings
    public int compass_radius = 500;
    public int compass_target_karma = -20;
    public int compass_cooldown = 600; // ticks (30 seconds)

    // Mode settings
    public int mode_switch_immobile_time = 100; // ticks (5 seconds)

    /**
     * Loads the configuration from the config file.
     * If the file doesn't exist, creates a default one.
     */
    public static void load() {
        Path configPath = Path.of("config", CONFIG_FILE);

        try {
            if (!Files.exists(configPath)) {
                INSTANCE = new ReputationConfig();
                save();
                ReputationFabricMod.LOGGER.info("Created default configuration file.");
            } else {
                try (Reader reader = Files.newBufferedReader(configPath)) {
                    INSTANCE = GSON.fromJson(reader, ReputationConfig.class);
                    ReputationFabricMod.LOGGER.info("Configuration loaded successfully.");
                }
            }
        } catch (IOException e) {
            ReputationFabricMod.LOGGER.error("Failed to load configuration: {}", e.getMessage(), e);
            INSTANCE = new ReputationConfig();
        }
    }

    /**
     * Saves the current configuration to the config file.
     */
    public static void save() {
        Path configPath = Path.of("config", CONFIG_FILE);

        try {
            Files.createDirectories(configPath.getParent());
            try (Writer writer = Files.newBufferedWriter(configPath)) {
                GSON.toJson(INSTANCE, writer);
            }
            ReputationFabricMod.LOGGER.info("Configuration saved successfully.");
        } catch (IOException e) {
            ReputationFabricMod.LOGGER.error("Failed to save configuration: {}", e.getMessage(), e);
        }
    }

    /**
     * Gets the active configuration instance.
     *
     * @return the current configuration
     */
    public static ReputationConfig get() {
        if (INSTANCE == null) {
            load();
        }
        return INSTANCE;
    }

    /**
     * Reloads the configuration from disk.
     */
    public static void reload() {
        load();
        ReputationFabricMod.LOGGER.info("Configuration reloaded from disk.");
    }
}
