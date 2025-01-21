package com.baka3k.testsecuritymodule.data

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys

class Preference(context: Context) {
    private val masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)
    val sharedPreferences = EncryptedSharedPreferences.create(
        // passing a file name to share a preferences
        "preferences",
        masterKeyAlias,
        context,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    fun setBundleHash(hash: String, path: String) {
        saveString(key = path, value = path)
    }

    fun getBundleHash(path: String): String {
        return getString(key = path, defaultValue = "")
    }

    fun setPublicKey(appId: String, publickeyAsString: String) {
        saveString(key = appId, value = publickeyAsString)
    }

    fun getPublicKey(appId: String): String {
        return getString(appId, defaultValue = "")
    }

    private fun saveString(key: String, value: String) {
        sharedPreferences.edit().putString(key, value).apply()
    }

    private fun getString(key: String, defaultValue: String = ""): String {
        return sharedPreferences.getString(key, defaultValue) ?: defaultValue
    }

    companion object {
        @Volatile
        private var instance: Preference? = null

        fun getInstance(context: Context): Preference {
            return instance ?: synchronized(this) {
                instance ?: Preference(context.applicationContext).also { instance = it }
            }
        }
    }
}
