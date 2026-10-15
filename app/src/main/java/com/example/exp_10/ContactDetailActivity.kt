package com.example.exp_10

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.widget.FrameLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton

/**
 * Kotlin Detailed view activity displaying contact details with sharing, calling, and messaging actions.
 */
class ContactDetailActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_CONTACT = "extra_contact"
    }

    private var contact: Contact? = null

    private lateinit var tvName: TextView
    private lateinit var tvPhone: TextView
    private lateinit var tvEmail: TextView
    private lateinit var tvCompany: TextView
    private lateinit var tvJobTitle: TextView
    private lateinit var tvInitials: TextView
    private lateinit var tvSharePreview: TextView
    private lateinit var flAvatar: FrameLayout
    private var llBackContainer: View? = null
    private var btnBack: View? = null

    private lateinit var btnShareCard: MaterialButton
    private lateinit var btnShareVCard: MaterialButton
    private lateinit var btnCall: MaterialButton
    private lateinit var btnMessage: MaterialButton
    private lateinit var btnCopy: MaterialButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_contact_detail)

        contact = if (intent != null && intent.hasExtra(EXTRA_CONTACT)) {
            @Suppress("DEPRECATION")
            intent.getSerializableExtra(EXTRA_CONTACT) as? Contact
        } else {
            null
        }

        if (contact == null) {
            Toast.makeText(this, "Failed to load contact details", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        initViews()
        bindContactData()
        setupListeners()
        setupBackNavigation()
    }

    private fun initViews() {
        tvName = findViewById(R.id.tvDetailName)
        tvPhone = findViewById(R.id.tvDetailPhone)
        tvEmail = findViewById(R.id.tvDetailEmail)
        tvCompany = findViewById(R.id.tvDetailCompany)
        tvJobTitle = findViewById(R.id.tvDetailJobTitle)
        tvInitials = findViewById(R.id.tvDetailInitials)
        tvSharePreview = findViewById(R.id.tvSharePreview)
        flAvatar = findViewById(R.id.flDetailAvatar)
        llBackContainer = findViewById(R.id.llBackContainer)
        btnBack = findViewById(R.id.btnBack)

        btnShareCard = findViewById(R.id.btnShareContact)
        btnShareVCard = findViewById(R.id.btnShareVCard)
        btnCall = findViewById(R.id.btnCall)
        btnMessage = findViewById(R.id.btnMessage)
        btnCopy = findViewById(R.id.btnCopyNumber)
    }

    private fun bindContactData() {
        val currentContact = contact ?: return

        tvName.text = currentContact.resolvedName
        tvPhone.text = currentContact.resolvedPhone
        tvEmail.text = currentContact.resolvedEmail
        tvCompany.text = currentContact.resolvedCompany
        tvJobTitle.text = "${currentContact.resolvedJobTitle} • ${currentContact.resolvedCompany}"
        tvInitials.text = currentContact.getInitials()
        tvSharePreview.text = currentContact.getFormattedShareText()

        // Dynamic avatar background shape
        val bgShape = GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            setColor(currentContact.getAvatarColor())
        }
        flAvatar.background = bgShape
    }

    private fun setupListeners() {
        val currentContact = contact ?: return

        llBackContainer?.setOnClickListener { finish() }
        btnBack?.setOnClickListener { finish() }

        // Share Formatted Contact Card
        btnShareCard.setOnClickListener {
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_SUBJECT, "Contact Card: ${currentContact.resolvedName}")
                putExtra(Intent.EXTRA_TEXT, currentContact.getFormattedShareText())
            }
            startActivity(Intent.createChooser(shareIntent, "Share Contact Card via"))
        }

        // Share vCard Format
        btnShareVCard.setOnClickListener {
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/x-vcard"
                putExtra(Intent.EXTRA_SUBJECT, "${currentContact.resolvedName}.vcf")
                putExtra(Intent.EXTRA_TEXT, currentContact.getVCardText())
            }
            startActivity(Intent.createChooser(shareIntent, "Share vCard via"))
        }

        // Call Action
        btnCall.setOnClickListener {
            try {
                val callIntent = Intent(Intent.ACTION_DIAL).apply {
                    data = Uri.parse("tel:${currentContact.resolvedPhone}")
                }
                startActivity(callIntent)
            } catch (e: Exception) {
                Toast.makeText(this, "Unable to launch dialer", Toast.LENGTH_SHORT).show()
            }
        }

        // Message Action
        btnMessage.setOnClickListener {
            try {
                val smsIntent = Intent(Intent.ACTION_SENDTO).apply {
                    data = Uri.parse("smsto:${currentContact.resolvedPhone}")
                }
                startActivity(smsIntent)
            } catch (e: Exception) {
                Toast.makeText(this, "Unable to launch messaging app", Toast.LENGTH_SHORT).show()
            }
        }

        // Copy Phone Number
        btnCopy.setOnClickListener {
            val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
            val clip = ClipData.newPlainText("Phone Number", currentContact.resolvedPhone)
            clipboard?.setPrimaryClip(clip)
            Toast.makeText(this, R.string.copied_to_clipboard, Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupBackNavigation() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                finish()
            }
        })
    }

    /**
     * XML OnClick Handler for Back Button
     */
    fun onBackClicked(view: View) {
        finish()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            finish()
            return true
        }
        return super.onOptionsItemSelected(item)
    }
}
