# MobileMinerOng — Full Codebase Audit & Implementation Roadmap

**Project:** MobileMinerOng  
**Platform:** Fabric client-side mod  
**Minecraft Version:** 1.21.x (Fabric loader, Mojmap mappings)  
**Language:** Java  
**Last Reviewed:** August 2026  

This document is a complete technical audit of the current codebase. It covers what is working, what is broken or incomplete, what needs to be built from scratch, and the exact order in which to build it. It is written so that any AI assistant or developer can pick this up cold and know exactly what to do.

---

## Section 1 — Project Architecture Summary

The project follows a strict four-layer architecture. This is the correct pattern and should not be changed.

```
PERCEPTION LAYER
  BlockScanner, ScoreboardParser, ActionBarParser
  → Reads game state, writes to BotContext

CONTEXT LAYER
  BotContext
  → Single synchronized state container, shared by everything

PLANNING LAYER
  PriorityTaskEngine, BotTask implementations
  → Decides what to do, reads from BotContext, changes BotState

EXECUTION LAYER
  RotationController, ActionController
  → Translates decisions into actual Minecraft input
```

The entry point is `MobileMinerClient`, which implements `ClientModInitializer`. It registers a `ClientTickEvents.END_CLIENT_TICK` listener that runs every tick (20Hz). Inside that tick loop, it calls `updateContext()` every tick and `updatePerception()` once per second (every 20 ticks).

This architecture is sound. Do not refactor it. All fixes described in this document are surgical changes to specific files and methods.

---

## Section 2 — Current File Inventory

| File | Package | Status |
|------|---------|--------|
| `MobileMinerClient.java` | `com.mobileminerong` | Working, minor gaps |
| `BotContext.java` | `com.mobileminerong.context` | Working, complete |
| `BotState.java` | `com.mobileminerong.state` | Working, complete |
| `PriorityTaskEngine.java` | `com.mobileminerong.state` | Working, has a design flaw |
| `DiagnosticManager.java` | `com.mobileminerong.diagnostic` | Broken — shared rate limit timer |
| `DebugLogger.java` | `com.mobileminerong.debug` | Working, complete |
| `RotationController.java` | `com.mobileminerong.control` | Working but not humanlike |
| `ActionController.java` | `com.mobileminerong.control` | Partially broken — hotbar slot unimplemented |
| `AStarPathfinder.java` | `com.mobileminerong.planning.pathfinding` | Working but has O(n) performance bug |
| `Node.java` | `com.mobileminerong.planning.pathfinding` | Working |
| `BotTask.java` | `com.mobileminerong.planning.task` | Working, complete interface |
| `DiagnosticTestTask.java` | `com.mobileminerong.planning.task` | Working |
| `TargetSearchTask.java` | `com.mobileminerong.planning.task` | Working, has lifecycle flaw |
| `BlockScanner.java` | `com.mobileminerong.perception` | Working |
| `ScoreboardParser.java` | `com.mobileminerong.perception` | Working |
| `ActionBarParser.java` | `com.mobileminerong.perception` | Working |
| `MacroCommandHandler.java` | `com.mobileminerong.command` | Working |

**Missing entirely (not yet created):**
- `MovementTask.java`
- `AimingTask.java`
- `MiningTask.java`
- `RecoveryTask.java`
- `NavigationInterface.java` (optional abstraction layer)

---

## Section 3 — Issues Found, Ranked by Severity

---

### ISSUE 1 — RotationController: Flat clamp is detectable by anti-cheat
**File:** `src/main/java/com/mobileminerong/control/RotationController.java`  
**Severity:** Critical for production use  

**What the code does now:**  
Every tick, it calculates the angular delta to the target and clamps it to exactly 22.5 degrees per tick, always. This means the rotation speed is constant from start to finish — it starts at full speed and stops at full speed. No acceleration, no deceleration, no variation.

**Why this is a problem:**  
Humans do not turn at a constant speed. They accelerate into a turn and decelerate as they approach the target. A flat 22.5°/tick cap produces a robotic movement signature that is trivially identifiable by any server-side or client-side anti-cheat that samples rotation deltas. Additionally, the exact same value every tick is a fingerprint. Real human input has micro-jitter — tiny random deviations of 0.01° to 0.3° that are noise from hand movement and mouse hardware.

**What needs to be implemented instead:**  
The rotation system needs to be replaced with smooth interpolation. The correct approach is:

1. When a new target is acquired, calculate the total angular distance to the target (both yaw and pitch).
2. Use an eased interpolation function — specifically ease-in/ease-out (also called "smoothstep") — to determine how much of that total distance to cover each tick. Early ticks cover less, middle ticks cover more, late ticks slow down again as the rotation approaches the target.
3. Add per-tick Gaussian noise (random small offset, approximately ±0.05° to ±0.25°, with the value sampled fresh each tick from a seeded random with occasional re-seed) to simulate human micro-jitter.
4. Store the interpolation progress (a `t` value from 0.0 to 1.0) as instance state, not static state, so multiple independent rotations can be tracked if needed.

