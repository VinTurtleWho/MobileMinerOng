# MobileMinerOng

MobileMinerOng is a client-side Minecraft mod built using the [Fabric Modding API](https://fabricmc.net/). It is designed to facilitate automated gameplay, perception, and navigation within Minecraft.

## Project Architecture

The project follows a standard Fabric mod structure.

- **Entry Point**: `com.mobileminerong.MobileMinerClient` implements `ClientModInitializer`.
- **Bot Core**: State management is centralized in `com.mobileminerong.context.BotContext` and updated via `ClientTickEvents`.
- **Perception**: Modules like `BlockScanner` and `ScoreboardParser` analyze game state.
- **Control**: `RotationController` and `ActionController` handle interaction with the game environment.
- **Commands**: `MacroCommandHandler` provides a basic command-line interface within chat.

## Development Philosophy

- **Architecture**: The project uses a layered architecture: Perception -> Context -> Planning -> Execution.
- **Modularity**: Avoid large monolithic classes ("god classes"). Prefer small, focused components.
- **State**: `BotContext` is the central shared state container.
- **Separation of Concerns**: Planning decides what should happen; execution performs actions.
- **Process**: Explain architecture changes before editing files. Never make large refactors without first creating a plan.
- **Patterns**: Preserve Fabric API patterns.

## Current Development Status

- The project is currently building a modular automation framework.
- Perception and context layers are being developed first.

## Building and Running

The project uses Gradle for build management.

- **Build**: `./gradlew build`
- **Development Environment**: Follow the [Fabric Modding Wiki](https://fabricmc.net/wiki/) for setting up your IDE (IntelliJ IDEA is recommended).

## Development Conventions

- **State Management**: All bot-related state must be managed through `BotContext`.
- **Tick Loop**: Use `ClientTickEvents.END_CLIENT_TICK` for mod logic execution. Heavy operations (like block scanning) should be rate-limited to avoid performance issues (currently 20 ticks/sec).
- **Logging**: Use `com.mobileminerong.debug.DebugLogger` for all diagnostic output.
- **Fabric**: Adhere to Fabric API patterns for event registration and client-side interactions.
