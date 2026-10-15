package com.example.exp_10

import android.content.Context
import android.content.Intent
import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Filter
import android.widget.Filterable
import android.widget.FrameLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import java.util.Locale

/**
 * Kotlin RecyclerView Adapter for displaying Contacts with search filtering and quick actions.
 */
class ContactAdapter(
    private val context: Context,
    private var contactList: List<Contact>,
    private val listener: OnContactClickListener
) : RecyclerView.Adapter<ContactAdapter.ContactViewHolder>(), Filterable {

    private var contactListFiltered: List<Contact> = ArrayList(contactList)

    fun interface OnContactClickListener {
        fun onContactClick(contact: Contact)
    }

    fun updateContacts(newContacts: List<Contact>) {
        contactList = ArrayList(newContacts)
        contactListFiltered = ArrayList(newContacts)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ContactViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.item_contact, parent, false)
        return ContactViewHolder(view)
    }

    override fun onBindViewHolder(holder: ContactViewHolder, position: Int) {
        val contact = contactListFiltered[position]

        holder.tvName.text = contact.resolvedName
        holder.tvPhone.text = contact.resolvedPhone
        holder.tvInitials.text = contact.getInitials()

        // Dynamic avatar background shape
        val bgShape = GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            setColor(contact.getAvatarColor())
        }
        holder.flAvatar.background = bgShape

        // Item click listener
        holder.itemView.setOnClickListener {
            listener.onContactClick(contact)
        }

        // Quick Share button click listener
        holder.btnQuickShare.setOnClickListener {
            shareContact(contact)
        }
    }

    private fun shareContact(contact: Contact) {
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, "Contact Card: ${contact.resolvedName}")
            putExtra(Intent.EXTRA_TEXT, contact.getFormattedShareText())
        }
        context.startActivity(Intent.createChooser(shareIntent, "Share Contact Card via"))
    }

    override fun getItemCount(): Int = contactListFiltered.size

    override fun getFilter(): Filter {
        return object : Filter() {
            override fun performFiltering(constraint: CharSequence?): FilterResults {
                val query = constraint?.toString()?.trim()?.lowercase(Locale.getDefault()) ?: ""
                val filtered = if (query.isEmpty()) {
                    contactList
                } else {
                    contactList.filter { c ->
                        c.resolvedName.lowercase(Locale.getDefault()).contains(query) ||
                                c.resolvedPhone.replace(Regex("\\s+"), "").contains(query)
                    }
                }

                return FilterResults().apply {
                    values = filtered
                    count = filtered.size
                }
            }

            @Suppress("UNCHECKED_CAST")
            override fun publishResults(constraint: CharSequence?, results: FilterResults?) {
                contactListFiltered = (results?.values as? List<Contact>) ?: emptyList()
                notifyDataSetChanged()
            }
        }
    }

    class ContactViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvName: TextView = itemView.findViewById(R.id.tvContactName)
        val tvPhone: TextView = itemView.findViewById(R.id.tvContactPhone)
        val tvInitials: TextView = itemView.findViewById(R.id.tvAvatarInitials)
        val flAvatar: FrameLayout = itemView.findViewById(R.id.flAvatar)
        val btnQuickShare: MaterialButton = itemView.findViewById(R.id.btnQuickShare)
    }
}
