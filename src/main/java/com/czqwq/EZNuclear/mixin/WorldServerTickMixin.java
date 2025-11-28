package com.czqwq.EZNuclear.mixin;

import net.minecraft.world.WorldServer;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.czqwq.EZNuclear.TaskBus;

/**
 * 在 WorldServer 每次实体更新（世界 tick）时，执行我们排队的任务。
 * 这样可确保代码运行在世界 tick 上下文，满足 Hodgepodge 的线程/上下文要求。
 */
@Mixin(value = WorldServer.class, remap = false)
public abstract class WorldServerTickMixin {

    // 1.7.10 的方法名：func_72939_s（updateEntities）
    @Inject(method = "func_72939_s", at = @At("TAIL"))
    private void eznuclear$drainWorldTickTasks(CallbackInfo ci) {
        TaskBus.drainOnWorldTick();
    }
}
