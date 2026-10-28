package com.example.reto8

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

/** The rows of the company list: name, classifications and contact. */
class CompanyAdapter(
    private val onClick: (Company) -> Unit,
    private val onLongClick: (Company) -> Unit,
) : RecyclerView.Adapter<CompanyAdapter.Holder>() {

    private var mCompanies: List<Company> = emptyList()

    // The list is small and reloaded whole after every change
    @SuppressLint("NotifyDataSetChanged")
    fun submit(companies: List<Company>) {
        mCompanies = companies
        notifyDataSetChanged()
    }

    override fun getItemCount() = mCompanies.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = Holder(
        LayoutInflater.from(parent.context).inflate(R.layout.item_company, parent, false)
    )

    override fun onBindViewHolder(holder: Holder, position: Int) {
        val company = mCompanies[position]
        val contact = listOf(company.phone, company.email)
            .filter { it.isNotEmpty() }
            .joinToString(" · ")

        holder.name.text = company.name
        holder.classifications.text =
            holder.itemView.context.classificationLabels(company.classifications)
        holder.contact.text = contact
        holder.contact.visibility = if (contact.isEmpty()) View.GONE else View.VISIBLE
        holder.itemView.setOnClickListener { onClick(company) }
        holder.itemView.setOnLongClickListener {
            onLongClick(company)
            true
        }
    }

    class Holder(view: View) : RecyclerView.ViewHolder(view) {
        val name: TextView = view.findViewById(R.id.name)
        val classifications: TextView = view.findViewById(R.id.classifications)
        val contact: TextView = view.findViewById(R.id.contact)
    }
}
