package com.minogin.anomaly.internal.profiler

import com.minogin.anomaly.internal.profiler.model.*
import com.minogin.anomaly.internal.profiler.model.JsonSchema.Primitive.*
import org.junit.jupiter.api.Test
import kotlin.test.*

class JsonSchemaDiffTest {
    private fun obj(vararg fields: Pair<String, JsonSchema>) = JsonSchema.ObjectSchema(mapOf(*fields))

    @Test
    fun `identical schemas produce an empty diff`() {
        val schema = obj("reply" to STRING, "discount" to INTEGER)
        assertTrue(JsonSchemaDiff.of(schema, schema).isEmpty())
    }

    @Test
    fun `reports added, removed and changed fields`() {
        val reference = obj("reply" to STRING, "discount" to INTEGER, "valid" to BOOLEAN)
        val current = obj("reply" to STRING, "discount" to DECIMAL, "code" to STRING)

        val diff = JsonSchemaDiff.of(reference, current)

        assertEquals(mapOf("code" to STRING), diff.added)
        assertEquals(mapOf("valid" to BOOLEAN), diff.removed)
        assertEquals(mapOf("discount" to (INTEGER to DECIMAL)), diff.changed)
    }

    @Test
    fun `wrapping a field in a new object shows as added object and removed field`() {
        val reference = obj("reply" to STRING, "discount" to INTEGER)
        val current = obj("reply" to STRING, "offer" to obj("discount" to INTEGER))

        val diff = JsonSchemaDiff.of(reference, current)

        assertEquals(mapOf("offer" to obj("discount" to INTEGER)), diff.added)
        assertEquals(mapOf("discount" to INTEGER), diff.removed)
        assertTrue(diff.changed.isEmpty())
    }

    @Test
    fun `nested changes use dotted paths`() {
        val reference = obj("offer" to obj("discount" to INTEGER, "currency" to STRING))
        val current = obj("offer" to obj("discount" to STRING, "expires" to STRING))

        val diff = JsonSchemaDiff.of(reference, current)

        assertEquals(mapOf("offer.expires" to STRING), diff.added)
        assertEquals(mapOf("offer.currency" to STRING), diff.removed)
        assertEquals(mapOf("offer.discount" to (INTEGER to STRING)), diff.changed)
    }

    @Test
    fun `formats schemas compactly`() {
        val schema = obj(
            "status" to STRING,
            "result" to obj("data" to JsonSchema.ArraySchema(setOf(INTEGER)), "confidence" to DECIMAL)
        )
        assertEquals("{status=STRING, result={data=[INTEGER], confidence=DECIMAL}}", schema.format())
    }
}
