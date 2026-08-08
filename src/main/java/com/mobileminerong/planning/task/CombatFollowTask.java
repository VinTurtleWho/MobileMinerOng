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
    private long lastClickTime = 0;
    private long nextAttackDelay = 0;
    private Vec3 currentAimPoint = null;
    private double heightOffset = 0.7;

    @Override
    public void onStart(BotContext ctx) {
        ctx.setState(BotState.MOVING_TO_TARGET, "Engaging combat");
        lastSelectedSlot = ctx.getCombatToolSlot();
        ActionController.selectHotbarSlot(ctx, lastSelectedSlot);
        this.lockedTarget = ctx.getTargetEntity();
        this.finished = false;
        this.targetLostTicks = 0;
        this.lastClickTime = 0;
        this.hasAttackedOnce = false;
        this.nextAttackDelay = com.mobileminerong.control.ClickGenerator.calculateInterval(0, false);
        this.currentAimPoint = null;
        this.heightOffset = 0.6 + (Math.random() * 0.25);
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
                if (newTarget != null) { 
                    lockedTarget = newTarget; 
                    hasAttackedOnce = false;
                    currentAimPoint = null;
                    this.heightOffset = 0.6 + (Math.random() * 0.25);
                }
                else { targetLostTicks++; if (targetLostTicks > 100) onFailure(ctx, "Target lost"); return; }
            }

            // Lead target by 2 ticks with Predictive Error
            Vec3 rawVelocity = lockedTarget.position().subtract(new Vec3(lockedTarget.xo, lockedTarget.yo, lockedTarget.zo));
            
            // Clamp velocity to prevent crosshair snapping during high knockback
            double maxSpeed = 0.6;
            if (rawVelocity.length() > maxSpeed) {
                rawVelocity = rawVelocity.normalize().scale(maxSpeed);
            }
            
            // Fluctuating lead factor (humans guess between 1.0 and 2.5 ticks of lead)
            double leadFactor = 1.0 + (Math.random() * 1.5);
            Vec3 targetVelocity = rawVelocity.scale(leadFactor);
            
            // Add a +/- 5% height jitter that changes every tick
            double heightJitter = (Math.random() * 0.1) - 0.05; 
            currentAimPoint = lockedTarget.position().add(targetVelocity).add(0, lockedTarget.getBbHeight() * (this.heightOffset + heightJitter), 0);

            // Aiming
            double distance = client.player.distanceTo(lockedTarget);
            
            if (currentAimPoint == null) return; // Prevent NPE

            // Movement logic with Forced Sprinting
            if (distance > 2.5) {
                if (!ctx.getRotationEngine().isActive()) {
                    ctx.getRotationEngine().startRotation(client.player.getYRot(), client.player.getXRot(), currentAimPoint);
                } else {

                    ctx.getRotationEngine().updateTarget(currentAimPoint);
                }
                ActionController.setKey(client.options.keyUp, true);
                ActionController.setKey(client.options.keySprint, true);
                client.player.setSprinting(true);
            } else {
                ActionController.setKey(client.options.keyUp, false);
                ActionController.setKey(client.options.keyDown, false);
                ActionController.setKey(client.options.keySprint, false);
                if (!ctx.getRotationEngine().isActive()) {
                    ctx.getRotationEngine().startRotation(client.player.getYRot(), client.player.getXRot(), currentAimPoint);
                } else {

                    ctx.getRotationEngine().updateTarget(currentAimPoint);
                }
            }

            // Attack logic (BAS-Synced)
            if (distance <= 3.0) {
                double attackSpeed = client.player.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_SPEED);
                int bonusAttackSpeed = (int) ((attackSpeed - 4.0) * 20);

                if (System.currentTimeMillis() > lastClickTime + nextAttackDelay) {
                    com.mobileminerong.control.ClickGenerator.performClick();
                    lastClickTime = System.currentTimeMillis();
                    nextAttackDelay = com.mobileminerong.control.ClickGenerator.calculateInterval(bonusAttackSpeed, false);
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
            if (isPlayer && ctx.getMobWhitelist().isEmpty()) return false;
            return entity != Minecraft.getInstance().player;
        }
    }

    private Entity findTarget(BotContext ctx) {
        Minecraft client = Minecraft.getInstance();
        Entity bestTarget = null;
        double minDistance = Double.MAX_VALUE;
        
        net.minecraft.world.phys.AABB searchBox = client.player.getBoundingBox().inflate(15.0);

        for (Entity entity : client.level.getEntities(null, searchBox)) {
            if (!isValidTarget(ctx, entity)) continue;
            
            if (ctx.getCombatTargetType() == BotContext.CombatTargetType.MOB) {
                boolean isPlayer = entity instanceof net.minecraft.world.entity.player.Player;
                
                if (ctx.getMobWhitelist().isEmpty()) {
                    boolean isMobOrSlime = (entity instanceof Mob || entity instanceof Slime);
                    if (!isMobOrSlime) continue;
                } else {
                    String rawName = (entity.hasCustomName() && entity.getCustomName() != null) 
                        ? entity.getCustomName().getString() 
                        : entity.getName().getString();
                    String strippedName = rawName.replaceAll("§.", "");
                    
                    boolean whitelisted = false;
                    for (String allowed : ctx.getMobWhitelist()) {
                        if (strippedName.toLowerCase().contains(allowed.toLowerCase())) { whitelisted = true; break; }
                    }
                    if (!whitelisted) continue; 
                }
            }

            double dist = client.player.distanceTo(entity);
            if (dist < minDistance) { minDistance = dist; bestTarget = entity; }
        }
        return bestTarget;
    }

    @Override
    public boolean isFinished(BotContext ctx) { return finished; }

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
