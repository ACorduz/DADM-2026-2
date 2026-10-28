package com.example.reto8

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.example.reto8.CompanyTable.COL_EMAIL
import com.example.reto8.CompanyTable.COL_ID
import com.example.reto8.CompanyTable.COL_NAME
import com.example.reto8.CompanyTable.COL_PHONE
import com.example.reto8.CompanyTable.COL_PRODUCTS
import com.example.reto8.CompanyTable.COL_URL
import com.example.reto8.CompanyTable.TABLE

/**
 * The CRUD operations on the companies table. [name] is the database file;
 * null keeps the database in memory (used by the instrumented tests).
 */
class CompanyDbHelper(context: Context, name: String? = DATABASE_NAME) :
    SQLiteOpenHelper(context, name, null, DATABASE_VERSION) {

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(CompanyTable.CREATE_SQL)
        SAMPLE_COMPANIES.forEach { db.insert(TABLE, null, it.toContentValues()) }
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE")
        onCreate(db)
    }

    /** Saves a new company; returns its id, or -1 if it couldn't be saved. */
    fun insert(company: Company): Long =
        writableDatabase.insert(TABLE, null, company.toContentValues())

    /** Overwrites the company with the same id; false if there's none. */
    fun update(company: Company): Boolean =
        writableDatabase.update(
            TABLE, company.toContentValues(), "$COL_ID = ?", arrayOf(company.id.toString())
        ) == 1

    /** False if there was no company with that id. */
    fun delete(id: Long): Boolean =
        writableDatabase.delete(TABLE, "$COL_ID = ?", arrayOf(id.toString())) == 1

    fun get(id: Long): Company? =
        query("$COL_ID = ?", arrayOf(id.toString())).firstOrNull()

    /** The companies that match the filters (see [CompanyFilter.build]), sorted by name. */
    fun search(
        nameQuery: String = "",
        classifications: Set<Classification> = emptySet(),
    ): List<Company> {
        val selection = CompanyFilter.build(nameQuery, classifications)
        return query(selection.where, selection.args.toTypedArray())
    }

    private fun query(where: String?, args: Array<String>): List<Company> =
        readableDatabase.query(
            TABLE, null, where, args, null, null, "$COL_NAME COLLATE NOCASE"
        ).use { cursor ->
            buildList { while (cursor.moveToNext()) add(cursor.toCompany()) }
        }

    companion object {
        const val DATABASE_NAME = "companies.db"
        const val DATABASE_VERSION = 1
    }
}

private fun Company.toContentValues() = ContentValues().apply {
    put(COL_NAME, name)
    put(COL_URL, url)
    put(COL_PHONE, phone)
    put(COL_EMAIL, email)
    put(COL_PRODUCTS, products)
    Classification.entries.forEach { put(it.column, if (it in classifications) 1 else 0) }
}

private fun Cursor.toCompany() = Company(
    id = getLong(getColumnIndexOrThrow(COL_ID)),
    name = getString(getColumnIndexOrThrow(COL_NAME)),
    url = getString(getColumnIndexOrThrow(COL_URL)),
    phone = getString(getColumnIndexOrThrow(COL_PHONE)),
    email = getString(getColumnIndexOrThrow(COL_EMAIL)),
    products = getString(getColumnIndexOrThrow(COL_PRODUCTS)),
    classifications = Classification.entries
        .filter { getInt(getColumnIndexOrThrow(it.column)) == 1 }
        .toSet(),
)
