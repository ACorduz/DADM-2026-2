package com.example.reto8

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.doAfterTextChanged
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.chip.Chip
import com.google.android.material.floatingactionbutton.FloatingActionButton

/**
 * The directory: lists the companies, filtered by name and/or
 * classification. Tap a company to see it, long-press it to delete it,
 * or use + to add a new one.
 */
class MainActivity : AppCompatActivity() {

    private lateinit var mDb: CompanyDbHelper
    private lateinit var mAdapter: CompanyAdapter
    private lateinit var mSearchView: EditText
    private lateinit var mEmptyView: TextView
    private lateinit var mChips: Map<Classification, Chip>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(findViewById(R.id.toolbar))
        supportActionBar?.setTitle(R.string.title_directory)
        applySystemBarInsets(findViewById(R.id.root), findViewById(R.id.app_bar))

        mDb = CompanyDbHelper(this)
        mAdapter = CompanyAdapter(
            onClick = { startActivity(CompanyDetailActivity.intent(this, it.id)) },
            onLongClick = { confirmDelete(this, mDb, it) { refresh() } },
        )
        findViewById<RecyclerView>(R.id.company_list).apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
            adapter = mAdapter
            addItemDecoration(
                DividerItemDecoration(this@MainActivity, DividerItemDecoration.VERTICAL)
            )
        }
        mEmptyView = findViewById(R.id.empty)

        mSearchView = findViewById(R.id.search)
        mSearchView.doAfterTextChanged { refresh() }
        mChips = mapOf(
            Classification.CONSULTING to findViewById(R.id.chip_consulting),
            Classification.CUSTOM_DEVELOPMENT to findViewById(R.id.chip_custom_development),
            Classification.SOFTWARE_FACTORY to findViewById(R.id.chip_software_factory),
        )
        mChips.values.forEach { chip -> chip.setOnCheckedChangeListener { _, _ -> refresh() } }

        findViewById<FloatingActionButton>(R.id.add_button).setOnClickListener {
            startActivity(Intent(this, CompanyFormActivity::class.java))
        }
    }

    override fun onResume() {
        super.onResume()
        // Picks up companies added, edited or deleted on the other screens
        refresh()
    }

    override fun onDestroy() {
        mDb.close()
        super.onDestroy()
    }

    private fun refresh() {
        val nameQuery = mSearchView.text.toString()
        val selected = mChips.filterValues { it.isChecked }.keys
        val companies = mDb.search(nameQuery, selected)

        mAdapter.submit(companies)
        supportActionBar?.subtitle =
            resources.getQuantityString(R.plurals.company_count, companies.size, companies.size)

        val filtered = nameQuery.isNotBlank() || selected.isNotEmpty()
        mEmptyView.setText(if (filtered) R.string.empty_filtered else R.string.empty_list)
        mEmptyView.visibility = if (companies.isEmpty()) View.VISIBLE else View.GONE
    }
}
