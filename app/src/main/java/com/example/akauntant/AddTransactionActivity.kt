package com.example.akauntant

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
import org.json.JSONArray
import org.json.JSONObject

class AddTransactionActivity : AppCompatActivity() {
    
    // Initialize calendar for date picking
    private val calendar = Calendar.getInstance()
    private lateinit var bottomNavigation: BottomNavigationView
    private lateinit var sharedPreferences: SharedPreferences
    
    // Category lists for income and expense
    private val incomeCategories = arrayOf(
        "Salary", "Freelance", "Investments", "Gifts", "Rental Income", "Other Income"
    )
    
    private val expenseCategories = arrayOf(
        "Food", "Transport", "Housing", "Entertainment", "Healthcare", "Shopping",
        "Education", "Utilities", "Travel", "Personal Care", "Other Expense"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_transaction)
        
        // Initialize SharedPreferences
        sharedPreferences = getSharedPreferences(MainActivity.PREFS_NAME, Context.MODE_PRIVATE)
        
        // Set up the toolbar with back navigation
        setupToolbar()
        
        // Set up category dropdown based on transaction type (default to expense)
        setupCategoryDropdown(expenseCategories)
        
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
                R.id.navigation_add -> {
                    // Already on this screen
                    true
                }
                R.id.navigation_history -> {
                    startActivity(Intent(this, HistoryActivity::class.java))
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
        
        // Set default selection
        if (categories.isNotEmpty()) {
            dropdownCategory.setText(categories[0], false)
        }
    }
    
    private fun setupTransactionTypeRadioButtons() {
        val rbIncome = findViewById<MaterialRadioButton>(R.id.rbIncome)
        val rbExpense = findViewById<MaterialRadioButton>(R.id.rbExpense)
        
        rbIncome.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                setupCategoryDropdown(incomeCategories)
                
                // Update currency symbol in amount field
                updateCurrencyPrefix()
            }
        }
        
        rbExpense.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                setupCategoryDropdown(expenseCategories)
                
                // Update currency symbol in amount field
                updateCurrencyPrefix()
            }
        }
    }
    
    private fun updateCurrencyPrefix() {
        val tilAmount = findViewById<TextInputLayout>(R.id.tilAmount)
        val currencyText = sharedPreferences.getString(MainActivity.PREF_CURRENCY, "USD ($)")
        val currencySymbol = getCurrencySymbol(currencyText ?: "USD ($)")
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
        
        // Set current date as default
        val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.US)
        etDate.setText(dateFormat.format(calendar.time))
        
        // Show date picker when clicking on the field or the icon
        val datePickerDialog = { 
            val dateListener = DatePickerDialog.OnDateSetListener { _, year, month, day ->
                calendar.set(Calendar.YEAR, year)
                calendar.set(Calendar.MONTH, month)
                calendar.set(Calendar.DAY_OF_MONTH, day)
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
        
        btnSaveTransaction.setOnClickListener {
            if (validateInputs()) {
                // Get all the input values
                val isIncome = findViewById<MaterialRadioButton>(R.id.rbIncome).isChecked
                val title = findViewById<TextInputEditText>(R.id.etTitle).text.toString()
                val amount = findViewById<TextInputEditText>(R.id.etAmount).text.toString().toDoubleOrNull() ?: 0.0
                val category = findViewById<AutoCompleteTextView>(R.id.dropdownCategory).text.toString()
                val date = findViewById<TextInputEditText>(R.id.etDate).text.toString()
                val notes = findViewById<TextInputEditText>(R.id.etNotes).text.toString()
                
                // Save transaction to SharedPreferences
                saveTransaction(isIncome, title, amount, category, date, notes)
                
                // Show success message
                val transactionType = if (isIncome) "Income" else "Expense"
                Toast.makeText(
                    this,
                    "$transactionType transaction saved successfully!",
                    Toast.LENGTH_SHORT
                ).show()
                
                // Return to main activity
                startActivity(Intent(this, MainActivity::class.java))
                overridePendingTransition(R.anim.fade_in, R.anim.fade_out)
                finish()
            }
        }
    }
    
    private fun saveTransaction(
        isIncome: Boolean,
        title: String,
        amount: Double,
        category: String,
        date: String,
        notes: String
    ) {
        try {
            // Create a JSON object for the transaction
            val transaction = JSONObject().apply {
                put("id", System.currentTimeMillis())
                put("isIncome", isIncome)
                put("title", title)
                put("amount", amount)
                put("category", category)
                put("date", date)
                put("notes", notes)
                put("timestamp", System.currentTimeMillis())
            }
            
            // Get existing transactions JSON array or create a new one
            val transactionsString = sharedPreferences.getString(MainActivity.TRANSACTIONS_PREF, "[]")
            val transactionsArray = JSONArray(transactionsString)
            
            // Add the new transaction
            transactionsArray.put(transaction)
            
            // Save back to SharedPreferences
            sharedPreferences.edit()
                .putString(MainActivity.TRANSACTIONS_PREF, transactionsArray.toString())
                .apply()
                
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Failed to save transaction", Toast.LENGTH_SHORT).show()
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