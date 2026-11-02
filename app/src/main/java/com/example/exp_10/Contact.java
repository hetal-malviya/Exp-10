package com.example.exp_10;

import android.graphics.Color;
import java.io.Serializable;

/**
 * Model class representing a Contact retrieved from ContactsContract ContentProvider.
 */
public class Contact implements Serializable {
    private String id;
    private String name;
    private String phoneNumber;
    private String email;
    private String company;
    private String jobTitle;
    private String photoUri;

    public Contact(String id, String name, String phoneNumber, String email, String company, String jobTitle, String photoUri) {
        this.id = id != null ? id : "";
        this.name = (name != null && !name.trim().isEmpty()) ? name : "Unknown Contact";
        this.phoneNumber = (phoneNumber != null && !phoneNumber.trim().isEmpty()) ? phoneNumber : "No Phone Number";
        this.email = (email != null && !email.trim().isEmpty()) ? email : generateSampleEmail(this.name);
        this.company = (company != null && !company.trim().isEmpty()) ? company : "ContactSync Network";
        this.jobTitle = (jobTitle != null && !jobTitle.trim().isEmpty()) ? jobTitle : "Verified Contact";
        this.photoUri = photoUri;
    }

    public Contact(String id, String name, String phoneNumber, String photoUri) {
        this(id, name, phoneNumber, null, null, null, photoUri);
    }

    private static String generateSampleEmail(String contactName) {
        if (contactName == null || contactName.trim().isEmpty() || contactName.equalsIgnoreCase("Unknown Contact")) {
            return "contact@contactsync.org";
        }
        String cleanName = contactName.toLowerCase().replaceAll("[^a-z0-9]", "");
        if (cleanName.isEmpty()) {
            cleanName = "contact";
        }
        return cleanName + "@contactsync.org";
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public String getEmail() {
        return email;
    }

    public String getCompany() {
        return company;
    }

    public String getJobTitle() {
        return jobTitle;
    }

    public String getPhotoUri() {
        return photoUri;
    }

    /**
     * Generates rich formatted text for sharing contact data.
     */
    public String getFormattedShareText() {
        return "==================================\n" +
               "📇 CONTACT CARD (ContactSync)\n" +
               "==================================\n" +
               "👤 Name: " + name + "\n" +
               "📞 Phone: " + phoneNumber + "\n" +
               "📧 Email: " + email + "\n" +
               "🏢 Organization: " + company + "\n" +
               "💼 Title: " + jobTitle + "\n" +
               "----------------------------------\n" +
               "📲 Shared via ContactSync App\n" +
               "(Android Contacts ContentProvider)\n" +
               "==================================";
    }

    /**
     * Generates standard vCard format for sharing contact data.
     */
    public String getVCardText() {
        return "BEGIN:VCARD\n" +
               "VERSION:3.0\n" +
               "FN:" + name + "\n" +
               "TEL;TYPE=CELL:" + phoneNumber + "\n" +
               "EMAIL:" + email + "\n" +
               "ORG:" + company + "\n" +
               "TITLE:" + jobTitle + "\n" +
               "NOTE:Shared via ContactSync ContentProvider\n" +
               "END:VCARD";
    }

    /**
     * Extracts initials (up to 2 letters) from contact name for avatar display.
     */
    public String getInitials() {
        if (name == null || name.isEmpty() || name.equalsIgnoreCase("Unknown Contact")) {
            return "?";
        }
        String[] parts = name.trim().split("\\s+");
        if (parts.length >= 2) {
            return (parts[0].substring(0, 1) + parts[1].substring(0, 1)).toUpperCase();
        } else if (parts[0].length() >= 2) {
            return parts[0].substring(0, 2).toUpperCase();
        } else if (parts[0].length() == 1) {
            return parts[0].toUpperCase();
        }
        return "?";
    }

    /**
     * Generates a deterministic vibrant color based on contact name hash code.
     */
    public int getAvatarColor() {
        String[] colors = {
            "#000000", "#18181B", "#27272A", "#3F3F46",
            "#52525B", "#71717A", "#1E293B", "#334155"
        };
        int hash = Math.abs(name.hashCode());
        int colorIndex = hash % colors.length;
        return Color.parseColor(colors[colorIndex]);
    }
}
