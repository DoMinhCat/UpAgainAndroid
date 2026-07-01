package com.example.upagain.util.datetime

import android.util.Log
import java.time.Instant
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

/**
 * Return a formatted locale date from a timestamptz string
 */
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

/**
 * Compare 2 timestamps
 * @param timestamp1
 * @param timestamp2
 * @return 1 if timestamp1 is more recent, -1 if timestamp2 is more recent, 0 if both are identical
 */
fun compareTimestamps(timestamp1: String, timestamp2: String): Int {
    val instant1 = Instant.parse(timestamp1)
    val instant2 = Instant.parse(timestamp2)

    when {
        instant1 > instant2 -> return 1
        instant1 < instant2 -> return -1
        else -> return 0
    }
}

/**
 * Compare 2 timestamps
 * @param timestamp1
 * @param timestamp2
 * @return 1 if timestamp1 is more recent, -1 if timestamp2 is more recent, 0 if both are identical
 */
fun compareTimestamps(timestamp1: kotlin.time.Instant, timestamp2: String): Int {
    val instant2 = kotlin.time.Instant.parse(timestamp2)

    when {
        timestamp1 > instant2 -> return 1
        timestamp1 < instant2 -> return -1
        else -> return 0
    }
}