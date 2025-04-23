package com.example.libra

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
import android.widget.LinearLayout

class MainActivity : AppCompatActivity() {
    
    private lateinit var bottomNavigation: BottomNavigationView
    private lateinit var incomeTotalValue: TextView
    private lateinit var expenseTotalValue: TextView
    private lateinit var savingsTotalValue: TextView
    private lateinit var budgetUsageText: TextView
    private lateinit var budgetProgressBar: ProgressBar
    private lateinit var tvBudgetWarning: TextView
    private lateinit var categoryChartPlaceholder: LinearLayout
    
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var transactionManager: TransactionManager
    
    companion object {
        const val PREFS_NAME = "LibraPrefs"
        const val PREF_MONTHLY_BUDGET = "monthly_budget"
        const val PREF_CURRENCY = "currency"
        const val TRANSACTIONS_PREF = "transactions"
        
        // Category color mapping
        private val CATEGORY_COLORS = mapOf(
            "Food" to "#FF9800",
            "Transport" to "#4CAF50",
            "Housing" to "#2196F3",
            "Entertainment" to "#9C27B0",
            "Health" to "#F44336",
            "Education" to "#009688",
            "Shopping" to "#E91E63",
            "Utilities" to "#607D8B",
            "Personal" to "#FFEB3B",
            "Travel" to "#795548",
            "Other" to "#9E9E9E",
            "Salary" to "#4CAF50",
            "Freelance" to "#8BC34A",
            "Gift" to "#CDDC39",
            "Investment" to "#00BCD4",
            "Refund" to "#03A9F4",
            "Other Income" to "#3F51B5"
        )
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        
        // Initialize SharedPreferences
        sharedPreferences = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        
        // Initialize TransactionManager
        transactionManager = TransactionManager(this)
        
        // Set up the toolbar
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        
        // Initialize views
        initializeViews()
        
        // Set up bottom navigation
        setupBottomNavigation()
        
        // Update dashboard with latest data
        updateDashboard()
        
        // Initialize notification channels and daily reminders if enabled
        initializeNotifications()
        
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
        categoryChartPlaceholder = findViewById(R.id.categoryChartPlaceholder)
    }
    
