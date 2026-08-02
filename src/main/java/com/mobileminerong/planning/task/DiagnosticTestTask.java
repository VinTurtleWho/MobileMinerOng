package com.mobileminerong.planning.task;

import com.mobileminerong.context.BotContext;
import com.mobileminerong.diagnostic.DiagnosticManager;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;


public class DiagnosticTestTask implements BotTask {


    private boolean finished = false;

    private static long lastRun = 0;

    private static final long COOLDOWN = 3000;


    @Override
    public void onStart(BotContext ctx) {

        long now = System.currentTimeMillis();


        if(now - lastRun < COOLDOWN) {
            return;
        }


        lastRun = now;


        Minecraft client = Minecraft.getInstance();


        if(client.player == null)
            return;



        DiagnosticManager.event(
                "Diagnostic run started"
        );


        client.player.sendSystemMessage(
                Component.literal(
                "§a[MobileMinerOng] Diagnostic Started"
                )
        );


        DiagnosticManager.report(
                "BOT",
                "State: " + ctx.getCurrentState()
        );


        DiagnosticManager.report(
                "POSITION",
                "Player: " + ctx.getPlayerPos()
        );


        DiagnosticManager.report(
                "ROTATION",
                "Yaw: " + ctx.getYRot()
                +
                " Pitch: "
                +
                ctx.getXRot()
        );


        DiagnosticManager.event(
                "Diagnostic run complete"
        );


        finished=true;

    }



    @Override
    public void onTick(BotContext ctx){}



    @Override
    public boolean isFinished(BotContext ctx){
        return finished;
    }



    @Override
    public void onFailure(
            BotContext ctx,
            String reason
    ){

        DiagnosticManager.error(
                "Diagnostic failed: "
                + reason
        );

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
