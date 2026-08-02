package com.mobileminerong.context;

import com.mobileminerong.state.BotState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

public class BotContext {
    private BotState currentState = BotState.IDLE;
    
    // Perception & Position
    private Vec3d playerPos = Vec3d.ZERO;
    private float yaw = 0f;
    private float pitch = 0f;
    
    // Targets
    private BlockPos currentTargetBlock = null;
    private BlockPos lastSeenTargetBlock = null;
    
    // Skyblock Parsed Stats
    private String currentZone = "Unknown";
    private int currentMana = 0;
    private int maxMana = 0;
    private int currentHealth = 0;
    private int maxHealth = 0;
    private int speedStat = 100;
    
    // Diagnostics & Flags
    private int stuckTicks = 0;
    private String lastAction = "NONE";
    private String stateChangeReason = "Initialization";

    // Getters and Setters
    public synchronized BotState getCurrentState() { return currentState; }
    public synchronized void setState(BotState state, String reason) {
        if (this.currentState != state) {
            this.stateChangeReason = reason;
            this.currentState = state;
        }
    }

    public synchronized Vec3d getPlayerPos() { return playerPos; }
    public synchronized void setPlayerPos(Vec3d pos) { this.playerPos = pos; }

    public synchronized float getYaw() { return yaw; }
    public synchronized float getPitch() { return pitch; }
    public synchronized void setRotations(float yaw, float pitch) {
        this.yaw = yaw;
        this.pitch = pitch;
    }

    public synchronized BlockPos getCurrentTargetBlock() { return currentTargetBlock; }
    public synchronized void setCurrentTargetBlock(BlockPos pos) { 
        if (pos != null) this.lastSeenTargetBlock = pos;
        this.currentTargetBlock = pos; 
    }

    public synchronized String getCurrentZone() { return currentZone; }
    public synchronized void setCurrentZone(String zone) { this.currentZone = zone; }

    public synchronized int getCurrentMana() { return currentMana; }
    public synchronized void setMana(int current, int max) { 
        this.currentMana = current; 
        this.maxMana = max; 
    }

    public synchronized int getStuckTicks() { return stuckTicks; }
    public synchronized void incrementStuckTicks() { this.stuckTicks++; }
    public synchronized void resetStuckTicks() { this.stuckTicks = 0; }

    public synchronized String getLastAction() { return lastAction; }
    public synchronized void setLastAction(String action) { this.lastAction = action; }
    public synchronized String getStateChangeReason() { return stateChangeReason; }
}
