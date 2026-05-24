package com.verifymc.util;

import com.verifymc.VerifyMC;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;

/**
 * NeoForge-compatible scheduler for VerifyMC.
 */
public final class PluginScheduler {

    private static final ScheduledExecutorService asyncExecutor = Executors.newScheduledThreadPool(4);
    private static final ExecutorService syncExecutor = Executors.newSingleThreadExecutor();
    private static final ConcurrentHashMap<Long, ScheduledTask> tasks = new ConcurrentHashMap<>();
    private static final AtomicLong taskIdCounter = new AtomicLong(0);

    private PluginScheduler() {
    }

    public static void init() {
        NeoForge.EVENT_BUS.addListener(PluginScheduler::onServerTick);
    }

    public static void shutdown() {
        asyncExecutor.shutdown();
        syncExecutor.shutdown();
        tasks.clear();
    }

    public static ScheduledTask runGlobal(Runnable task) {
        long taskId = taskIdCounter.incrementAndGet();
        ScheduledTask scheduledTask = new ScheduledTaskImpl(taskId, task, 0, 0, false);
        tasks.put(taskId, scheduledTask);
        syncExecutor.submit(task);
        return scheduledTask;
    }

    public static ScheduledTask runAsync(Runnable task) {
        long taskId = taskIdCounter.incrementAndGet();
        ScheduledTask scheduledTask = new ScheduledTaskImpl(taskId, task, 0, 0, false);
        tasks.put(taskId, scheduledTask);
        asyncExecutor.submit(task);
        return scheduledTask;
    }

    public static ScheduledTask runAsyncLater(Runnable task, long delayTicks) {
        long taskId = taskIdCounter.incrementAndGet();
        long executeTick = getCurrentTick() + delayTicks;
        ScheduledTask scheduledTask = new ScheduledTaskImpl(taskId, task, executeTick, 0, false);
        tasks.put(taskId, scheduledTask);
        return scheduledTask;
    }

    public static ScheduledTask runAsyncTimer(Runnable task, long delayTicks, long periodTicks) {
        long taskId = taskIdCounter.incrementAndGet();
        long executeTick = getCurrentTick() + delayTicks;
        ScheduledTask scheduledTask = new ScheduledTaskImpl(taskId, task, executeTick, periodTicks, true);
        tasks.put(taskId, scheduledTask);
        return scheduledTask;
    }

    private static void onServerTick(ServerTickEvent.Post event) {
        incrementTick();
        long currentTick = getCurrentTick();

        tasks.values().removeIf(task -> {
            if (task instanceof ScheduledTaskImpl) {
                ScheduledTaskImpl impl = (ScheduledTaskImpl) task;
                if (impl.isCancelled()) {
                    return true;
                }
                if (currentTick >= impl.getExecuteTick()) {
                    asyncExecutor.submit(() -> {
                        try {
                            impl.getTask().run();
                        } catch (Exception e) {
                            VerifyMC.LOGGER.error("Error executing scheduled task", e);
                        }
                    });

                    if (impl.isRepeating()) {
                        impl.setExecuteTick(currentTick + impl.getPeriodTicks());
                    } else {
                        impl.cancel();
                        return true;
                    }
                }
            }
            return false;
        });
    }

    private static long currentTick = 0;

    private static long getCurrentTick() {
        return currentTick;
    }

    private static void incrementTick() {
        currentTick++;
    }

    public interface ScheduledTask {
        void cancel();
        boolean isCancelled();
    }

    private static class ScheduledTaskImpl implements ScheduledTask {
        private final long taskId;
        private final Runnable task;
        private long executeTick;
        private final long periodTicks;
        private final boolean repeating;
        private volatile boolean cancelled = false;

        public ScheduledTaskImpl(long taskId, Runnable task, long executeTick, long periodTicks, boolean repeating) {
            this.taskId = taskId;
            this.task = task;
            this.executeTick = executeTick;
            this.periodTicks = periodTicks;
            this.repeating = repeating;
        }

        @Override
        public void cancel() {
            cancelled = true;
            tasks.remove(taskId);
        }

        @Override
        public boolean isCancelled() {
            return cancelled;
        }

        public Runnable getTask() {
            return task;
        }

        public long getExecuteTick() {
            return executeTick;
        }

        public void setExecuteTick(long executeTick) {
            this.executeTick = executeTick;
        }

        public long getPeriodTicks() {
            return periodTicks;
        }

        public boolean isRepeating() {
            return repeating;
        }
    }
}
