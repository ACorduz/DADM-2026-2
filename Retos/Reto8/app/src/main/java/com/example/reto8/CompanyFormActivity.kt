package com.example.reto8

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.CheckBox
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.textfield.TextInputLayout

/** Creates a company, or edits one when started with [intent] and its id. */
class CompanyFormActivity : AppCompatActivity() {

    private lateinit var mDb: CompanyDbHelper
    // 0 while creating a new company
    private var mCompanyId = 0L

    private lateinit var mNameLayout: TextInputLayout
    private lateinit var mUrlLayout: TextInputLayout
    private lateinit var mPhoneLayout: TextInputLayout
    private lateinit var mEmailLayout: TextInputLayout
    private lateinit var mProductsLayout: TextInputLayout
    private lateinit var mChecks: Map<Classification, CheckBox>
    private lateinit var mClassificationError: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_company_form)
        setSupportActionBar(findViewById(R.id.toolbar))
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        applySystemBarInsets(findViewById(R.id.root), findViewById(R.id.app_bar))

        mDb = CompanyDbHelper(this)
        mCompanyId = intent.getLongExtra(EXTRA_ID, 0)
        supportActionBar?.setTitle(if (mCompanyId == 0L) R.string.title_new else R.string.title_edit)

        mNameLayout = findViewById(R.id.name_layout)
        mUrlLayout = findViewById(R.id.url_layout)
        mPhoneLayout = findViewById(R.id.phone_layout)
        mEmailLayout = findViewById(R.id.email_layout)
        mProductsLayout = findViewById(R.id.products_layout)
        mChecks = mapOf(
            Classification.CONSULTING to findViewById(R.id.check_consulting),
            Classification.CUSTOM_DEVELOPMENT to findViewById(R.id.check_custom_development),
            Classification.SOFTWARE_FACTORY to findViewById(R.id.check_software_factory),
        )
        mClassificationError = findViewById(R.id.classification_error)
        findViewById<Button>(R.id.save_button).setOnClickListener { save() }

        // After a rotation the views restore what the user had typed
        if (savedInstanceState == null && mCompanyId != 0L) {
            val company = mDb.get(mCompanyId)
            if (company == null) {
                finish()
                return
            }
            fill(company)
        }
    }

    override fun onDestroy() {
        mDb.close()
        super.onDestroy()
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }

    private fun fill(company: Company) {
        mNameLayout.editText?.setText(company.name)
        mUrlLayout.editText?.setText(company.url)
        mPhoneLayout.editText?.setText(company.phone)
        mEmailLayout.editText?.setText(company.email)
        mProductsLayout.editText?.setText(company.products)
        mChecks.forEach { (classification, check) ->
            check.isChecked = classification in company.classifications
        }
    }

    private fun readForm() = Company(
        id = mCompanyId,
        name = mNameLayout.text(),
        url = mUrlLayout.text(),
        phone = mPhoneLayout.text(),
        email = mEmailLayout.text(),
        products = mProductsLayout.text(),
        classifications = mChecks.filterValues { it.isChecked }.keys,
    )

    private fun TextInputLayout.text() = editText?.text.toString().trim()

    private fun save() {
        val company = readForm()
        val errors = CompanyValidator.validate(company)

        mNameLayout.error = if (Field.NAME in errors) getString(R.string.error_name) else null
        mUrlLayout.error = if (Field.URL in errors) getString(R.string.error_url) else null
        mPhoneLayout.error = if (Field.PHONE in errors) getString(R.string.error_phone) else null
        mEmailLayout.error = if (Field.EMAIL in errors) getString(R.string.error_email) else null
        mClassificationError.visibility =
            if (Field.CLASSIFICATION in errors) View.VISIBLE else View.GONE
        if (errors.isNotEmpty()) return

        val saved = if (mCompanyId == 0L) mDb.insert(company) != -1L else mDb.update(company)
        if (saved) {
            Toast.makeText(this, R.string.saved, Toast.LENGTH_SHORT).show()
            finish()
        } else {
            Toast.makeText(this, R.string.save_failed, Toast.LENGTH_SHORT).show()
        }
    }

    companion object {
        private const val EXTRA_ID = "company_id"

        fun intent(context: Context, companyId: Long): Intent =
            Intent(context, CompanyFormActivity::class.java).putExtra(EXTRA_ID, companyId)
    }
}
