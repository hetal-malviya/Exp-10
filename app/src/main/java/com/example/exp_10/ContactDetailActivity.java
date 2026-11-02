package com.example.exp_10;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.button.MaterialButton;

/**
 * Detailed view activity displaying contact details with sharing, calling, and messaging actions.
 */
public class ContactDetailActivity extends AppCompatActivity {

    public static final String EXTRA_CONTACT = "extra_contact";

    private Contact contact;
    private TextView tvName, tvPhone, tvEmail, tvCompany, tvJobTitle, tvInitials, tvSharePreview;
    private FrameLayout flAvatar;
    private View llBackContainer, btnBack;
    private MaterialButton btnShareCard, btnShareVCard, btnCall, btnMessage, btnCopy;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_contact_detail);

        if (getIntent() != null && getIntent().hasExtra(EXTRA_CONTACT)) {
            contact = (Contact) getIntent().getSerializableExtra(EXTRA_CONTACT);
        }

        if (contact == null) {
            Toast.makeText(this, "Failed to load contact details", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        initViews();
        bindContactData();
        setupListeners();
        setupBackNavigation();
    }

    private void initViews() {
        tvName = findViewById(R.id.tvDetailName);
        tvPhone = findViewById(R.id.tvDetailPhone);
        tvEmail = findViewById(R.id.tvDetailEmail);
        tvCompany = findViewById(R.id.tvDetailCompany);
        tvJobTitle = findViewById(R.id.tvDetailJobTitle);
        tvInitials = findViewById(R.id.tvDetailInitials);
        tvSharePreview = findViewById(R.id.tvSharePreview);
        flAvatar = findViewById(R.id.flDetailAvatar);
        llBackContainer = findViewById(R.id.llBackContainer);
        btnBack = findViewById(R.id.btnBack);

        btnShareCard = findViewById(R.id.btnShareContact);
        btnShareVCard = findViewById(R.id.btnShareVCard);
        btnCall = findViewById(R.id.btnCall);
        btnMessage = findViewById(R.id.btnMessage);
        btnCopy = findViewById(R.id.btnCopyNumber);
    }

    private void bindContactData() {
        tvName.setText(contact.getName());
        tvPhone.setText(contact.getPhoneNumber());
        if (tvEmail != null) tvEmail.setText(contact.getEmail());
        if (tvCompany != null) tvCompany.setText(contact.getCompany());
        if (tvJobTitle != null) tvJobTitle.setText(contact.getJobTitle() + " • " + contact.getCompany());
        tvInitials.setText(contact.getInitials());

        if (tvSharePreview != null) {
            tvSharePreview.setText(contact.getFormattedShareText());
        }

        // Set dynamic avatar background color
        GradientDrawable bgShape = new GradientDrawable();
        bgShape.setShape(GradientDrawable.OVAL);
        bgShape.setColor(contact.getAvatarColor());
        flAvatar.setBackground(bgShape);
    }

    private void setupListeners() {
        if (llBackContainer != null) {
            llBackContainer.setOnClickListener(v -> finish());
        }
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        }

        // Share Formatted Contact Card Action
        btnShareCard.setOnClickListener(v -> {
            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("text/plain");
            shareIntent.putExtra(Intent.EXTRA_SUBJECT, "Contact Card: " + contact.getName());
            shareIntent.putExtra(Intent.EXTRA_TEXT, contact.getFormattedShareText());

            startActivity(Intent.createChooser(shareIntent, "Share Contact Card via"));
        });

        // Share vCard Format Action
        btnShareVCard.setOnClickListener(v -> {
            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("text/x-vcard");
            shareIntent.putExtra(Intent.EXTRA_SUBJECT, contact.getName() + ".vcf");
            shareIntent.putExtra(Intent.EXTRA_TEXT, contact.getVCardText());

            startActivity(Intent.createChooser(shareIntent, "Share vCard via"));
        });

        // Call Contact Action
        btnCall.setOnClickListener(v -> {
            try {
                Intent callIntent = new Intent(Intent.ACTION_DIAL);
                callIntent.setData(Uri.parse("tel:" + contact.getPhoneNumber()));
                startActivity(callIntent);
            } catch (Exception e) {
                Toast.makeText(this, "Unable to launch dialer", Toast.LENGTH_SHORT).show();
            }
        });

        // Message Contact Action
        btnMessage.setOnClickListener(v -> {
            try {
                Intent smsIntent = new Intent(Intent.ACTION_SENDTO);
                smsIntent.setData(Uri.parse("smsto:" + contact.getPhoneNumber()));
                startActivity(smsIntent);
            } catch (Exception e) {
                Toast.makeText(this, "Unable to launch messaging app", Toast.LENGTH_SHORT).show();
            }
        });

        // Copy Phone Number Action
        btnCopy.setOnClickListener(v -> {
            ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            ClipData clip = ClipData.newPlainText("Phone Number", contact.getPhoneNumber());
            if (clipboard != null) {
                clipboard.setPrimaryClip(clip);
                Toast.makeText(this, R.string.copied_to_clipboard, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setupBackNavigation() {
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                finish();
            }
        });
    }

    /**
     * XML OnClick Handler for Back Button
     */
    public void onBackClicked(View view) {
        finish();
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
