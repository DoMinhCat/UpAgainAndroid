package com.example.upagain.util.datetime

import android.util.Log
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

fun formatTimestamptz(input: String, currentLocale: Locale = Locale.getDefault()): String {
    try {
        val zonedDateTime = ZonedDateTime.parse(input, DateTimeFormatter.ISO_DATE_TIME)
        val localizedFormatter = DateTimeFormatter
            .ofLocalizedDate(FormatStyle.LONG)
            .withLocale(currentLocale)

        return zonedDateTime.format(localizedFormatter)
    } catch (e: Exception) {
        Log.e("formatTimestamptz", "Failed to parse timestamp", e)
        return "Invalid Date"
    }
}