The smoothstep formula for eased interpolation is: `t_smooth = t * t * (3 - 2 * t)` where `t` goes from 0.0 to 1.0 across the duration of the rotation.

The `RotationController` should become a stateful instance class rather than a static utility class, because it needs to track the interpolation progress between ticks.

**Fields to add to RotationController:**
```
private float startYaw
private float startPitch
private float targetYaw
private float targetPitch
private float progress  // 0.0 to 1.0
private float totalTicks // how many ticks the full rotation should take
private float currentTick
private final Random jitterRandom = new Random()
```

**Method signature to replace:**
```java
// Old (static, no state):
public static void lookAt(BotContext ctx, Vec3 targetPos)

// New (instance, stateful):
public void setTarget(Vec3 targetPos, float player_current_yaw, float player_current_pitch)
public boolean tick(BotContext ctx)  // returns true when rotation is complete
public boolean isAligned()
```

---

### ISSUE 2 — AStarPathfinder: O(n) open set membership check
**File:** `src/main/java/com/mobileminerong/planning/pathfinding/AStarPathfinder.java`  
**Severity:** High — causes tick stuttering on long paths  

**What the code does now:**  
Inside the main loop, the condition `!openSet.contains(neighbor)` is called on a `PriorityQueue`. Java's `PriorityQueue.contains()` is O(n) — it iterates through every element in the queue to find a match. In complex terrain where the open set can grow to hundreds of nodes, this is called dozens of times per iteration, turning what should be an O(log n) algorithm into something much closer to O(n²) in the worst case.

The tick loop runs at 20Hz on Minecraft's render thread. Any pathfinding call that takes more than ~5ms will cause a visible frame stutter. On long paths in the Dwarven Mines (which have complex 3D terrain), this will happen consistently.

**What needs to be implemented instead:**  
Maintain a separate `HashSet<BlockPos> openSetMembership` alongside the existing `PriorityQueue<Node> openSet`. Use `openSetMembership.contains(neighborPos)` for membership checks (O(1)) and use `openSetMembership.add(neighborPos)` and `openSetMembership.remove(pos)` when adding/removing from the queue.

The `allNodes` HashMap that already exists in the code is partially solving this — the fix is to replace `openSet.contains(neighbor)` with `openSetMembership.contains(neighborPos)` throughout.

**Also fix — path smoothing is missing entirely:**  
After `retracePath()` returns a raw list of `BlockPos` waypoints, the path is a staircase of grid-aligned positions. Walking this literally means the bot takes 45-degree turns and has jerky direction changes. This needs a post-processing pass called string-pulling or waypoint reduction.

The simplest correct implementation: iterate the raw path and for each pair of non-adjacent waypoints, raycast between them using `isWalkable()`. If the straight line between waypoint[0] and waypoint[2] is fully walkable (all intermediate blocks clear), remove waypoint[1]. This collapses redundant intermediate points and produces a smoother path with fewer, longer segments.

**Also fix — heuristic is using `Math.sqrt` unnecessarily:**  
The heuristic is `Math.sqrt(a.distSqr(b))`. Since A* only needs relative cost comparisons, and `distSqr` already gives a consistent ordering, the `sqrt` call is pure waste. Replace `heuristic()` with `return a.distSqr(b)` and change `gCost` accumulation to use `distSqr` as well, keeping units consistent.

Note: if you use squared distance for heuristic you must also use squared distance for gCost accumulation. Do not mix sqrt and squared — they produce inconsistent f-costs and the algorithm will give wrong paths.

---

### ISSUE 3 — DiagnosticManager: Single shared rate-limit timer
**File:** `src/main/java/com/mobileminerong/diagnostic/DiagnosticManager.java`  
**Severity:** High — silently drops log messages during debugging  

**What the code does now:**  
There is one static field `lastReportTime` shared across all calls to `report()`. The logic is: if less than 1000ms have passed since the last report, drop the message. This means if `report("BOT", ...)` fires at t=0ms, and then `report("PERCEPTION", ...)` fires at t=500ms, the PERCEPTION message is silently dropped because the global timer hasn't expired yet.

During active development and debugging — especially when building MovementTask and watching navigation — multiple categories will try to log at nearly the same time every tick. With a shared timer, only the first category that fires in any given second will ever actually log. All others are silently swallowed. This will make debugging navigation feel broken and unpredictable.

