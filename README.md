# AI Anomaly Detector

Detect structural drift in AI/LLM workflow outputs between versions.

## The Problem

AI workflows can silently break when you change a prompt, model, or code. The output may still look reasonable to a human, but its structure has changed — a JSON object becomes a quoted string, a field gets added or removed, a classifier starts returning markdown instead of a scalar. Downstream code that parses these outputs then breaks without an obvious error.

Real examples confirmed by developers:

- `{"riskLevel":"HIGH"}` → `"{\"riskLevel\":\"HIGH\"}"` (JSON got quoted)
- `APPROVE` → `**APPROVE**` (scalar became markdown bold)
- `{"status":"ok","count":3}` → `{"result":{"status":"ok","count":3}}` (field wrapped in new object)

## How It Works

Instrument your workflow with checkpoints:

```kotlin
val detector = AnomalyDetector(
    basePath = ".ai-anomaly-detector",
    currentVersion = "1.1",
)

// wrap LLM calls
val response = llm.call(prompt)
detector.checkpoint(step = "classify-query", input = "", output = response.content)

// optionally record which branch was taken
val queryType = parse(response.content)
detector.nextStep(step = "classify-query", nextStep = queryType.name)
```

Checkpoints are written to disk immediately after each call. Run your app as version `1.0` to record a baseline, then bump to `1.1` and run again. Compare with the CLI:

```
java -jar anomaly-detector-cli.jar .ai-anomaly-detector 1.1 1.0
```

## Example Output

```
Anomaly Detector: 1.0 → 1.1
==================================================
FINDINGS

[HIGH] Output form changed at 'classify-query'
  Reference: MARKDOWN
  Current:   STRING

PROFILE (1.1)

  classify-query: STRING → APPROVE, REJECT
  specialist-run: JSON_OBJECT {status=STRING, result=JSON_OBJECT {data=JSON_ARRAY, confidence=DECIMAL}}
```

## What It Detects

- **Output type changed** — `JSON_OBJECT` became `STRING`, `MARKDOWN` became `quoted(STRING)`, etc. (HIGH)
- **JSON schema changed** — field added, removed, renamed, or type changed within a JSON object (HIGH)
- **Inconsistent outputs** — same step producing different output types within a single version (HIGH)
- **Routing changed** — step used to transition to A, now transitions to B or C (MID)
- **Step disappeared** — step present in reference but missing in current version (MID)
- **New step** — step not present in reference version (LOW)

## Output Classification

Outputs are classified by structural type before comparison:

| Type | Example |
|------|---------|
| `JSON_OBJECT` | `{"riskLevel":"HIGH"}` |
| `JSON_ARRAY` | `[{"id":1},{"id":2}]` |
| `INTEGER` | `42` |
| `DECIMAL` | `0.95`, `1e10` |
| `STRING` | `APPROVE` |
| `MARKDOWN` | `**APPROVE**`, `# Report`, `- item` |
| `HTML` | `<p>text</p>` |
| `quoted(...)` | `"42"`, `"{\"key\":\"value\"}"` |

For JSON objects and arrays, the schema (field names and value types) is also captured and compared, so `{"status":"ok"}` and `{"result":"ok"}` are treated as different even though both are `JSON_OBJECT`.

## Installation

Add to your project via local Maven:

```
./gradlew publishToMavenLocal
```

```kotlin
repositories {
    mavenLocal()
    mavenCentral()
}

dependencies {
    implementation("com.minogin:anomaly-detector:0.2.2")
}
```

## What It Is Not

- Not an agent framework — no workflow restructuring required
- Not a correctness judge — it reports that something changed, not whether the change is good or bad
- Not a semantic analyzer — structural drift only

## Status

Early prototype (0.2.x). Core detection is working and has caught real bugs. Koog integration module and JitPack/Maven Central publishing are planned.
