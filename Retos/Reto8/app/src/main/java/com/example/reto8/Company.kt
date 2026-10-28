package com.example.reto8

import androidx.annotation.StringRes

/** The kinds of company in the directory; a company can be several of them. */
enum class Classification(val column: String, @param:StringRes val label: Int) {
    CONSULTING(CompanyTable.COL_CONSULTING, R.string.class_consulting),
    CUSTOM_DEVELOPMENT(CompanyTable.COL_CUSTOM_DEVELOPMENT, R.string.class_custom_development),
    SOFTWARE_FACTORY(CompanyTable.COL_SOFTWARE_FACTORY, R.string.class_software_factory),
}

/** A software company in the directory. [id] is 0 until it's saved. */
data class Company(
    val id: Long = 0,
    val name: String,
    val url: String = "",
    val phone: String = "",
    val email: String = "",
    val products: String = "",
    val classifications: Set<Classification> = emptySet(),
)

/** Table and column names of the SQLite schema. */
object CompanyTable {
    const val TABLE = "companies"
    const val COL_ID = "_id"
    const val COL_NAME = "name"
    const val COL_URL = "url"
    const val COL_PHONE = "phone"
    const val COL_EMAIL = "email"
    const val COL_PRODUCTS = "products"
    const val COL_CONSULTING = "is_consulting"
    const val COL_CUSTOM_DEVELOPMENT = "is_custom_development"
    const val COL_SOFTWARE_FACTORY = "is_software_factory"

    const val CREATE_SQL = """
        CREATE TABLE $TABLE (
            $COL_ID INTEGER PRIMARY KEY AUTOINCREMENT,
            $COL_NAME TEXT NOT NULL,
            $COL_URL TEXT NOT NULL DEFAULT '',
            $COL_PHONE TEXT NOT NULL DEFAULT '',
            $COL_EMAIL TEXT NOT NULL DEFAULT '',
            $COL_PRODUCTS TEXT NOT NULL DEFAULT '',
            $COL_CONSULTING INTEGER NOT NULL DEFAULT 0,
            $COL_CUSTOM_DEVELOPMENT INTEGER NOT NULL DEFAULT 0,
            $COL_SOFTWARE_FACTORY INTEGER NOT NULL DEFAULT 0
        )"""
}

/** Companies loaded when the database is created, so the list isn't empty. All fictional. */
val SAMPLE_COMPANIES = listOf(
    Company(
        name = "Andina Software",
        url = "https://www.andinasoftware.example",
        phone = "+57 601 555 0101",
        email = "contacto@andinasoftware.example",
        products = "Apps móviles y web a la medida para el sector financiero",
        classifications = setOf(Classification.CUSTOM_DEVELOPMENT),
    ),
    Company(
        name = "Cóndor Consultores TI",
        url = "https://www.condorti.example",
        phone = "+57 604 555 0202",
        email = "info@condorti.example",
        products = "Arquitectura de software, transformación digital y auditoría de sistemas",
        classifications = setOf(Classification.CONSULTING),
    ),
    Company(
        name = "Fábrica Digital del Caribe",
        url = "https://www.fabricacaribe.example",
        phone = "+57 605 555 0303",
        email = "ventas@fabricacaribe.example",
        products = "Desarrollo de software por lotes, pruebas y mantenimiento",
        classifications = setOf(Classification.SOFTWARE_FACTORY, Classification.CUSTOM_DEVELOPMENT),
    ),
    Company(
        name = "Pacífico Analytics",
        url = "https://www.pacificoanalytics.example",
        phone = "+57 602 555 0404",
        email = "hola@pacificoanalytics.example",
        products = "Analítica de datos, tableros e integración de sistemas",
        classifications = setOf(Classification.CONSULTING, Classification.CUSTOM_DEVELOPMENT),
    ),
    Company(
        name = "Llanos Code Factory",
        url = "https://www.llanoscode.example",
        phone = "+57 608 555 0505",
        email = "contacto@llanoscode.example",
        products = "Equipos de desarrollo dedicados y QA",
        classifications = setOf(Classification.SOFTWARE_FACTORY),
    ),
)
