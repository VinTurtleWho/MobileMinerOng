# MobileMinerOng

MobileMinerOng is a client-side Minecraft mod built using the [Fabric Modding API](https://fabricmc.net/). It is designed to facilitate automated gameplay, perception, and navigation within Minecraft.

## Project Architecture

The project follows a standard Fabric mod structure.

- **Entry Point**: `com.mobileminerong.MobileMinerClient` implements `ClientModInitializer`.
- **Bot Core**: State management is centralized in `com.mobileminerong.context.BotContext` and updated via `ClientTickEvents`.
- **Perception**: Modules like `BlockScanner` and `ScoreboardParser` analyze game state.
- **Control**: `RotationController` (deprecated) and `ActionController` handle interaction with the game environment.
- **Commands**: `MacroCommandHandler` provides a command-line interface within chat.
- **Logging/Utilities**: `ChatLogger` handles persistent chat logging; `DebugLogger` handles diagnostic outputs.

## Development Philosophy

- **Architecture**: Layered: Perception -> Context -> Planning -> Execution.
- **Modularity**: Avoid large monolithic classes ("god classes"). Prefer small, focused components.
- **State**: `BotContext` is the central shared state container. All bot tasks must read/write through this.
- **Separation of Concerns**: Planning decides what should happen; execution performs actions.
- **Task Pattern**: Tasks are stateful (`BotTask` interface). Tasks are either **persistent** (like `TargetSearchTask`) or **finite** (like `MovementTask` and `AimingTask`).
- **Rotation Engine**: Uses a 5th-order Minimum-Jerk trajectory generator for human-like movement, replacing legacy PD controllers.
- **Combat Execution**: Native click injection via `KeyMappingAccessor` (using `timesPressed` / `clickCount`) to ensure genuine server-side registration while bypassing anticheat signature checks.
- **Combat Logic**: Implements a Cognitive Ring Buffer (delayed velocity) to simulate human reaction times and Ornstein-Uhlenbeck drift for non-deterministic, human-like aim tracking.
- **Input Handling**: All input modifications (key-down/up) must be queued via `client.execute()` to prevent main-thread race conditions.
- **Process**: Explain architecture changes before editing files. Never make large refactors without first creating a plan.
- **Patterns**: Preserve Fabric API patterns. Use Mixins for accessing private Minecraft internals via Accessor interfaces.

## Current Development Status

- The project has successfully stabilized its core movement and pathfinding engines (Phase 1 complete).
- **Feature Set**: Supports MINER and COMBAT modes, activated/deactivated via the 'O' keybind.
- **Combat Enhancement**: Implemented robust combat target filtering (`PLAYER`/`MOB` modes, mob whitelisting, name-tag parsing with color-code stripping, and NPC/ArmorStand exclusion) for Hypixel Skyblock compatibility.
- **Combat Movement**: Implemented forced sprinting and 15-block AABB entity scanning to reduce latency and improve responsiveness.
- **Task System**: Supports dynamic task switching based on the current `MacroMode`.

## Building and Running

The project uses Gradle for build management.

- **Build**: `./gradlew build`
- **Development Environment**: Follow the [Fabric Modding Wiki](https://fabricmc.net/wiki/) for setting up your IDE.

## Development Conventions

- **State Management**: All bot-related state must be managed through `BotContext`.
- **Tick Loop**: Use `ClientTickEvents.END_CLIENT_TICK` for mod logic execution. Heavy operations (like block scanning) should be rate-limited (currently 20 ticks/sec).
- **Logging**: Use `com.mobileminerong.debug.DebugLogger` for all diagnostic output. All chat interactions must be routed through `com.mobileminerong.util.ChatLogger` for anti-cheat verification.
- **Fabric**: Adhere to Fabric API patterns for event registration and client-side interactions.
- **Combat Safety (Hypixel)**: All targeting loops must strictly exclude `ArmorStand` entities, invulnerable entities, and entities flagged as dead/dying/invisible. Use prioritized custom name parsing with §-code stripping for target whitelisting.
- **Performance**: Path-heavy tasks must implement cooldown-based re-pathing to prevent tick-lag.
- **Pathfinding & Collisions**: All pathfinding walkability checks must utilize voxel-based collision checks (`getCollisionShape`) rather than full-block queries. This ensures partial blocks like stairs, slabs, and fences are handled correctly.
- **Movement Alignment**: Movement tasks must ensure the bot is fully rotated and aligned with the target/waypoint before applying forward/directional input keys.
- **Modes**: Modes are managed by `MacroMode` and updated via command: `!macro mode [miner|combat|idle]`.
- **Activation**: Toggle macro mode with 'O'.
