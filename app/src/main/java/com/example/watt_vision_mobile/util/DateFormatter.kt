package com.example.watt_vision_mobile.util

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object DateFormatter {

    /**
     * Retorna a máscara de formatação Java baseada na preferência escolhida pelo usuário.
     */
    fun getPattern(formatoData: String): String {
        return when (formatoData) {
            "MM/DD/YYYY" -> "MM/dd/yyyy"
            "YYYY/MM/DD" -> "yyyy/MM/dd"
            else -> "dd/MM/yyyy"
        }
    }

    /**
     * Formata uma string de data no formato ISO (YYYY-MM-DD) para a máscara selecionada pelo usuário.
     */
    fun formatIsoDate(dateStr: String, formatoData: String = "DD/MM/YYYY"): String {
        if (dateStr.isBlank()) return ""
        return try {
            val parts = dateStr.split("-")
            if (parts.size == 3) {
                val year = parts[0]
                val month = parts[1]
                val day = parts[2]
                return when (formatoData) {
                    "MM/DD/YYYY" -> "$month/$day/$year"
                    "YYYY/MM/DD" -> "$year/$month/$day"
                    else -> "$day/$month/$year"
                }
            }
            dateStr
        } catch (e: Exception) {
            dateStr
        }
    }

    /**
     * Formata o timestamp atual para exibição de último acesso ou registros.
     */
    fun formatTimestamp(timestampMs: Long, formatoData: String = "DD/MM/YYYY"): String {
        val pattern = "${getPattern(formatoData)} HH:mm"
        val sdf = SimpleDateFormat(pattern, Locale.getDefault())
        return sdf.format(Date(timestampMs))
    }
}
