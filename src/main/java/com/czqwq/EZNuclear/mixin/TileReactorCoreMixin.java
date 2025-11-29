package com.czqwq.EZNuclear.mixin;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ChunkCoordinates;
import net.minecraft.world.World;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.brandon3055.draconicevolution.common.tileentities.multiblocktiles.reactor.ReactorExplosion;
import com.brandon3055.draconicevolution.common.tileentities.multiblocktiles.reactor.TileReactorCore;
import com.czqwq.EZNuclear.Config;
import com.czqwq.EZNuclear.TaskBus;
import com.czqwq.EZNuclear.data.PendingMeltdown;
import com.czqwq.EZNuclear.listener.ChatTriggerListener;
import com.czqwq.EZNuclear.util.ChatHelper;

@Mixin(value = TileReactorCore.class, remap = false)
public abstract class TileReactorCoreMixin {

    @Unique
    private static final ScheduledExecutorService SCHEDULER = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "EZNuclear-DEExplosionScheduler");
        t.setDaemon(true);
        return t;
    });

    @Inject(method = "goBoom", at = @At("HEAD"), cancellable = true)
    private void interceptGoBoom(CallbackInfo ci) {
        final TileEntity te = (TileEntity) (Object) this;
        final World world = te.getWorldObj();
        final ChunkCoordinates pos = new ChunkCoordinates(te.xCoord, te.yCoord, te.zCoord);
        final int dim = world.provider.dimensionId;

        System.out.println("[EZNuclear] Intercepted goBoom() at dim=" + dim + " " + pos);

        if (!Config.DEExplosion) {
            ci.cancel();
            ChatHelper.broadcast("info.ezunclear.config.disabled");
            return;
        }

        if (PendingMeltdown.consumeReentry(pos, dim)) {
            ChatHelper.broadcast("info.ezunclear.reentry.allowed");
            return;
        }

        ci.cancel();

        final long delayMs = Math.max(0, Config.ExplosionDelayMs);
        final long windowStart = System.currentTimeMillis();
        final long windowEnd = windowStart + delayMs;

        ChatHelper.broadcast("info.ezunclear.countdown");
        if (Config.RequireChatTrigger) {
            ChatHelper.broadcast("info.ezunclear.chattrigger.required");
        }

        PendingMeltdown.schedule(pos, () -> {
            SCHEDULER.schedule(() -> {
                TaskBus.postToWorldTick(() -> {
                    try {
                        PendingMeltdown.markReentry(pos, dim);

                        float power = 10F;
                        try {
                            Field rf = TileReactorCore.class.getField("reactorFuel");
                            Field cf = TileReactorCore.class.getField("convertedFuel");
                            int r = rf.getInt(te);
                            int c = cf.getInt(te);
                            power = 2F + ((r + c) / (10368F + 1F)) * 18F;
                        } catch (Throwable ignored) {}

                        if (Config.RequireChatTrigger) {
                            boolean ok = ChatTriggerListener.consumeTriggerInWindow(windowStart, windowEnd);
                            if (!ok) {
                                ChatHelper.broadcast("info.ezunclear.cancelled");
                                return;
                            }
                        }

                        triggerExplosion(world, te.xCoord, te.yCoord, te.zCoord, power);
                        ChatHelper.broadcast("info.ezunclear.triggered");
                    } catch (Throwable t) {
                        t.printStackTrace();
                        ChatHelper.broadcast("info.ezunclear.failed: " + t.getMessage());
                    }
                });
            }, delayMs, TimeUnit.MILLISECONDS);
        }, delayMs, dim);
    }

    @Unique
    private void triggerExplosion(World world, int x, int y, int z, float power) {
        try {
            ReactorExplosion exp = new ReactorExplosion(world, x, y, z, power);
            Class<?> iProcess = Class.forName("com.brandon3055.brandonscore.common.handlers.IProcess");
            Class<?> handler = Class.forName("com.brandon3055.brandonscore.common.handlers.ProcessHandler");
            Method add = handler.getMethod("addProcess", iProcess);
            add.invoke(null, exp);
            System.out.println("[EZNuclear] ReactorExplosion re-added via reflection");
        } catch (Throwable t) {
            System.err.println("[EZNuclear] Failed to re-add ReactorExplosion: " + t.getMessage());
            try {
                net.minecraft.world.Explosion e = new net.minecraft.world.Explosion(
                    world,
                    null,
                    x + 0.5,
                    y + 0.5,
                    z + 0.5,
                    power);
                e.doExplosionA();
                e.doExplosionB(true);
                world.setBlockToAir(x, y, z);
            } catch (Throwable ex) {
                ex.printStackTrace();
            }
        }
    }
}
