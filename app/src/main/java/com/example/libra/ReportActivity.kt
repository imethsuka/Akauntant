package com.example.libra

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.*
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.github.mikephil.charting.utils.ColorTemplate
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.radiobutton.MaterialRadioButton
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList

class ReportActivity : AppCompatActivity() {
    
    private lateinit var lineChart: LineChart
    private lateinit var pieChartIncome: PieChart
    private lateinit var pieChartExpense: PieChart
    private lateinit var bottomNavigation: BottomNavigationView
    private lateinit var transactionManager: TransactionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_report)
        
        // Initialize TransactionManager
        transactionManager = TransactionManager(this)
        
        // Initialize views
        lineChart = findViewById(R.id.lineChart)
        pieChartIncome = findViewById(R.id.pieChartIncome)
        pieChartExpense = findViewById(R.id.pieChartExpense)
        bottomNavigation = findViewById(R.id.bottomNavigation)
        
        // Set up navigation
        setupBottomNavigation()
        
        // Set up time frame listeners
        setupTimeFrameListeners()
        
        // Initial data load
        loadWeeklyData() // Default to weekly view
    }
    
    @Suppress("DEPRECATION")
    private fun setupBottomNavigation() {
        bottomNavigation = findViewById(R.id.bottomNavigation)
        bottomNavigation.selectedItemId = R.id.navigation_report
        
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
    
    private fun setupTimeFrameListeners() {
        findViewById<MaterialRadioButton>(R.id.rbWeek).setOnClickListener {
            loadWeeklyData()
        }
        
        findViewById<MaterialRadioButton>(R.id.rbMonth).setOnClickListener {
            loadMonthlyData()
        }
        
        findViewById<MaterialRadioButton>(R.id.rbYear).setOnClickListener {
            loadYearlyData()
        }
    }
    
    private fun loadWeeklyData() {
        // Get transactions from the past week
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.DAY_OF_YEAR, -7)
        val startDate = calendar.timeInMillis
        val transactions = transactionManager.getAllTransactions()
            .filter { transaction -> 
                parseTransactionDate(transaction.date).time >= startDate 
            }
        
        // Setup charts with weekly data
        setupIncomeVsExpenseChart(transactions, "Weekly")
        setupCategoryCharts(transactions)
    }
    
    private fun loadMonthlyData() {
        // Get transactions from the past month
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.MONTH, -1)
        val startDate = calendar.timeInMillis
        val transactions = transactionManager.getAllTransactions()
            .filter { transaction -> 
                parseTransactionDate(transaction.date).time >= startDate 
            }
        
        // Setup charts with monthly data
        setupIncomeVsExpenseChart(transactions, "Monthly")
        setupCategoryCharts(transactions)
    }
    
    private fun loadYearlyData() {
        // Get transactions from the past year
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.YEAR, -1)
        val startDate = calendar.timeInMillis
        val transactions = transactionManager.getAllTransactions()
            .filter { transaction -> 
                parseTransactionDate(transaction.date).time >= startDate 
            }
        
        // Setup charts with yearly data
        setupIncomeVsExpenseChart(transactions, "Yearly")
        setupCategoryCharts(transactions)
    }
    
    private fun setupIncomeVsExpenseChart(transactions: List<Transaction>, timeFrame: String) {
        // Clear previous data
        lineChart.clear()
        
        // Define date formatter based on time frame
        val dateFormat = when(timeFrame) {
            "Weekly" -> SimpleDateFormat("EEE", Locale.getDefault()) // Day of week
            "Monthly" -> SimpleDateFormat("dd", Locale.getDefault()) // Day of month
            else -> SimpleDateFormat("MMM", Locale.getDefault()) // Month abbreviation
        }
        
        // Group transactions by day
        val groupedTransactions = when(timeFrame) {
            "Weekly" -> groupTransactionsByDay(transactions, Calendar.DAY_OF_WEEK, 7)
            "Monthly" -> groupTransactionsByDay(transactions, Calendar.DAY_OF_MONTH, 30)
            else -> groupTransactionsByDay(transactions, Calendar.MONTH, 12)
        }
        
        // Prepare income and expense entries
        val incomeEntries = ArrayList<Entry>()
        val expenseEntries = ArrayList<Entry>()
        val labels = ArrayList<String>()
        
        // Process each date group with its index
        var index = 0
        for (entry in groupedTransactions.entries) {
            val dateKey = entry.key
            val transactionsForDate = entry.value
            
            // Add formatted date to labels
            labels.add(dateFormat.format(Date(dateKey)))
            
            // Calculate income/expense for this period
            var incomeSum = 0f
            var expenseSum = 0f
            
            for (transaction in transactionsForDate) {
                if (transaction.isIncome) {
                    incomeSum += transaction.amount.toFloat()
                } else {
                    expenseSum += transaction.amount.toFloat()
                }
            }
            
            // Add data points to chart entries
            incomeEntries.add(Entry(index.toFloat(), incomeSum))
            expenseEntries.add(Entry(index.toFloat(), expenseSum))
            
            index++
        }
        
        // Create datasets
        val incomeDataSet = LineDataSet(incomeEntries, "Income")
        incomeDataSet.color = Color.GREEN
        incomeDataSet.setCircleColor(Color.GREEN)
        incomeDataSet.lineWidth = 2f
        
        val expenseDataSet = LineDataSet(expenseEntries, "Expense")
        expenseDataSet.color = Color.RED
        expenseDataSet.setCircleColor(Color.RED)
        expenseDataSet.lineWidth = 2f
        
        // Combine datasets
        val lineData = LineData(incomeDataSet, expenseDataSet)
        
        // Configure chart
        lineChart.data = lineData
        lineChart.description.isEnabled = false
        lineChart.legend.isEnabled = true
        lineChart.setTouchEnabled(true)
        lineChart.setPinchZoom(true)
        
        // Configure X-axis
        val xAxis = lineChart.xAxis
        xAxis.position = XAxis.XAxisPosition.BOTTOM
        xAxis.valueFormatter = IndexAxisValueFormatter(labels)
        xAxis.labelCount = labels.size
        xAxis.granularity = 1f
        
        lineChart.invalidate() // refresh
    }
    
    private fun setupCategoryCharts(transactions: List<Transaction>) {
        // Separate income and expense transactions
        val incomeTransactions = transactions.filter { it.isIncome }
        val expenseTransactions = transactions.filter { !it.isIncome }
        
        // Group by categories
        val incomeByCategory = incomeTransactions.groupBy { it.category }
            .map { (category, transactions) -> 
                CategorySum(category, transactions.sumOf { it.amount.toDouble() }.toFloat()) 
            }
        
        val expenseByCategory = expenseTransactions.groupBy { it.category }
            .map { (category, transactions) -> 
                CategorySum(category, transactions.sumOf { it.amount.toDouble() }.toFloat()) 
            }
        
        // Setup pie charts
        setupPieChart(pieChartIncome, incomeByCategory, "Income by Category")
        setupPieChart(pieChartExpense, expenseByCategory, "Expense by Category")
        
        // TODO: Setup RecyclerViews for detailed category lists
    }
    
    private fun setupPieChart(chart: PieChart, data: List<CategorySum>, title: String) {
        // Clear previous data
        chart.clear()
        
        // Create entries for the pie chart
        val entries = ArrayList<PieEntry>()
        data.forEach { categorySum ->
            if (categorySum.sum > 0) {
                entries.add(PieEntry(categorySum.sum, categorySum.category))
            }
        }
        
        // Create dataset
        val dataSet = PieDataSet(entries, "")
        dataSet.colors = ColorTemplate.MATERIAL_COLORS.toList()
        dataSet.valueTextSize = 12f
        dataSet.valueTextColor = Color.WHITE
        
        // Configure chart
        val pieData = PieData(dataSet)
        chart.data = pieData
        chart.description.isEnabled = false
        chart.centerText = title
        chart.setCenterTextSize(14f)
        chart.setDrawEntryLabels(false)
        chart.legend.isEnabled = true
        
        chart.invalidate() // refresh
    }
    
    // Helper function to group transactions by time period
    private fun groupTransactionsByDay(
        transactions: List<Transaction>, 
        calendarField: Int, 
        periods: Int
    ): Map<Long, List<Transaction>> {
        val result = mutableMapOf<Long, MutableList<Transaction>>()
        
        // Set up calendar for time bucketing
        val calendar = Calendar.getInstance()
        
        // Initialize buckets for all periods (days, months, etc.)
        for (i in 0 until periods) {
            calendar.timeInMillis = System.currentTimeMillis()
            calendar.add(calendarField, -i)
            
            // Reset time part for consistent keys
            if (calendarField == Calendar.DAY_OF_WEEK || calendarField == Calendar.DAY_OF_MONTH) {
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)
            } else if (calendarField == Calendar.MONTH) {
                calendar.set(Calendar.DAY_OF_MONTH, 1)
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)
            }
            
            result[calendar.timeInMillis] = mutableListOf()
        }
        
        // Assign transactions to appropriate buckets
        for (transaction in transactions) {
            // Parse the date string from the transaction
            val transactionDate = parseTransactionDate(transaction.date)
            calendar.time = transactionDate
            
            // Reset time part for consistent keys
            if (calendarField == Calendar.DAY_OF_WEEK || calendarField == Calendar.DAY_OF_MONTH) {
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)
            } else if (calendarField == Calendar.MONTH) {
                calendar.set(Calendar.DAY_OF_MONTH, 1)
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)
            }
            
            val key = calendar.timeInMillis
            result[key]?.add(transaction) ?: run {
                result[key] = mutableListOf(transaction)
            }
        }
        
        return result
    }
    
    // Data class for category summary
    data class CategorySum(val category: String, val sum: Float)
    
    // Helper function to parse transaction dates
    private fun parseTransactionDate(dateString: String): Date {
        return try {
            // Try parsing with format "MMM dd, yyyy" first
            val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
            dateFormat.parse(dateString) ?: Date()
        } catch (e: Exception) {
            try {
                // If that fails, try with format "yyyy-MM-dd"
                val alternativeFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                alternativeFormat.parse(dateString) ?: Date()
            } catch (e: Exception) {
                Date() // Return current date as fallback
            }
        }
    }
}