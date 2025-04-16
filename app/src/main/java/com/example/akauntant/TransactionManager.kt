package com.example.akauntant

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class TransactionManager(private val context: Context) {
    
    private val sharedPreferences: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    
    companion object {
        private const val PREFS_NAME = "akauntant_prefs"
        private const val TRANSACTIONS_KEY = "transactions"
        private const val MONTHLY_BUDGET_KEY = "monthly_budget"
        private const val CURRENCY_KEY = "currency"
        private const val BACKUP_FOLDER = "backups"
        
        // Default categories
        val EXPENSE_CATEGORIES = listOf(
            "Food", "Transport", "Housing", "Entertainment", "Health", "Education", 
            "Shopping", "Utilities", "Personal", "Travel", "Other"
        )
        
        val INCOME_CATEGORIES = listOf(
            "Salary", "Freelance", "Gift", "Investment", "Refund", "Other"
        )
    }
    
    // Transaction Management Functions
    
    /**
     * Gets all transactions from SharedPreferences
     */
    fun getAllTransactions(): List<Transaction> {
        val transactionsJson = sharedPreferences.getString(TRANSACTIONS_KEY, "[]") ?: "[]"
        return try {
            val jsonArray = JSONArray(transactionsJson)
            val transactions = mutableListOf<Transaction>()
            
            for (i in 0 until jsonArray.length()) {
                val transactionObject = jsonArray.getJSONObject(i)
                transactions.add(Transaction.fromJson(transactionObject))
            }
            
            transactions.sortedByDescending { it.timestamp }
        } catch (e: JSONException) {
            Log.e("TransactionManager", "Error parsing transactions", e)
            emptyList()
        }
    }
    
    /**
     * Adds a new transaction
     */
    fun addTransaction(transaction: Transaction): Boolean {
        val currentTransactions = getAllTransactions().toMutableList()
        currentTransactions.add(transaction)
        return saveTransactions(currentTransactions)
    }
    
    /**
     * Updates an existing transaction
     */
    fun updateTransaction(transaction: Transaction): Boolean {
        val currentTransactions = getAllTransactions().toMutableList()
        val index = currentTransactions.indexOfFirst { it.id == transaction.id }
        
        return if (index != -1) {
            currentTransactions[index] = transaction
            saveTransactions(currentTransactions)
        } else {
            false
        }
    }
    
    /**
     * Deletes a transaction
     */
    fun deleteTransaction(transaction: Transaction): Boolean {
        val currentTransactions = getAllTransactions().toMutableList()
        val result = currentTransactions.removeIf { it.id == transaction.id }
        
        return if (result) {
            saveTransactions(currentTransactions)
        } else {
            false
        }
    }
    
    /**
     * Saves list of transactions to SharedPreferences
     */
    private fun saveTransactions(transactions: List<Transaction>): Boolean {
        return try {
            val jsonArray = JSONArray()
            transactions.forEach { transaction ->
                jsonArray.put(transaction.toJson())
            }
            
            sharedPreferences.edit()
                .putString(TRANSACTIONS_KEY, jsonArray.toString())
                .apply()
            true
        } catch (e: JSONException) {
            Log.e("TransactionManager", "Error saving transactions", e)
            false
        }
    }
    
    // Budget Management Functions
    
    /**
     * Gets the monthly budget amount
     */
    fun getMonthlyBudget(): Double {
        return sharedPreferences.getFloat(MONTHLY_BUDGET_KEY, 0f).toDouble()
    }
    
    /**
     * Sets the monthly budget amount
     */
    fun setMonthlyBudget(amount: Double) {
        sharedPreferences.edit()
            .putFloat(MONTHLY_BUDGET_KEY, amount.toFloat())
            .apply()
    }
    
    /**
     * Gets the preferred currency
     */
    fun getCurrency(): String {
        return sharedPreferences.getString(CURRENCY_KEY, "USD") ?: "USD"
    }
    
    /**
     * Sets the preferred currency
     */
    fun setCurrency(currency: String) {
        sharedPreferences.edit()
            .putString(CURRENCY_KEY, currency)
            .apply()
    }
    
    // Analysis Functions
    
    /**
     * Gets all transactions for current month
     */
    fun getCurrentMonthTransactions(): List<Transaction> {
        val calendar = Calendar.getInstance()
        val currentMonth = calendar.get(Calendar.MONTH)
        val currentYear = calendar.get(Calendar.YEAR)
        
        return getAllTransactions().filter { transaction ->
            try {
                // Try parsing with format "MMM dd, yyyy" first
                var date: Date? = null
                try {
                    date = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).parse(transaction.date)
                } catch (e: Exception) {
                    // If that fails, try with format "yyyy-MM-dd"
                    try {
                        date = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(transaction.date)
                    } catch (innerE: Exception) {
                        Log.e("TransactionManager", "Error parsing date: ${transaction.date}", innerE)
                    }
                }
                
                if (date != null) {
                    val transactionCalendar = Calendar.getInstance().apply { time = date }
                    transactionCalendar.get(Calendar.MONTH) == currentMonth && 
                            transactionCalendar.get(Calendar.YEAR) == currentYear
                } else {
                    false
                }
            } catch (e: Exception) {
                Log.e("TransactionManager", "Error processing transaction date", e)
                false
            }
        }
    }
    
    /**
     * Gets total income for current month
     */
    fun getCurrentMonthIncome(): Double {
        return getCurrentMonthTransactions()
            .filter { it.isIncome }
            .sumOf { it.amount }
    }
    
    /**
     * Gets total expenses for current month
     */
    fun getCurrentMonthExpenses(): Double {
        return getCurrentMonthTransactions()
            .filter { !it.isIncome }
            .sumOf { it.amount }
    }
    
    /**
     * Gets percentage of budget spent
     */
    fun getBudgetSpentPercentage(): Float {
        val budget = getMonthlyBudget()
        if (budget <= 0) return 0f
        
        val spent = getCurrentMonthExpenses()
        return (spent / budget * 100).toFloat().coerceIn(0f, 100f)
    }
    
    /**
     * Gets expenses grouped by category
     */
    fun getCategoryExpenses(): Map<String, Double> {
        return getCurrentMonthTransactions()
            .filter { !it.isIncome }
            .groupBy { it.category }
            .mapValues { (_, transactions) -> 
                transactions.sumOf { it.amount }
            }
    }
    
    /**
     * Gets percent of total expenses for each category
     */
    fun getCategoryPercentages(): Map<String, Float> {
        val categoryExpenses = getCategoryExpenses()
        val totalExpenses = getCurrentMonthExpenses()
        
        if (totalExpenses <= 0) return categoryExpenses.mapValues { 0f }
        
        return categoryExpenses.mapValues { (_, amount) -> 
            (amount / totalExpenses * 100).toFloat()
        }
    }
    
    // Backup and Restore
    
    /**
     * Backs up all transactions to a JSON file
     * Returns the file path if successful, null otherwise
     */
    fun backupTransactions(): String? {
        try {
            val backupDir = File(context.filesDir, BACKUP_FOLDER)
            if (!backupDir.exists()) {
                backupDir.mkdirs()
            }
            
            // Create backup file with timestamp
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val backupFile = File(backupDir, "akauntant_backup_$timestamp.json")
            
            // Write transactions to file
            val transactions = getAllTransactions()
            val jsonArray = JSONArray()
            transactions.forEach { transaction ->
                jsonArray.put(transaction.toJson())
            }
            
            // Add app settings to backup
            val backupObject = JSONObject()
            backupObject.put("transactions", jsonArray)
            backupObject.put("monthlyBudget", getMonthlyBudget())
            backupObject.put("currency", getCurrency())
            backupObject.put("backupDate", timestamp)
            
            FileWriter(backupFile).use { writer ->
                writer.write(backupObject.toString(2))
            }
            
            return backupFile.absolutePath
        } catch (e: Exception) {
            Log.e("TransactionManager", "Error backing up transactions", e)
            return null
        }
    }
    
    /**
     * Restores transactions from a backup file
     */
    fun restoreFromBackup(backupFilePath: String): Boolean {
        try {
            val backupFile = File(backupFilePath)
            if (!backupFile.exists()) return false
            
            val jsonString = backupFile.readText()
            val backupObject = JSONObject(jsonString)
            
            // Restore transactions
            val jsonArray = backupObject.getJSONArray("transactions")
            val transactions = mutableListOf<Transaction>()
            
            for (i in 0 until jsonArray.length()) {
                val transactionObject = jsonArray.getJSONObject(i)
                transactions.add(Transaction.fromJson(transactionObject))
            }
            
            // Save transactions to preferences
            saveTransactions(transactions)
            
            // Restore settings
            val monthlyBudget = backupObject.optDouble("monthlyBudget", 0.0)
            val currency = backupObject.optString("currency", "USD")
            
            setMonthlyBudget(monthlyBudget)
            setCurrency(currency)
            
            return true
        } catch (e: Exception) {
            Log.e("TransactionManager", "Error restoring from backup", e)
            return false
        }
    }
}