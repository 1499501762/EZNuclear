package com.czqwq.EZNuclear.mixin;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import net.minecraft.server.MinecraftServer;
import net.minecraft.util.ChatComponentTranslation;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.czqwq.EZNuclear.Config;
import com.czqwq.EZNuclear.listener.ChatTriggerListener;

import cpw.mods.fml.common.FMLCommonHandler;

@Mixin(value = ic2.core.ExplosionIC2.class, remap = false)
public class IC2ExplosionMixin {

    // 只保留 ExplosionIC2 自身字段的 Shadow 声明
    @Shadow
    private net.minecraft.world.World worldObj;
    @Shadow
    private float power;
    @Shadow
    private float explosionDropRate;
    @Shadow
    private ic2.core.ExplosionIC2.Type type;
    @Shadow
    private int radiationRange;
    @Shadow
    private net.minecraft.entity.EntityLivingBase igniter;

    @Unique
    private static final ScheduledExecutorService SCHEDULER = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "EZNuclear-IC2ExplosionScheduler");
        t.setDaemon(true);
        return t;
    });

    @Unique
    private static volatile boolean eznuclear_allowNextExplosion = false;

    static {
        System.out.println("[EZNuclear] IC2ExplosionMixin loaded");
        FMLCommonHandler.instance()
            .bus()
            .register(new ChatTriggerListener());
    }

    @Unique
    private volatile boolean eznuclear_ignoreNext = false;
    @Unique
    private volatile boolean eznuclear_pendingCountdown = false;

    // 缓存爆炸参数
    @Unique
    private net.minecraft.world.World cachedWorld;
    @Unique
    private double cachedX, cachedY, cachedZ;
    @Unique
    private float cachedPower, cachedDrop;
    @Unique
    private Object cachedType;
    @Unique
    private net.minecraft.entity.EntityLivingBase cachedIgniter;
    @Unique
    private int cachedRadiation;

    @Inject(method = "doExplosion", at = @At("HEAD"), cancellable = true)
    private void onDoExplosion(CallbackInfo ci) {
        if (eznuclear_allowNextExplosion) {
            eznuclear_allowNextExplosion = false;
            return;
        }

        if (!Config.IC2Explosion) {
            ci.cancel();
            return;
        }

        if (Config.RequireChatTrigger) {
            ci.cancel();

            final MinecraftServer server = FMLCommonHandler.instance()
                .getMinecraftServerInstance();

            if (!eznuclear_pendingCountdown) {
                eznuclear_pendingCountdown = true;

                cacheExplosionFieldsIfNeeded();

                if (server != null) {
                    server.getConfigurationManager()
                        .sendChatMsg(new ChatComponentTranslation("§c现在的发电量是10A"));
                    server.getConfigurationManager()
                        .sendChatMsg(new ChatComponentTranslation("§c核电,轻而易举啊"));
                }

                SCHEDULER.schedule(() -> {
                    runOnServerThread(() -> {
                        try {
                            if (ChatTriggerListener.eznuclear_triggerExplosion) {
                                try {
                                    triggerNewExplosionFromCache();
                                    if (server != null) {
                                        server.getConfigurationManager()
                                            .sendChatMsg(new ChatComponentTranslation("§c爆炸已被触发！"));
                                    }
                                } catch (Throwable t) {
                                    t.printStackTrace();
                                }
                            } else {
                                if (server != null) {
                                    server.getConfigurationManager()
                                        .sendChatMsg(new ChatComponentTranslation("§a爆炸已被取消！"));
                                }
                            }
                        } finally {
                            eznuclear_pendingCountdown = false;
                            ChatTriggerListener.eznuclear_triggerExplosion = false;
                        }
                    });
                }, 5L, TimeUnit.SECONDS);
            }

            return;
        }

        if (eznuclear_ignoreNext) {
            eznuclear_ignoreNext = false;
            return;
        }

        MinecraftServer server = FMLCommonHandler.instance()
            .getMinecraftServerInstance();
        if (server == null) return;

        try {
            server.getConfigurationManager()
                .sendChatMsg(new ChatComponentTranslation("info.ezunclear"));
        } catch (Throwable t) {
            t.printStackTrace();
        }

        ci.cancel();

        SCHEDULER.schedule(() -> {
            runOnServerThread(() -> {
                MinecraftServer s = FMLCommonHandler.instance()
                    .getMinecraftServerInstance();
                if (s != null) {
                    try {
                        s.getConfigurationManager()
                            .sendChatMsg(new ChatComponentTranslation("info.ezunclear.interact"));
                    } catch (Throwable t) {
                        t.printStackTrace();
                    }

                    eznuclear_ignoreNext = true;

                    try {
                        cacheExplosionFieldsIfNeeded();
                        triggerNewExplosionFromCache();
                    } catch (Throwable t) {
                        t.printStackTrace();
                    }
                }
            });
        }, 5L, TimeUnit.SECONDS);
    }

    @Unique
    private void cacheExplosionFieldsIfNeeded() {
        try {
            Class<?> baseExplosionClass = net.minecraft.world.Explosion.class;

            cachedX = getField(baseExplosionClass, "field_77284_b").getDouble(this);
            cachedY = getField(baseExplosionClass, "field_77285_c").getDouble(this);
            cachedZ = getField(baseExplosionClass, "field_77282_d").getDouble(this);
            cachedIgniter = igniter;
            cachedWorld = worldObj;
            cachedPower = power;
            cachedDrop = explosionDropRate;
            cachedRadiation = radiationRange;
            cachedType = type;

            System.out.println("[EZNuclear] Cached explosion parameters:");
            System.out.println("  world = " + cachedWorld);
            System.out.println("  coords = (" + cachedX + ", " + cachedY + ", " + cachedZ + ")");
            System.out.println("  power = " + cachedPower);
            System.out.println("  dropRate = " + cachedDrop);
            System.out.println("  radiation = " + cachedRadiation);
            System.out.println("  type = " + (cachedType != null ? cachedType.toString() : "null"));
            System.out
                .println("  igniter = " + (cachedIgniter != null ? cachedIgniter.getCommandSenderName() : "null"));
        } catch (Throwable t) {
            System.err.println("[EZNuclear] Failed to cache explosion parameters:");
            t.printStackTrace();
        }
    }

    @Unique
    private Field getField(Class<?> cls, String name) throws NoSuchFieldException {
        Field f = cls.getDeclaredField(name);
        f.setAccessible(true);
        return f;
    }

    @Unique
    private void triggerNewExplosionFromCache() throws Exception {
        if (cachedType == null) {
            cacheExplosionFieldsIfNeeded();
            if (cachedType == null) {
                System.err.println("[EZNuclear] cachedType is still null; aborting explosion to avoid NPE.");
                return;
            }
        }

        Class<?> explosionClass = Class.forName("ic2.core.ExplosionIC2");
        Class<?> typeClass = cachedType.getClass();

        Constructor<?> ctor = explosionClass.getConstructor(
            net.minecraft.world.World.class,
            net.minecraft.entity.Entity.class,
            double.class,
            double.class,
            double.class,
            float.class,
            float.class,
            typeClass,
            net.minecraft.entity.EntityLivingBase.class,
            int.class);

        Object newExplosion = ctor.newInstance(
            cachedWorld,
            null,
            cachedX,
            cachedY,
            cachedZ,
            cachedPower,
            cachedDrop,
            cachedType,
            cachedIgniter,
            cachedRadiation);

        eznuclear_allowNextExplosion = true;

        Method doExplosion = explosionClass.getMethod("doExplosion");
        doExplosion.invoke(newExplosion);
    }

    @Unique
    private void runOnServerThread(Runnable task) {
        final net.minecraft.world.World boundWorld = this.worldObj;
        com.czqwq.EZNuclear.TaskBus.postToWorldTick(() -> {
            // 如果需要，可以在这里检查 MinecraftServer/WorldServer 状态
            task.run();
        });
    }
}
