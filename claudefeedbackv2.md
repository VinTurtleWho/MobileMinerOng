# MobileMinerOng — Full Codebase Audit V2 & General Development Approach

**Project:** MobileMinerOng  
**Platform:** Fabric client-side mod  
**Minecraft Version:** 26.1.2  
**Java Version:** 25  
**Last Audited:** August 2026  
**Previous Audit:** V1 (pre-Gemini session)  
**Changes Since V1:** Gemini CLI session (~3h 24m, 114 tool calls, 11M tokens)

This document supersedes V1. It reflects the actual current state of the codebase after Gemini's work, documents every bug and fix with line-level precision, gives the exact implementation roadmap for the ShadowBot testing phase, and then gives a general approach for when there is no document like this to follow.

---

## Part 1 — What Changed Since V1

The following V1 issues were correctly resolved by Gemini:

**DiagnosticManager** — Fixed. Now uses `ConcurrentHashMap<String, Long>` for per-category rate-limit timers. Each category gets its own independent 1-second window. This was the most important debugging prerequisite and it is done correctly.

**AStarPathfinder — O(n) membership check** — Fixed. `openSetMembership` HashSet added alongside `PriorityQueue`. All `openSet.contains()` calls replaced with `openSetMembership.contains()` (O(1)). Correct.

**AStarPathfinder — heuristic consistency** — Fixed. Both `heuristic()` and `gCost` accumulation now use squared distance (`distSqr`). Units are consistent. No mixed sqrt/squared math.

**AStarPathfinder — path smoothing** — Implemented. `smoothPath()` added as a post-processing pass using `isStraightLineWalkable()` raycasting. Architecture is correct.

**RotationController** — Refactored from static utility to stateful instance class. Smoothstep interpolation (`t * t * (3 - 2 * t)`) implemented. Micro-jitter added. `setTarget()` / `tick()` / `isAligned()` interface is correct.

**ActionController.selectHotbarSlot()** — Fixed. Now uses `InventoryAccessor` Mixin (`@Accessor("selected")`) to set hotbar slot. The Mixin is registered in `mobileminerong.mixins.json`. Correct approach.

**TargetSearchTask** — Fixed. Now persistent: `isFinished()` returns `false` always. Never removes itself from the pool. Correct.

**MovementTask, AimingTask** — Created. Basic structure is correct. Both have lifecycle bugs (documented below).

**ShadowBotTask** — Created for testing. Wires MovementTask and AimingTask together manually outside the engine. Has structural bugs (documented below) but the approach is valid for testing.

**BotContext** — Extended with `targetPlayer`, `currentMana`, `maxMana` fields. Correct additions.

---

## Part 2 — Current File Inventory

| File | Package | Status |
|------|---------|--------|
| `MobileMinerClient.java` | `com.mobileminerong` | Working, ShadowBotTask registration note below |
| `BotContext.java` | `com.mobileminerong.context` | Working, complete |
| `BotState.java` | `com.mobileminerong.state` | Working, complete |
| `PriorityTaskEngine.java` | `com.mobileminerong.state` | Working |
| `DiagnosticManager.java` | `com.mobileminerong.diagnostic` | **Fixed and working** |
| `DebugLogger.java` | `com.mobileminerong.debug` | Working, complete |
| `RotationController.java` | `com.mobileminerong.control` | **Mostly fixed — 2 bugs remain** |
| `ActionController.java` | `com.mobileminerong.control` | **Fixed and working** |
| `AStarPathfinder.java` | `com.mobileminerong.planning.pathfinding` | **Mostly fixed — 2 bugs remain** |
| `Node.java` | `com.mobileminerong.planning.pathfinding` | Working |
| `BotTask.java` | `com.mobileminerong.planning.task` | Working, complete interface |
| `DiagnosticTestTask.java` | `com.mobileminerong.planning.task` | Working |
| `TargetSearchTask.java` | `com.mobileminerong.planning.task` | **Fixed and working** |
| `MovementTask.java` | `com.mobileminerong.planning.task` | **Has 1 critical bug** |
| `AimingTask.java` | `com.mobileminerong.planning.task` | **Has 1 critical bug** |
| `ShadowBotTask.java` | `com.mobileminerong.planning.task` | **Working for testing — 1 design note** |
| `InventoryAccessor.java` | `com.mobileminerong.mixin.accessor` | **Fixed and working** |
| `BlockScanner.java` | `com.mobileminerong.perception` | Working |
| `ScoreboardParser.java` | `com.mobileminerong.perception` | **Stub — not implemented** |
| `ActionBarParser.java` | `com.mobileminerong.perception` | Working |
| `ChatLogger.java` | `com.mobileminerong.util` | Working |
| `MacroCommandHandler.java` | `com.mobileminerong.command` | Working |

