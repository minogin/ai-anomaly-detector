# CLAUDE.md

## Working rules

1. Do not make big changes without Andrey's consent.
2. Do not run long-running tasks without Andrey's consent.
3. If in doubt, ask Andrey.
4. Discuss first, then implement once everything is clear.
5. All functional code must be covered with tests, except where a test would be completely useless.
6. Explain issues briefly and in plain words. Do not use self-invented terms or jargon without defining them first.

## Project facts

- Kotlin/JVM library plus CLI that detects structural drift in LLM workflow outputs between two recorded versions.
- Build: `./gradlew build` (JDK 21 toolchain, Gradle 9.5, Kotlin 2.3). Tests: `./gradlew test`. Fat CLI jar: `./gradlew cliJar`.
- Current goal: prepare the repo for the 6 October 2026 talk. The task description is kept outside the repo (it must never be committed); its Non-goals section is binding: no framework integrations, no monitoring mode, no UI, no semantic analysis, no new detectors, no broad refactoring.
- NDA: everything in the repo, demo and docs must be synthetic. Never reference a real system, prompt, enum member or data.
