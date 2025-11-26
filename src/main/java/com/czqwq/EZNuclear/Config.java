package com.czqwq.EZNuclear;

import java.io.File;

import net.minecraftforge.common.config.Configuration;

public class Config {

    public static boolean IC2Explosion = true;
    public static boolean DEExplosion = true;

    // 新增开关：是否要求玩家输入关键字才能触发爆炸
    public static boolean RequireChatTrigger = false;

    public static void synchronizeConfiguration(File configFile) {
        Configuration configuration = new Configuration(configFile);

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
            "Require players to type '坏了坏了' within 5 seconds to trigger IC2 nuclear explosion");

        if (configuration.hasChanged()) {
            configuration.save();
        }
    }
}