**Still missing (not created):**
- `MiningTask.java` — not yet built, gated on ShadowBot testing

---

## Part 3 — All Remaining Bugs, With Exact Fixes

---

### BUG 1 — RotationController: Yaw lerp breaks across ±180° boundary
**File:** `RotationController.java`  
**Method:** `tick(BotContext ctx)`  
**Severity:** High — causes visible full-circle spin on certain angle transitions  

**What the code does:**
```java
float yaw = Mth.lerp(smoothProgress, startYaw, targetYaw);
```
This lerps numerically between two raw degree values. If `startYaw` is 170° and `targetYaw` is -170°, these are only 20° apart in actual angle space but 340° apart numerically. The lerp travels the 340° long way around. The player visibly spins a full near-circle instead of making a small 20° turn. This is an obvious anti-cheat flag and looks completely inhuman.

**The fix:**  
Do not lerp between raw values. Compute the shortest-path angular delta using `Mth.wrapDegrees()` and lerp the offset from `startYaw`.

Replace:
```java
float yaw = Mth.lerp(smoothProgress, startYaw, targetYaw);
float pitch = Mth.lerp(smoothProgress, startPitch, targetPitch);
```

With:
```java
float deltaYaw = Mth.wrapDegrees(targetYaw - startYaw);
float deltaPitch = Mth.wrapDegrees(targetPitch - startPitch);
float yaw = startYaw + smoothProgress * deltaYaw;
float pitch = startPitch + smoothProgress * deltaPitch;
```

This always takes the shortest angular path regardless of where ±180° falls.

---

### BUG 2 — RotationController: Bimodal jitter distribution is detectable
**File:** `RotationController.java`  
**Method:** `tick(BotContext ctx)`  
**Severity:** Medium — anti-cheat rotation delta analysis will flag non-Gaussian noise  

**What the code does:**
```java
float jitterYaw = (jitterRandom.nextFloat() * 0.2f - 0.1f) + (jitterRandom.nextBoolean() ? 0.15f : -0.15f);
```
This calls `nextFloat()` for a value in [-0.1, +0.1] and then adds either +0.15 or -0.15 from `nextBoolean()`. The result is a bimodal distribution — values cluster around -0.05 and +0.05 with a gap near zero. Real human mouse movement produces normally distributed (Gaussian) noise centered at zero with no gap. Anti-cheats that analyze the statistical distribution of rotation deltas over time will see the bimodal pattern as non-human.

**The fix:**  
Replace both jitter lines with Gaussian noise:
```java
float jitterYaw = (float)(jitterRandom.nextGaussian() * 0.07);
float jitterPitch = (float)(jitterRandom.nextGaussian() * 0.07);
```
`nextGaussian()` returns values from a normal distribution centered at 0 with standard deviation 1. Multiplying by 0.07 gives a standard deviation of 0.07°, which matches real mouse hardware noise at low DPI. Values will occasionally reach ±0.2° which is realistic.

---

### BUG 3 — MovementTask: Calls setTarget() every tick, resetting rotation progress
**File:** `MovementTask.java`  
**Method:** `onTick(BotContext ctx)`  
**Severity:** Critical — the bot never moves because rotation never completes  

