package com.czqwq.EZNuclear;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

public final class TaskBus {

    private static final Queue<Runnable> WORLD_TICK_TASKS = new ConcurrentLinkedQueue<>();

    private TaskBus() {}

    public static void postToWorldTick(Runnable r) {
        if (r != null) WORLD_TICK_TASKS.add(r);
    }

    public static void drainOnWorldTick() {
        Runnable r;
        while ((r = WORLD_TICK_TASKS.poll()) != null) {
            try {
                r.run();
            } catch (Throwable t) {
                t.printStackTrace();
            }
        }
    }
}
