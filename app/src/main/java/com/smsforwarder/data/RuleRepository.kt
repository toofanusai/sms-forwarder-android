package com.smsforwarder.data

import android.content.Context
import androidx.lifecycle.LiveData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.UUID

class RuleRepository(context: Context) {

    private val db = AppDatabase.getInstance(context)
    private val ruleDao = db.ruleDao()
    private val logDao = db.logDao()

    // ---- Rules ----

    fun getAllRules(): LiveData<List<Rule>> = ruleDao.getAllRules()

    suspend fun getEnabledRules(): List<Rule> = withContext(Dispatchers.IO) {
        ruleDao.getEnabledRules()
    }

    suspend fun getRuleById(id: String): Rule? = withContext(Dispatchers.IO) {
        ruleDao.getRuleById(id)
    }

    suspend fun saveRule(rule: Rule) = withContext(Dispatchers.IO) {
        ruleDao.insertRule(rule)
    }

    suspend fun deleteRule(rule: Rule) = withContext(Dispatchers.IO) {
        ruleDao.deleteRule(rule)
    }

    suspend fun deleteRuleById(id: String) = withContext(Dispatchers.IO) {
        ruleDao.deleteById(id)
    }

    suspend fun setRuleEnabled(id: String, enabled: Boolean) = withContext(Dispatchers.IO) {
        ruleDao.setEnabled(id, enabled)
    }

    fun newRuleId(): String = UUID.randomUUID().toString()

    // ---- Logs ----

    fun getRecentLogs(): LiveData<List<LogEntry>> = logDao.getRecentLogs()

    suspend fun addLog(entry: LogEntry) = withContext(Dispatchers.IO) {
        logDao.insertLog(entry)
        logDao.trimToLimit(200)
    }

    suspend fun clearLogs() = withContext(Dispatchers.IO) {
        logDao.clearAll()
    }
}