**What the code does:**
```java
// In onTick(), every tick:
rotationController.setTarget(waypointVec, client.player.getYRot(), client.player.getXRot());
rotationController.tick(ctx);
client.options.keyUp.setDown(true);
```
`setTarget()` resets `currentTick = 0` and `progress = 0.0f` every time it's called. Since it's called on every tick, the rotation restarts from zero every 50ms and never reaches `totalTicks`. `isAligned()` never returns true. `keyUp` presses every tick regardless of whether the player is facing the waypoint, so the bot walks in whatever direction it's currently facing instead of toward the waypoint.

**The fix:**  
Track whether the rotation controller has been initialized for the current waypoint. Only call `setTarget()` when the waypoint changes. Only press `keyUp` after alignment is confirmed.

Replace the entire bottom half of `onTick()` (the section after stuck detection) with:

```java
if (currentIndex >= path.size()) {
    finished = true;
    ActionController.stopAllInputs();
    ctx.setState(BotState.AIMING, "Reached destination");
    return;
}

BlockPos currentWaypoint = path.get(currentIndex);
Vec3 waypointVec = Vec3.atCenterOf(currentWaypoint);

// Advance waypoint if close enough
if (client.player.position().distanceToSqr(waypointVec) < 0.5) {
    currentIndex++;
    rotationInitialized = false; // reset flag so setTarget fires for next waypoint
    ActionController.stopAllInputs();
    return;
}

// Initialize rotation for this waypoint only once
if (!rotationInitialized) {
    rotationController.setTarget(waypointVec, client.player.getYRot(), client.player.getXRot());
    rotationInitialized = true;
}

// Tick rotation every tick
rotationController.tick(ctx);

// Only move forward once facing the right direction
if (rotationController.isAligned()) {
    client.options.keyUp.setDown(true);
} else {
    client.options.keyUp.setDown(false);
}
```

Add `private boolean rotationInitialized = false;` as a field. Reset it in `onStart()` and whenever `currentIndex` advances.

---

### BUG 4 — AimingTask: isFinished() always returns false, no clean completion
**File:** `AimingTask.java`  
**Method:** `isFinished(BotContext ctx)` and `onFailure()`  
**Severity:** High — task can never complete successfully, only time out into RECOVERING  

**What the code does:**
```java
@Override
public boolean isFinished(BotContext ctx) {
    return false; // hardcoded
}
```
And `onFailure()` sets state to RECOVERING but does not set a finished flag. The task has a `timeoutTicks` countdown that calls `onFailure()` after 40 ticks, but since `isFinished()` always returns false, the engine never removes it from the pool. When used inside `ShadowBotTask` (which manages it manually), this is masked — but when used in the main engine, this task will run indefinitely regardless of outcome.

**The fix:**  
Add a `private boolean finished = false` field. Set it to true in both the success path (when `rotationController.isAligned()` returns true) and in `onFailure()`. Make `isFinished()` return `finished`. Also: the current task never calls `rotationController.isAligned()` to check if it succeeded — it just waits for timeout. Add the success check.

```java
private boolean finished = false;

@Override
public void onTick(BotContext ctx) {
    timeoutTicks--;
    if (timeoutTicks <= 0) {
        onFailure(ctx, "Aiming timed out");
        return;
    }

    rotationController.tick(ctx);

    if (rotationController.isAligned()) {
        ctx.setState(BotState.MINING, "Aiming complete");
        finished = true;
    }
}

@Override
public boolean isFinished(BotContext ctx) {
    return finished;
}

@Override
public void onFailure(BotContext ctx, String reason) {
    finished = true;
    ctx.setState(BotState.RECOVERING, "Aiming failed: " + reason);
    MobileMinerClient.TASK_ENGINE.reportTaskFailure(this, reason);
}
```

---

### BUG 5 — AStarPathfinder: isWalkable() misses non-full-block solid geometry
**File:** `AStarPathfinder.java`  
**Method:** `isWalkable(Level world, BlockPos pos)`  
**Severity:** High — pathfinder clips through stairs, slabs, fences in Dwarven Mines  

