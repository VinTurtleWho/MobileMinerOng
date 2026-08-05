# Adversarial Checklist: Exhaustive Multi-Pass Audit

This checklist must be used for EVERY review cycle. A fix does NOT excuse the reviewer from performing the full audit. The audit must act as if the code is brand new.

## Pass 1: Architecture & Invariants
- [ ] Are layering patterns (Perception->Context->Planning->Execution) violated?
- [ ] Is `BotContext` misused (e.g., direct field access instead of methods)?
- [ ] Are `PriorityTaskEngine` life-cycle invariants maintained?
- [ ] Does the implementation introduce any "god classes"?

## Pass 2: State & Lifecycle
- [ ] Are there null-pointer possibilities for `Minecraft.getInstance().player`?
- [ ] Are there uninitialized states (e.g., `RotationController` accessed before `setTarget`)?
- [ ] Is there stale state left over between task transitions?
- [ ] Are `targetLostTicks` and `stallTicks` handled correctly during task re-initialization?

## Pass 3: Concurrency & Environment
- [ ] Does this logic run on the main `RenderThread` properly?
- [ ] Are there race conditions possible between `onTick` and external game events?
- [ ] Are there any assumptions about timing that break under varying tick-rates?

## Pass 4: Integration & Regression (The "Fresh Start" Pass)
- [ ] Does this fix break `MovementTask` or `AimingTask`?
- [ ] Is there a code path where the bot becomes "stuck" (a no-op loop)?
- [ ] Does this change the `ActionController` in a way that breaks other modules?
- [ ] Are the changes *actually* applied at runtime (check Mixin application)?

## Pass 5: Edge Cases & Attack
- [ ] What happens if the target moves instantly?
- [ ] What happens if the bot is in a `SURVIVAL` vs `null` mode?
- [ ] Does rapid mode-switching (`!macro mode COMBAT`) cause state corruption?
- [ ] Can an entity become non-attackable mid-task without triggering `onFailure`?
