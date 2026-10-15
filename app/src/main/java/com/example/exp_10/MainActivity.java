package com.example.exp_10;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.ContentObserver;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.ContactsContract;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import com.google.android.material.button.MaterialButton;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Main Activity for ContactSync demonstrating Contacts Content Provider reading, live observer sync, and data sharing.
 */
public class MainActivity extends AppCompatActivity implements ContactAdapter.OnContactClickListener {

    private EditText etSearch;
    private ImageButton btnClearSearch, btnInfo;
    private TextView tvContactCountBadge;
    private MaterialButton btnRefresh, btnGrantPermission;
    private SwipeRefreshLayout swipeRefreshLayout;
    private RecyclerView rvContacts;
    private ProgressBar progressBar;
    private LinearLayout viewEmptyState, viewPermissionState;

    private ContactAdapter adapter;
    private List<Contact> contactList = new ArrayList<>();
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    // Live ContentObserver to auto-detect contacts added, modified or deleted in phonebook
    private final ContentObserver contactsObserver = new ContentObserver(mainHandler) {
        @Override
        public void onChange(boolean selfChange) {
            super.onChange(selfChange);
            loadContactsFromContentProvider();
        }
    };

    private final ActivityResultLauncher<String> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) {
                    showPermissionGrantedState();
                    registerContactsObserver();
                    loadContactsFromContentProvider();
                } else {
                    showPermissionDeniedState();
                    Toast.makeText(this, "Permission denied to read contacts", Toast.LENGTH_SHORT).show();
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initViews();
        setupRecyclerView();
        setupListeners();
        checkAndRequestPermission();
    }

    private void initViews() {
        etSearch = findViewById(R.id.etSearch);
        btnClearSearch = findViewById(R.id.btnClearSearch);
        btnInfo = findViewById(R.id.btnInfo);
        tvContactCountBadge = findViewById(R.id.tvContactCountBadge);
        btnRefresh = findViewById(R.id.btnRefresh);
        btnGrantPermission = findViewById(R.id.btnGrantPermission);
        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout);
        rvContacts = findViewById(R.id.rvContacts);
        progressBar = findViewById(R.id.progressBar);
        viewEmptyState = findViewById(R.id.viewEmptyState);
        viewPermissionState = findViewById(R.id.viewPermissionState);
    }

    private void setupRecyclerView() {
        rvContacts.setLayoutManager(new LinearLayoutManager(this));
        adapter = new ContactAdapter(this, contactList, this);
        rvContacts.setAdapter(adapter);
    }

    private void setupListeners() {
        // Search Filter Text Watcher
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                String query = s.toString().trim();
                if (btnClearSearch != null) {
                    btnClearSearch.setVisibility(query.isEmpty() ? View.GONE : View.VISIBLE);
                }
                if (adapter != null) {
                    adapter.getFilter().filter(query);
                }
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        if (btnClearSearch != null) {
            btnClearSearch.setOnClickListener(v -> etSearch.setText(""));
        }

        // Refresh Actions
        swipeRefreshLayout.setOnRefreshListener(this::loadContactsFromContentProvider);
        btnRefresh.setOnClickListener(v -> loadContactsFromContentProvider());

        // Info Button Action
        if (btnInfo != null) {
            btnInfo.setOnClickListener(v -> {
                Intent intent = new Intent(this, ContentProviderInfoActivity.class);
                startActivity(intent);
            });
        }

        // Grant Permission Button
        btnGrantPermission.setOnClickListener(v -> requestPermissionLauncher.launch(Manifest.permission.READ_CONTACTS));
    }

    private void checkAndRequestPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS)
                == PackageManager.PERMISSION_GRANTED) {
            showPermissionGrantedState();
            registerContactsObserver();
            loadContactsFromContentProvider();
        } else {
            showPermissionDeniedState();
            requestPermissionLauncher.launch(Manifest.permission.READ_CONTACTS);
        }
    }

    private void registerContactsObserver() {
        try {
            getContentResolver().registerContentObserver(
                    ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                    true,
                    contactsObserver
            );
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void unregisterContactsObserver() {
        try {
            getContentResolver().unregisterContentObserver(contactsObserver);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void showPermissionGrantedState() {
        viewPermissionState.setVisibility(View.GONE);
        swipeRefreshLayout.setVisibility(View.VISIBLE);
    }

    private void showPermissionDeniedState() {
        viewPermissionState.setVisibility(View.VISIBLE);
        swipeRefreshLayout.setVisibility(View.GONE);
        viewEmptyState.setVisibility(View.GONE);
        progressBar.setVisibility(View.GONE);
        tvContactCountBadge.setText("0 Contacts");
    }

    /**
     * Retrieves contacts using ContentResolver and ContactsContract.CommonDataKinds.Phone.CONTENT_URI off the main thread.
     */
    private void loadContactsFromContentProvider() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS)
                != PackageManager.PERMISSION_GRANTED) {
            swipeRefreshLayout.setRefreshing(false);
            showPermissionDeniedState();
            return;
        }

        progressBar.setVisibility(View.VISIBLE);
        viewEmptyState.setVisibility(View.GONE);

        executorService.execute(() -> {
            List<Contact> fetchedContacts = new ArrayList<>();
            Uri phoneUri = ContactsContract.CommonDataKinds.Phone.CONTENT_URI;

            String[] projection = new String[]{
                    ContactsContract.CommonDataKinds.Phone.CONTACT_ID,
                    ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                    ContactsContract.CommonDataKinds.Phone.NUMBER,
                    ContactsContract.CommonDataKinds.Phone.PHOTO_URI
            };

            String sortOrder = ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME + " ASC";

            try (Cursor cursor = getContentResolver().query(phoneUri, projection, null, null, sortOrder)) {
                if (cursor != null) {
                    int idIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.CONTACT_ID);
                    int nameIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME);
                    int numberIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER);
                    int photoIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.PHOTO_URI);

                    while (cursor.moveToNext()) {
                        String id = (idIndex != -1) ? cursor.getString(idIndex) : "";
                        String name = (nameIndex != -1) ? cursor.getString(nameIndex) : "";
                        String number = (numberIndex != -1) ? cursor.getString(numberIndex) : "";
                        String photoUri = (photoIndex != -1) ? cursor.getString(photoIndex) : null;

                        if (number != null && !number.trim().isEmpty()) {
                            fetchedContacts.add(new Contact(id, name, number, photoUri));
                        }
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

            // Fallback sample contacts if emulator phonebook is completely empty
            if (fetchedContacts.isEmpty()) {
                fetchedContacts.add(new Contact("1", "Alex Morgan", "+1 (555) 019-2834", "alex.morgan@techcorp.com", "TechCorp Global", "Senior Software Engineer", null));
                fetchedContacts.add(new Contact("2", "Beatriz Ramos", "+1 (555) 014-9821", "b.ramos@designstudio.org", "Design Studio Co.", "Product Design Lead", null));
                fetchedContacts.add(new Contact("3", "Carlos Mendes", "+1 (555) 017-3342", "carlos.mendes@innovate.io", "Innovate AI Labs", "Machine Learning Researcher", null));
                fetchedContacts.add(new Contact("4", "David Kim", "+1 (555) 012-7711", "david.kim@fintech.net", "FinTech Solutions", "Operations Manager", null));
                fetchedContacts.add(new Contact("5", "Elena Rostova", "+1 (555) 018-4490", "elena.r@cybersec.com", "CyberSec Analytics", "Security Architect", null));
            }

            mainHandler.post(() -> {
                progressBar.setVisibility(View.GONE);
                swipeRefreshLayout.setRefreshing(false);

                contactList = fetchedContacts;
                adapter.updateContacts(contactList);

                if (contactList.isEmpty()) {
                    viewEmptyState.setVisibility(View.VISIBLE);
                    tvContactCountBadge.setText("0 Contacts");
                } else {
                    viewEmptyState.setVisibility(View.GONE);
                    tvContactCountBadge.setText(contactList.size() + " Contacts");
                }
            });
        });
    }

    @Override
    public void onContactClick(Contact contact) {
        Intent intent = new Intent(this, ContactDetailActivity.class);
        intent.putExtra(ContactDetailActivity.EXTRA_CONTACT, contact);
        startActivity(intent);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED) {
            registerContactsObserver();
            loadContactsFromContentProvider();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterContactsObserver();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterContactsObserver();
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdown();
        }
    }
}
