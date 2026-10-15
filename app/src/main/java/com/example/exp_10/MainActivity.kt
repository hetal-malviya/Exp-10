package com.example.exp_10

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.database.ContentObserver
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.ContactsContract
import android.view.View
import android.widget.EditText
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.widget.doOnTextChanged
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.google.android.material.button.MaterialButton
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

/**
 * Main Activity for ContactSync in Kotlin demonstrating Contacts Content Provider reading, live ContentObserver, and data sharing.
 */
class MainActivity : AppCompatActivity(), ContactAdapter.OnContactClickListener {

    private lateinit var etSearch: EditText
    private var btnClearSearch: ImageButton? = null
    private var btnInfo: ImageButton? = null
    private lateinit var tvContactCountBadge: TextView
    private lateinit var btnRefresh: MaterialButton
    private lateinit var btnGrantPermission: MaterialButton
    private lateinit var swipeRefreshLayout: SwipeRefreshLayout
    private lateinit var rvContacts: RecyclerView
    private lateinit var progressBar: ProgressBar
    private lateinit var viewEmptyState: LinearLayout
    private lateinit var viewPermissionState: LinearLayout

    private lateinit var adapter: ContactAdapter
    private var contactList = ArrayList<Contact>()
    private val executorService: ExecutorService = Executors.newSingleThreadExecutor()
    private val mainHandler = Handler(Looper.getMainLooper())

    // Live ContentObserver to auto-detect contacts added, modified or deleted in phonebook
    private val contactsObserver = object : ContentObserver(mainHandler) {
        override fun onChange(selfChange: Boolean) {
            super.onChange(selfChange)
            loadContactsFromContentProvider()
        }
    }

