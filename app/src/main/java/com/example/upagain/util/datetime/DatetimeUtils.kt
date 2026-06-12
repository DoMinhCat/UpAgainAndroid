package com.example.upagain.util.datetime

import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

fun formatTimestamptz(input: String, currentLocale: Locale = Locale.getDefault()): String {
    return try {
        // 1. Parse incoming backend format string explicitly
        val inputFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSSSSSX", Locale.US)
        val zonedDateTime = ZonedDateTime.parse(input, inputFormatter)

        // 2. Generate a regional localizer configuration based on the target language
        val localizedFormatter = DateTimeFormatter
            .ofLocalizedDate(FormatStyle.LONG)
            .withLocale(currentLocale)

        zonedDateTime.format(localizedFormatter)
    } catch (e: Exception) {
        "Invalid Date"
    }
}