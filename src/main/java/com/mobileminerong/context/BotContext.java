package com.mobileminerong.context;

import com.mobileminerong.state.BotState;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.ArrayList;
import java.util.List;

public class BotContext {
    private BotState currentState = BotState.IDLE;
    
    // Perception & Position
    private Vec3 playerPos = Vec3.ZERO;
    private float yaw = 0f;
    private float pitch = 0f;
    
    // Targets
    private BlockPos currentTargetBlock = null;
    private BlockPos lastSeenTargetBlock = null;
    private net.minecraft.world.entity.player.Player targetPlayer = null;
    private boolean pendingPlayerSearch = false; // Add this flag
    
    // Skyblock Parsed Stats
    private String currentZone = "Unknown";
    private int currentMana = 0;
    private int maxMana = 0;
    
    // Diagnostics & Flags
    private String lastAction = "NONE";
    private String stateChangeReason = "Initialization";
    
    // Observability
    public record StateEvent(BotState state, String reason, long timestamp) {}
    private final Deque<StateEvent> stateHistory = new ArrayDeque<>(10);
    
    private long lastPerceptionUpdate = 0;
    private int perceptionFailures = 0;
    private long lastTickDurationNano = 0;
    
    public record TaskEvent(String name, String status, String reason, long timestamp) {}
    private final Deque<TaskEvent> taskHistory = new ArrayDeque<>(10);

    // Getters and Setters
    public synchronized BotState getCurrentState() { return currentState; }
    public synchronized void setState(BotState state, String reason) {
        if (this.currentState != state) {
            this.stateChangeReason = reason;
            this.currentState = state;
            
            if (stateHistory.size() >= 10) stateHistory.removeFirst();
            stateHistory.addLast(new StateEvent(state, reason, System.currentTimeMillis()));
        }
    }

    public synchronized Vec3 getPlayerPos() { return playerPos; }
    public synchronized void setPlayerPos(Vec3 pos) { this.playerPos = pos; }

    public synchronized float getYRot() { return yaw; }
    public synchronized float getXRot() { return pitch; }
    public synchronized void setRotations(float yaw, float pitch) {
        this.yaw = yaw;
        this.pitch = pitch;
    }

    public synchronized BlockPos getCurrentTargetBlock() { return currentTargetBlock; }
    public synchronized void setCurrentTargetBlock(BlockPos pos) { 
        if (pos != null) this.lastSeenTargetBlock = pos;
        this.currentTargetBlock = pos; 
    }

    public synchronized net.minecraft.world.entity.player.Player getTargetPlayer() { return targetPlayer; }
    public synchronized void setTargetPlayer(net.minecraft.world.entity.player.Player player) { this.targetPlayer = player; }

    public synchronized boolean isPendingPlayerSearch() { return pendingPlayerSearch; }
    public synchronized void setPendingPlayerSearch(boolean pending) { this.pendingPlayerSearch = pending; }

    public synchronized String getCurrentZone() { return currentZone; }
    public synchronized void setCurrentZone(String zone) { this.currentZone = zone; }

    public synchronized int getCurrentMana() { return currentMana; }
    public synchronized void setMana(int current, int max) { 
        this.currentMana = current; 
        this.maxMana = max; 
    }

    public synchronized String getLastAction() { return lastAction; }
    public synchronized void setLastAction(String action) { this.lastAction = action; }
    public synchronized String getStateChangeReason() { return stateChangeReason; }

    // Observability Getters/Setters
    public synchronized void updatePerceptionHealth(boolean success) {
        this.lastPerceptionUpdate = System.currentTimeMillis();
        if (!success) this.perceptionFailures++;
    }
    public synchronized void setLastTickDurationNano(long durationNano) { this.lastTickDurationNano = durationNano; }
    public synchronized long getLastTickDurationMicros() { return lastTickDurationNano / 1000; }
    public synchronized int getPerceptionFailures() { return perceptionFailures; }
    public synchronized long getLastPerceptionUpdate() { return lastPerceptionUpdate; }

    public synchronized void addTaskEvent(String name, String status, String reason) {
        if (taskHistory.size() >= 10) taskHistory.removeFirst();
        taskHistory.addLast(new TaskEvent(name, status, reason, System.currentTimeMillis()));
    }
    public synchronized List<StateEvent> getStateHistory() { return new ArrayList<>(stateHistory); }
    public synchronized List<TaskEvent> getTaskHistory() { return new ArrayList<>(taskHistory); }
}
