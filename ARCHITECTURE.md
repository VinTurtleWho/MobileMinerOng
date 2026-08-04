# MobileMinerOng Architecture Specification

MobileMinerOng is a high-performance, event-driven client-side Minecraft automation framework built on the Fabric Modding API. 

This document details the architectural components, execution loops, state propagation mechanisms, and data flows, including the mode-based orchestration system.

---

## 1. Architectural Overview

The system is organized into layers to maintain separation of concerns:

1. **Perception**: Parses game data (`BlockScanner`, `ScoreboardParser`).
2. **Context**: Thread-safe state container (`BotContext`).
3. **Planning**: Task engine and mode-specific task management (`PriorityTaskEngine`, `MacroMode`).
4. **Execution**: Input simulation (`ActionController`, `RotationController`).

---

## 2. Mode-Based Orchestration

The system now utilizes a `MacroMode` state machine to drive dynamic task registration.

- **`MacroMode`**: Defines the current bot role (`IDLE`, `MINER`, `COMBAT`).
- **Dynamic Task Loading**: The `PriorityTaskEngine` is cleared and re-populated with mode-specific tasks (`MiningTask`, `CombatFollowTask`) in `MobileMinerClient` whenever the `MacroMode` changes.
- **Activation**: Toggle macro active/idle state using the 'O' keybind.

---

## 3. Layer Breakdown (Updates)

### 3.1 Planning Layer (`com.mobileminerong.planning` & `com.mobileminerong.state`)

* **`MacroMode`**: Central enum managing the operating mode.
* **`PriorityTaskEngine`**: Now supports `clearTasks()` to allow switching modes at runtime.
* **`MiningTask`**: Handles tool selection via `ActionController` and triggers mining actions based on `BotContext` targets.
* **`CombatFollowTask`**: Handles tracking and attacking entities (`targetEntity`), maintaining a 2-block engagement range.

### 3.2 Control Layer (`com.mobileminerong.control`)

* **`ActionController`**: Expanded to include `startAttack()` and `stopAttack()` for interaction.
* **`BotContext`**: Expanded to include `targetEntity` for combat targeting.
