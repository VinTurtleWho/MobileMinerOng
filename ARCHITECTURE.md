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
└───────────────────────────┬────────────────────────────┘
                            │ (Hooks via Mixins/Accessors)
                            ▼
                MINECRAFT CLIENT ENGINE
```

---

## 2. Core Execution Loop & Threading

The lifecycle and main execution flow of the client are managed by `MobileMinerClient`.

### 2.1 Entry Point Initialization
On client initialization, `MobileMinerClient` (which implements `ClientModInitializer`):
1. Initializes diagnostic pipelines (`DebugLogger.init()`).
2. Registers custom chat input hooks through `ClientSendMessageEvents.ALLOW_CHAT` and `ClientReceiveMessageEvents.GAME` to intercept and dispatch command directives (e.g., `!macro debug`) and persist chat logs to `chatlog.txt`.
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

* **Concurrency & Safety**: Every getter and setter in `BotContext` is marked `synchronized` to ensure thread safety during multi-threaded access.
* **Key Properties**:
  * **Motion & Kinematics**: Stores precise physical positions (`Vec3 playerPos`) and view orientations (`yaw`, `pitch`).
  * **Targeting**: Manages focus blocks (`currentTargetBlock`), last seen blocks (`lastSeenTargetBlock`), and live player targets (`targetPlayer`).
  * **Failsafes**: Stores execution state history (`lastAction`) and registers operational delays (`stuckTicks`).

### 3.2 Perception Layer (`com.mobileminerong.perception`)
The perception layer queries local game memory and translates it into typed objects inside `BotContext`.

* **`BlockScanner`**: Evaluates blocks within a specified 3D Manhattan bounding box around the player position.
* **`ScoreboardParser`**: Extracts text lines from the active sidebar objectives.
* **`ActionBarParser`**: Utilizes regular expressions to extract numerical mana allocations, syncing current/max values to `BotContext`.

### 3.3 Planning Layer (`com.mobileminerong.planning` & `com.mobileminerong.state`)
Decides which actions to take based on the parameters available in `BotContext`.

* **`PriorityTaskEngine`**: Handles task sorting, context-sensitive preemption, and execution tick routing.
* **`BotTask` Interface**: Defines behaviors using life-cycle phases (`onStart`, `onTick`, `isFinished`, `onFailure`).
* **Tasks**:
  * **`MovementTask`**: Path-finding and traversal using `AStarPathfinder`. Implements path-recalculation cooldowns to prevent lag.
  * **`AimingTask`**: Precisely orienting player view toward target vectors.
  * **`ShadowBotTask`**: Testing orchestrator; dynamically cycles between movement and aiming to maintain a constant distance from a target player.

### 3.4 Execution & Control Layer (`com.mobileminerong.control` & `com.mobileminerong.mixin`)
The control layer translates strategic decisions into actual client-side actions.

* **`ActionController`**: Simulates inputs by programmatically altering the `down` state of Minecraft's key bindings (`KeyMapping`).
* **`RotationController`**: A stateful instance class that uses eased interpolation (`smoothstep`) and Gaussian jitter for humanized rotation.
* **Mixin/Accessors**: Utilizes Fabric Mixins (e.g., `InventoryAccessor`) to interact with private Minecraft internals (e.g., setting the selected hotbar slot) safely.

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

---

## 5. Diagnostic Pipelines (`com.mobileminerong.debug` & `com.mobileminerong.util`)

Diagnostic infrastructure runs parallel to standard workflows to log state modifications and interactions:
* **`DebugLogger`**: Provides categorized file/console logging with colored ANSI formatting.
* **`DiagnosticManager`**: Implements interval-throttled event reports (e.g., limit logs to once per 1000ms via `REPORT_INTERVAL`).
* **`ChatLogger`**: Asynchronously logs all inbound and outbound chat packets to `chatlog.txt` to maintain a persistent record for anti-cheat assessment.
