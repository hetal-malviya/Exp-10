package com.example.exp_10;

import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.FrameLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.button.MaterialButton;
import java.util.ArrayList;
import java.util.List;

/**
 * Adapter for displaying Contacts in a RecyclerView with filtering support.
 */
public class ContactAdapter extends RecyclerView.Adapter<ContactAdapter.ContactViewHolder> implements Filterable {

    private Context context;
    private List<Contact> contactList;
    private List<Contact> contactListFiltered;
    private OnContactClickListener listener;

    public interface OnContactClickListener {
        void onContactClick(Contact contact);
    }

    public ContactAdapter(Context context, List<Contact> contactList, OnContactClickListener listener) {
        this.context = context;
        this.contactList = contactList;
        this.contactListFiltered = new ArrayList<>(contactList);
        this.listener = listener;
    }

    public void updateContacts(List<Contact> newContacts) {
        this.contactList = new ArrayList<>(newContacts);
        this.contactListFiltered = new ArrayList<>(newContacts);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ContactViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_contact, parent, false);
        return new ContactViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ContactViewHolder holder, int position) {
        Contact contact = contactListFiltered.get(position);

        holder.tvName.setText(contact.getName());
        holder.tvPhone.setText(contact.getPhoneNumber());
        holder.tvInitials.setText(contact.getInitials());

        // Set dynamic circle avatar color
        GradientDrawable bgShape = new GradientDrawable();
        bgShape.setShape(GradientDrawable.OVAL);
        bgShape.setColor(contact.getAvatarColor());
        holder.flAvatar.setBackground(bgShape);

        // Click on item to open details
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onContactClick(contact);
            }
        });

        // Click on Quick Share button
        holder.btnQuickShare.setOnClickListener(v -> {
            shareContact(contact);
        });
    }

    private void shareContact(Contact contact) {
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_SUBJECT, "Contact Card: " + contact.getName());
        shareIntent.putExtra(Intent.EXTRA_TEXT, contact.getFormattedShareText());

        context.startActivity(Intent.createChooser(shareIntent, "Share Contact via"));
    }

    @Override
    public int getItemCount() {
        return contactListFiltered.size();
    }

    @Override
    public Filter getFilter() {
        return new Filter() {
            @Override
            protected FilterResults performFiltering(CharSequence constraint) {
                String query = constraint.toString().trim().toLowerCase();
                List<Contact> filtered = new ArrayList<>();

                if (query.isEmpty()) {
                    filtered.addAll(contactList);
                } else {
                    for (Contact c : contactList) {
                        if (c.getName().toLowerCase().contains(query) ||
                            c.getPhoneNumber().replaceAll("\\s+", "").contains(query)) {
                            filtered.add(c);
                        }
                    }
                }

                FilterResults results = new FilterResults();
                results.values = filtered;
                results.count = filtered.size();
                return results;
            }

            @SuppressWarnings("unchecked")
            @Override
            protected void publishResults(CharSequence constraint, FilterResults results) {
                contactListFiltered = (List<Contact>) results.values;
                notifyDataSetChanged();
            }
        };
    }

    public static class ContactViewHolder extends RecyclerView.ViewHolder {
        TextView tvName, tvPhone, tvInitials;
        FrameLayout flAvatar;
        MaterialButton btnQuickShare;

        public ContactViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tvContactName);
            tvPhone = itemView.findViewById(R.id.tvContactPhone);
            tvInitials = itemView.findViewById(R.id.tvAvatarInitials);
            flAvatar = itemView.findViewById(R.id.flAvatar);
            btnQuickShare = itemView.findViewById(R.id.btnQuickShare);
        }
    }
}
