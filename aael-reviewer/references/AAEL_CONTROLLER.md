# AAEL Controller Procedure

This file defines the strict operational procedures for the Autonomous Adversarial Engineering Loop (AAEL). As the agent, you MUST follow this protocol in every turn.

## 1. Loop Enforcement
1.  **Read State**: Always start by reading `aael_state.json`.
2.  **Determine Phase**:
    - **IMPLEMENTING**: Work on the current subgoal.
    - **BUILDING**: Run `./gradlew build`.
    - **TESTING**: Run tests (if applicable).
    - **ADVERSARIAL_REVIEW**: Invoke `aael-reviewer` skill as a sub-agent.
    - **FIXING_DEFECTS**: Implement all defects from the reviewer's report.
    - **VERIFIED**: Mark subgoal as verified, increment `current_subgoal_index`, reset `review_cycle` to 0.
    - **COMPLETED**: Stop all tasks.
3.  **Update State**: Update `aael_state.json` *before* and *after* every phase transition or action.

## 2. Adversarial Review Rules
- Reviewer invocation: Always run as a separate sub-agent (`invoke_agent`).
- Defect Handling:
  - If reviewer reports defects:
    - Phase -> `FIXING_DEFECTS`
    - Apply ALL fixes.
    - Return to `BUILDING` phase.
  - If reviewer reports "STABLE":
    - Phase -> `VERIFIED`
    - Record review result in `verification_history`.
    - Advance to next subgoal.

## 3. Autonomy & Persistence
- Do NOT pause for permission during the loop.
- If you lose context or are interrupted, load `aael_state.json` to resume the phase from the exact point of interruption.
- The controller is strictly responsible for advancing the state machine.