    private fun setupBottomNavigation() {
        bottomNavigation.setOnItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.navigation_home -> {
                    // Already on home screen
                    true
                }
                R.id.navigation_history -> {
                    startActivity(Intent(this, HistoryActivity::class.java))
                    overridePendingTransition(R.anim.fade_in, R.anim.fade_out)
                    false
                }
                R.id.navigation_add -> {
                    startActivity(Intent(this, AddTransactionActivity::class.java))
                    overridePendingTransition(R.anim.fade_in, R.anim.fade_out)
                    false
                }
                R.id.navigation_report -> {
                    startActivity(Intent(this, ReportActivity::class.java))
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
        // Get currency symbol from TransactionManager
        val currencyText = transactionManager.getCurrency()
        val currencySymbol = getCurrencySymbol(currencyText)
        
        // Get real transaction data from TransactionManager
        val totalIncome = transactionManager.getCurrentMonthIncome()
        val totalExpense = transactionManager.getCurrentMonthExpenses()
        val savings = totalIncome - totalExpense
        
        // Format currency values
        incomeTotalValue.text = "$currencySymbol${String.format("%,.2f", totalIncome)}"
        expenseTotalValue.text = "$currencySymbol${String.format("%,.2f", totalExpense)}"
        savingsTotalValue.text = "$currencySymbol${String.format("%,.2f", savings)}"
        
        // Update budget progress
        updateBudgetStatus(totalExpense)
        
        // Update category spending chart
        updateCategorySpending(currencySymbol)
    }
    
    private fun updateCategorySpending(currencySymbol: String) {
        // Get category expense data from TransactionManager
        val categoryExpenses = transactionManager.getCategoryExpenses()
        val categoryPercentages = transactionManager.getCategoryPercentages()
        
        // Clear the existing views in the category chart container
        categoryChartPlaceholder.removeAllViews()
        
        // If no expense data, show a message
        if (categoryExpenses.isEmpty()) {
            val noDataText = TextView(this)
            noDataText.text = "No expense data available for this month"
            noDataText.setPadding(0, 20, 0, 20)
            categoryChartPlaceholder.addView(noDataText)
            return
        }
        
        // Sort categories by expense amount (descending)
        val sortedCategories = categoryExpenses.entries.sortedByDescending { it.value }
        
        // Add category views dynamically
        for ((category, amount) in sortedCategories) {
            val percentage = categoryPercentages[category] ?: 0f
            
            // Create the category row layout
            val categoryLayout = layoutInflater.inflate(
                R.layout.item_category_progress, 
                categoryChartPlaceholder, 
                false
            )
            
            // Set the category name
            val categoryNameView = categoryLayout.findViewById<TextView>(R.id.categoryName)
            categoryNameView.text = category
            
            // Set the amount and percentage
            val categoryAmountView = categoryLayout.findViewById<TextView>(R.id.categoryAmount)
            categoryAmountView.text = "$currencySymbol${String.format("%,.2f", amount)} (${percentage.toInt()}%)"
            
            // Set the progress bar value and color
            val progressBar = categoryLayout.findViewById<ProgressBar>(R.id.categoryProgressBar)
            progressBar.progress = percentage.toInt()
            
            // Try to set color if available
            try {
                val colorString = CATEGORY_COLORS[category] ?: "#2196F3"
                val color = android.graphics.Color.parseColor(colorString)
                progressBar.progressTintList = android.content.res.ColorStateList.valueOf(color)
            } catch (e: Exception) {
                // Use default color if parsing fails
            }
            
            // Add the category view to the container
            categoryChartPlaceholder.addView(categoryLayout)
        }
    }
    
    private fun updateBudgetStatus(totalExpense: Double) {
        // Get budget from TransactionManager
        val budget = transactionManager.getMonthlyBudget()
        val tvBudgetRemaining = findViewById<TextView>(R.id.tvBudgetRemaining)
        
        if (budget <= 0) {
            // Handle case where no budget is set
            budgetUsageText.text = "Budget Usage: N/A"
            tvBudgetRemaining.text = "No budget set"
            tvBudgetWarning.visibility = View.GONE
            budgetProgressBar.progress = 0
            return
        }
        
        // Calculate percentage of budget used
        val percentageUsed = ((totalExpense / budget) * 100).toInt().coerceAtMost(100)
        
        // Update progress bar and text
        budgetProgressBar.progress = percentageUsed
        budgetUsageText.text = "Budget Usage: $percentageUsed%"
        
        // Get currency symbol for the budget remaining text
        val currencySymbol = getCurrencySymbol(transactionManager.getCurrency())
        val remainingBudget = budget - totalExpense
        val remainingText = "$currencySymbol${String.format("%,.2f", remainingBudget)} remaining"
        
        // Update the remaining budget text
        tvBudgetRemaining.text = remainingText
        
        // Show warning if approaching or exceeding budget
        if (percentageUsed >= 100) {
            tvBudgetWarning.text = "Warning: You've exceeded your monthly budget!"
            tvBudgetWarning.visibility = View.VISIBLE
            budgetProgressBar.progressTintList = android.content.res.ColorStateList.valueOf(android.graphics.Color.RED)
        } else if (percentageUsed >= 75) {
            tvBudgetWarning.text = "Warning: You're approaching your monthly budget limit!"
            tvBudgetWarning.visibility = View.VISIBLE
            budgetProgressBar.progressTintList = android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#FF9800")) // Orange
        } else {
            tvBudgetWarning.visibility = View.GONE
            budgetProgressBar.progressTintList = android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#4CAF50")) // Green
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

    /**
     * Initialize notification system
     * Creates notification channels and schedules daily reminders if enabled
     */
    private fun initializeNotifications() {
        // Create notification channels
        NotificationService.createNotificationChannels(this)
        
        // Check if notifications and daily reminders are enabled
        val notificationsEnabled = sharedPreferences.getBoolean(SettingsActivity.NOTIFICATIONS_KEY, false)
        val dailyRemindersEnabled = sharedPreferences.getBoolean(SettingsActivity.DAILY_REMINDERS_KEY, false)
        
        // Schedule daily reminders if both settings are enabled
        if (notificationsEnabled && dailyRemindersEnabled) {
            NotificationService.scheduleDailyReminder(this, true)
        }
        
        // Check current budget status and send notification if necessary
        val budgetAlertsEnabled = sharedPreferences.getBoolean(SettingsActivity.BUDGET_ALERT_KEY, false)
        if (notificationsEnabled && budgetAlertsEnabled) {
            transactionManager.checkBudgetStatusAndNotify()
        }
    }
}