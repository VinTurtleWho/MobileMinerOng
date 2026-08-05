package com.mobileminerong.perception;

import com.mobileminerong.context.BotContext;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;

public class PerceptionManager {
    public static void updatePerception(BotContext ctx) {
        int timer = ctx.getPerceptionTimer();
        if (timer++ < 5) {
            ctx.setPerceptionTimer(timer);
            return;
        }
        ctx.setPerceptionTimer(0);

        Minecraft client = Minecraft.getInstance();
        if (client.level == null || client.player == null) return;

        if (ctx.getMode() == com.mobileminerong.state.MacroMode.COMBAT) {
            Entity nearest = null;
            double minDist = Double.MAX_VALUE;
            
            for (Entity entity : client.level.entitiesForRendering()) {
                if (entity == client.player || !entity.isAlive()) continue;
                if (entity instanceof Player || entity instanceof Monster) {
                    double dist = entity.distanceToSqr(client.player);
                    if (dist < minDist) {
                        minDist = dist;
                        nearest = entity;
                    }
                }
            }
            ctx.setTargetEntity(nearest);
        } else {
            ctx.setTargetEntity(null);
        }
    }
}