**What the code does:**
```java
boolean feetClear = feet.isAir() || !feet.isCollisionShapeFullBlock(world, pos);
boolean headClear = head.isAir() || !head.isCollisionShapeFullBlock(world, pos.above());
boolean floorSolid = !floor.isAir() && floor.isCollisionShapeFullBlock(world, pos.below());
```
`isCollisionShapeFullBlock()` returns true only for perfect full-cube blocks. Stairs, slabs, fences, walls, and any other partial-block solids return false. The result: the pathfinder thinks you can walk through a stair block (because it's "not a full block") and marks it as clear. In Dwarven Mines, which has extensive stair and slab geometry, this produces paths that cut through walls. The bot then physically cannot follow those paths because Minecraft's collision system blocks it correctly.

**The fix:**  
Replace `isCollisionShapeFullBlock()` with `!state.getCollisionShape(world, pos).isEmpty()` for the blocking checks. This catches any block with any collision shape — full cubes, stairs, slabs, fences, everything. Only actually passable air-like blocks return an empty collision shape.

```java
public static boolean isWalkable(Level world, BlockPos pos) {
    BlockState feet = world.getBlockState(pos);
    BlockState head = world.getBlockState(pos.above());
    BlockState floor = world.getBlockState(pos.below());

    // A position blocks passage if it has any collision shape at all
    boolean feetClear = feet.getCollisionShape(world, pos).isEmpty();
    boolean headClear = head.getCollisionShape(world, pos.above()).isEmpty();
    // Floor must have collision to stand on
    boolean floorSolid = !floor.getCollisionShape(world, pos.below()).isEmpty();

    return feetClear && headClear && floorSolid;
}
```

Remove the debug log line inside `isWalkable()` — it fires on every non-walkable neighbor during every pathfinding call and will flood the log file with thousands of lines per second. Move pathfinding debug output to the level above (log once when the full path result is returned, not per-block).

---

### BUG 6 — AStarPathfinder: smoothPath raycast uses null entity context
**File:** `AStarPathfinder.java`  
**Method:** `isStraightLineWalkable()`  
**Severity:** Medium — straight-line walkability check doesn't match actual player collision  

**What the code does:**
```java
net.minecraft.world.phys.shapes.CollisionContext.of(null)
```
`CollisionContext.of(null)` creates a context with no entity. The raycast uses generic block collision rules rather than player-specific ones. Some blocks behave differently depending on the entity type (e.g., fluids, scaffolding, certain modded blocks). Passing `null` means the smoothing check might greenlight a straight line that the player physically can't traverse.

**The fix:**
```java
CollisionContext.of(Minecraft.getInstance().player)
```
Pass the actual player instance so the raycast uses the same collision rules that apply to real player movement.

---

### BUG 7 — ShadowBotTask: Spawns new MovementTask every time target moves
**File:** `ShadowBotTask.java`  
**Method:** `onTick(BotContext ctx)`  
**Severity:** Medium — causes repeated A* calls as target player walks around  
**Note:** This is a testing task. Acceptable for now, but document for when you refactor.

**What the code does:**
Every tick, it checks `movementTask == null || movementTask.isFinished(ctx)`. If the MovementTask finishes (player reached the target position), and the target player has moved more than 3 blocks again, it creates a brand new `MovementTask` with `onStart()`. This means `AStarPathfinder.findPath()` runs every time the player moves far enough away. During normal walking this fires constantly, running a full A* search on the main thread every few ticks.

**The fix for production** (not for testing — for when you replace ShadowBotTask with real mining):  
Add a cooldown before allowing a repath, and only repath if the target has moved more than N blocks from the original path destination. For the ShadowBot test, the current behavior is acceptable — just know it's burning cycles.

---

### BUG 8 — ScoreboardParser: Completely unimplemented
**File:** `ScoreboardParser.java`  
**Method:** `getSidebarLines()` and implicitly `updateZone()`  
**Severity:** Low now — will matter before Dwarven Mines deployment  

**What the code does:**
```java
public static List<String> getSidebarLines(Minecraft client) {
    return Collections.emptyList();
}
```
`getSidebarLines()` is a stub that returns nothing. `updateZone()` sets the zone to the raw dimension string (`minecraft:overworld` or similar) instead of the actual SkyBlock zone name from the scoreboard sidebar.

**The fix (when needed):**  
SkyBlock displays zone info in the scoreboard sidebar. The correct way to read it in Fabric 1.21.x:
```java
Scoreboard scoreboard = client.level.getScoreboard();
Objective objective = scoreboard.getDisplayObjective(DisplaySlot.SIDEBAR);
if (objective != null) {
    Collection<PlayerTeam> teams = scoreboard.getPlayerTeams();
    // iterate teams, read getDisplayName() for sidebar lines
}
```
This is not needed until you're deploying to actual SkyBlock. Leave it alone for now.

---

### NOTE — build.gradle uses implementation instead of modImplementation
**File:** `build.gradle`  
**Severity:** Low — currently working but is technically wrong  

The Fabric API dependency uses `implementation` instead of `modImplementation`. Fabric Loom uses `modImplementation` to correctly include mod dependencies in the environment. With plain `implementation`, the classes are available at compile time but Loom's mod processing (jar remapping, dependency inclusion) doesn't apply. This is working right now possibly because the loader is lenient, but it may break after a Fabric or Loom update.

If a future build starts failing with missing class errors at runtime, change:
```groovy
implementation "net.fabricmc.fabric-api:fabric-api:${project.fabric_api_version}"
```
to:
```groovy
modImplementation "net.fabricmc.fabric-api:fabric-api:${project.fabric_api_version}"
```

---

## Part 4 — ShadowBot Testing Phase Roadmap

This is the current priority. Do not build MiningTask or wire the ore mining loop until these pass.

**The goal of ShadowBot testing:** Validate that `RotationController` produces humanlike rotation deltas and that `AStarPathfinder` produces paths the player can physically follow. Both must work correctly before anything is built on top of them.

**What "passing" looks like:**
- Bot successfully follows a walking player for 2 continuous minutes without getting stuck
- Bot rotates smoothly and never spins the wrong direction around angle boundaries
- Bot stops and re-routes when blocked rather than walking into walls
- Debug log shows rotation deltas that vary naturally, not constant values
- No visible snap or teleport in rotation at any point

**Fix order for ShadowBot phase (do these in order, test after each):**

1. Fix `RotationController` yaw lerp wrapping bug (Bug 1 above). Test: ask someone to stand at various compass angles relative to you. Bot should turn the short way to each one, never the long way.

2. Fix `RotationController` jitter to Gaussian (Bug 2 above). Test: enable debug, watch rotation delta log lines. Values should vary continuously with no bimodal clustering.

3. Fix `MovementTask.onTick()` — `rotationInitialized` flag, only call `setTarget()` once per waypoint, only press `keyUp` after alignment (Bug 3 above). Test: bot should now actually walk toward each waypoint rather than spinning in place.

4. Fix `AStarPathfinder.isWalkable()` collision shape check (Bug 5 above). Test: generate a path through terrain with stairs or slabs. Bot should route around them rather than through them.

5. Fix `AStarPathfinder.isStraightLineWalkable()` entity context (Bug 6 above).

6. Fix `AimingTask` finished flag and alignment check (Bug 4 above). This is used by ShadowBotTask when the bot gets within 3 blocks of the target — it should aim at the player and hold aim cleanly.

After all six: run the ShadowBot test for 2 minutes of active player walking. If it passes, the movement and rotation foundation is production quality. Then build MiningTask.

---

## Part 5 — MiningTask Specification

This is the next task to build after ShadowBot testing passes. It does not exist yet.

**File to create:** `src/main/java/com/mobileminerong/plannin
