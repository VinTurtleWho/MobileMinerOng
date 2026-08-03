package com.mobileminerong.state;

import com.mobileminerong.context.BotContext;
import com.mobileminerong.planning.task.BotTask;
import java.util.ArrayList;
import java.util.List;

public class PriorityTaskEngine {
    private final List<BotTask> taskPool = new ArrayList<>();
    private BotTask activeTask = null;
    private BotTask lastCompletedTask = null;
    private BotTask lastFailedTask = null;
    private String lastFailureReason = "NONE";
    private BotTask lastPreemptedTask = null;
    private String lastPreemptedReason = "NONE";

    public void registerTask(BotTask task) {
        taskPool.add(task);
        taskPool.sort((a, b) -> Integer.compare(b.getPriority(), a.getPriority()));
    }

    public void tick(BotContext ctx) {
        long startTime = System.nanoTime();
        
        if (taskPool.isEmpty()) {
            ctx.setLastTickDurationNano(System.nanoTime() - startTime);
            return;
        }

        BotTask highestPriorityTask = taskPool.get(0);

        if (activeTask != highestPriorityTask) {
            if (activeTask != null && !activeTask.isFinished(ctx)) {
                String reason = "Preempted by higher priority task: " + highestPriorityTask.getName();
                ctx.setState(BotState.IDLE, reason);
                lastPreemptedTask = activeTask;
                lastPreemptedReason = reason;
                ctx.addTaskEvent(activeTask.getName(), "PREEMPTED", reason);
            }
            activeTask = highestPriorityTask;
            activeTask.onStart(ctx);
            ctx.addTaskEvent(activeTask.getName(), "STARTED", "Task started");
        }

        if (activeTask != null) {
            if (activeTask.isFinished(ctx)) {
                lastCompletedTask = activeTask;
                ctx.addTaskEvent(activeTask.getName(), "COMPLETED", "Task finished successfully");
                taskPool.remove(activeTask);
                activeTask = null;
            } else {
                activeTask.onTick(ctx);
            }
        }
        
        ctx.setLastTickDurationNano(System.nanoTime() - startTime);
    }
    
    public void reportTaskFailure(BotTask task, String reason) {
        lastFailedTask = task;
        lastFailureReason = reason;
    }

    public BotTask getActiveTask() { return activeTask; }
    public BotTask getLastCompletedTask() { return lastCompletedTask; }
    public BotTask getLastFailedTask() { return lastFailedTask; }
    public String getLastFailureReason() { return lastFailureReason; }
    public BotTask getLastPreemptedTask() { return lastPreemptedTask; }
    public String getLastPreemptedReason() { return lastPreemptedReason; }
    public int getTaskPoolSize() { return taskPool.size(); }
}
