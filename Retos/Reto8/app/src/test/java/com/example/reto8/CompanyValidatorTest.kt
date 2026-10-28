package com.example.reto8

import com.example.reto8.Classification.CONSULTING
import org.junit.Assert.assertEquals
import org.junit.Test

class CompanyValidatorTest {

    private val valid = Company(
        name = "Andina Software",
        url = "https://www.andina.example/productos?x=1",
        phone = "+57 (601) 555-1234",
        email = "contacto@andina.example",
        products = "Apps móviles",
        classifications = setOf(CONSULTING)
    )

    private fun errors(company: Company) = CompanyValidator.validate(company)

    @Test
    fun completeCompany_isValid() {
        assertEquals(emptySet<Field>(), errors(valid))
    }

    @Test
    fun onlyNameAndClassification_isValid() {
        assertEquals(
            emptySet<Field>(),
            errors(Company(name = "X", classifications = setOf(CONSULTING)))
        )
    }

    @Test
    fun blankName_isRequired() {
        assertEquals(setOf(Field.NAME), errors(valid.copy(name = "  ")))
    }

    @Test
    fun noClassification_isRequired() {
        assertEquals(setOf(Field.CLASSIFICATION), errors(valid.copy(classifications = emptySet())))
    }

    @Test
    fun urlWithoutScheme_isValid() {
        assertEquals(emptySet<Field>(), errors(valid.copy(url = "www.andina.example")))
    }

    @Test
    fun malformedUrl_isRejected() {
        assertEquals(setOf(Field.URL), errors(valid.copy(url = "no es una url")))
        assertEquals(setOf(Field.URL), errors(valid.copy(url = "ftp://andina.example")))
    }

    @Test
    fun malformedEmail_isRejected() {
        assertEquals(setOf(Field.EMAIL), errors(valid.copy(email = "ana@")))
        assertEquals(setOf(Field.EMAIL), errors(valid.copy(email = "ana.andina.example")))
    }

    @Test
    fun phoneWithTooFewDigitsOrLetters_isRejected() {
        assertEquals(setOf(Field.PHONE), errors(valid.copy(phone = "12-34")))
        assertEquals(setOf(Field.PHONE), errors(valid.copy(phone = "-------")))
        assertEquals(setOf(Field.PHONE), errors(valid.copy(phone = "555-CALL-NOW")))
    }

    @Test
    fun severalProblems_areAllReported() {
        assertEquals(
            setOf(Field.NAME, Field.EMAIL, Field.CLASSIFICATION),
            errors(Company(name = "", email = "x", classifications = emptySet()))
        )
    }
}