**What needs to be implemented instead:**  
Replace the single `lastReportTime` with a `ConcurrentHashMap<String, Long>` keyed by category. Each category gets its own independent rate limit timer. The logic becomes: look up the last report time for `category`, compare to `now`, and update only that category's timer if the interval has passed.

```java
// Replace:
private static long lastReportTime = 0;

// With:
private static final ConcurrentHashMap<String, Long> lastReportTimes = new ConcurrentHashMap<>();

// And in report():
long last = lastReportTimes.getOrDefault(category, 0L);
if (now - last >= REPORT_INTERVAL) {
    DebugLogger.info(category, message);
    lastReportTimes.put(category, now);
}
```

---

### ISSUE 4 — TargetSearchTask: Removes itself from pool permanently after completion
**File:** `src/main/java/com/mobileminerong/planning/task/TargetSearchTask.java`  
**Severity:** High — causes the bot to stop working after the first mining cycle  

**What the code does now:**  
`TargetSearchTask` is registered once at startup via `TASK_ENGINE.registerTask(new TargetSearchTask())`. When it finds a target, it sets `finished = true`. The `PriorityTaskEngine` sees `isFinished()` returning true, removes the task from the pool, and it is gone forever. After the first target is mined and `currentTargetBlock` becomes null again, there is no task left to search for the next one. The bot idles permanently.

**What needs to be implemented instead:**  
There are two valid approaches:

Option A — Make `TargetSearchTask` a persistent recurring task that never finishes. It watches `BotContext` and only transitions state when a target appears. It never removes itself from the pool. Its `isFinished()` always returns false. This is simpler.

Option B — Make `MobileMinerClient` re-register a fresh `TargetSearchTask` whenever the task pool drops below a certain size or when `currentTargetBlock` becomes null. This is more flexible but requires polling logic in the tick loop.

Option A is recommended because it keeps the logic inside the task rather than scattering it across the client class.

---

### ISSUE 5 — ActionController: Hotbar slot selection is not implemented
**File:** `src/main/java/com/mobileminerong/control/ActionController.java`  
**Severity:** Medium — blocks tool selection for mining  

**What the code does now:**  
The `selectHotbarSlot()` method has a commented-out line and a TODO comment:
```java
// client.player.getInventory().selected = slot; // TODO: Use proper Mojmap setter
```
This method does nothing. Calling it has no effect on the player's selected item.

**What needs to be implemented:**  
In Fabric with Mojang mappings on 1.21.x, the correct way to set the hotbar slot is:
```java
client.player.getInventory().selected = slot;
```
This field is directly accessible and settable. The original TODO was misplaced — this is the correct Mojmap accessor. Remove the comment and uncomment this line. The bounds check (`slot >= 0 && slot < 9`) is already correct and should be kept.

---

### ISSUE 6 — PriorityTaskEngine: No re-evaluation between ticks
**File:** `src/main/java/com/mobileminerong/state/PriorityTaskEngine.java`  
**Severity:** Medium — can cause stale task execution  

**What the code does now:**  
The engine picks the highest priority task at the start of each tick and sticks with it. If a task was highest priority at tick N but a new higher-priority task gets registered mid-execution, the engine will notice the mismatch on tick N+1 and preempt. This is fine. However, the pool is a static sorted list — there is no mechanism for tasks to declare themselves temporarily inactive or to yield without finishing.

**What should be considered:**  
When `MovementTask` and `MiningTask` are implemented, there will be sequences where the bot needs to wait (e.g., waiting for rotation to complete before starting to move). Currently, the only options are tick (do something) or finish (remove from pool). A `shouldYield(BotContext ctx)` method on `BotTask` would let tasks pause themselves without leaving the pool. This is an optional enhancement but will make the movement/aiming/mining chain cleaner to implement.

---

## Section 4 — Missing Tasks: Specification for What to Build

The following three tasks do not exist yet and must be created. They are listed in implementation order.

---

### Task A — MovementTask
**File to create:** `src/main/java/com/mobileminerong/planning/task/MovementTask.java`  
**Priority:** 15 (runs below TargetSearchTask's 20, above mining's 10)  
**Purpose:** Walk the bot from its current position to `BotContext.getCurrentTargetBlock()`  

**Behavior:**
1. On `onStart()`: call `AStarPathfinder.findPath()` with current player position and target block position. Store the resulting `List<BlockPos>` as instance state. Set `BotState.MOVING_TO_TARGET`. If the path is empty, call `onFailure()`.
2. On `onTick()`: check if the player is within 0.5 blocks of the current waypoint. If yes, advance to the next waypoint. Press the appropriate movement keys via `ActionController.setKey()` based on the direction to the next waypoint. Call `RotationController` to face the movement direction.
3. When the final waypoint is reached (player is within 1.5 blocks of target): call `ActionController.stopAllInputs()`, set state to `AIMING`, set `finished = true`.
4. If `stuckTicks` in `BotContext` exceed a threshold (e.g., 60 ticks = 3 seconds without position change): call `onFailure()`.
5. On `onFailure()`: call `stopAllInputs()`, set state to `RECOVERING`, report failure via `DiagnosticManager`, report to engine via `MobileMinerClient.TASK_ENGINE.reportTaskFailure()`.

