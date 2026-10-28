package com.example.reto8

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.net.toUri

/**
 * Shows one company. Tapping its web page, phone or email opens the
 * browser, dialer or mail app; the app bar has Edit and Delete.
 */
class CompanyDetailActivity : AppCompatActivity() {

    private lateinit var mDb: CompanyDbHelper
    private var mCompanyId = 0L
    private var mCompany: Company? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_company_detail)
        setSupportActionBar(findViewById(R.id.toolbar))
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        applySystemBarInsets(findViewById(R.id.root), findViewById(R.id.app_bar))

        mDb = CompanyDbHelper(this)
        mCompanyId = intent.getLongExtra(EXTRA_ID, 0)

        findViewById<View>(R.id.url_row).setOnClickListener {
            mCompany?.let { startExternal(Intent(Intent.ACTION_VIEW, webUri(it.url))) }
        }
        findViewById<View>(R.id.phone_row).setOnClickListener {
            mCompany?.let { startExternal(Intent(Intent.ACTION_DIAL, Uri.fromParts("tel", it.phone, null))) }
        }
        findViewById<View>(R.id.email_row).setOnClickListener {
            mCompany?.let { startExternal(Intent(Intent.ACTION_SENDTO, Uri.fromParts("mailto", it.email, null))) }
        }
    }

    override fun onResume() {
        super.onResume()
        // Reloads after coming back from the edit screen
        val company = mDb.get(mCompanyId)
        if (company == null) {
            finish()
            return
        }
        mCompany = company
        show(company)
    }

    override fun onDestroy() {
        mDb.close()
        super.onDestroy()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.detail_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val company = mCompany ?: return super.onOptionsItemSelected(item)
        return when (item.itemId) {
            R.id.action_edit -> {
                startActivity(CompanyFormActivity.intent(this, company.id))
                true
            }
            R.id.action_delete -> {
                confirmDelete(this, mDb, company) { finish() }
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }

    private fun show(company: Company) {
        supportActionBar?.title = company.name
        findViewById<TextView>(R.id.classifications).text =
            classificationLabels(company.classifications)
        showRow(R.id.products_row, R.id.products, company.products)
        showRow(R.id.url_row, R.id.url, company.url)
        showRow(R.id.phone_row, R.id.phone, company.phone)
        showRow(R.id.email_row, R.id.email, company.email)
    }

    // Hides the optional fields that were left empty
    private fun showRow(rowId: Int, valueId: Int, value: String) {
        findViewById<View>(rowId).visibility = if (value.isEmpty()) View.GONE else View.VISIBLE
        findViewById<TextView>(valueId).text = value
    }

    // Companies can be saved as "www.example.com"; the browser needs a scheme
    private fun webUri(url: String): Uri =
        (if (url.startsWith("http://") || url.startsWith("https://")) url else "https://$url").toUri()

    companion object {
        private const val EXTRA_ID = "company_id"

        fun intent(context: Context, companyId: Long): Intent =
            Intent(context, CompanyDetailActivity::class.java).putExtra(EXTRA_ID, companyId)
    }
}
