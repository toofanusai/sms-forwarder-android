package com.smsforwarder.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverter
import androidx.room.TypeConverters

enum class DeliveryStatus { PENDING, SUCCESS, FAILED }

@Entity(tableName = "log_entries")
@TypeConverters(LogConverters::class)
data class LogEntry(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val timestamp: Long,                // epoch millis
    val sender: String,
    val body: String,
    val simSlot: Int,                   // 1 or 2
    val matchedRules: List<String>,     // rule names
    val destinations: List<String>,     // e.g. ["TELEGRAM", "WEBHOOK"]
    val status: DeliveryStatus,
    val errorMessage: String = ""
)

class LogConverters {

    @TypeConverter
    fun fromStringList(value: List<String>): String = value.joinToString("|")

    @TypeConverter
    fun toStringList(value: String): List<String> =
        if (value.isBlank()) emptyList() else value.split("|")

    @TypeConverter
    fun fromDeliveryStatus(value: DeliveryStatus): String = value.name

    @TypeConverter
    fun toDeliveryStatus(value: String): DeliveryStatus = DeliveryStatus.valueOf(value)
}
