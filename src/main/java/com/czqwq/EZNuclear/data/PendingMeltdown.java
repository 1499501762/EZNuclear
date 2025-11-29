package com.czqwq.EZNuclear.data;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import net.minecraft.util.ChunkCoordinates;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.czqwq.EZNuclear.TaskBus;

public class PendingMeltdown {

    private static final CopyOnWriteArrayList<Scheduled> SCHEDULED = new CopyOnWriteArrayList<>();
    private static final Set<PosKey> POSITIONS = java.util.Collections.newSetFromMap(new ConcurrentHashMap<>());
    private static final Set<PosKey> REENTRY = java.util.Collections.newSetFromMap(new ConcurrentHashMap<>());
    private static final Logger LOGGER = LogManager.getLogger(PendingMeltdown.class);

    private static class Scheduled {

        final long executeAtMillis;
        final Runnable task;
        final PosKey pos;

        Scheduled(long executeAtMillis, Runnable task, PosKey pos) {
            this.executeAtMillis = executeAtMillis;
            this.task = task;
            this.pos = pos;
        }
    }

    public static final class PosKey {

        public final int x, y, z, dim;

        public PosKey(ChunkCoordinates c, int dim) {
            this.x = c.posX;
            this.y = c.posY;
            this.z = c.posZ;
            this.dim = dim;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof PosKey)) return false;
            PosKey k = (PosKey) o;
            return k.x == x && k.y == y && k.z == z && k.dim == dim;
        }

        @Override
        public int hashCode() {
            int h = x;
            h = 31 * h + y;
            h = 31 * h + z;
            h = 31 * h + dim;
            return h;
        }

        @Override
        public String toString() {
            return "PosKey{" + x + "," + y + "," + z + " @ " + dim + "}";
        }
    }

    public static boolean schedule(ChunkCoordinates pos, Runnable task, long delayMs, int dimension) {
        if (pos == null || task == null) return false;
        final PosKey key = new PosKey(pos, dimension);
        if (!POSITIONS.add(key)) return false; // already pending
        final long execAt = System.currentTimeMillis() + Math.max(0, delayMs);
        LOGGER.info("PendingMeltdown.schedule: {} delayMs={} execAt={}", key, delayMs, execAt);
        SCHEDULED.add(new Scheduled(execAt, wrapMarkAndRun(task, key), key));
        return true;
    }

    public static void markReentry(ChunkCoordinates pos, int dimension) {
        REENTRY.add(new PosKey(pos, dimension));
    }

    public static boolean consumeReentry(ChunkCoordinates pos, int dimension) {
        return REENTRY.remove(new PosKey(pos, dimension));
    }

    public static java.util.Map<ChunkCoordinates, Integer> getPending() {
        java.util.Map<ChunkCoordinates, Integer> map = new java.util.HashMap<>();
        for (PosKey k : POSITIONS) {
            map.put(new ChunkCoordinates(k.x, k.y, k.z), k.dim);
        }
        return map;
    }

    public static int getScheduledCount() {
        return SCHEDULED.size();
    }

    // If you keep central ticking, otherwise remove this method entirely.
    public static void tick(long nowMillis) {
        List<Scheduled> due = new ArrayList<>();
        for (Scheduled s : SCHEDULED) {
            if (s.executeAtMillis <= nowMillis) due.add(s);
        }
        if (due.isEmpty()) return;

        SCHEDULED.removeAll(due);
        for (Scheduled s : due) {
            try {
                TaskBus.postToWorldTick(s.task);
            } catch (Throwable t) {
                LOGGER.error("PendingMeltdown.tick: error posting task for {}: {}", s.pos, t.getMessage());
                POSITIONS.remove(s.pos);
                REENTRY.remove(s.pos);
            }
        }
    }

    private static Runnable wrapMarkAndRun(Runnable task, PosKey key) {
        return () -> {
            try {
                task.run();
            } finally {
                POSITIONS.remove(key);
                REENTRY.remove(key);
            }
        };
    }
}
