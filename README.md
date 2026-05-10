# AI Anomaly Detector

Detect unexpected output and behavior changes in AI workflows.

AI workflows can silently change after prompt, model, tool, or code changes. This tool is an early prototype for recording important workflow checkpoints and comparing how their outputs and transitions change between versions.

## Problem

Intermediate AI workflow steps often produce outputs that downstream code depends on.

Small changes can break assumptions without obvious errors:

- JSON object becomes quoted JSON
- scalar decision becomes explanatory text
- tool call disappears
- next workflow step changes
- output framing changes while the semantic intent looks similar

Example:

Before:

```json
{"riskLevel":"MEDIUM","reason":"Ownership is not disclosed."}
```

After:

```text
"{\"riskLevel\":\"LOW\",\"reason\":\"Supplier is active and clear.\"}"
```

Both are understandable, but the second one may break code that expects a real JSON object.

Another example:

```text
REQUEST_MORE_INFORMATION
```

becomes:

```text
The supplier requires more information before approval.
```

Again, this may be semantically understandable, but it changes the output framing.

## Basic idea

Add checkpoints to important workflow steps:

```kotlin
checkpoint(
    step = "llm-classify-risk",
    input = prompt,
    output = llmOutput,
    nextStep = "llm-select-action",
)
```

The tool records observed output shapes for a version:

```text
v1.jsonl
v2.jsonl
```

Then it compares versions and reports anomalies.

## What it detects

Current prototype direction:

- output shape changes
- node / step disappearance
- next step changes
- same step producing new output framing in a later version

Example report:

```text
OUTPUT SHAPE CHANGED

Step:
  llm-classify-risk

v1:
  JSON_OBJECT x1

v2:
  QUOTED_JSON_OBJECT x1

Reference example:
  {"riskLevel":"MEDIUM","reason":"Ownership is not disclosed."}

Current example:
  "{\"riskLevel\":\"LOW\",\"reason\":\"Supplier is active and clear.\"}"
```

Another report:

```text
STEP DISAPPEARED

Step:
  tool-call-company-registry-lookup

v1:
  present

v2:
  missing
```

## What this is not

This is not an agent framework.

It does not require workflows to be rewritten as state machines.

It does not try to decide whether the new behavior is correct.

It only reports that behavior or output framing changed and should be reviewed.

## Storage model

For the prototype, records are stored as local JSONL files:

```text
.ai-anomaly-detector/
  supplier-risk/
    v1.jsonl
    v2.jsonl
```

A version file contains aggregated samples.

Conceptually:

```kotlin
data class Sample(
    val key: Key,
    val count: Int,
    val exampleInput: String,
    val exampleOutput: String,
) {
    data class Key(
        val version: String,
        val step: String,
        val inputHash: String,
        val outputKind: OutputKind,
        val nextStep: String?,
    )
}
```

## Output kinds

Initial output classification is deliberately simple:

```kotlin
enum class OutputKind {
    EMPTY,
    JSON_OBJECT,
    JSON_ARRAY,
    QUOTED_JSON_OBJECT,
    QUOTED_JSON_ARRAY,
    QUOTED_SCALAR,
    SCALAR,
    MULTILINE_TEXT
}
```

The goal is not perfect semantic understanding. The goal is to catch obvious structural changes that often break downstream code.

## Status

Early prototype.

The current goal is to validate whether checkpoint-based comparison can produce a report that is more useful than manually reading logs.
