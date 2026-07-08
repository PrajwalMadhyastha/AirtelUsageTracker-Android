package com.prajwal.utilities.tools.wealthtracker.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.wealthDataStore: DataStore<Preferences> by preferencesDataStore(name = "wealth_calculator_settings")

/**
 * DataStore-backed preferences for the milestone calculator settings.
 * All three fields are independently updatable at any time.
 */
class WealthPreferences(private val context: Context) {

    companion object {
        val MONTHLY_INVESTMENT = doublePreferencesKey("monthly_investment")
        val ANNUAL_STEPUP_PERCENT = doublePreferencesKey("annual_stepup_percent")
        val EXPECTED_RETURN_PERCENT = doublePreferencesKey("expected_return_percent")
        val IS_BIOMETRIC_ENABLED = booleanPreferencesKey("is_biometric_enabled")

        // Defaults
        const val DEFAULT_MONTHLY_INVESTMENT = 10000.0   // ₹10,000/month
        const val DEFAULT_ANNUAL_STEPUP_PERCENT = 10.0   // 10% step-up per year
        const val DEFAULT_EXPECTED_RETURN_PERCENT = 12.0 // 12% annual return
    }

    val monthlyInvestment: Flow<Double> = context.wealthDataStore.data
        .map { it[MONTHLY_INVESTMENT] ?: DEFAULT_MONTHLY_INVESTMENT }

    val annualStepupPercent: Flow<Double> = context.wealthDataStore.data
        .map { it[ANNUAL_STEPUP_PERCENT] ?: DEFAULT_ANNUAL_STEPUP_PERCENT }

    val expectedReturnPercent: Flow<Double> = context.wealthDataStore.data
        .map { it[EXPECTED_RETURN_PERCENT] ?: DEFAULT_EXPECTED_RETURN_PERCENT }

    val isBiometricEnabled: Flow<Boolean> = context.wealthDataStore.data
        .map { it[IS_BIOMETRIC_ENABLED] ?: false }

    suspend fun updateMonthlyInvestment(amount: Double) {
        context.wealthDataStore.edit { it[MONTHLY_INVESTMENT] = amount }
    }

    suspend fun updateAnnualStepup(percent: Double) {
        context.wealthDataStore.edit { it[ANNUAL_STEPUP_PERCENT] = percent }
    }

    suspend fun updateExpectedReturn(percent: Double) {
        context.wealthDataStore.edit { it[EXPECTED_RETURN_PERCENT] = percent }
    }

    suspend fun updateBiometricEnabled(enabled: Boolean) {
        context.wealthDataStore.edit { it[IS_BIOMETRIC_ENABLED] = enabled }
    }
}
