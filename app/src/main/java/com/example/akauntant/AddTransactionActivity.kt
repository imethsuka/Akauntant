package com.example.libra

import android.app.DatePickerDialog
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.view.MenuItem
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.button.MaterialButton
import com.google.android.material.radiobutton.MaterialRadioButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import android.content.Context

class AddTransactionActivity : AppCompatActivity() {
    
    // Initialize calendar for date picking
    private val calendar = Calendar.getInstance()
    private lateinit var bottomNavigation: BottomNavigationView
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var transactionManager: TransactionManager
    
    // Track whether we're editing an existing transaction
    private var isEditMode = false
    private var transactionId: Long = -1
    private var existingTransaction: Transaction? = null
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_transaction)
        
        // Initialize SharedPreferences
        sharedPreferences = getSharedPreferences(MainActivity.PREFS_NAME, Context.MODE_PRIVATE)
        
        // Initialize TransactionManager
        transactionManager = TransactionManager(this)
        
        // Check if we're in edit mode
        transactionId = intent.getLongExtra("TRANSACTION_ID", -1)
        isEditMode = transactionId != -1L
        
        // Set up the toolbar with back navigation
        setupToolbar()
        
        // If editing, load existing transaction data
        if (isEditMode) {
            loadExistingTransaction()
        } else {
            // Set up category dropdown based on transaction type (default to expense)
            setupCategoryDropdown(TransactionManager.EXPENSE_CATEGORIES.toTypedArray())
        }
        
        // Handle radio button changes to update category dropdown
        setupTransactionTypeRadioButtons()
        
        // Set up date picker functionality
        setupDatePicker()
        
        // Set up bottom navigation
        setupBottomNavigation()
        
        // Set up save button functionality
        setupSaveButton()
    }
    
    private fun setupToolbar() {
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)
        
        // Update title based on mode
        supportActionBar?.title = if (isEditMode) "Edit Transaction" else "Add Transaction"
    }
    
    private fun loadExistingTransaction() {
        val transactions = transactionManager.getAllTransactions()
        existingTransaction = transactions.find { it.id == transactionId }
        
        existingTransaction?.let { transaction ->
            // Set transaction type
            val rbIncome = findViewById<MaterialRadioButton>(R.id.rbIncome)
            val rbExpense = findViewById<MaterialRadioButton>(R.id.rbExpense)
            
            if (transaction.isIncome) {
                rbIncome.isChecked = true
                setupCategoryDropdown(TransactionManager.INCOME_CATEGORIES.toTypedArray())
            } else {
                rbExpense.isChecked = true
                setupCategoryDropdown(TransactionManager.EXPENSE_CATEGORIES.toTypedArray())
            }
            
            // Set title
            findViewById<TextInputEditText>(R.id.etTitle).setText(transaction.title)
            
            // Set amount
            findViewById<TextInputEditText>(R.id.etAmount).setText(transaction.amount.toString())
            
            // Set category
            findViewById<AutoCompleteTextView>(R.id.dropdownCategory).setText(transaction.category, false)
            
            // Set date
            findViewById<TextInputEditText>(R.id.etDate).setText(transaction.date)
            
            // Parse date to calendar
            try {
                val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.US)
                dateFormat.parse(transaction.date)?.let { date ->
                    calendar.time = date
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
            
            // Set notes
            findViewById<TextInputEditText>(R.id.etNotes).setText(transaction.notes)
            
            // Update currency symbol
            updateCurrencyPrefix()
        } ?: run {
            Toast.makeText(this, "Failed to load transaction", Toast.LENGTH_SHORT).show()
            finish()
        }
    }
    
    private fun setupBottomNavigation() {
        bottomNavigation = findViewById(R.id.bottomNavigation)
        bottomNavigation.selectedItemId = R.id.navigation_add
        
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
                    // Already on this screen
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
    
    private fun setupCategoryDropdown(categories: Array<String>) {
        val adapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, categories)
        val dropdownCategory = findViewById<AutoCompleteTextView>(R.id.dropdownCategory)
        dropdownCategory.setAdapter(adapter)
        
        // Set default selection if not in edit mode or if category is empty
        if (!isEditMode && categories.isNotEmpty()) {
            dropdownCategory.setText(categories[0], false)
        }
    }
    
    private fun setupTransactionTypeRadioButtons() {
        val rbIncome = findViewById<MaterialRadioButton>(R.id.rbIncome)
        val rbExpense = findViewById<MaterialRadioButton>(R.id.rbExpense)
        
        rbIncome.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                setupCategoryDropdown(TransactionManager.INCOME_CATEGORIES.toTypedArray())
                
                // Update currency symbol in amount field
                updateCurrencyPrefix()
            }
        }
        
        rbExpense.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                setupCategoryDropdown(TransactionManager.EXPENSE_CATEGORIES.toTypedArray())
                
                // Update currency symbol in amount field
                updateCurrencyPrefix()
            }
        }
    }
    
    private fun updateCurrencyPrefix() {
        val tilAmount = findViewById<TextInputLayout>(R.id.tilAmount)
        val currencySymbol = getCurrencySymbol(transactionManager.getCurrency())
        tilAmount.prefixText = currencySymbol
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
    
    private fun setupDatePicker() {
        val etDate = findViewById<TextInputEditText>(R.id.etDate)
        val tilDate = findViewById<TextInputLayout>(R.id.tilDate)
        
        // Set current date as default if not in edit mode
        if (!isEditMode) {
            val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.US)
            etDate.setText(dateFormat.format(calendar.time))
        }
        
        // Show date picker when clicking on the field or the icon
        val datePickerDialog = { 
            val dateListener = DatePickerDialog.OnDateSetListener { _, year, month, day ->
                calendar.set(Calendar.YEAR, year)
                calendar.set(Calendar.MONTH, month)
                calendar.set(Calendar.DAY_OF_MONTH, day)
                
                val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.US)
                etDate.setText(dateFormat.format(calendar.time))
            }
            
            DatePickerDialog(
                this,
                dateListener,
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
            ).show()
        }
        
        etDate.setOnClickListener { datePickerDialog() }
        
        // Enable end icon click listener for date picker
        tilDate.setEndIconOnClickListener { datePickerDialog() }
    }
    
    private fun setupSaveButton() {
        val btnSaveTransaction = findViewById<MaterialButton>(R.id.btnSaveTransaction)
        
        // Update button text based on mode
        btnSaveTransaction.text = if (isEditMode) "Update Transaction" else "Save Transaction"
        
        btnSaveTransaction.setOnClickListener {
            if (validateInputs()) {
                // Get all the input values
                val isIncome = findViewById<MaterialRadioButton>(R.id.rbIncome).isChecked
                val title = findViewById<TextInputEditText>(R.id.etTitle).text.toString()
                val amount = findViewById<TextInputEditText>(R.id.etAmount).text.toString().toDoubleOrNull() ?: 0.0
                val category = findViewById<AutoCompleteTextView>(R.id.dropdownCategory).text.toString()
                val date = findViewById<TextInputEditText>(R.id.etDate).text.toString()
                val notes = findViewById<TextInputEditText>(R.id.etNotes).text.toString()
                
                if (isEditMode && existingTransaction != null) {
                    // Update existing transaction
                    val updatedTransaction = existingTransaction!!.copy(
                        isIncome = isIncome,
                        title = title,
                        amount = amount,
                        category = category,
                        date = date,
                        notes = notes
                    )
                    
                    // Save transaction using TransactionManager
                    val success = transactionManager.updateTransaction(updatedTransaction)
                    
                    if (success) {
                        Toast.makeText(
                            this,
                            "Transaction updated successfully!",
                            Toast.LENGTH_SHORT
                        ).show()
                        
                        // Check budget status and trigger notification if necessary
                        // Only check if it's an expense transaction
                        if (!isIncome) {
                            transactionManager.checkBudgetStatusAndNotify()
                        }
                    } else {
                        Toast.makeText(
                            this,
                            "Failed to update transaction",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                } else {
                    // Create transaction object
                    val transaction = Transaction(
                        id = System.currentTimeMillis(),
                        isIncome = isIncome,
                        title = title,
                        amount = amount,
                        category = category,
                        date = date,
                        notes = notes,
                        timestamp = System.currentTimeMillis()
                    )
                    
                    // Save transaction using TransactionManager
                    val success = transactionManager.addTransaction(transaction)
                    
                    if (success) {
                        // Show success message
                        val transactionType = if (isIncome) "Income" else "Expense"
                        Toast.makeText(
                            this,
                            "$transactionType transaction saved successfully!",
                            Toast.LENGTH_SHORT
                        ).show()
                        
                        // Check budget status and trigger notification if necessary
                        if (!isIncome) { // Only check budget on expense transactions
                            transactionManager.checkBudgetStatusAndNotify()
                        }
                    } else {
                        Toast.makeText(
                            this,
                            "Failed to save transaction",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
                
                // Return to main activity
                startActivity(Intent(this, MainActivity::class.java))
                overridePendingTransition(R.anim.fade_in, R.anim.fade_out)
                finish()
            }
        }
    }
    
    private fun validateInputs(): Boolean {
        val etTitle = findViewById<TextInputEditText>(R.id.etTitle)
        val etAmount = findViewById<TextInputEditText>(R.id.etAmount)
        val tilTitle = findViewById<TextInputLayout>(R.id.tilTitle)
        val tilAmount = findViewById<TextInputLayout>(R.id.tilAmount)
        
        var isValid = true
        
        // Validate title
        if (etTitle.text.isNullOrBlank()) {
            tilTitle.error = "Title is required"
            isValid = false
        } else {
            tilTitle.error = null
        }
        
        // Validate amount
        if (etAmount.text.isNullOrBlank()) {
            tilAmount.error = "Amount is required"
            isValid = false
        } else {
            val amount = etAmount.text.toString().toDoubleOrNull()
            if (amount == null || amount <= 0) {
                tilAmount.error = "Please enter a valid amount"
                isValid = false
            } else {
                tilAmount.error = null
            }
        }
        
        return isValid
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
        super.onBackPressed()
        overridePendingTransition(R.anim.fade_in, R.anim.fade_out)
    }
}