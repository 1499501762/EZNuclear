package com.czqwq.EZNuclear.listener;

import net.minecraftforge.event.ServerChatEvent;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;

public class ChatTriggerListener {

    public static volatile boolean eznuclear_triggerExplosion = false;

    @SubscribeEvent
    public void onPlayerChat(ServerChatEvent event) {
        if ("坏了坏了".equals(event.message.trim())) {
            eznuclear_triggerExplosion = true;
        }
    }
}
