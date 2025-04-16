package com.example.akauntant

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.card.MaterialCardView
import java.text.SimpleDateFormat
import java.util.Locale

class TransactionAdapter(private val listener: OnTransactionClickListener? = null) : 
    RecyclerView.Adapter<TransactionAdapter.TransactionViewHolder>() {
    
    private var transactions: List<Transaction> = listOf()
    
    interface OnTransactionClickListener {
        fun onTransactionClick(transaction: Transaction)
        fun onTransactionLongClick(transaction: Transaction): Boolean
    }
    
    class TransactionViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val cardView: MaterialCardView = itemView.findViewById(R.id.cardView)
        val tvTitle: TextView = itemView.findViewById(R.id.tvTitle)
        val tvAmount: TextView = itemView.findViewById(R.id.tvAmount)
        val tvCategory: TextView = itemView.findViewById(R.id.tvCategory)
        val tvDate: TextView = itemView.findViewById(R.id.tvDate)
        val ivTransactionType: ImageView = itemView.findViewById(R.id.ivTransactionType)
    }
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TransactionViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_transaction, parent, false)
        return TransactionViewHolder(view)
    }
    
    override fun onBindViewHolder(holder: TransactionViewHolder, position: Int) {
        val transaction = transactions[position]
        val context = holder.itemView.context
        
        // Set transaction title
        holder.tvTitle.text = transaction.title
        
        // Set amount with proper formatting and color
        val amountText = String.format("%s%.2f", if (transaction.isIncome) "+" else "-", transaction.amount)
        holder.tvAmount.text = amountText
        holder.tvAmount.setTextColor(context.getColor(
            if (transaction.isIncome) R.color.income_green else R.color.expense_red
        ))
        
        // Set category
        holder.tvCategory.text = transaction.category
        
        // Format and set date
        holder.tvDate.text = Transaction.formatDate(transaction.date)
        
        // Set transaction type icon
        holder.ivTransactionType.setImageResource(
            if (transaction.isIncome) R.drawable.ic_income else R.drawable.ic_expense
        )
        
        // Set click listeners
        holder.cardView.setOnClickListener {
            listener?.onTransactionClick(transaction)
        }
        
        holder.cardView.setOnLongClickListener {
            listener?.onTransactionLongClick(transaction) ?: false
        }
    }
    
    override fun getItemCount(): Int = transactions.size
    
    @SuppressLint("NotifyDataSetChanged")
    fun submitList(newTransactions: List<Transaction>) {
        transactions = newTransactions
        notifyDataSetChanged()
    }
}