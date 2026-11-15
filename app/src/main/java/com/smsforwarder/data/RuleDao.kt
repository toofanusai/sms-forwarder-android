package com.smsforwarder.data

import androidx.lifecycle.LiveData
import androidx.room.*

@Dao
interface RuleDao {

    @Query("SELECT * FROM rules ORDER BY name ASC")
    fun getAllRules(): LiveData<List<Rule>>

    @Query("SELECT * FROM rules ORDER BY name ASC")
    suspend fun getAllRulesSuspend(): List<Rule>

    @Query("SELECT * FROM rules WHERE enabled = 1 ORDER BY name ASC")
    suspend fun getEnabledRules(): List<Rule>

    @Query("SELECT * FROM rules WHERE id = :id")
    suspend fun getRuleById(id: String): Rule?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRule(rule: Rule)

    @Update
    suspend fun updateRule(rule: Rule)

    @Delete
    suspend fun deleteRule(rule: Rule)

    @Query("UPDATE rules SET enabled = :enabled WHERE id = :id")
    suspend fun setEnabled(id: String, enabled: Boolean)

    @Query("DELETE FROM rules WHERE id = :id")
    suspend fun deleteById(id: String)
}
