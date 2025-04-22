package com.example.akauntant

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
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
        // Load monthly budget
        monthlyBudget = sharedPreferences.getFloat(MONTHLY_BUDGET_KEY, 0f).toDouble()
        etMonthlyBudget.setText(if (monthlyBudget > 0) monthlyBudget.toString() else "")
        
        // Load currency
        selectedCurrency = sharedPreferences.getString(CURRENCY_KEY, "$") ?: "$"
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
            // Get current budget from the input field if it's a valid number, otherwise from SharedPreferences
            val enteredBudget = etMonthlyBudget.text.toString().toDoubleOrNull()
            val monthlyBudget = enteredBudget ?: sharedPreferences.getFloat(MONTHLY_BUDGET_KEY, 0f).toDouble()
            
            if (monthlyBudget <= 0) {
                // No budget set yet, clear the summary text
                findViewById<android.widget.TextView>(R.id.tvBudgetSummary).text = "No budget set yet"
                findViewById<android.widget.ProgressBar>(R.id.budgetProgressBar).progress = 0
                return
            }
            
            // Calculate current month's expenses
            val currentMonth = SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(Date())
            val transactionsString = sharedPreferences.getString(MainActivity.TRANSACTIONS_PREF, "[]")
            val transactionsArray = JSONArray(transactionsString)
            
            var totalExpenses = 0.0
            
            for (i in 0 until transactionsArray.length()) {
                val transactionObject = transactionsArray.getJSONObject(i)
                if (!transactionObject.optBoolean("isIncome", false)) {
                    val date = transactionObject.optString("date", "")
                    if (date.startsWith(currentMonth)) {
                        totalExpenses += transactionObject.optDouble("amount", 0.0)
                    }
                }
            }
            
            // Update progress bar and summary text
            val progress = if (monthlyBudget > 0) (totalExpenses / monthlyBudget * 100).toInt().coerceAtMost(100) else 0
            findViewById<android.widget.ProgressBar>(R.id.budgetProgressBar).progress = progress
            
            // Use the current selected currency symbol
            val currencySymbol = selectedCurrency
            
            val summaryText = "You've spent $currencySymbol${String.format("%.2f", totalExpenses)} " +
                    "out of $currencySymbol${String.format("%.2f", monthlyBudget)} " +
                    "($progress% of your budget)"
            findViewById<android.widget.TextView>(R.id.tvBudgetSummary).text = summaryText
            
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
        
        // Save monthly budget
        sharedPreferences.edit()
            .putFloat(MONTHLY_BUDGET_KEY, monthlyBudget.toFloat())
            .putString(CURRENCY_KEY, selectedCurrency)
            .apply()
        
        Toast.makeText(this, "Settings saved successfully", Toast.LENGTH_SHORT).show()
        
        // Update budget status UI
        updateBudgetStatus()
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
            val transactionsString = sharedPreferences.getString(MainActivity.TRANSACTIONS_PREF, "[]")
            if (transactionsString.isNullOrEmpty() || transactionsString == "[]") {
                Toast.makeText(this, "No transactions to backup", Toast.LENGTH_SHORT).show()
                return
            }
            
            val backupFile = createBackupFile()
            FileOutputStream(backupFile).use { output ->
                output.write(transactionsString.toByteArray())
            }
            
            Toast.makeText(
                this, 
                "Backup saved to: ${backupFile.name}", 
                Toast.LENGTH_LONG
            ).show()
        } catch (e: IOException) {
            e.printStackTrace()
            Toast.makeText(this, "Backup failed: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun restoreTransactions() {
        val backupFiles = listBackupFiles()
        if (backupFiles.isEmpty()) {
            Toast.makeText(this, "No backup files found", Toast.LENGTH_SHORT).show()
            return
        }

        // Show dialog to select backup file if multiple exist
        val fileNames = backupFiles.map { it.name }.toTypedArray()
        AlertDialog.Builder(this)
            .setTitle("Select Backup to Restore")
            .setItems(fileNames) { _, which ->
                val selectedFile = backupFiles[which]
                restoreFromFile(selectedFile)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun restoreFromFile(backupFile: File) {
        try {
            val transactionsString = FileInputStream(backupFile).use { input ->
                val bytes = input.readBytes()
                String(bytes, Charsets.UTF_8)
            }
            
            // Validate JSON format
            try {
                JSONArray(transactionsString)
            } catch (e: JSONException) {
                Toast.makeText(this, "Invalid backup file format", Toast.LENGTH_SHORT).show()
                return
            }
            
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
            
        } catch (e: IOException) {
            e.printStackTrace()
            Toast.makeText(this, "Restore failed: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun createBackupFile(): File {
        val backupDir = File(filesDir, "backups")
        if (!backupDir.exists()) {
            backupDir.mkdirs()
        }
        
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        return File(backupDir, "akauntant_backup_$timestamp.json")
    }

    private fun listBackupFiles(): List<File> {
        val backupDir = File(filesDir, "backups")
        if (!backupDir.exists()) {
            return emptyList()
        }
        
        return backupDir.listFiles { file ->
            file.isFile && file.name.endsWith(".json")
        }?.sortedByDescending { it.lastModified() } ?: emptyList()
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
    }
}