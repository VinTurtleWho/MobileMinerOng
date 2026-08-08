# Rotation System Documentation

This document outlines the current implementation of the `RotationEngine` and its integration within the `MobileMinerOng` framework as of August 2026.

## 1. Overview
The rotation system is designed to provide smooth, human-like aim transitions while avoiding anti-cheat triggers. It replaces complex, noisy, and non-deterministic logic with a clean, predictive trajectory model.

## 2. Core Components

### 2.1 Trajectory Generation (`RotationEngine.java`)
- **Model**: Uses a 5th-order Minimum-Jerk trajectory generator (`10τ³ - 15τ⁴ + 6τ⁵`) to ensure velocity and acceleration profiles are smooth and continuous.
- **Goal**: To move from the current orientation to the target orientation without abrupt snaps or robotic movements.
- **Stability Mechanism**: Implements a **hysteresis deadband** (0.5 degrees). The engine ignores destination update requests if the target coordinate has moved by less than 0.5 degrees, preventing high-frequency jitter and snapping during tracking.

### 2.2 Aim Point Selection (`CombatFollowTask.java`)
- **Logic**: Uses stable center-mass targeting to avoid unnatural ping-ponging between hitboxes.
- **Strategy**: 
    1.  **Center-Mass**: Targets `entity.position().add(0, entity.getBbHeight() * 0.5, 0)`.
- **Consistency**: This ensures a steady, predictable aim point that is less prone to sudden jumps, significantly reducing detection vectors.

## 3. Lifecycle
1.  **Activation**: `startRotation()` is called with the current yaw/pitch and target vector. It initializes the trajectory curve.
2.  **Tracking**: `updateTarget()` is called periodically. If the target moves significantly, the internal target destination is updated.
3.  **Deactivation**: `abort()` is explicitly called during macro deactivation to clear state and release control.

## 4. Anti-Cheat Considerations
- The system avoids non-deterministic "noise" or "drift," which often look suspicious to heuristic anti-cheat algorithms.
- Smoothness is achieved purely through the Minimum-Jerk trajectory curve.
- Destination updates are throttled to prevent jitter, making movement appear intentional and controlled.
