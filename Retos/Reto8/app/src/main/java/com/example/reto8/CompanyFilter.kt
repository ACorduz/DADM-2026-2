package com.example.reto8

/** A WHERE clause (null = every row) and its arguments, for SQLiteDatabase.query(). */
data class Selection(val where: String?, val args: List<String>)

object CompanyFilter {

    /**
     * Companies whose name contains [nameQuery] (case-insensitive; blank =
     * any name) and that have at least one of [classifications] (empty =
     * any classification).
     */
    fun build(nameQuery: String, classifications: Set<Classification>): Selection {
        val clauses = mutableListOf<String>()
        val args = mutableListOf<String>()

        val name = nameQuery.trim()
        if (name.isNotEmpty()) {
            clauses += "${CompanyTable.COL_NAME} LIKE ? ESCAPE '\\'"
            args += "%" + escapeLike(name) + "%"
        }
        if (classifications.isNotEmpty()) {
            clauses += classifications.sortedBy { it.ordinal }
                .joinToString(" OR ", "(", ")") { "${it.column} = 1" }
        }
        return Selection(clauses.joinToString(" AND ").ifEmpty { null }, args)
    }

    // So a typed % or _ matches itself instead of acting as a wildcard
    private fun escapeLike(text: String) =
        text.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_")
}
