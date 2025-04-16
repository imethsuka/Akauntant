package com.example.akauntant

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout

class SettingsActivity : AppCompatActivity() {

    private lateinit var etMonthlyBudget: TextInputEditText
    private lateinit var spinnerCurrency: AutoCompleteTextView
    private lateinit var tilCurrency: TextInputLayout
    private lateinit var btnSaveSettings: MaterialButton
    private lateinit var budgetProgressBar: ProgressBar
    private lateinit var tvBudgetSummary: TextView
    private lateinit var btnBackupData: MaterialButton
    private lateinit var btnRestoreData: MaterialButton
    private lateinit var bottomNavigation: BottomNavigationView
    
    private lateinit var sharedPreferences: SharedPreferences
    
    // Constants for SharedPreferences
    companion object {
        const val PREFS_NAME = "AkauntantPrefs"
        const val PREF_MONTHLY_BUDGET = "monthly_budget"
        const val PREF_CURRENCY = "currency"
    }
    
    // Available currencies
    private val currencies = arrayOf(
        "USD ($)", "EUR (€)", "JPY (¥)", "GBP (£)", "AUD ($)", 
        "CAD ($)", "CHF (Fr)", "CNY (¥)", "INR (₹)", "MYR (RM)"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)
        
        // Initialize SharedPreferences
        sharedPreferences = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        
        // Set up the toolbar with back navigation
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)
        
        // Initialize views
        initializeViews()
        
        // Load saved settings
        loadSettings()
        
        // Set up listeners
        setupListeners()
        
        // Set up bottom navigation
        setupBottomNavigation()
    }

    private fun initializeViews() {
        etMonthlyBudget = findViewById(R.id.etMonthlyBudget)
        spinnerCurrency = findViewById(R.id.spinnerCurrency)
        tilCurrency = findViewById(R.id.tilCurrency)
        btnSaveSettings = findViewById(R.id.btnSaveSettings)
        budgetProgressBar = findViewById(R.id.budgetProgressBar)
        tvBudgetSummary = findViewById(R.id.tvBudgetSummary)
        btnBackupData = findViewById(R.id.btnBackupData)
        btnRestoreData = findViewById(R.id.btnRestoreData)
        bottomNavigation = findViewById(R.id.bottomNavigation)
        
        // Set up currency spinner
        val adapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, currencies)
        spinnerCurrency.setAdapter(adapter)
    }
    
    private fun setupBottomNavigation() {
        bottomNavigation.selectedItemId = R.id.navigation_settings
        
        bottomNavigation.setOnItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.navigation_home -> {
                    startActivity(Intent(this, MainActivity::class.java))
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
                R.id.navigation_history -> {
                    startActivity(Intent(this, HistoryActivity::class.java))
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
    
    private fun loadSettings() {
        // Load saved monthly budget
        val savedBudget = sharedPreferences.getFloat(PREF_MONTHLY_BUDGET, 1000f)
        etMonthlyBudget.setText(savedBudget.toString())
        
        // Load saved currency
        val savedCurrency = sharedPreferences.getString(PREF_CURRENCY, "USD ($)")
        spinnerCurrency.setText(savedCurrency, false)
        
        // Update budget status display
        updateBudgetStatusDisplay()
    }
    
    private fun setupListeners() {
        btnSaveSettings.setOnClickListener {
            saveSettings()
        }
        
        btnBackupData.setOnClickListener {
            // In a real app, this would handle the backup process
            Toast.makeText(this, "Data backup started", Toast.LENGTH_SHORT).show()
        }
        
        btnRestoreData.setOnClickListener {
            // In a real app, this would handle the restore process
            Toast.makeText(this, "Data restore started", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun saveSettings() {
        val budgetText = etMonthlyBudget.text.toString()
        val currency = spinnerCurrency.text.toString()
        
        if (budgetText.isEmpty()) {
            Toast.makeText(this, "Please enter your monthly budget", Toast.LENGTH_SHORT).show()
            return
        }
        
        val budget = budgetText.toFloatOrNull()
        if (budget == null || budget <= 0) {
            Toast.makeText(this, "Please enter a valid budget amount", Toast.LENGTH_SHORT).show()
            return
        }
        
        // Save to SharedPreferences
        val editor = sharedPreferences.edit()
        editor.putFloat(PREF_MONTHLY_BUDGET, budget)
        editor.putString(PREF_CURRENCY, currency)
        editor.apply()
        
        // Update budget status display
        updateBudgetStatusDisplay()
        
        Toast.makeText(this, "Settings saved successfully", Toast.LENGTH_SHORT).show()
    }
    
    private fun updateBudgetStatusDisplay() {
        // In a real app, this would load actual expense data from a database
        // For demo purposes, we'll use 65% of the budget as spent
        val budget = sharedPreferences.getFloat(PREF_MONTHLY_BUDGET, 1000f)
        val spentPercentage = 65
        val spentAmount = budget * spentPercentage / 100
        
        // Update the progress bar
        budgetProgressBar.progress = spentPercentage
        
        // Get currency symbol
        val currencyText = sharedPreferences.getString(PREF_CURRENCY, "USD ($)")
        val currencySymbol = when {
            currencyText!!.contains("$") -> "$"
            currencyText.contains("€") -> "€"
            currencyText.contains("£") -> "£"
            currencyText.contains("¥") -> "¥"
            currencyText.contains("₹") -> "₹"
            currencyText.contains("RM") -> "RM"
            currencyText.contains("Fr") -> "Fr"
            else -> "$"
        }
        
        // Update the budget summary text
        tvBudgetSummary.text = "You've spent $currencySymbol${spentAmount.toInt()} out of $currencySymbol${budget.toInt()} ($spentPercentage% of your budget)"
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle the back arrow button in the action bar
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