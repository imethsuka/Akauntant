package com.example.akauntant

import org.json.JSONException
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

data class Transaction(
    val id: Long,
    val title: String,
    val amount: Double,
    val category: String,
    val isIncome: Boolean,
    val date: String,
    val notes: String = "",
    val timestamp: Long = System.currentTimeMillis()
) {
    companion object {
        /**
         * Creates Transaction object from JSON
         */
        fun fromJson(jsonObject: JSONObject): Transaction {
            return try {
                Transaction(
                    id = jsonObject.optLong("id", System.currentTimeMillis()),
                    title = jsonObject.optString("title", ""),
                    amount = jsonObject.optDouble("amount", 0.0),
                    category = jsonObject.optString("category", ""),
                    isIncome = jsonObject.optBoolean("isIncome", false),
                    date = jsonObject.optString("date", getCurrentDate()),
                    notes = jsonObject.optString("notes", ""),
                    timestamp = jsonObject.optLong("timestamp", System.currentTimeMillis())
                )
            } catch (e: JSONException) {
                Transaction(
                    id = System.currentTimeMillis(),
                    title = "",
                    amount = 0.0,
                    category = "",
                    isIncome = false,
                    date = getCurrentDate()
                )
            }
        }

        /**
         * Returns current date in specified format
         */
        fun getCurrentDate(): String {
            val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            return dateFormat.format(Date())
        }
        
        /**
         * Returns formatted date string
         */
        fun formatDate(dateString: String): String {
            try {
                val parsedDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(dateString)
                return SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(parsedDate!!)
            } catch (e: Exception) {
                return dateString
            }
        }
    }

    /**
     * Converts the Transaction to JSONObject
     */
    fun toJson(): JSONObject {
        val jsonObject = JSONObject()
        try {
            jsonObject.put("id", id)
            jsonObject.put("title", title)
            jsonObject.put("amount", amount)
            jsonObject.put("category", category)
            jsonObject.put("isIncome", isIncome)
            jsonObject.put("date", date)
            jsonObject.put("notes", notes)
            jsonObject.put("timestamp", timestamp)
        } catch (e: JSONException) {
            e.printStackTrace()
        }
        return jsonObject
    }
}