**Stuck detection:**
Store player position at the start of each tick. If position has not changed by more than 0.1 blocks after 60 consecutive ticks, the bot is stuck. Increment a stuck counter. At threshold, trigger failure.

---

### Task B — AimingTask
**File to create:** `src/main/java/com/mobileminerong/planning/task/AimingTask.java`  
**Priority:** 12  
**Purpose:** Rotate the player's view to aim precisely at the center of `BotContext.getCurrentTargetBlock()`  

**Behavior:**
1. On `onStart()`: set `BotState.AIMING`. Initialize the `RotationController` instance with the target block center (block position + Vec3(0.5, 0.5, 0.5)).
2. On `onTick()`: call `rotationController.tick(ctx)`. Check `rotationController.isAligned()`. When aligned (within 2.0 degrees of target on both axes), set `finished = true` and set state to `MINING`.
3. Timeout: if alignment is not achieved within 40 ticks (2 seconds), call `onFailure()`.

---

### Task C — MiningTask
**File to create:** `src/main/java/com/mobileminerong/planning/task/MiningTask.java`  
**Priority:** 10  
**Purpose:** Hold the attack key to mine the target block, release when the block is gone  

**Behavior:**
1. On `onStart()`: set `BotState.MINING`. Record the target block position from `BotContext`. Press `client.options.keyAttack` via `ActionController.setKey()`.
2. On `onTick()`: check if the target block still exists in the world via `Minecraft.getInstance().level.getBlockState(targetPos).isAir()`. If the block is air (it has been mined), release the attack key, set `BotContext.currentTargetBlock` to null, set state to `SEARCHING_TARGET`, set `finished = true`.
3. Keep calling `RotationController.tick()` every tick during mining to maintain aim as the block breaks (the visual feedback of breaking can cause slight drift).
4. Timeout: if the block has not been mined after 200 ticks (10 seconds), it is likely unbreakable or access is blocked. Call `onFailure()`.
5. On `onFailure()`: release attack key via `stopAllInputs()`, set state to `RECOVERING`.

---

## Section 5 — Implementation Order

Do not implement these out of order. Each step depends on the previous one being correct.

**Step 1** — Fix `DiagnosticManager` (per-category rate limit timers). This is a prerequisite for debugging everything else.

**Step 2** — Fix `AStarPathfinder` (add `openSetMembership` HashSet, fix heuristic to use squared distance consistently, add string-pull smoothing to `retracePath`).

**Step 3** — Refactor `RotationController` from a static utility into a stateful instance class with eased interpolation and jitter noise.

**Step 4** — Fix `ActionController.selectHotbarSlot()` (uncomment the line, remove the TODO).

**Step 5** — Fix `TargetSearchTask` to be persistent (never removes itself from the pool).

**Step 6** — Implement `MovementTask`.

**Step 7** — Implement `AimingTask`.

**Step 8** — Implement `MiningTask`.

**Step 9** — Register `MovementTask`, `AimingTask`, and `MiningTask` in `MobileMinerClient.onInitializeClient()`.

**Step 10** — Test the full loop: IDLE → SEARCHING → MOVING → AIMING → MINING → SEARCHING (repeat).

---

## Section 6 — Context for Any AI Assistant Continuing This Work

If you are an AI assistant reading this document to continue development, here is what you need to know:

- This is a Fabric client-side mod for Minecraft 1.21.x using Mojang mappings (Mojmap). All Minecraft class names and method names in the existing code use Mojmap naming conventions.
- The entry point is `MobileMinerClient` which implements `ClientModInitializer`. The tick loop is registered via `ClientTickEvents.END_CLIENT_TICK`.
- `BotContext` is the single source of truth. All state reads and writes go through it. All its methods are `synchronized`. Do not add fields to other classes that duplicate state already in `BotContext`.
- `PriorityTaskEngine` manages task lifecycle. Tasks are registered via `registerTask()`. A task is removed from the pool when `isFinished()` returns true.
- `DiagnosticManager` is the correct output interface for all task-level logging. Do not use `System.out.println` or direct `DebugLogger` calls from tasks — route through `DiagnosticManager`.
- Do not output diagnostic messages directly to Minecraft chat from tasks. The existing `MacroCommandHandler` and `DebugLogger` handle 
