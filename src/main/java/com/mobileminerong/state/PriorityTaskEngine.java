package com.mobileminerong.state;

import com.mobileminerong.context.BotContext;
import com.mobileminerong.planning.task.BotTask;
import java.util.ArrayList;
import java.util.List;

public class PriorityTaskEngine {
    private final List<BotTask> taskPool = new ArrayList<>();
    private BotTask activeTask = null;

    public void registerTask(BotTask task) {
        taskPool.add(task);
        taskPool.sort((a, b) -> Integer.compare(b.getPriority(), a.getPriority()));
    }

    public void tick(BotContext ctx) {
        long startTime = System.currentTimeMillis();
        
        if (taskPool.isEmpty()) {
            ctx.setLastTickDuration(System.currentTimeMillis() - startTime);
            return;
        }

        BotTask highestPriorityTask = taskPool.get(0);

        if (activeTask != highestPriorityTask) {
            if (activeTask != null && !activeTask.isFinished(ctx)) {
                ctx.setState(BotState.IDLE, "Preempted by higher priority task: " + highestPriorityTask.getName());
            }
            activeTask = highestPriorityTask;
            activeTask.onStart(ctx);
        }

        if (activeTask != null) {
            if (activeTask.isFinished(ctx)) {
                taskPool.remove(activeTask);
                activeTask = null;
            } else {
                activeTask.onTick(ctx);
            }
        }
        
        ctx.setLastTickDuration(System.currentTimeMillis() - startTime);
    }
}
