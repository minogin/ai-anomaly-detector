package com.minogin.anomaly.internal.profiler.model

/**
 * Field-level difference between two JSON schemas.
 * Paths are dotted for nested objects, e.g. `offer.discount`.
 */
internal data class JsonSchemaDiff(
    val added: Map<String, JsonSchema>,
    val removed: Map<String, JsonSchema>,
    val changed: Map<String, Pair<JsonSchema, JsonSchema>>,
) {
    fun isEmpty(): Boolean = added.isEmpty() && removed.isEmpty() && changed.isEmpty()

    companion object {
        fun of(reference: JsonSchema, current: JsonSchema): JsonSchemaDiff {
            val added = linkedMapOf<String, JsonSchema>()
            val removed = linkedMapOf<String, JsonSchema>()
            val changed = linkedMapOf<String, Pair<JsonSchema, JsonSchema>>()

            fun walk(path: String, ref: JsonSchema, cur: JsonSchema) {
                if (ref == cur) return
                if (ref is JsonSchema.ObjectSchema && cur is JsonSchema.ObjectSchema) {
                    (cur.fields.keys - ref.fields.keys).forEach { added[join(path, it)] = cur.fields.getValue(it) }
                    (ref.fields.keys - cur.fields.keys).forEach { removed[join(path, it)] = ref.fields.getValue(it) }
                    (ref.fields.keys intersect cur.fields.keys).forEach {
                        walk(join(path, it), ref.fields.getValue(it), cur.fields.getValue(it))
                    }
                } else {
                    changed[path] = ref to cur
                }
            }

            walk("", reference, current)
            return JsonSchemaDiff(added, removed, changed)
        }

        private fun join(path: String, field: String): String =
            if (path.isEmpty()) field else "$path.$field"
    }
}

internal fun JsonSchema.format(): String = when (this) {
    is JsonSchema.Primitive -> name
    is JsonSchema.ObjectSchema -> fields.entries.joinToString(", ", "{", "}") { (name, schema) -> "$name=${schema.format()}" }
    is JsonSchema.ArraySchema -> elementSchemas.joinToString(" | ", "[", "]") { it.format() }
}
