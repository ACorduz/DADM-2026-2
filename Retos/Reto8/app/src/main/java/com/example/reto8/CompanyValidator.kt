package com.example.reto8

/** The form fields that can fail validation. */
enum class Field { NAME, URL, PHONE, EMAIL, CLASSIFICATION }

object CompanyValidator {

    private val URL = Regex("""^(https?://)?([\w-]+\.)+[a-zA-Z]{2,}(:\d+)?(/\S*)?$""")
    private val EMAIL = Regex("""^[\w.+-]+@([\w-]+\.)+[a-zA-Z]{2,}$""")
    private val PHONE = Regex("""^\+?[\d\s()-]+$""")

    /**
     * The fields of [company] that are invalid; empty when it can be saved.
     * Only the name and at least one classification are required; the
     * contact fields are checked only when filled in.
     */
    fun validate(company: Company): Set<Field> = buildSet {
        if (company.name.isBlank()) add(Field.NAME)
        if (company.url.isNotEmpty() && !URL.matches(company.url)) add(Field.URL)
        if (company.phone.isNotEmpty() && !isPhone(company.phone)) add(Field.PHONE)
        if (company.email.isNotEmpty() && !EMAIL.matches(company.email)) add(Field.EMAIL)
        if (company.classifications.isEmpty()) add(Field.CLASSIFICATION)
    }

    private fun isPhone(phone: String) =
        PHONE.matches(phone) && phone.count { it.isDigit() } in 7..15
}
