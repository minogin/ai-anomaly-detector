package com.minogin.anomaly.internal.util

import org.junit.jupiter.api.Test
import kotlin.test.*

class ConcurrentListTest {

    @Test
    fun `add and snapshot`() {
        val list = ConcurrentList<String>()
        list.add("a")
        list.add("b")
        assertEquals(listOf("a", "b"), list.snapshot())
    }

    @Test
    fun `snapshot of empty list`() {
        assertTrue(ConcurrentList<String>().snapshot().isEmpty())
    }

    @Test
    fun `updateLast updates the last matching item`() {
        val list = ConcurrentList<String>()
        list.add("a")
        list.add("b")
        list.add("a")

        val result = list.updateLast({ it == "a" }) { it.uppercase() }

        assertEquals("A", result)
        assertEquals(listOf("a", "b", "A"), list.snapshot())
    }

    @Test
    fun `updateLast returns null when no item matches`() {
        val list = ConcurrentList<String>()
        list.add("a")

        val result = list.updateLast({ it == "x" }) { it.uppercase() }

        assertNull(result)
        assertEquals(listOf("a"), list.snapshot())
    }

    @Test
    fun `updateLast on empty list returns null`() {
        val result = ConcurrentList<String>().updateLast({ true }) { it }
        assertNull(result)
    }

    @Test
    fun `remove removes first matching item`() {
        val list = ConcurrentList<String>()
        list.add("a")
        list.add("b")
        list.remove("a")
        assertEquals(listOf("b"), list.snapshot())
    }
}
