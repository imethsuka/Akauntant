package com.example.akauntant

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import android.widget.TextView
import android.widget.ProgressBar
import androidx.appcompat.widget.Toolbar
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.button.MaterialButton
import android.content.Context
import android.view.View

class MainActivity : AppCompatActivity() {
    
    private lateinit var bottomNavigation: BottomNavigationView
    private lateinit var incomeTotalValue: TextView
    private lateinit var expenseTotalValue: TextView
    private lateinit var savingsTotalValue: TextView
    private lateinit var budgetUsageText: TextView
    private lateinit var budgetProgressBar: ProgressBar
    private lateinit var tvBudgetWarning: TextView
    
    private lateinit var sharedPreferences: SharedPreferences
    
    companion object {
        const val PREFS_NAME = "AkauntantPrefs"
        const val PREF_MONTHLY_BUDGET = "monthly_budget"
        const val PREF_CURRENCY = "currency"
        const val TRANSACTIONS_PREF = "transactions"
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        
        // Initialize SharedPreferences
        sharedPreferences = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        
        // Set up the toolbar
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        
        // Initialize views
        initializeViews()
        
        // Set up bottom navigation
        setupBottomNavigation()
        
        // Update dashboard with latest data
        updateDashboard()
        
        // For edge-to-edge display
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }
    
    private fun initializeViews() {
        bottomNavigation = findViewById(R.id.bottomNavigation)
        incomeTotalValue = findViewById(R.id.incomeTotalValue)
        expenseTotalValue = findViewById(R.id.expenseTotalValue)
        savingsTotalValue = findViewById(R.id.savingsTotalValue)
        budgetUsageText = findViewById(R.id.budgetUsageText)
        budgetProgressBar = findViewById(R.id.budgetProgressBar)
        tvBudgetWarning = findViewById(R.id.tvBudgetWarning)
    }
    
    private fun setupBottomNavigation() {
        bottomNavigation.setOnItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.navigation_home -> {
                    // Already on home screen
                    true
                }
                R.id.navigation_add -> {
                    startActivity(Intent(this, AddTransactionActivity::class.java))
                    overridePendingTransition(R.anim.fade_in, R.anim.fade_out)
                    false
                }
                R.id.navigation_history -> {
                    startActivity(Intent(this, HistoryActivity::class.java))
                    overridePendingTransition(R.anim.fade_in, R.anim.fade_out)
                    false
                }
                R.id.navigation_settings -> {
                    startActivity(Intent(this, SettingsActivity::class.java))
                    overridePendingTransition(R.anim.fade_in, R.anim.fade_out)
                    false
                }
                else -> false
            }
        }
    }
    
    private fun updateDashboard() {
        // Get currency symbol
        val currencyText = sharedPreferences.getString(PREF_CURRENCY, "USD ($)")
        val currencySymbol = getCurrencySymbol(currencyText ?: "USD ($)")
        
        // In a real app, these values would come from transaction data
        // For now, we'll use placeholder values
        val totalIncome = 4250.00
        val totalExpense = 2845.75
        val savings = totalIncome - totalExpense
        
        // Format currency values
        incomeTotalValue.text = "$currencySymbol${String.format("%,.2f", totalIncome)}"
        expenseTotalValue.text = "$currencySymbol${String.format("%,.2f", totalExpense)}"
        savingsTotalValue.text = "$currencySymbol${String.format("%,.2f", savings)}"
        
        // Update budget progress
        updateBudgetStatus(totalExpense)
    }
    
    private fun updateBudgetStatus(totalExpense: Double) {
        val budget = sharedPreferences.getFloat(PREF_MONTHLY_BUDGET, 3800f).toDouble()
        
        // Calculate percentage of budget used
        val percentageUsed = if (budget > 0) ((totalExpense / budget) * 100).toInt() else 0
        
        // Update progress bar and text
        budgetProgressBar.progress = percentageUsed
        budgetUsageText.text = "Budget Usage: $percentageUsed%"
        
        // Show warning if approaching or exceeding budget
        if (percentageUsed >= 90) {
            tvBudgetWarning.text = "Warning: You've exceeded your monthly budget!"
            tvBudgetWarning.visibility = View.VISIBLE
        } else if (percentageUsed >= 75) {
            tvBudgetWarning.text = "Warning: You're approaching your monthly budget limit!"
            tvBudgetWarning.visibility = View.VISIBLE
        } else {
            tvBudgetWarning.visibility = View.GONE
        }
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
    
    override fun onResume() {
        super.onResume()
        // Refresh dashboard data when returning to the activity
        updateDashboard()
        
        // Ensure the home item is selected in the bottom navigation
        bottomNavigation.selectedItemId = R.id.navigation_home
    }
    
    override fun finish() {
        super.finish()
        overridePendingTransition(R.anim.fade_in, R.anim.fade_out)
    }
}