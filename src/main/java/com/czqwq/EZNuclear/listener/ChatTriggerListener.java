package com.czqwq.EZNuclear.listener;

import net.minecraftforge.event.ServerChatEvent;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;

public class ChatTriggerListener {

    // 是否启用全局触发已不再使用，保留兼容但不再依赖它
    public static volatile boolean eznuclear_triggerExplosion = false;

    // 新增：最后一次触发的时间戳（毫秒）
    public static volatile long eznuclear_lastTriggerAtMillis = 0L;

    @SubscribeEvent
    public void onChat(ServerChatEvent event) {
        if (event == null || event.message == null) return;

        // 触发关键字（可以改成配置或多关键字匹配）
        String trigger = "坏了坏了";

        if (event.message.trim()
            .equalsIgnoreCase(trigger)) {
            eznuclear_triggerExplosion = true;
            eznuclear_lastTriggerAtMillis = System.currentTimeMillis();
        }
    }

    /**
     * 消耗一次触发：只有当触发时间在 [windowStart, windowEnd] 内才返回 true，
     * 并且一旦返回 true，就清除触发标志，避免影响后续爆炸。
     */
    public static boolean consumeTriggerInWindow(long windowStartMillis, long windowEndMillis) {
        long t = eznuclear_lastTriggerAtMillis;
        boolean ok = (t >= windowStartMillis) && (t <= windowEndMillis);
        // 消耗一次
        eznuclear_triggerExplosion = false;
        eznuclear_lastTriggerAtMillis = 0L;
        return ok;
    }
}
