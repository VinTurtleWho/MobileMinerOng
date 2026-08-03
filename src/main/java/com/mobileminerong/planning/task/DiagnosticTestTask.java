package com.mobileminerong.planning.task;

import com.mobileminerong.context.BotContext;
import com.mobileminerong.diagnostic.DiagnosticManager;
import com.mobileminerong.state.BotState;
import com.mobileminerong.state.PriorityTaskEngine;
import com.mobileminerong.MobileMinerClient;

public class DiagnosticTestTask implements BotTask {

    private boolean finished = false;
    private static long lastRun = 0;
    private static final long COOLDOWN = 3000;

    @Override
    public void onStart(BotContext ctx) {
        ctx.setState(BotState.RECOVERING, "Diagnostic Starting");

        long now = System.currentTimeMillis();
        if(now - lastRun < COOLDOWN) {
            ctx.setState(BotState.IDLE, "Diagnostic on cooldown");
            finished = true; 
            return;
        }

        lastRun = now;

        DiagnosticManager.event("Diagnostic run started");
        
        DiagnosticManager.report("BOT", "State: " + ctx.getCurrentState());
        DiagnosticManager.report("POSITION", "Player: " + ctx.getPlayerPos());
        DiagnosticManager.report("ROTATION", "Yaw: " + ctx.getYRot() + " Pitch: " + ctx.getXRot());
        
        DiagnosticManager.event("Diagnostic run complete");

        finished=true;
        ctx.setState(BotState.IDLE, "Diagnostic Finished");
    }

    @Override
    public void onTick(BotContext ctx){}

    @Override
    public boolean isFinished(BotContext ctx){
        return finished;
    }

    @Override
    public void onFailure(BotContext ctx, String reason){
        ctx.setState(BotState.ERROR, "Diagnostic Failed: " + reason);
        MobileMinerClient.TASK_ENGINE.reportTaskFailure(this, reason);
        ctx.addTaskEvent(getName(), "FAILED", reason);
        DiagnosticManager.error("Diagnostic failed: " + reason);
        finished = true;
    }

    @Override
    public int getPriority(){
        return 100;
    }

    @Override
    public String getName(){
        return "DiagnosticTestTask";
    }
}
