package com.example.exp_10

import android.graphics.Color
import java.io.Serializable
import kotlin.math.abs

/**
 * Kotlin Data Model class representing a Contact retrieved from ContactsContract ContentProvider.
 */
data class Contact(
    val id: String = "",
    val name: String = "Unknown Contact",
    val phoneNumber: String = "No Phone Number",
    val email: String = "",
    val company: String = "ContactSync Network",
    val jobTitle: String = "Verified Contact",
    val photoUri: String? = null
) : Serializable {

    val resolvedName: String
        get() = name.ifBlank { "Unknown Contact" }

    val resolvedPhone: String
        get() = phoneNumber.ifBlank { "No Phone Number" }

    val resolvedEmail: String
        get() = email.ifBlank { generateSampleEmail(resolvedName) }

    val resolvedCompany: String
        get() = company.ifBlank { "ContactSync Network" }

    val resolvedJobTitle: String
        get() = jobTitle.ifBlank { "Verified Contact" }

    private fun generateSampleEmail(contactName: String): String {
        if (contactName.isBlank() || contactName.equals("Unknown Contact", ignoreCase = true)) {
            return "contact@contactsync.org"
        }
        val cleanName = contactName.lowercase().replace(Regex("[^a-z0-9]"), "")
        return if (cleanName.isEmpty()) "contact@contactsync.org" else "$cleanName@contactsync.org"
    }

    /**
     * Generates rich formatted text for sharing contact data.
     */
    fun getFormattedShareText(): String {
        return """
            ==================================
            📇 CONTACT CARD (ContactSync)
            ==================================
            👤 Name: $resolvedName
            📞 Phone: $resolvedPhone
            📧 Email: $resolvedEmail
            🏢 Organization: $resolvedCompany
            💼 Title: $resolvedJobTitle
            ----------------------------------
            📲 Shared via ContactSync App
            (Android Contacts ContentProvider)
            ==================================
        """.trimIndent()
    }

    /**
     * Generates standard vCard format for sharing contact data.
     */
    fun getVCardText(): String {
        return """
            BEGIN:VCARD
            VERSION:3.0
            FN:$resolvedName
            TEL;TYPE=CELL:$resolvedPhone
            EMAIL:$resolvedEmail
            ORG:$resolvedCompany
            TITLE:$resolvedJobTitle
            NOTE:Shared via ContactSync ContentProvider
            END:VCARD
        """.trimIndent()
    }

    /**
     * Extracts initials (up to 2 letters) from contact name for avatar display.
     */
    fun getInitials(): String {
        val clean = resolvedName.trim()
        if (clean.isEmpty() || clean.equals("Unknown Contact", ignoreCase = true)) {
            return "?"
        }
        val parts = clean.split(Regex("\\s+"))
        return when {
            parts.size >= 2 -> (parts[0].take(1) + parts[1].take(1)).uppercase()
            parts[0].length >= 2 -> parts[0].take(2).uppercase()
            parts[0].isNotEmpty() -> parts[0].uppercase()
            else -> "?"
        }
    }

    /**
     * Generates a deterministic vibrant color based on contact name hash code.
     */
    fun getAvatarColor(): Int {
        val colors = arrayOf(
            "#000000", "#18181B", "#27272A", "#3F3F46",
            "#52525B", "#71717A", "#1E293B", "#334155"
        )
        val hash = abs(resolvedName.hashCode())
        val colorIndex = hash % colors.size
        return Color.parseColor(colors[colorIndex])
    }
}
