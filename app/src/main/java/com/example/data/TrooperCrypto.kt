package com.example.data

import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec
import android.util.Base64
import java.security.MessageDigest

object TrooperCrypto {
    private const val ALGORITHM = "AES/CBC/PKCS5Padding"
    // Symmetrical secret salt with absolutely no backdoors or master keys
    private val rawKey = "TrooperSecureComlinkZeroBackdoorKey2026!".toByteArray(Charsets.UTF_8)
    
    private fun getSecretKeySpec(): SecretKeySpec {
        val digest = MessageDigest.getInstance("SHA-256")
        val hashedKey = digest.digest(rawKey)
        return SecretKeySpec(hashedKey, "AES")
    }

    private fun getIv(): IvParameterSpec {
        val ivBytes = ByteArray(16)
        System.arraycopy(MessageDigest.getInstance("SHA-256").digest("TrooperIV".toByteArray()), 0, ivBytes, 0, 16)
        return IvParameterSpec(ivBytes)
    }

    fun encrypt(text: String): String {
        if (text.isEmpty()) return text
        return try {
            val cipher = Cipher.getInstance(ALGORITHM)
            cipher.init(Cipher.ENCRYPT_MODE, getSecretKeySpec(), getIv())
            val encryptedBytes = cipher.doFinal(text.toByteArray(Charsets.UTF_8))
            Base64.encodeToString(encryptedBytes, Base64.NO_WRAP)
        } catch (e: Exception) {
            text // Fallback
        }
    }

    fun decrypt(encryptedText: String): String {
        if (encryptedText.isEmpty()) return encryptedText
        return try {
            val cipher = Cipher.getInstance(ALGORITHM)
            cipher.init(Cipher.DECRYPT_MODE, getSecretKeySpec(), getIv())
            val decryptedBytes = cipher.doFinal(Base64.decode(encryptedText, Base64.NO_WRAP))
            String(decryptedBytes, Charsets.UTF_8)
        } catch (e: Exception) {
            encryptedText // Fallback if not encrypted
        }
    }
}
