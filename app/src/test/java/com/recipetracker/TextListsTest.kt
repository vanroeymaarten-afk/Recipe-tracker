package com.recipetracker

import com.recipetracker.model.Scores
import com.recipetracker.util.TextLists
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TextListsTest {
    @Test
    fun pastedListsAreCleaned() {
        val pasted = "1. Preheat the oven\n\n2) Mix flour\nStep 3: Bake\n• 200 g sugar\n- 2 eggs"
        assertEquals(listOf("Preheat the oven", "Mix flour", "Bake", "200 g sugar", "2 eggs"), TextLists.splitPasted(pasted))
    }

    @Test
    fun quantitiesAreNotMistakenForNumbering() {
        assertEquals("200 g flour", TextLists.clean("200 g flour"))
        assertEquals("2 eggs", TextLists.clean("2 eggs"))
    }

    @Test
    fun subheadings() {
        assertTrue(TextLists.isSubheading("For the sauce:"))
        assertFalse(TextLists.isSubheading("200 g flour"))
    }

    @Test
    fun scores() {
        assertEquals(null, Scores.average(emptyList()))
        assertEquals(4f, Scores.average(listOf(3f, 5f))!!, 0.0001f)
        assertEquals("Really good", Scores.label(4f))
        assertEquals("Perfect!", Scores.label(5f))
        assertEquals("4.3", Scores.format(4.333f))
    }
}
