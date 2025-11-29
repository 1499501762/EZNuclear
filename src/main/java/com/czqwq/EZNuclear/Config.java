package com.czqwq.EZNuclear;

import java.io.File;

import net.minecraftforge.common.config.Configuration;

public class Config {

    public static boolean IC2Explosion = true;
    public static boolean DEExplosion = true;
    public static boolean RequireChatTrigger = true;

    // 新增：爆炸延迟时间（毫秒）
    public static int ExplosionDelayMs = 5000;

    private static Configuration configuration;
    private static File configFile;

    public static void load() {
        if (configFile == null) {
            configFile = new File("config/eznuclear.cfg");
        }
        synchronizeConfiguration(configFile);
    }

    public static void synchronizeConfiguration(File file) {
        configFile = file;
        configuration = new Configuration(file);

        IC2Explosion = configuration.getBoolean(
            "IC2Explosion",
            Configuration.CATEGORY_GENERAL,
            IC2Explosion,
            "Allow IC2 nuclear explosions\nAttention: IC2 NuclearReactor Will Remove after prevent explosion");

        DEExplosion = configuration.getBoolean(
            "DEExplosion",
            Configuration.CATEGORY_GENERAL,
            DEExplosion,
            "Allow Draconic Evolution nuclear explosions");

        RequireChatTrigger = configuration.getBoolean(
            "RequireChatTrigger",
            Configuration.CATEGORY_GENERAL,
            RequireChatTrigger,
            "Require players to type '坏了坏了' within delay time to trigger nuclear explosion");

        ExplosionDelayMs = configuration.getInt(
            "ExplosionDelayMs",
            Configuration.CATEGORY_GENERAL,
            ExplosionDelayMs,
            0,
            60000,
            "Delay before explosion is triggered (in milliseconds)");

        if (configuration.hasChanged()) {
            configuration.save();
        }
    }
}
