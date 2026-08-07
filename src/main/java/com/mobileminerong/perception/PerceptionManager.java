package com.mobileminerong.perception;

import com.mobileminerong.context.BotContext;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
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

        // Player Search Logic
        if (ctx.isPendingPlayerSearch()) {
            Player nearest = null;
            double minDist = Double.MAX_VALUE;
            for (Player p : client.level.players()) {
                if (p == null || p == client.player || !p.isAlive()) continue;
                double dist = p.distanceToSqr(client.player);
                if (dist < minDist) {
                    minDist = dist;
                    nearest = p;
                }
            }
            if (nearest != null) {
                ctx.setTargetPlayer(nearest);
                ctx.setTargetEntity(nearest);
                client.player.sendSystemMessage(Component.literal("§a[MobileMinerOng] Targeting: " + nearest.getName().getString()));
            } else {
                client.player.sendSystemMessage(Component.literal("§c[MobileMinerOng] No players found"));
            }
            ctx.setPendingPlayerSearch(false);
        }

        if (ctx.getMode() == com.mobileminerong.state.MacroMode.COMBAT) {
            // Purge expired blacklist entries
            ctx.getPendingKills().entrySet().removeIf(entry -> System.currentTimeMillis() > entry.getValue());

            Entity nearest = null;
            double minDist = Double.MAX_VALUE;
            
            for (Entity entity : client.level.entitiesForRendering()) {
                if (entity == client.player || !entity.isAlive()) continue;
                
                // Skip blacklisted entities
                if (ctx.getPendingKills().containsKey(entity.getUUID())) continue;

                if (entity instanceof Player || entity instanceof Monster) {
                    double dist = entity.distanceToSqr(client.player);
                    if (dist < minDist) {
                        minDist = dist;
                        nearest = entity;
                    }
                }
            }
            if (nearest != null) {
                ctx.setTargetEntity(nearest);
                com.mobileminerong.debug.DebugLogger.debug("PERCEPTION", "Found combat target: " + nearest.getName().getString());
            }
        } else {
            // Only clear entity if we are not explicitly searching for a player and not in COMBAT mode
            if (!ctx.isPendingPlayerSearch()) {
                ctx.setTargetEntity(null);
            }
        }
    }
}
