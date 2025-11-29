package com.czqwq.EZNuclear.util;

import java.util.List;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.IChatComponent;
import net.minecraft.util.StatCollector;

import cpw.mods.fml.common.FMLCommonHandler;

public class ChatHelper {

    private static final String PREFIX = ""; // 可选：§7[EZNuclear] §r

    public static void broadcast(String keyOrText) {
        broadcast(keyOrText, false);
    }

    public static void broadcast(String keyOrText, boolean forceRaw) {
        MinecraftServer server = FMLCommonHandler.instance()
            .getMinecraftServerInstance();
        if (server == null) return;

        List<EntityPlayerMP> players = server.getConfigurationManager().playerEntityList;
        IChatComponent msg = createComponent(keyOrText, forceRaw);
        for (EntityPlayerMP p : players) {
            p.addChatMessage(msg);
        }
    }

    public static void sendTo(EntityPlayerMP player, String keyOrText) {
        sendTo(player, keyOrText, false);
    }

    public static void sendTo(EntityPlayerMP player, String keyOrText, boolean forceRaw) {
        if (player == null) return;
        player.addChatMessage(createComponent(keyOrText, forceRaw));
    }

    public static IChatComponent createComponent(String keyOrText, boolean forceRaw) {
        if (forceRaw || keyOrText.startsWith("§")) {
            return new ChatComponentText(PREFIX + keyOrText);
        }

        int idx = keyOrText.indexOf(":");
        if (idx > 0) {
            String key = keyOrText.substring(0, idx)
                .trim();
            String suffix = keyOrText.substring(idx + 1)
                .trim();
            String base = StatCollector.translateToLocal(key);
            return new ChatComponentText(PREFIX + base + " " + suffix);
        }

        return new ChatComponentText(PREFIX + StatCollector.translateToLocal(keyOrText));
    }
}
