package com.czqwq.EZNuclear.mixin;

import java.lang.reflect.Field;

import net.minecraft.server.MinecraftServer;
import net.minecraft.util.ChatComponentTranslation;
import net.minecraft.util.ChunkCoordinates;
import net.minecraft.world.World;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.brandon3055.brandonscore.common.handlers.ProcessHandler;
import com.brandon3055.draconicevolution.common.tileentities.multiblocktiles.reactor.ReactorExplosion;
import com.czqwq.EZNuclear.Config;
import com.czqwq.EZNuclear.data.PendingMeltdown;
import com.czqwq.EZNuclear.listener.ChatTriggerListener;

import cpw.mods.fml.common.FMLCommonHandler;

/**
 * Mixin for intercepting Draconic Evolution's ReactorExplosion logic,
 * enabling delayed and chat-triggered detonation.
 */
@Mixin(value = ReactorExplosion.class, remap = false)
public abstract class ReactorExplosionMixin {

    private static final Logger LOGGER = LogManager.getLogger("EZNuclear.ReactorExplosionMixin");

    static {
        // Register chat trigger listener
        FMLCommonHandler.instance()
            .bus()
            .register(new ChatTriggerListener());
    }

    /**
     * Intercepts run/onRun method to implement explosion control logic.
     */
    @Inject(method = { "run", "onRun" }, at = @At("HEAD"), cancellable = true, remap = false)
    private void onRun(CallbackInfo ci) {
        // Check if DE explosions are disabled in config
        if (!Config.DEExplosion) {
            ci.cancel();
            return;
        }

        // Chat-triggered explosion mode
        if (Config.RequireChatTrigger) {
            ci.cancel();
            MinecraftServer server = FMLCommonHandler.instance()
                .getMinecraftServerInstance();

            // Schedule a 5-second delay to check for chat trigger
            PendingMeltdown.schedule(new ChunkCoordinates(0, 0, 0), () -> {
                if (ChatTriggerListener.eznuclear_triggerExplosion) {
                    LOGGER.info("ReactorExplosionMixin: chat trigger accepted, allowing explosion");
                } else {
                    if (server != null) {
                        server.getConfigurationManager()
                            .sendChatMsg(new ChatComponentTranslation("§aExplosion cancelled!"));
                    }
                }
                ChatTriggerListener.eznuclear_triggerExplosion = false;
            }, 5L);

            return;
        }

        // ===== Original logic =====
        try {
            // Reflectively access explosion position and world
            Class<?> cls = this.getClass();
            Field xField = cls.getDeclaredField("x");
            Field yField = cls.getDeclaredField("y");
            Field zField = cls.getDeclaredField("z");
            Field worldField = cls.getDeclaredField("world");
            xField.setAccessible(true);
            yField.setAccessible(true);
            zField.setAccessible(true);
            worldField.setAccessible(true);
            int x = xField.getInt(this);
            int y = yField.getInt(this);
            int z = zField.getInt(this);
            World world = (World) worldField.get(this);

            ChunkCoordinates pos = new ChunkCoordinates(x, y, z);

            // If PendingMeltdown already marked reentry, allow original run
            if (PendingMeltdown.consumeReentry(pos)) {
                LOGGER.info("ReactorExplosionMixin: reentry present for {}. allowing run", pos);
                return;
            }

            // Otherwise, cancel original run and schedule delayed explosion
            ci.cancel();
            float power = 10F;
            // TODO: Optionally read actual power value via reflection
            final float fpower = power;

            PendingMeltdown.schedule(pos, () -> {
                try {
                    PendingMeltdown.markReentry(pos);
                    ReactorExplosion newExp = new ReactorExplosion(world, x, y, z, fpower);
                    ProcessHandler.addProcess(newExp);
                } catch (Throwable t) {
                    LOGGER.warn("Error scheduling ReactorExplosion: {}", t.getMessage());
                }
            }, 0L);

        } catch (Throwable t) {
            LOGGER.warn("ReactorExplosionMixin interception failed: {}", t.getMessage());
        }
    }
}
