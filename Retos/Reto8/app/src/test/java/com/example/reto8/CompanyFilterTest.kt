package com.example.reto8

import com.example.reto8.Classification.CONSULTING
import com.example.reto8.Classification.CUSTOM_DEVELOPMENT
import com.example.reto8.Classification.SOFTWARE_FACTORY
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CompanyFilterTest {

    @Test
    fun noFilters_selectsEverything() {
        val selection = CompanyFilter.build("", emptySet())
        assertNull(selection.where)
        assertEquals(emptyList<String>(), selection.args)
    }

    @Test
    fun blankName_isIgnored() {
        assertNull(CompanyFilter.build("   ", emptySet()).where)
    }

    @Test
    fun name_matchesTrimmedSubstring() {
        val selection = CompanyFilter.build("  soft ", emptySet())
        assertEquals("name LIKE ? ESCAPE '\\'", selection.where)
        assertEquals(listOf("%soft%"), selection.args)
    }

    @Test
    fun likeWildcardsInName_areEscaped() {
        val selection = CompanyFilter.build("50%_a\\b", emptySet())
        assertEquals(listOf("%50\\%\\_a\\\\b%"), selection.args)
    }

    @Test
    fun classifications_areOredInDeclarationOrder() {
        val selection = CompanyFilter.build("", setOf(SOFTWARE_FACTORY, CONSULTING))
        assertEquals("(is_consulting = 1 OR is_software_factory = 1)", selection.where)
        assertEquals(emptyList<String>(), selection.args)
    }

    @Test
    fun nameAndClassification_areAnded() {
        val selection = CompanyFilter.build("a", setOf(CUSTOM_DEVELOPMENT))
        assertEquals(
            "name LIKE ? ESCAPE '\\' AND (is_custom_development = 1)",
            selection.where
        )
        assertEquals(listOf("%a%"), selection.args)
    }
}
