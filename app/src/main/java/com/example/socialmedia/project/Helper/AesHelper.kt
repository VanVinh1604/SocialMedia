package com.example.socialmedia.project.Helper

import android.util.Base64
import android.util.Log
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

object AesHelper {

    private const val TAG = "AesHelper"
    private const val AES_ALGORITHM = "AES"
    private const val AES_TRANSFORMATION = "AES/GCM/NoPadding"
    private const val IV_SIZE = 12 // 96-bit cho GCM
    private const val GCM_TAG_LENGTH = 128 // bits

    fun generateKey(): SecretKey {
        val keyGen = KeyGenerator.getInstance(AES_ALGORITHM)
        keyGen.init(256)
        return keyGen.generateKey()
    }

    fun keyToString(key: SecretKey): String {
        return Base64.encodeToString(key.encoded, Base64.NO_WRAP)
    }

    fun stringToKey(keyStr: String): SecretKey {
        val decoded = Base64.decode(keyStr, Base64.NO_WRAP)
        return SecretKeySpec(decoded, AES_ALGORITHM)
    }

    fun generateIv(): ByteArray {
        val iv = ByteArray(IV_SIZE)
        SecureRandom().nextBytes(iv)
        return iv
    }

    // ✅ Encrypt: trả về Base64 = IV + cipherText
    fun encrypt(plainText: String, key: SecretKey): String {
        val iv = generateIv()
        val cipher = Cipher.getInstance(AES_TRANSFORMATION)
        val spec = GCMParameterSpec(GCM_TAG_LENGTH, iv)
        cipher.init(Cipher.ENCRYPT_MODE, key, spec)
        val cipherText = cipher.doFinal(plainText.toByteArray(Charsets.UTF_8))
        val combined = ByteArray(iv.size + cipherText.size)
        System.arraycopy(iv, 0, combined, 0, iv.size)
        System.arraycopy(cipherText, 0, combined, iv.size, cipherText.size)
        return Base64.encodeToString(combined, Base64.NO_WRAP)
    }

    // ✅ Decrypt: đọc từ Base64 IV + cipherText
    fun decrypt(encryptedBase64: String, key: SecretKey): String {
        return try {
            val combined = Base64.decode(encryptedBase64, Base64.NO_WRAP)
            val iv = combined.copyOfRange(0, IV_SIZE)
            val cipherText = combined.copyOfRange(IV_SIZE, combined.size)
            val cipher = Cipher.getInstance(AES_TRANSFORMATION)
            cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(GCM_TAG_LENGTH, iv))
            String(cipher.doFinal(cipherText), Charsets.UTF_8)
        } catch (e: Exception) {
            Log.e(TAG, "Decryption failed", e)
            "[Decryption Error]"
        }
    }
}

