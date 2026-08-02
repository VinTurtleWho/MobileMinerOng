package com.mobileminerong.planning.task;

import com.mobileminerong.context.BotContext;

public interface BotTask {
    void onStart(BotContext ctx);
    void onTick(BotContext ctx);
    boolean isFinished(BotContext ctx);
    void onFailure(BotContext ctx, String reason);
    
    /**
     * Priority spectrum:
     * 100 = Failsafe / Emergency
     * 80  = Hazard Recovery
     * 50  = Combat Defense
     * 20  = Commissions
     * 10  = Standard Mining
     */
    int getPriority();
    String getName();
}
