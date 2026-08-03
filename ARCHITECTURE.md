# MobileMinerOng Architecture Specification

MobileMinerOng is a high-performance, event-driven client-side Minecraft automation framework built on the Fabric Modding API. The mod utilizes a modular, multi-tiered architecture to decouple input processing, state representation, decision-making, and physics execution. 

This document details the architectural components, execution loops, state propagation mechanisms, and data flows within the codebase.

---

## 1. Architectural Overview

The system is organized into four distinct logical layers to maintain a strict separation of concerns, avoid monolithic state management, and ensure high runtime performance on the client render thread:

```
┌────────────────────────────────────────────────────────┐
│                    PERCEPTION LAYER                    │
│   Parses environmental/UI data & identifies targets    │
└───────────────────────────┬────────────────────────────┘
                            │ (Writes)
                            ▼
┌────────────────────────────────────────────────────────┐
│                     CONTEXT LAYER                      │
│        Centralized thread-safe state container         │
└───────────────────────────┬────────────────────────────┘
                            │ (Reads)
                            ▼
┌────────────────────────────────────────────────────────┐
│                     PLANNING LAYER                     │
│    Sorts task priorities & executes behavioral trees   │
└───────────────────────────┬────────────────────────────┘
                            │ (Dispatches intent)
                            ▼
┌────────────────────────────────────────────────────────┐
│                    EXECUTION LAYER                     │
│ Smooths visual transitions & dispatches physical input │
└────────────────────────────────────────────────────────┘
```

---

## 2. Core Execution Loop & Threading

The lifecycle and main execution flow of the client are managed by `MobileMinerClient`.

### 2.1 Entry Point Initialization
On client initialization, `MobileMinerClient` (which implements `ClientModInitializer`):
1. Initializes diagnostic pipelines (`DebugLogger.init()`).
2. Registers custom chat input hooks through `ClientSendMessageEvents.ALLOW_CHAT` to intercept and dispatch command directives (e.g., `!macro debug`) without sending them to the server.
3. Subscribes to the client's post-tick lifecycle event: `ClientTickEvents.END_CLIENT_TICK`.

### 2.2 Tick Scheduling & Rate-Limiting
To prevent framerate degradation on Minecraft's primary rendering thread, tasks are scheduled with varying execution frequencies inside the 20 Hz tick loop:

* **High-Frequency (Every Tick / 50ms)**:
  * Synchronization of active coordinates and local viewing rotations via `updateContext(Minecraft)`.
  * Incremental countdown of task engine steps and emergency fail-safe metrics.
* **Low-Frequency (Every 20 Ticks / 1000ms)**:
  * Scanning nearby 3D blocks for ores via `updatePerception()`.
  * Querying active sidebar scoreboard/zone data.
  * Printing serialized telemetry reports when debugging is active (`debugTimer`).

---

## 3. Layer Breakdown

### 3.1 Context Layer (`com.mobileminerong.context`)
The `BotContext` class is the single source of truth for the mod's runtime state.

* **Concurrency & Safety**: Every getter and setter in `BotContext` is marked `synchronized` to ensure thread safety during multi-threaded access (such as async web integration or async pathfinding).
* **Properties Categorization**:
  * **Behavioral State**: Tracks active machine states (`BotState`) and logs the change rationale (`stateChangeReason`).
  * **Motion & Kinematics**: Stores precise physical positions (`Vec3 playerPos`) and view orientations (`yaw`, `pitch`).
  * **Target Buffers**: Manages current focus blocks (`currentTargetBlock`) and maintains tracking of the last known valid target (`lastSeenTargetBlock`).
  * **RPG HUD Statistics**: Caches zone telemetry (`currentZone`), resources (`currentMana`, `maxMana`), and attributes.
  * **Failsafes**: Stores execution state history (`lastAction`) and registers operational delays (`stuckTicks`).

### 3.2 Perception Layer (`com.mobileminerong.perception`)
The perception layer queries local game memory and translates it into typed objects inside `BotContext`.

* **`BlockScanner`**: Evaluates blocks within a specified 3D Manhattan bounding box around the player position. It filters blocks against defined block states (supporting target ores like Prismarine, Terracotta, Wool, and Quartz variants for Dwarven Mines mining) and sorts candidates based on Euclidean squared distance (`distSqr`) to return the closest options.
* **`ScoreboardParser`**: Extracts text lines from the active sidebar objectives. It maps structural properties (such as current world dimension) to register area-specific parameters in the `BotContext` state.
* **`ActionBarParser`**: Utilizes regular expressions (e.g., `(\\d+)/(\\d+)✎\\s*Mana`) to extract numerical mana allocations, syncing current/max values to `BotContext`.

### 3.3 Planning Layer (`com.mobileminerong.planning` & `com.mobileminerong.state`)
Decides which actions to take based on the parameters available in `BotContext`.

* **`PriorityTaskEngine`**: Handles task sorting, context-sensitive preemption, and execution tick routing.
* **`BotTask` Interface**: Defines behaviors using life-cycle phases (`onStart`, `onTick`, `isFinished`, `onFailure`).
  Tasks are either **persistent** (e.g., `TargetSearchTask`) or **finite** (e.g., `MovementTask`, `AimingTask`).
  Tasks define clear, numeric priorities.
* **Pathfinding (`AStarPathfinder` & `Node`)**: Formulates coordinate paths from a source to a target node.

### 3.4 Execution & Control Layer (`com.mobileminerong.control`)
The control layer translates the strategic decisions of active tasks into actual client-side actions.

* **`ActionController`**: Simulates inputs by programmatically altering the `down` state of Minecraft's key bindings (`KeyMapping`). Includes functional hotbar slot selection.
* **`RotationController`**: Now a **stateful instance class** (not static). Uses eased interpolation (`smoothstep`) and Gaussian jitter for humanized rotation. Computes angular yaw/pitch differences from eyes coordinates to three-dimensional coordinates using trigonometry (`atan2`).

---

## 4. Operational State Machine (`BotState`)

Decisions made by the task engine transition the client through an explicit, finite sequence of operational states:

```
       IDLE
         │
         ▼
  SEARCHING_TARGET ──(Target Found)──► MOVING_TO_TARGET
         ▲                                   │
         │ (No Targets/Completed)            ▼
      MINING ◄──────(Aligned)─────── AIMING (Smooth Look)
         │
         ▼ (Obstructed/Stuck)
     RECOVERING ──► ERROR (Failsafe Trip)
```

1. **`IDLE`**: Awaiting task assignments or evaluating system inputs.
2. **`SEARCHING_TARGET`**: Actively parsing surrounding block spaces to locate valid ores or entities.
3. **`MOVING_TO_TARGET`**: Computing and traversing paths to approach the target.
4. **`AIMING`**: Orienting the player's crosshair toward the center of the target block.
5. **`MINING`**: Holding down the primary attack key on the target block.
6. **`RECOVERING`**: Executing jump/unstuck maneuvers if the player's position remains unchanged for a threshold number of ticks (`stuckTicks`).
7. **`ERROR`**: Failsafe halt state triggered on major logic failures or system exceptions.

---

## 5. Diagnostic Pipelines (`com.mobileminerong.debug` & `com.mobileminerong.diagnostic`)

Diagnostic infrastructure runs parallel to standard workflows to log state modifications:
* **`DebugLogger`**: Provides categorized file/console logging with colored ANSI formatting.
* **`DiagnosticManager`**: Implements interval-throttled event reports (e.g., limit logs to once per 1000ms via `REPORT_INTERVAL`), letting developers log performance metrics or pathfinding updates without flooding the game chat or standard output.
