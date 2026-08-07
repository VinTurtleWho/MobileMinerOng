# MobileMinerOng Architecture Specification

MobileMinerOng is a high-performance, event-driven client-side Minecraft automation framework built on the Fabric Modding API. 

## 1. Architectural Overview

The system is organized into layers to maintain separation of concerns:

1. **Perception**: Parses game data (`BlockScanner`, `ScoreboardParser`).
2. **Context**: Thread-safe state container (`BotContext`).
3. **Planning**: Task engine and mode-specific task management (`PriorityTaskEngine`, `MacroMode`).
4. **Execution**: Input simulation (`ActionController`, `RotationEngine`).

## 2. Mode-Based Orchestration

The system utilizes a `MacroMode` state machine to drive dynamic task registration.

- **`MacroMode`**: Defines the current bot role (`IDLE`, `MINER`, `COMBAT`).
- **Dynamic Task Loading**: The `PriorityTaskEngine` is cleared and re-populated with mode-specific tasks in `MobileMinerClient` whenever the `MacroMode` changes.
- **Activation**: Toggle macro active/idle state using the 'O' keybind.

## 3. Layer Breakdown

### 3.1 Planning Layer (`com.mobileminerong.planning` & `com.mobileminerong.state`)

* **`MacroMode`**: Central enum managing the operating mode.
* **`PriorityTaskEngine`**: Manages task pool and ticks active task. Supports `clearTasks()` and `registerTask()` for dynamic mode switching.
* **`MiningTask`**: Handles tool selection via `ActionController`, executes block mining, and implements timeout/completion detection.
* **`CombatFollowTask`**: Handles tracking and attacking entities, implementing zero-latency targeting, pathfinding, and forced sprinting.

### 3.2 Control Layer (`com.mobileminerong.control`)

* **`ActionController`**: Manages input simulation, ensuring all interactions are queued via `client.execute()` to remain thread-safe.
* **`ClickGenerator`**: Handles native-event-like clicks by directly manipulating `timesPressed` / `clickCount` via `KeyMappingAccessor`, simulating genuine HID events.
* **`RotationEngine`**: Implements a 5th-order Minimum-Jerk trajectory (10τ³ - 15τ⁴ + 6τ⁵) to provide snappy, human-like rotation, preventing anti-cheat flags.

### 3.3 Combat System (Security & Safety)

- **Cognitive Ring Buffer**: Maintains a 3-tick (150ms) history of target velocity to simulate human cognitive delay.
- **Ornstein-Uhlenbeck Drift**: Adds stochastic, mean-reverting drift to the target aim-point for realistic "swimming" mouse movement.
- **Target Filtering**: Employs strict filtering to exclude `ArmorStand` entities, invulnerable entities, dead mobs, and invisible entities. NPCs/Players are strictly excluded in `MOB` target mode unless specifically whitelisted.
- **Performance**: Entity searching is constrained to a 15-block AABB radius scan performed at 20Hz.