    private val requestPermissionLauncher: ActivityResultLauncher<String> =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) {
                showPermissionGrantedState()
                registerContactsObserver()
                loadContactsFromContentProvider()
            } else {
                showPermissionDeniedState()
                Toast.makeText(this, "Permission denied to read contacts", Toast.LENGTH_SHORT).show()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        initViews()
        setupRecyclerView()
        setupListeners()
        checkAndRequestPermission()
    }

    private fun initViews() {
        etSearch = findViewById(R.id.etSearch)
        btnClearSearch = findViewById(R.id.btnClearSearch)
        btnInfo = findViewById(R.id.btnInfo)
        tvContactCountBadge = findViewById(R.id.tvContactCountBadge)
        btnRefresh = findViewById(R.id.btnRefresh)
        btnGrantPermission = findViewById(R.id.btnGrantPermission)
        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout)
        rvContacts = findViewById(R.id.rvContacts)
        progressBar = findViewById(R.id.progressBar)
        viewEmptyState = findViewById(R.id.viewEmptyState)
        viewPermissionState = findViewById(R.id.viewPermissionState)
    }

    private fun setupRecyclerView() {
        rvContacts.layoutManager = LinearLayoutManager(this)
        adapter = ContactAdapter(this, contactList, this)
        rvContacts.adapter = adapter
    }

    private fun setupListeners() {
        // Search Filter Text Watcher using Kotlin KTX
        etSearch.doOnTextChanged { text, _, _, _ ->
            val query = text?.toString()?.trim() ?: ""
            btnClearSearch?.visibility = if (query.isEmpty()) View.GONE else View.VISIBLE
            adapter.filter.filter(query)
        }

        btnClearSearch?.setOnClickListener { etSearch.setText("") }

        // Refresh Actions
        swipeRefreshLayout.setOnRefreshListener { loadContactsFromContentProvider() }
        btnRefresh.setOnClickListener { loadContactsFromContentProvider() }

        // Info Button Action
        btnInfo?.setOnClickListener {
            val intent = Intent(this, ContentProviderInfoActivity::class.java)
            startActivity(intent)
        }

        // Grant Permission Button
        btnGrantPermission.setOnClickListener {
            requestPermissionLauncher.launch(Manifest.permission.READ_CONTACTS)
        }
    }

    private fun checkAndRequestPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS)
            == PackageManager.PERMISSION_GRANTED
        ) {
            showPermissionGrantedState()
            registerContactsObserver()
            loadContactsFromContentProvider()
        } else {
            showPermissionDeniedState()
            requestPermissionLauncher.launch(Manifest.permission.READ_CONTACTS)
        }
    }

    private fun registerContactsObserver() {
        try {
            contentResolver.registerContentObserver(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                true,
                contactsObserver
            )
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun unregisterContactsObserver() {
        try {
            contentResolver.unregisterContentObserver(contactsObserver)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun showPermissionGrantedState() {
        viewPermissionState.visibility = View.GONE
        swipeRefreshLayout.visibility = View.VISIBLE
    }

    private fun showPermissionDeniedState() {
        viewPermissionState.visibility = View.VISIBLE
        swipeRefreshLayout.visibility = View.GONE
        viewEmptyState.visibility = View.GONE
        progressBar.visibility = View.GONE
        tvContactCountBadge.text = "0 Contacts"
    }

    /**
     * Retrieves contacts using ContentResolver and ContactsContract.CommonDataKinds.Phone.CONTENT_URI off the main thread.
     */
    private fun loadContactsFromContentProvider() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS)
            != PackageManager.PERMISSION_GRANTED
        ) {
            swipeRefreshLayout.isRefreshing = false
            showPermissionDeniedState()
            return
        }

        progressBar.visibility = View.VISIBLE
        viewEmptyState.visibility = View.GONE

        executorService.execute {
            val fetchedContacts = ArrayList<Contact>()
            val phoneUri: Uri = ContactsContract.CommonDataKinds.Phone.CONTENT_URI

            val projection = arrayOf(
                ContactsContract.CommonDataKinds.Phone.CONTACT_ID,
                ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                ContactsContract.CommonDataKinds.Phone.NUMBER,
                ContactsContract.CommonDataKinds.Phone.PHOTO_URI
            )

            val sortOrder = "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} ASC"

            try {
                contentResolver.query(phoneUri, projection, null, null, sortOrder)?.use { cursor ->
                    val idIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.CONTACT_ID)
                    val nameIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
                    val numberIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                    val photoIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.PHOTO_URI)

                    while (cursor.moveToNext()) {
                        val id = if (idIndex != -1) cursor.getString(idIndex) ?: "" else ""
                        val name = if (nameIndex != -1) cursor.getString(nameIndex) ?: "" else ""
                        val number = if (numberIndex != -1) cursor.getString(numberIndex) ?: "" else ""
                        val photoUri = if (photoIndex != -1) cursor.getString(photoIndex) else null

                        if (number.isNotBlank()) {
                            fetchedContacts.add(Contact(id, name, number, photoUri = photoUri))
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }

            // Fallback sample contacts if emulator phonebook is completely empty
            if (fetchedContacts.isEmpty()) {
                fetchedContacts.add(Contact("1", "Alex Morgan", "+1 (555) 019-2834", "alex.morgan@techcorp.com", "TechCorp Global", "Senior Software Engineer", null))
                fetchedContacts.add(Contact("2", "Beatriz Ramos", "+1 (555) 014-9821", "b.ramos@designstudio.org", "Design Studio Co.", "Product Design Lead", null))
                fetchedContacts.add(Contact("3", "Carlos Mendes", "+1 (555) 017-3342", "carlos.mendes@innovate.io", "Innovate AI Labs", "Machine Learning Researcher", null))
                fetchedContacts.add(Contact("4", "David Kim", "+1 (555) 012-7711", "david.kim@fintech.net", "FinTech Solutions", "Operations Manager", null))
                fetchedContacts.add(Contact("5", "Elena Rostova", "+1 (555) 018-4490", "elena.r@cybersec.com", "CyberSec Analytics", "Security Architect", null))
            }

            mainHandler.post {
                progressBar.visibility = View.GONE
                swipeRefreshLayout.isRefreshing = false

                contactList = fetchedContacts
                adapter.updateContacts(contactList)

                if (contactList.isEmpty()) {
                    viewEmptyState.visibility = View.VISIBLE
                    tvContactCountBadge.text = "0 Contacts"
                } else {
                    viewEmptyState.visibility = View.GONE
                    tvContactCountBadge.text = "${contactList.size} Contacts"
                }
            }
        }
    }

    override fun onContactClick(contact: Contact) {
        val intent = Intent(this, ContactDetailActivity::class.java).apply {
            putExtra(ContactDetailActivity.EXTRA_CONTACT, contact)
        }
        startActivity(intent)
    }

    override fun onResume() {
        super.onResume()
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED) {
            registerContactsObserver()
            loadContactsFromContentProvider()
        }
    }

    override fun onPause() {
        super.onPause()
        unregisterContactsObserver()
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterContactsObserver()
        if (!executorService.isShutdown) {
            executorService.shutdown()
        }
    }
}
