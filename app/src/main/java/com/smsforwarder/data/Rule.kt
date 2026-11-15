package com.smsforwarder.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

enum class SimFilter { SIM1, SIM2, BOTH }
enum class SenderFilter { ANY, WHITELIST }
enum class DestinationType { TELEGRAM, WEBHOOK }

@Entity(tableName = "rules")
@TypeConverters(RuleConverters::class)
data class Rule(
    @PrimaryKey
    val id: String,
    val name: String,
    val sim: SimFilter = SimFilter.BOTH,
    val senderFilter: SenderFilter = SenderFilter.ANY,
    val senderList: List<String> = emptyList(),
    val destinations: Set<DestinationType> = emptySet(),
    val enabled: Boolean = true,

    // Telegram fields
    val telegramBotToken: String = "",
    val telegramChatId: String = "",

    // Webhook fields
    val webhookUrl: String = ""
)

class RuleConverters {
    private val gson = Gson()

    @TypeConverter
    fun fromSimFilter(value: SimFilter): String = value.name

    @TypeConverter
    fun toSimFilter(value: String): SimFilter = SimFilter.valueOf(value)

    @TypeConverter
    fun fromSenderFilter(value: SenderFilter): String = value.name

    @TypeConverter
    fun toSenderFilter(value: String): SenderFilter = SenderFilter.valueOf(value)

    @TypeConverter
    fun fromSenderList(value: List<String>): String = gson.toJson(value)

    @TypeConverter
    fun toSenderList(value: String): List<String> {
        val type = object : TypeToken<List<String>>() {}.type
        return gson.fromJson(value, type) ?: emptyList()
    }

    @TypeConverter
    fun fromDestinations(value: Set<DestinationType>): String = gson.toJson(value.map { it.name })

    @TypeConverter
    fun toDestinations(value: String): Set<DestinationType> {
        val type = object : TypeToken<List<String>>() {}.type
        val list: List<String> = gson.fromJson(value, type) ?: emptyList()
        return list.map { DestinationType.valueOf(it) }.toSet()
    }
}
