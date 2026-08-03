# MobileMinerOng

MobileMinerOng is a client-side Minecraft mod built using the [Fabric Modding API](https://fabricmc.net/). It is designed to facilitate automated gameplay, perception, and navigation within Minecraft.

## Project Architecture

The project follows a standard Fabric mod structure.

- **Entry Point**: `com.mobileminerong.MobileMinerClient` implements `ClientModInitializer`.
- **Bot Core**: State management is centralized in `com.mobileminerong.context.BotContext` and updated via `ClientTickEvents`.
- **Perception**: Modules like `BlockScanner` and `ScoreboardParser` analyze game state.
- **Control**: `RotationController` and `ActionController` handle interaction with the game environment.
- **Commands**: `MacroCommandHandler` provides a command-line interface within chat.
- **Logging/Utilities**: `ChatLogger` handles persistent chat logging; `DebugLogger` handles diagnostic outputs.

## Development Philosophy

- **Architecture**: Layered: Perception -> Context -> Planning -> Execution.
- **Modularity**: Avoid large monolithic classes ("god classes"). Prefer small, focused components.
- **State**: `BotContext` is the central shared state container. All bot tasks must read/write through this.
- **Separation of Concerns**: Planning decides what should happen; execution performs actions.
- **Task Pattern**: Tasks are stateful (`BotTask` interface). Tasks are either **persistent** (like `TargetSearchTask`, `ShadowBotTask`) or **finite** (like `MovementTask` and `AimingTask`).
- **Rotation Pattern**: Use `RotationController` as an **instance-based** state machine for smooth interpolation, rather than static utility methods.
- **Process**: Explain architecture changes before editing files. Never make large refactors without first creating a plan.
- **Patterns**: Preserve Fabric API patterns. Use Mixins for accessing private Minecraft internals via Accessor interfaces.

## Current Development Status

- The project has moved from basic framework setup to implementing robust navigation and target-following capabilities.
- Core tasks (`MovementTask`, `AimingTask`, `ShadowBotTask`) are functional.
- Anti-cheat bypass testing and pathfinding stabilization are the current priorities.

## Building and Running

The project uses Gradle for build management.

- **Build**: `./gradlew build`
- **Development Environment**: Follow the [Fabric Modding Wiki](https://fabricmc.net/wiki/) for setting up your IDE (IntelliJ IDEA is recommended).

## Development Conventions

- **State Management**: All bot-related state must be managed through `BotContext`.
- **Tick Loop**: Use `ClientTickEvents.END_CLIENT_TICK` for mod logic execution. Heavy operations (like block scanning) should be rate-limited (currently 20 ticks/sec).
- **Logging**: Use `com.mobileminerong.debug.DebugLogger` for all diagnostic output. All chat interactions must be routed through `com.mobileminerong.util.ChatLogger` for anti-cheat verification.
- **Fabric**: Adhere to Fabric API patterns for event registration and client-side interactions.
- **Performance**: Path-heavy tasks must implement cooldown-based re-pathing to prevent tick-lag.
