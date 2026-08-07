package com.mobileminerong.planning.task;

import com.mobileminerong.context.BotContext;
import com.mobileminerong.control.ActionController;
import com.mobileminerong.state.BotState;
import com.mobileminerong.MobileMinerClient;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Slime;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.Vec3;

public class CombatFollowTask implements BotTask {

    private boolean finished = false;
    private int targetLostTicks = 0;
    private int lastSelectedSlot = -1;
    private boolean hasAttackedOnce = false;
    private Entity lockedTarget = null;

    private java.util.Deque<Vec3> velocityHistory = new java.util.ArrayDeque<>();
    private Vec3 lastPos = null;
    private long lastClickTime = 0;
    private long nextAttackDelay = 0;

    @Override
    public void onStart(BotContext ctx) {
        ctx.setState(BotState.MOVING_TO_TARGET, "Engaging combat");
        lastSelectedSlot = ctx.getCombatToolSlot();
        ActionController.selectHotbarSlot(ctx, lastSelectedSlot);
        ctx.getRotationEngine().setActive(true);
        this.lockedTarget = ctx.getTargetEntity();
        this.finished = false;
        this.targetLostTicks = 0;
        this.lastPos = lockedTarget != null ? lockedTarget.position() : null;
        this.velocityHistory.clear();
        this.lastClickTime = 0;
        this.hasAttackedOnce = false;
        this.nextAttackDelay = com.mobileminerong.control.ClickGenerator.calculateInterval(0, false);
    }

    @Override
    public void onTick(BotContext ctx) {
        try {
            Minecraft client = Minecraft.getInstance();
            if (client == null || client.player == null) return;

            // Weapon check
            if (lastSelectedSlot != ctx.getCombatToolSlot()) {
                lastSelectedSlot = ctx.getCombatToolSlot();
                ActionController.selectHotbarSlot(ctx, lastSelectedSlot);
            }

            // Target filtering
            if (lockedTarget == null || !isValidTarget(ctx, lockedTarget)) {
                Entity newTarget = findTarget(ctx);
                if (newTarget != null) { lockedTarget = newTarget; hasAttackedOnce = false; }
                else { targetLostTicks++; if (targetLostTicks > 100) onFailure(ctx, "Target lost"); return; }
            }

            // Aiming (AimPoint logic)
            Vec3 currentTargetPos = lockedTarget.position();
            if (lastPos != null) {
                Vec3 currentVelocity = currentTargetPos.subtract(lastPos);
                velocityHistory.addLast(currentVelocity);
                if (velocityHistory.size() > 3) velocityHistory.removeFirst();
            }
            lastPos = currentTargetPos;

            Vec3 delayedVelocity = velocityHistory.size() >= 3 ? velocityHistory.peekFirst() : Vec3.ZERO;
            Vec3 aimPoint = lockedTarget.getEyePosition()
                .add(delayedVelocity.scale(5.0));

            double distance = client.player.distanceTo(lockedTarget);
            
            // Movement logic with Forced Sprinting
            if (distance > 2.5) {
                if (!ctx.getRotationEngine().isActive()) {
                    ctx.getRotationEngine().startRotation(client.player.getYRot(), client.player.getXRot(), aimPoint);
                }
                ActionController.setKey(client.options.keyUp, true);
                ActionController.setKey(client.options.keySprint, true);
                client.player.setSprinting(true);
            } else {
                ActionController.setKey(client.options.keyUp, false);
                ActionController.setKey(client.options.keyDown, false);
                ActionController.setKey(client.options.keySprint, false);
                if (!ctx.getRotationEngine().isActive()) {
                    ctx.getRotationEngine().startRotation(client.player.getYRot(), client.player.getXRot(), aimPoint);
                }
            }
            int[] steps = ctx.getRotationEngine().computeNextFrameSteps(aimPoint);
            ctx.setPendingMouseDelta(steps[0], steps[1]);

            // Attack logic (BAS-Synced)
            if (distance <= 3.0) {
                if (System.currentTimeMillis() > lastClickTime + nextAttackDelay) {
                    com.mobileminerong.control.ClickGenerator.performClick();
                    lastClickTime = System.currentTimeMillis();
                    nextAttackDelay = com.mobileminerong.control.ClickGenerator.calculateInterval(0, false);
                }
            }
        } catch (Exception e) {
            com.mobileminerong.debug.DebugLogger.error("COMBAT", "Error: " + e.getMessage());
            onFailure(ctx, "Exception: " + e.getMessage());
        }
    }

    private boolean isValidTarget(BotContext ctx, Entity entity) {
        if (entity == null || !entity.isAlive() || entity.isRemoved() || entity.isInvulnerable() || entity instanceof net.minecraft.world.entity.decoration.ArmorStand) return false;
        if (entity.isInvisible()) return false;
        
        boolean isPlayer = entity instanceof net.minecraft.world.entity.player.Player;
        if (ctx.getCombatTargetType() == BotContext.CombatTargetType.PLAYER) {
            return isPlayer && entity != Minecraft.getInstance().player;
        } else { // MOB MODE
            // Hard-block real players in MOB mode unless whitelist is populated
            if (isPlayer && ctx.getMobWhitelist().isEmpty()) return false;
            return entity != Minecraft.getInstance().player;
        }
    }

    private Entity findTarget(BotContext ctx) {
        Minecraft client = Minecraft.getInstance();
        Entity bestTarget = null;
        double minDistance = Double.MAX_VALUE;
        
        // Optimize search to 15-block radius AABB
        net.minecraft.world.phys.AABB searchBox = client.player.getBoundingBox().inflate(15.0);

        for (Entity entity : client.level.getEntities(null, searchBox)) {
            if (!isValidTarget(ctx, entity)) continue;
            
            if (ctx.getCombatTargetType() == BotContext.CombatTargetType.MOB) {
                boolean isPlayer = entity instanceof net.minecraft.world.entity.player.Player;
                
                if (ctx.getMobWhitelist().isEmpty()) {
                    // NPC Exclusion Fix: Restrict fallback to Mob/Slime
                    boolean isMobOrSlime = (entity instanceof Mob || entity instanceof Slime);
                    if (!isMobOrSlime) continue;
                } else {
                    // Name-Tag Retrieval Fix
                    String rawName = (entity.hasCustomName() && entity.getCustomName() != null) 
                        ? entity.getCustomName().getString() 
                        : entity.getName().getString();
                    String strippedName = rawName.replaceAll("§.", "");
                    
                    boolean whitelisted = false;
                    for (String allowed : ctx.getMobWhitelist()) {
                        if (strippedName.toLowerCase().contains(allowed.toLowerCase())) { whitelisted = true; break; }
                    }
                    if (!whitelisted) continue; // Block players/NPCs if not specifically whitelisted
                }
            }

            double dist = client.player.distanceTo(entity);
            if (dist < minDistance) { minDistance = dist; bestTarget = entity; }
        }
        return bestTarget;
    }

    @Override
    public boolean isFinished(BotContext ctx) {
        return finished;
    }

    @Override
    public void onFailure(BotContext ctx, String reason) {
        ctx.getRotationEngine().abort();
        ActionController.stopAllInputs();
        finished = true;
        ctx.setState(BotState.RECOVERING, "Combat failed: " + reason);
        MobileMinerClient.TASK_ENGINE.reportTaskFailure(this, reason);
    }

    @Override
    public int getPriority() { return 50; }

    @Override
    public String getName() { return "CombatFollowTask"; }
}
