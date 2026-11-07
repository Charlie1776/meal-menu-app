package com.example.mealmenu

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MenuParserTest {
    @Test fun parsesDatedWrapper() {
        val m = MenuParser.parse("""{"date":"2026-11-08","meals":[{"name":"Breakfast","calories":400,"protein":30,"eaten":false}]}""")
        assertEquals("2026-11-08", m.date)
        assertEquals(Meal("Breakfast", 400, 30, false), m.meals[0])
    }
    @Test fun parsesLegacyList() {
        val m = MenuParser.parse("""[{"name":"Lunch","calories":550,"protein":45}]""")
        assertNull(m.date); assertEquals(1, m.meals.size)
    }
    private fun bad(s: String) {
        try { MenuParser.parse(s); throw AssertionError("accepted: $s") } catch (e: MenuFormatException) {}
    }
    @Test fun rejectsInvalid() {
        bad("not json"); bad("{}"); bad("""{"date":"11/08/2026","meals":[]}""")
        bad("""{"date":"2026-11-08","meals":[]}""")
        bad("""{"date":"2026-11-08","meals":[{"name":"x","calories":"400","protein":1}]}""")
        bad("""{"date":"2026-11-08","meals":[{"name":"x","calories":4.5,"protein":1}]}""")
        bad("""{"date":"2026-11-08","meals":[{"calories":4,"protein":1}]}""")
        bad("""{"date":"2026-11-08","meals":[{"name":"x","calories":4,"protein":1,"eaten":"no"}]}""")
        bad("42")
    }
}
