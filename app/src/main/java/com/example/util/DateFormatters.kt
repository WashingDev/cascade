package com.example.util

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import kotlin.math.abs

enum class DeadlineBucket(val label: String) {
  OVERDUE("Overdue"),
  TODAY("Due Today"),
  THIS_WEEK("This Week"),
  LATER("Upcoming"),
  NO_DATE("No Deadline"),
  COMPLETED("Completed"),
}

object DateFormatters {
  private val shortDateFormat: SimpleDateFormat
    get() = SimpleDateFormat("MMM d", Locale.getDefault())

  private val fullDateFormat: SimpleDateFormat
    get() = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())

  private val headerDateFormat: SimpleDateFormat
    get() = SimpleDateFormat("EEEE, MMM d", Locale.getDefault())

  fun formatTodayHeader(nowMs: Long = System.currentTimeMillis()): String {
    return headerDateFormat.format(Date(nowMs))
  }

  fun formatShortDate(epochMs: Long): String {
    return shortDateFormat.format(Date(epochMs))
  }

  fun formatFullDate(epochMs: Long): String {
    return fullDateFormat.format(Date(epochMs))
  }

  /**
   * Normalizes a UTC midnight epoch ms from Material 3 DatePicker into local noon
   * so calendar date comparisons match the user's local timezone cleanly.
   */
  fun utcPickerMillisToLocalNoon(utcMidnightMs: Long): Long {
    val utcCal = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
      timeInMillis = utcMidnightMs
    }
    val localCal = Calendar.getInstance().apply {
      set(Calendar.YEAR, utcCal.get(Calendar.YEAR))
      set(Calendar.MONTH, utcCal.get(Calendar.MONTH))
      set(Calendar.DAY_OF_MONTH, utcCal.get(Calendar.DAY_OF_MONTH))
      set(Calendar.HOUR_OF_DAY, 12)
      set(Calendar.MINUTE, 0)
      set(Calendar.SECOND, 0)
      set(Calendar.MILLISECOND, 0)
    }
    return localCal.timeInMillis
  }

  fun localMillisToUtcPickerMillis(localMs: Long): Long {
    val localCal = Calendar.getInstance().apply { timeInMillis = localMs }
    val utcCal = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
      clear()
      set(Calendar.YEAR, localCal.get(Calendar.YEAR))
      set(Calendar.MONTH, localCal.get(Calendar.MONTH))
      set(Calendar.DAY_OF_MONTH, localCal.get(Calendar.DAY_OF_MONTH))
    }
    return utcCal.timeInMillis
  }

  fun startOfDayMs(epochMs: Long): Long {
    val cal = Calendar.getInstance().apply {
      timeInMillis = epochMs
      set(Calendar.HOUR_OF_DAY, 0)
      set(Calendar.MINUTE, 0)
      set(Calendar.SECOND, 0)
      set(Calendar.MILLISECOND, 0)
    }
    return cal.timeInMillis
  }

  fun daysDeltaFromToday(targetMs: Long, nowMs: Long = System.currentTimeMillis()): Int {
    val todayStart = startOfDayMs(nowMs)
    val targetStart = startOfDayMs(targetMs)
    val diffMs = targetStart - todayStart
    return (diffMs / 86_400_000L).toInt()
  }

  fun formatRelativeDeadline(targetMs: Long, nowMs: Long = System.currentTimeMillis()): String {
    val delta = daysDeltaFromToday(targetMs, nowMs)
    return when {
      delta < -1 -> "Overdue ${abs(delta)}d"
      delta == -1 -> "Overdue 1d"
      delta == 0 -> "Due today"
      delta == 1 -> "Tomorrow"
      delta in 2..6 -> "In ${delta}d"
      else -> formatShortDate(targetMs)
    }
  }

  fun bucketForTask(isCompleted: Boolean, dueDateMs: Long?, nowMs: Long = System.currentTimeMillis()): DeadlineBucket {
    if (isCompleted) return DeadlineBucket.COMPLETED
    if (dueDateMs == null) return DeadlineBucket.NO_DATE
    val delta = daysDeltaFromToday(dueDateMs, nowMs)
    return when {
      delta < 0 -> DeadlineBucket.OVERDUE
      delta == 0 -> DeadlineBucket.TODAY
      delta in 1..7 -> DeadlineBucket.THIS_WEEK
      else -> DeadlineBucket.LATER
    }
  }
}
