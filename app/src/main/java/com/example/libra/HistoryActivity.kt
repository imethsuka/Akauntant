package com.example.libra

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
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

class HistoryActivity : AppCompatActivity(), TransactionAdapter.OnTransactionClickListener {

    private lateinit var recyclerView: RecyclerView
    private lateinit var tvNoTransactions: TextView
    private lateinit var adapter: TransactionAdapter
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var bottomNavigation: BottomNavigationView
    private lateinit var transactionManager: TransactionManager
    
    // Permission handling
    private var pendingAction: (() -> Unit)? = null
    
    private val requestPermissionLauncher = registerForActivityResult(
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_history)
        
        // Initialize SharedPreferences
        sharedPreferences = getSharedPreferences(MainActivity.PREFS_NAME, Context.MODE_PRIVATE)
        
        // Initialize TransactionManager
        transactionManager = TransactionManager(this)

        // Setup toolbar
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)
        
        // Initialize views
        recyclerView = findViewById(R.id.rvTransactions)
        tvNoTransactions = findViewById(R.id.tvNoTransactions)
        
        // Setup RecyclerView
        setupRecyclerView()
        
        // Setup bottom navigation
        setupBottomNavigation()
        
        // Load transactions
        loadTransactions()
    }
    
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_history, menu)
        return true
    }
    
    private fun setupRecyclerView() {
        recyclerView.layoutManager = LinearLayoutManager(this)
        adapter = TransactionAdapter(this)
        recyclerView.adapter = adapter
    }
    
    @Suppress("DEPRECATION")
    private fun setupBottomNavigation() {
        bottomNavigation = findViewById(R.id.bottomNavigation)
        bottomNavigation.selectedItemId = R.id.navigation_history
        
        bottomNavigation.setOnItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.navigation_home -> {
                    startActivity(Intent(this, MainActivity::class.java))
                    overridePendingTransition(R.anim.fade_in, R.anim.fade_out)
                    finish()
                    true
                }
                R.id.navigation_history -> {
                    // Already on this screen
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
                    startActivity(Intent(this, SettingsActivity::class.java))
                    overridePendingTransition(R.anim.fade_in, R.anim.fade_out)
                    finish()
                    true
                }
                else -> false
            }
        }
    }
    
    private fun loadTransactions() {
        // Use TransactionManager to get transactions instead of directly accessing SharedPreferences
        val transactions = transactionManager.getAllTransactions()
        
        // Update adapter
        adapter.submitList(transactions)
        
        // Show/hide empty state
        if (transactions.isEmpty()) {
            tvNoTransactions.visibility = View.VISIBLE
            recyclerView.visibility = View.GONE
        } else {
            tvNoTransactions.visibility = View.GONE
            recyclerView.visibility = View.VISIBLE
        }
    }
    
    override fun onTransactionClick(transaction: Transaction) {
        // Show transaction details dialog
        showTransactionDetailsDialog(transaction)
    }
    
    override fun onTransactionLongClick(transaction: Transaction): Boolean {
        // Show options dialog (Edit/Delete)
        showTransactionOptionsDialog(transaction)
        return true
    }
    
    private fun showTransactionDetailsDialog(transaction: Transaction) {
        val dialogBuilder = AlertDialog.Builder(this)
        dialogBuilder.setTitle(transaction.title)
        
        // Get currency symbol from TransactionManager
        val currencySymbol = getCurrencySymbol(transactionManager.getCurrency())
        
        val message = StringBuilder()
            .append("Amount: ${if (transaction.isIncome) "+" else "-"}$currencySymbol${String.format("%.2f", transaction.amount)}\n")
            .append("Category: ${transaction.category}\n")
            .append("Date: ${transaction.date}\n")
            .append("Notes: ${transaction.notes}")
            
        dialogBuilder.setMessage(message)
        dialogBuilder.setPositiveButton("Close") { dialog, _ ->
            dialog.dismiss()
        }
        
        dialogBuilder.create().show()
    }
    
    private fun showTransactionOptionsDialog(transaction: Transaction) {
        val options = arrayOf("Edit", "Delete")
        
        AlertDialog.Builder(this)
            .setTitle("Transaction Options")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> {
                        // Edit transaction
                        val intent = Intent(this, AddTransactionActivity::class.java)
                        intent.putExtra("TRANSACTION_ID", transaction.id)
                        startActivity(intent)
                        overridePendingTransition(R.anim.fade_in, R.anim.fade_out)
                    }
                    1 -> {
                        // Delete transaction
                        deleteTransaction(transaction)
                    }
                }
            }
            .show()
    }
    
    private fun deleteTransaction(transaction: Transaction) {
        // Confirm deletion
        AlertDialog.Builder(this)
            .setTitle("Delete Transaction")
            .setMessage("Are you sure you want to delete this transaction?")
            .setPositiveButton("Delete") { _, _ ->
                // Use TransactionManager to delete the transaction
                val success = transactionManager.deleteTransaction(transaction)
                
                if (success) {
                    // Reload transactions
                    loadTransactions()
                    Toast.makeText(this, "Transaction deleted successfully", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "Failed to delete transaction", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
    
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                onBackPressed()
                return true
            }
            R.id.action_backup -> {
                checkPermissionsAndRun {
                    backupTransactions()
                }
                return true
            }
            R.id.action_restore -> {
                checkPermissionsAndRun {
                    restoreTransactions()
                }
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }
    
    override fun finish() {
        super.finish()
        overridePendingTransition(R.anim.fade_in, R.anim.fade_out)
    }
    
    @Suppress("DEPRECATION")
    override fun onBackPressed() {
        super.onBackPressed()
        overridePendingTransition(R.anim.fade_in, R.anim.fade_out)
    }

    private fun checkPermissionsAndRun(action: () -> Unit) {
        // Store the action to run after permissions are granted
        pendingAction = action
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // For API 33+, we need READ_MEDIA_IMAGES permission
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_IMAGES) 
                != PackageManager.PERMISSION_GRANTED) {
                requestPermissionLauncher.launch(arrayOf(Manifest.permission.READ_MEDIA_IMAGES))
            } else {
                action()
            }
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // For API 30-32, we need READ_EXTERNAL_STORAGE permission
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) 
                != PackageManager.PERMISSION_GRANTED) {
                requestPermissionLauncher.launch(arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE))
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
                requestPermissionLauncher.launch(arrayOf(
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
            // Use TransactionManager to backup transactions
            val backupPath = transactionManager.backupTransactions()
            
            if (backupPath != null) {
                Toast.makeText(
                    this, 
                    "Backup saved successfully", 
                    Toast.LENGTH_LONG
                ).show()
            } else {
                Toast.makeText(this, "No transactions to backup", Toast.LENGTH_SHORT).show()
            }
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
                    
                    // Reload data
                    loadTransactions()
                    
                    Toast.makeText(this, "Data restored successfully!", Toast.LENGTH_SHORT).show()
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
        return File(backupDir, "libra_backup_$timestamp.json")
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

    private fun getCurrencySymbol(currencyText: String): String {
        return when {
            currencyText.contains("$") -> "$"
            currencyText.contains("€") -> "€"
            currencyText.contains("£") -> "£"
            currencyText.contains("¥") -> "¥"
            currencyText.contains("₹") -> "₹"
            currencyText.contains("RM") -> "RM"
            currencyText.contains("Fr") -> "Fr"
            else -> "$"
        }
    }
}