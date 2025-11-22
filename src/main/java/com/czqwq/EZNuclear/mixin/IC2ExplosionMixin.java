package com.czqwq.EZNuclear.mixin;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import net.minecraft.server.MinecraftServer;
import net.minecraft.util.ChatComponentTranslation;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.czqwq.EZNuclear.Config;

import cpw.mods.fml.common.FMLCommonHandler;

@Mixin(value = ic2.core.ExplosionIC2.class, remap = false)
public class IC2ExplosionMixin {

    @Unique
    private static final ScheduledExecutorService SCHEDULER = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "EZNuclear-IC2ExplosionScheduler");
        t.setDaemon(true);
        return t;
    });

    static {
        System.out.println("[EZNuclear] IC2ExplosionMixin loaded");
    }

    // Flag to allow the deferred explosion to run once without being re-cancelled
    @Unique
    private volatile boolean eznuclear_ignoreNext = false;

    @Inject(method = "doExplosion", at = @At("HEAD"), cancellable = true)
    private void onDoExplosion(CallbackInfo ci) {
        // Check if IC2 explosions are disabled in config
        if (!Config.IC2Explosion) {
            ci.cancel();
            return;
        }

        // If this is the deferred invocation, allow it to proceed once
        if (eznuclear_ignoreNext) {
            eznuclear_ignoreNext = false;
            return;
        }

        // Only run on server side -- use server global instead of shadowing world
        MinecraftServer server = FMLCommonHandler.instance()
            .getMinecraftServerInstance();
        if (server == null) return;

        // Send initial message to players (run on server thread directly)
        try {
            server.getConfigurationManager()
                .sendChatMsg(new ChatComponentTranslation("info.ezunclear"));
        } catch (Throwable t) {
            t.printStackTrace();
        }

        // Cancel immediate explosion and schedule the real one after 5 seconds
        ci.cancel();

        SCHEDULER.schedule(() -> {
            MinecraftServer s = FMLCommonHandler.instance()
                .getMinecraftServerInstance();
            if (s != null) {
                try {
                    s.getConfigurationManager()
                        .sendChatMsg(new ChatComponentTranslation("info.ezunclear.interact"));
                } catch (Throwable t) {
                    t.printStackTrace();
                }

                // Set flag so the next doExplosion invocation is allowed through
                eznuclear_ignoreNext = true;

                // Invoke the original method on the target instance
                try {
                    ((ic2.core.ExplosionIC2) (Object) this).doExplosion();
                } catch (Throwable t) {
                    t.printStackTrace();
                }
            }
        }, 5L, TimeUnit.SECONDS);
    }
}
