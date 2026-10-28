package com.example.reto8

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.reto8.Classification.CONSULTING
import com.example.reto8.Classification.CUSTOM_DEVELOPMENT
import com.example.reto8.Classification.SOFTWARE_FACTORY
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CompanyDbHelperTest {

    private lateinit var mDb: CompanyDbHelper

    private val company = Company(
        name = "Test SAS",
        url = "https://test.example",
        phone = "3001234567",
        email = "a@test.example",
        products = "Apps",
        classifications = setOf(CONSULTING, SOFTWARE_FACTORY),
    )

    @Before
    fun setUp() {
        // In memory, seeded with the sample companies
        mDb = CompanyDbHelper(InstrumentationRegistry.getInstrumentation().targetContext, null)
    }

    @After
    fun tearDown() {
        mDb.close()
    }

    @Test
    fun newDatabase_hasSampleCompanies() {
        assertEquals(SAMPLE_COMPANIES.size, mDb.search().size)
    }

    @Test
    fun insertAndGet_roundTripsAllFields() {
        val id = mDb.insert(company)
        assertEquals(company.copy(id = id), mDb.get(id))
    }

    @Test
    fun update_changesStoredCompany() {
        val id = mDb.insert(company)
        val changed = company.copy(id = id, name = "Nuevo", classifications = setOf(CUSTOM_DEVELOPMENT))
        assertTrue(mDb.update(changed))
        assertEquals(changed, mDb.get(id))
    }

    @Test
    fun delete_removesCompany() {
        val id = mDb.insert(company)
        assertTrue(mDb.delete(id))
        assertNull(mDb.get(id))
        assertFalse(mDb.delete(id))
    }

    @Test
    fun search_filtersByNameAndClassification() {
        mDb.insert(Company(name = "Zeta Labs", classifications = setOf(CONSULTING)))
        mDb.insert(Company(name = "zeta Factory", classifications = setOf(SOFTWARE_FACTORY)))

        fun names(query: String, vararg classes: Classification) =
            mDb.search(query, classes.toSet()).map { it.name }

        assertEquals(listOf("zeta Factory", "Zeta Labs"), names("ZETA"))
        assertEquals(listOf("Zeta Labs"), names("zeta", CONSULTING))
        assertEquals(listOf("zeta Factory", "Zeta Labs"), names("zeta", CONSULTING, SOFTWARE_FACTORY))
        assertEquals(emptyList<String>(), names("zeta", CUSTOM_DEVELOPMENT))
    }
}
