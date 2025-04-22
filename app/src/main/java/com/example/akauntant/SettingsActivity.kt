package com.example.akauntant

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.text.Editable
import android.text.TextWatcher
import android.view.MenuItem
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.bottomnavigation.BottomNavigationView
import org.json.JSONArray
import org.json.JSONException
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class SettingsActivity : AppCompatActivity() {

    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var etMonthlyBudget: TextInputEditText
    private lateinit var spinnerCurrency: AutoCompleteTextView
    private lateinit var btnSaveSettings: MaterialButton
    private lateinit var btnBackupData: MaterialButton
    private lateinit var btnRestoreData: MaterialButton
    private lateinit var switchNotifications: SwitchCompat
    private lateinit var switchBudgetAlert: SwitchCompat
    private lateinit var switchDailyReminders: SwitchCompat
    
    private var monthlyBudget: Double = 0.0
    private var selectedCurrency: String = "$"
    
    // Permission handling
    private var pendingAction: (() -> Unit)? = null
    
    // Permission launchers for storage and notifications
    private val requestStoragePermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.entries.all { it.value }
        if (allGranted) {
            // Execute the pending action if permissions are granted
            pendingAction?.invoke()
        } else {
            Toast.makeText(
                this,
                "Storage permission is required for backup/restore functionality",
                Toast.LENGTH_LONG
            ).show()
        }
    }
    
    // Permission launcher for notifications
    private val requestNotificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            // Notifications permission granted
            switchNotifications.isChecked = true
            sharedPreferences.edit()
                .putBoolean(NOTIFICATIONS_KEY, true)
                .apply()
            
            // Enable the other notification toggles
            switchBudgetAlert.isEnabled = true
            switchDailyReminders.isEnabled = true
            
        } else {
            // Notifications permission denied
            switchNotifications.isChecked = false
            sharedPreferences.edit()
                .putBoolean(NOTIFICATIONS_KEY, false)
                .apply()
            
            // Disable other notification toggles
            switchBudgetAlert.isEnabled = false
            switchDailyReminders.isEnabled = false
            
            Toast.makeText(
                this,
                "Notification permission is required for alerts",
                Toast.LENGTH_LONG
            ).show()
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)
        
        // Initialize SharedPreferences
        sharedPreferences = getSharedPreferences(MainActivity.PREFS_NAME, Context.MODE_PRIVATE)
        
        // Setup toolbar
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)
        
        // Initialize views
        etMonthlyBudget = findViewById(R.id.etMonthlyBudget)
        spinnerCurrency = findViewById(R.id.spinnerCurrency)
        btnSaveSettings = findViewById(R.id.btnSaveSettings)
        btnBackupData = findViewById(R.id.btnBackupData)
        btnRestoreData = findViewById(R.id.btnRestoreData)
        switchNotifications = findViewById(R.id.switchNotifications)
        switchBudgetAlert = findViewById(R.id.switchBudgetAlert)
        switchDailyReminders = findViewById(R.id.switchDailyReminders)
        
        // Create notification channels
        NotificationService.createNotificationChannels(this)
        
        // Setup currency dropdown
        setupCurrencyDropdown()
        
        // Load current settings
        loadSettings()
        
        // Setup button click listeners
        setupButtonListeners()
        
        // Setup text change listeners
        setupTextChangeListeners()
        
        // Update UI with current budget status
        updateBudgetStatus()
        
        // Setup bottom navigation
        setupBottomNavigation()
    }
    
    private fun setupCurrencyDropdown() {
        val currencies = arrayOf("$", "€", "£", "¥", "₹", "LKR")
        val adapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, currencies)
        spinnerCurrency.setAdapter(adapter)
    }
    
    private fun loadSettings() {
        // Create a TransactionManager to work with budget consistently
        val transactionManager = TransactionManager(this)
        
        // Load monthly budget using TransactionManager for consistency
        monthlyBudget = transactionManager.getMonthlyBudget()
        etMonthlyBudget.setText(if (monthlyBudget > 0) monthlyBudget.toString() else "")
        
        // Load currency
        selectedCurrency = transactionManager.getCurrency()
        spinnerCurrency.setText(selectedCurrency, false)
        
        // Set the currency prefix on the budget input field
        findViewById<com.google.android.material.textfield.TextInputLayout>(R.id.tilMonthlyBudget).prefixText = selectedCurrency

        // Load notification setting
        val notificationsEnabled = sharedPreferences.getBoolean(NOTIFICATIONS_KEY, false)
        switchNotifications.isChecked = notificationsEnabled

        // Load budget alert setting
        val budgetAlertEnabled = sharedPreferences.getBoolean(BUDGET_ALERT_KEY, false)
        switchBudgetAlert.isChecked = budgetAlertEnabled
        switchBudgetAlert.isEnabled = notificationsEnabled

        // Load daily reminders setting
        val dailyRemindersEnabled = sharedPreferences.getBoolean(DAILY_REMINDERS_KEY, false)
        switchDailyReminders.isChecked = dailyRemindersEnabled
        switchDailyReminders.isEnabled = notificationsEnabled
    }
    
    private fun setupButtonListeners() {
        btnSaveSettings.setOnClickListener {
            saveSettings()
        }
        
        btnBackupData.setOnClickListener {
            checkPermissionsAndRun {
                backupTransactions()
            }
        }
        
        btnRestoreData.setOnClickListener {
            checkPermissionsAndRun {
                restoreTransactions()
            }
        }
    }
    
    private fun setupTextChangeListeners() {
        etMonthlyBudget.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            
            override fun afterTextChanged(s: Editable?) {
                val budgetText = s.toString()
                monthlyBudget = if (budgetText.isNotEmpty()) budgetText.toDoubleOrNull() ?: 0.0 else 0.0
                
                // Update budget status whenever the budget value changes
                updateBudgetStatus()
            }
        })
        
        spinnerCurrency.setOnItemClickListener { _, _, position, _ ->
            val currencies = arrayOf("$", "€", "£", "¥", "₹", "LKR")
            selectedCurrency = currencies[position]
            
            // Update budget input field prefix with the selected currency
            findViewById<com.google.android.material.textfield.TextInputLayout>(R.id.tilMonthlyBudget).prefixText = selectedCurrency
            
            // Update budget status with the new currency
            updateBudgetStatus()
        }

        switchNotifications.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                // Request notification permission
                requestNotificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            } else {
                sharedPreferences.edit()
                    .putBoolean(NOTIFICATIONS_KEY, false)
                    .apply()
                
                // Disable other notification toggles
                switchBudgetAlert.isEnabled = false
                switchDailyReminders.isEnabled = false
            }
        }

        switchBudgetAlert.setOnCheckedChangeListener { _, isChecked ->
            sharedPreferences.edit()
                .putBoolean(BUDGET_ALERT_KEY, isChecked)
                .apply()
        }

        switchDailyReminders.setOnCheckedChangeListener { _, isChecked ->
            sharedPreferences.edit()
                .putBoolean(DAILY_REMINDERS_KEY, isChecked)
                .apply()
        }
    }
    
    private fun updateBudgetStatus() {
        try {
            // Create a TransactionManager instance to access transaction data properly
            val transactionManager = TransactionManager(this)
            
            // Get current budget from the input field if it's a valid number, otherwise from TransactionManager
            val enteredBudget = etMonthlyBudget.text.toString().toDoubleOrNull()
            val monthlyBudget = enteredBudget ?: transactionManager.getMonthlyBudget()
            
            if (monthlyBudget <= 0) {
                // No budget set yet, clear the summary text
                findViewById<android.widget.TextView>(R.id.tvBudgetSummary).text = "No budget set yet"
                findViewById<android.widget.ProgressBar>(R.id.budgetProgressBar).progress = 0
                return
            }
            
            // Get total expenses for the current month directly from TransactionManager
            val totalExpenses = transactionManager.getCurrentMonthExpenses()
            
            // Update progress bar and summary text
            val progress = if (monthlyBudget > 0) (totalExpenses / monthlyBudget * 100).toInt().coerceAtMost(100) else 0
            findViewById<android.widget.ProgressBar>(R.id.budgetProgressBar).progress = progress
            
            // Use the current selected currency symbol
            val currencySymbol = selectedCurrency
            
            val summaryText = "You've spent $currencySymbol${String.format("%.2f", totalExpenses)} " +
                    "out of $currencySymbol${String.format("%.2f", monthlyBudget)} " +
                    "($progress% of your budget)"
            findViewById<android.widget.TextView>(R.id.tvBudgetSummary).text = summaryText
            
            // Color the progress bar based on percentage used
            val progressBar = findViewById<android.widget.ProgressBar>(R.id.budgetProgressBar)
            if (progress >= 90) {
                progressBar.progressTintList = android.content.res.ColorStateList.valueOf(android.graphics.Color.RED)
            } else if (progress >= 75) {
                progressBar.progressTintList = android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#FF9800")) // Orange
            } else {
                progressBar.progressTintList = android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#4CAF50")) // Green
            }
            
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    private fun saveSettings() {
        // Validate monthly budget
        if (etMonthlyBudget.text.toString().isEmpty()) {
            Toast.makeText(this, "Please enter a monthly budget", Toast.LENGTH_SHORT).show()
            return
        }
        
        try {
            // Parse the budget value
            val budgetValue = etMonthlyBudget.text.toString().toDoubleOrNull() ?: 0.0
            
            // Use TransactionManager to save budget consistently
            val transactionManager = TransactionManager(this)
            transactionManager.setMonthlyBudget(budgetValue)
            
            // Save currency setting
            transactionManager.setCurrency(selectedCurrency)
            
            // Check if notification settings have changed
            val notificationsEnabled = switchNotifications.isChecked
            val dailyRemindersEnabled = switchDailyReminders.isChecked
            
            // Save notification settings
            sharedPreferences.edit()
                .putBoolean(NOTIFICATIONS_KEY, notificationsEnabled)
                .putBoolean(BUDGET_ALERT_KEY, switchBudgetAlert.isChecked)
                .putBoolean(DAILY_REMINDERS_KEY, dailyRemindersEnabled)
                .apply()
            
            // Schedule or cancel daily reminders based on setting
            if (notificationsEnabled && dailyRemindersEnabled) {
                NotificationService.scheduleDailyReminder(this, true)
            } else {
                NotificationService.scheduleDailyReminder(this, false)
            }
            
            // Check budget status and send notification if necessary
            if (notificationsEnabled && switchBudgetAlert.isChecked) {
                transactionManager.checkBudgetStatusAndNotify()
            }
            
            Toast.makeText(this, "Settings saved successfully", Toast.LENGTH_SHORT).show()
            
            // Update budget status UI
            updateBudgetStatus()
        } catch (e: Exception) {
            Toast.makeText(this, "Error saving settings: ${e.message}", Toast.LENGTH_SHORT).show()
            e.printStackTrace()
        }
    }
    
    private fun checkPermissionsAndRun(action: () -> Unit) {
        // Store the action to run after permissions are granted
        pendingAction = action
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // For API 33+, we need READ_MEDIA_IMAGES permission
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_IMAGES) 
                != PackageManager.PERMISSION_GRANTED) {
                requestStoragePermissionLauncher.launch(arrayOf(Manifest.permission.READ_MEDIA_IMAGES))
            } else {
                action()
            }
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // For API 30-32, we need READ_EXTERNAL_STORAGE permission
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) 
                != PackageManager.PERMISSION_GRANTED) {
                requestStoragePermissionLauncher.launch(arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE))
            } else {
                action()
            }
        } else {
            // For API 29 and below, we need both READ and WRITE permissions
            val readPermission = ContextCompat.checkSelfPermission(
                this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
            val writePermission = ContextCompat.checkSelfPermission(
                this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
                
            if (!readPermission || !writePermission) {
                requestStoragePermissionLauncher.launch(arrayOf(
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                ))
            } else {
                action()
            }
        }
    }
    
    private fun backupTransactions() {
        try {
            val transactionManager = TransactionManager(this)
            val transactions = transactionManager.getAllTransactions()
            
            if (transactions.isEmpty()) {
                Toast.makeText(this, "No transactions to backup", Toast.LENGTH_SHORT).show()
                return
            }
            
            val backupFile = createBackupFile()
            if (backupFile != null) {
                // Create a proper JSON backup with all transactions
                val jsonArray = JSONArray()
                transactions.forEach { transaction ->
                    jsonArray.put(transaction.toJson())
                }
                
                FileOutputStream(backupFile).use { output ->
                    output.write(jsonArray.toString().toByteArray())
                }
                
                Toast.makeText(
                    this, 
                    "Backup saved to: ${backupFile.path}", 
                    Toast.LENGTH_LONG
                ).show()
            } else {
                Toast.makeText(this, "Could not create backup file", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Backup failed: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun restoreTransactions() {
        try {
            // Create intent to pick a file
            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                addCategory(Intent.CATEGORY_OPENABLE)
                type = "application/json"
            }
            
            // Launch the file picker
            startActivityForResult(intent, REQUEST_CODE_PICK_BACKUP_FILE)
        } catch (e: Exception) {
            Toast.makeText(this, "Error opening file picker: ${e.message}", Toast.LENGTH_SHORT).show()
            e.printStackTrace()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        
        if (requestCode == REQUEST_CODE_PICK_BACKUP_FILE && resultCode == RESULT_OK) {
            data?.data?.let { uri ->
                try {
                    // Read the content from the selected file
                    val transactionsString = contentResolver.openInputStream(uri)?.use { input ->
                        val bytes = input.readBytes()
                        String(bytes, Charsets.UTF_8)
                    }
                    
                    // Validate JSON format
                    if (transactionsString != null) {
                        try {
                            JSONArray(transactionsString)
                            
                            // Confirm restoration
                            AlertDialog.Builder(this)
                                .setTitle("Restore Data")
                                .setMessage("This will replace all your current transaction data. Continue?")
                                .setPositiveButton("Restore") { _, _ ->
                                    // Apply the restore
                                    sharedPreferences.edit()
                                        .putString(MainActivity.TRANSACTIONS_PREF, transactionsString)
                                        .apply()
                                    
                                    Toast.makeText(this, "Data restored successfully!", Toast.LENGTH_SHORT).show()
                                    
                                    // Update budget status
                                    updateBudgetStatus()
                                }
                                .setNegativeButton("Cancel", null)
                                .show()
                        } catch (e: JSONException) {
                            Toast.makeText(this, "Invalid backup file format", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        Toast.makeText(this, "Could not read the selected file", Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    Toast.makeText(this, "Error reading file: ${e.message}", Toast.LENGTH_SHORT).show()
                    e.printStackTrace()
                }
            }
        }
    }

    private fun createBackupFile(): File? {
        try {
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val fileName = "akauntant_backup_$timestamp.json"
            
            // For Android 10+ (API 29+)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val contentValues = android.content.ContentValues().apply {
                    put(android.provider.MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                    put(android.provider.MediaStore.MediaColumns.MIME_TYPE, "application/json")
                    put(android.provider.MediaStore.MediaColumns.RELATIVE_PATH, "Download/Akauntant")
                }
                
                val uri = contentResolver.insert(
                    android.provider.MediaStore.Downloads.EXTERNAL_CONTENT_URI,
                    contentValues
                )
                
                if (uri != null) {
                    contentResolver.openOutputStream(uri)?.close()
                    return File(getPathFromUri(uri) ?: "")
                }
            } else {
                // For older Android versions
                val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                val akauntantDir = File(downloadsDir, "Akauntant")
                if (!akauntantDir.exists()) {
                    akauntantDir.mkdirs()
                }
                
                val file = File(akauntantDir, fileName)
                return file
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Error creating backup file: ${e.message}", Toast.LENGTH_SHORT).show()
        }
        return null
    }
    
    private fun getPathFromUri(uri: android.net.Uri): String? {
        val projection = arrayOf(android.provider.MediaStore.MediaColumns.DATA)
        contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val columnIndex = cursor.getColumnIndexOrThrow(android.provider.MediaStore.MediaColumns.DATA)
                return cursor.getString(columnIndex)
            }
        }
        return uri.path
    }

    // For older API compatibility - not needed in the previous version
    private fun listBackupFiles(): List<File> {
        try {
            // For Android 10+ (API 29+), this is only used as a fallback
            val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            val akauntantDir = File(downloadsDir, "Akauntant")
            if (!akauntantDir.exists()) {
                return emptyList()
            }
            
            return akauntantDir.listFiles { file ->
                file.isFile && file.name.endsWith(".json")
            }?.sortedByDescending { it.lastModified() } ?: emptyList()
        } catch (e: Exception) {
            e.printStackTrace()
            return emptyList()
        }
    }
    
    private fun setupBottomNavigation() {
        val bottomNavigation = findViewById<BottomNavigationView>(R.id.bottomNavigation)
        bottomNavigation.selectedItemId = R.id.navigation_settings
        
        bottomNavigation.setOnItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.navigation_home -> {
                    startActivity(Intent(this, MainActivity::class.java))
                    overridePendingTransition(R.anim.fade_in, R.anim.fade_out)
                    finish()
                    true
                }
                R.id.navigation_history -> {
                    startActivity(Intent(this, HistoryActivity::class.java))
                    overridePendingTransition(R.anim.fade_in, R.anim.fade_out)
                    finish()
                    true
                }
                R.id.navigation_add -> {
                    startActivity(Intent(this, AddTransactionActivity::class.java))
                    overridePendingTransition(R.anim.fade_in, R.anim.fade_out)
                    finish()
                    true
                }
                R.id.navigation_report -> {
                    startActivity(Intent(this, ReportActivity::class.java))
                    overridePendingTransition(R.anim.fade_in, R.anim.fade_out)
                    finish()
                    true
                }
                R.id.navigation_settings -> {
                    // Already on this screen
                    true
                }
                else -> false
            }
        }
    }
    
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            onBackPressed()
            return true
        }
        return super.onOptionsItemSelected(item)
    }
    
    override fun finish() {
        super.finish()
        overridePendingTransition(R.anim.fade_in, R.anim.fade_out)
    }
    
    @Suppress("DEPRECATION")
    override fun onBackPressed() {
        // Navigate back to home activity
        startActivity(Intent(this, MainActivity::class.java))
        super.onBackPressed()
    }
    
    companion object {
        const val MONTHLY_BUDGET_KEY = "monthly_budget"
        const val CURRENCY_KEY = "currency"
        const val NOTIFICATIONS_KEY = "notifications"
        const val BUDGET_ALERT_KEY = "budget_alert"
        const val DAILY_REMINDERS_KEY = "daily_reminders"
        const val REQUEST_CODE_PICK_BACKUP_FILE = 1001
    }
}