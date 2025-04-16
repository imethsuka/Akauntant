package com.example.akauntant

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomnavigation.BottomNavigationView
import org.json.JSONArray
import org.json.JSONException

class HistoryActivity : AppCompatActivity(), TransactionAdapter.OnTransactionClickListener {

    private lateinit var recyclerView: RecyclerView
    private lateinit var tvNoTransactions: TextView
    private lateinit var adapter: TransactionAdapter
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var bottomNavigation: BottomNavigationView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_history)
        
        // Initialize SharedPreferences
        sharedPreferences = getSharedPreferences(MainActivity.PREFS_NAME, Context.MODE_PRIVATE)

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
    
    private fun setupRecyclerView() {
        recyclerView.layoutManager = LinearLayoutManager(this)
        adapter = TransactionAdapter(this)
        recyclerView.adapter = adapter
    }
    
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
                R.id.navigation_add -> {
                    startActivity(Intent(this, AddTransactionActivity::class.java))
                    overridePendingTransition(R.anim.fade_in, R.anim.fade_out)
                    finish()
                    true
                }
                R.id.navigation_history -> {
                    // Already on this screen
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
        try {
            val transactionsString = sharedPreferences.getString(MainActivity.TRANSACTIONS_PREF, "[]")
            val transactionsArray = JSONArray(transactionsString)
            
            val transactions = mutableListOf<Transaction>()
            
            for (i in 0 until transactionsArray.length()) {
                val transactionObject = transactionsArray.getJSONObject(i)
                val transaction = Transaction.fromJson(transactionObject)
                transactions.add(transaction)
            }
            
            // Sort transactions by timestamp (most recent first)
            transactions.sortByDescending { it.timestamp }
            
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
            
        } catch (e: JSONException) {
            e.printStackTrace()
            tvNoTransactions.visibility = View.VISIBLE
            recyclerView.visibility = View.GONE
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
        
        val message = StringBuilder()
            .append("Amount: ${if (transaction.isIncome) "+" else "-"}$${String.format("%.2f", transaction.amount)}\n")
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
                        // In a real app you'd open AddTransactionActivity with the transaction data
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
                // Remove from SharedPreferences
                try {
                    val transactionsString = sharedPreferences.getString(MainActivity.TRANSACTIONS_PREF, "[]")
                    val transactionsArray = JSONArray(transactionsString)
                    val newTransactionsArray = JSONArray()
                    
                    // Copy all transactions except the one to delete
                    for (i in 0 until transactionsArray.length()) {
                        val transactionObject = transactionsArray.getJSONObject(i)
                        if (transactionObject.optLong("id") != transaction.id) {
                            newTransactionsArray.put(transactionObject)
                        }
                    }
                    
                    // Save back to SharedPreferences
                    sharedPreferences.edit()
                        .putString(MainActivity.TRANSACTIONS_PREF, newTransactionsArray.toString())
                        .apply()
                    
                    // Reload transactions
                    loadTransactions()
                    
                } catch (e: JSONException) {
                    e.printStackTrace()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
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