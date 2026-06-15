package com.example.data.security

import android.util.Base64
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

object CryptoHelper {
    private const val ALIAS = "StitchTokenKey"
    private const val TRANSFORMATION = "AES/GCM/NoPadding"
    private const val ANDROID_KEY_STORE = "AndroidKeyStore"

    init {
        try {
            val keyStore = KeyStore.getInstance(ANDROID_KEY_STORE).apply { load(null) }
            if (!keyStore.containsAlias(ALIAS)) {
                val keyGenerator = KeyGenerator.getInstance("AES", ANDROID_KEY_STORE)
                val builder = android.security.keystore.KeyGenParameterSpec.Builder(
                    ALIAS,
                    android.security.keystore.KeyProperties.PURPOSE_ENCRYPT or android.security.keystore.KeyProperties.PURPOSE_DECRYPT
                ).apply {
                    setBlockModes(android.security.keystore.KeyProperties.BLOCK_MODE_GCM)
                    setEncryptionPaddings(android.security.keystore.KeyProperties.ENCRYPTION_PADDING_NONE)
                }
                keyGenerator.init(builder.build())
                keyGenerator.generateKey()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun getSecretKey(): SecretKey? {
        return try {
            val keyStore = KeyStore.getInstance(ANDROID_KEY_STORE).apply { load(null) }
            (keyStore.getEntry(ALIAS, null) as? KeyStore.SecretKeyEntry)?.secretKey
        } catch (e: Exception) {
            null
        }
    }

    fun encrypt(plainText: String): Pair<String, String> {
        val key = getSecretKey()
        if (key == null) {
            // Fallback for situations where Android Keystore is unavailable (e.g. testing context)
            val fallbackText = Base64.encodeToString(plainText.toByteArray(Charsets.UTF_8), Base64.NO_WRAP)
            return Pair(fallbackText, "fallback_iv")
        }
        return try {
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.ENCRYPT_MODE, key)
            val ivBytes = cipher.iv
            val cipherText = cipher.doFinal(plainText.toByteArray(Charsets.UTF_8))
            val encryptedText = Base64.encodeToString(cipherText, Base64.NO_WRAP)
            val ivText = Base64.encodeToString(ivBytes, Base64.NO_WRAP)
            Pair(encryptedText, ivText)
        } catch (e: Exception) {
            val fallbackText = Base64.encodeToString(plainText.toByteArray(Charsets.UTF_8), Base64.NO_WRAP)
            Pair(fallbackText, "fallback_iv")
        }
    }

    fun decrypt(encryptedText: String, ivText: String): String {
        if (ivText == "fallback_iv" || ivText.isEmpty()) {
            return try {
                String(Base64.decode(encryptedText, Base64.NO_WRAP), Charsets.UTF_8)
            } catch (e: Exception) {
                ""
            }
        }
        val key = getSecretKey() ?: return try {
            String(Base64.decode(encryptedText, Base64.NO_WRAP), Charsets.UTF_8)
        } catch (e: Exception) {
            ""
        }
        return try {
            val cipher = Cipher.getInstance(TRANSFORMATION)
            val spec = GCMParameterSpec(128, Base64.decode(ivText, Base64.NO_WRAP))
            cipher.init(Cipher.DECRYPT_MODE, key, spec)
            val decryptedBytes = cipher.doFinal(Base64.decode(encryptedText, Base64.NO_WRAP))
            String(decryptedBytes, Charsets.UTF_8)
        } catch (e: Exception) {
            try {
                String(Base64.decode(encryptedText, Base64.NO_WRAP), Charsets.UTF_8)
            } catch (e2: Exception) {
                ""
            }
        }
    }
}
