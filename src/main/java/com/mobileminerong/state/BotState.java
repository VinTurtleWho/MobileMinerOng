package com.mobileminerong.state;

public enum BotState {
    IDLE,               // Standing by, waiting for tasks
    SEARCHING_TARGET,   // Scanning for ores/mobs
    MOVING_TO_TARGET,   // Walking/pathfinding
    AIMING,             // Aligning crosshair to target block center
    MINING,             // Holding click on target block
    RECOVERING,         // Unstuck or hazard recovery
    ERROR               // Failsafe or emergency stop
}

public enum MacroMode {
    IDLE,
    MINER,
    COMBAT
}
