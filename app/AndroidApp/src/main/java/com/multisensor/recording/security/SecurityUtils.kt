package com.multisensor.recording.security

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import com.multisensor.recording.util.Logger
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.security.KeyStore
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec
import javax.net.ssl.KeyManagerFactory
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManagerFactory
import javax.net.ssl.X509TrustManager
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate

class SecurityUtils(
    private val context: Context,
    private val logger: Logger
) {

    companion object {
        private const val ANDROID_KEYSTORE = "AndroidKeyStore"
        private const val AES_TRANSFORMATION = "AES/GCM/NoPadding"
        private const val KEY_ALIAS = "MultiSensorRecordingKey"
        private const val GCM_IV_LENGTH = 12
        private const val GCM_TAG_LENGTH = 16
        private const val AUTH_TOKEN_LENGTH = 32

        private val PINNED_CERTIFICATES = setOf(
            "sha256/YOUR_SERVER_CERT_FINGERPRINT_HERE"
        )
    }

    private val secureRandom = SecureRandom()

    fun initializeEncryptionKey(): Boolean {
        return try {
            val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE)
            keyStore.load(null)

            if (!keyStore.containsAlias(KEY_ALIAS)) {
                val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
                val keyGenParameterSpec = KeyGenParameterSpec.Builder(
                    KEY_ALIAS,
                    KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
                )
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .setUserAuthenticationRequired(false)
                    .build()

                keyGenerator.init(keyGenParameterSpec)
                keyGenerator.generateKey()
                logger.info("Encryption key generated and stored in Android Keystore")
            }
            true
        } catch (e: Exception) {
            logger.error("Failed to initialize encryption key", e)
            false
        }
    }

    fun encryptData(data: ByteArray): EncryptedData? {
        return try {
            val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE)
            keyStore.load(null)
            val secretKey = keyStore.getKey(KEY_ALIAS, null) as SecretKey

            val cipher = Cipher.getInstance(AES_TRANSFORMATION)
            cipher.init(Cipher.ENCRYPT_MODE, secretKey)

            val iv = cipher.iv
            val encryptedData = cipher.doFinal(data)

            EncryptedData(encryptedData, iv)
        } catch (e: Exception) {
            logger.error("Failed to encrypt data", e)
            null
        }
    }

    fun decryptData(encryptedData: EncryptedData): ByteArray? {
        return try {
            val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE)
            keyStore.load(null)
            val secretKey = keyStore.getKey(KEY_ALIAS, null) as SecretKey

            val cipher = Cipher.getInstance(AES_TRANSFORMATION)
            val spec = GCMParameterSpec(GCM_TAG_LENGTH * 8, encryptedData.iv)
            cipher.init(Cipher.DECRYPT_MODE, secretKey, spec)

            cipher.doFinal(encryptedData.data)
        } catch (e: Exception) {
            logger.error("Failed to decrypt data", e)
            null
        }
    }

    fun encryptFile(inputFile: File, outputFile: File): Boolean {
        return try {
            val data = inputFile.readBytes()
            val encryptedData = encryptData(data) ?: return false

            FileOutputStream(outputFile).use { fos ->
                fos.write(encryptedData.iv.size)
                fos.write(encryptedData.iv)
                fos.write(encryptedData.data)
            }

            logger.debug("File encrypted successfully: ${inputFile.name}")
            true
        } catch (e: Exception) {
            logger.error("Failed to encrypt file: ${inputFile.name}", e)
            false
        }
    }

    fun decryptFile(inputFile: File, outputFile: File): Boolean {
        return try {
            FileInputStream(inputFile).use { fis ->
                val ivLength = fis.read()
                val iv = ByteArray(ivLength)
                fis.read(iv)

                val encryptedData = fis.readBytes()

                val decryptedData = decryptData(EncryptedData(encryptedData, iv)) ?: return false
                outputFile.writeBytes(decryptedData)
            }

            logger.debug("File decrypted successfully: ${inputFile.name}")
            true
        } catch (e: Exception) {
            logger.error("Failed to decrypt file: ${inputFile.name}", e)
            false
        }
    }

    fun generateAuthToken(): String {
        val tokenBytes = ByteArray(AUTH_TOKEN_LENGTH)
        secureRandom.nextBytes(tokenBytes)
        return Base64.encodeToString(tokenBytes, Base64.URL_SAFE or Base64.NO_WRAP)
    }

    fun validateAuthToken(token: String): Boolean {
        return try {
            if (token.length < 32) return false
            val decoded = Base64.decode(token, Base64.URL_SAFE or Base64.NO_WRAP)
            decoded.size >= 24
        } catch (e: Exception) {
            logger.warning("Invalid auth token format")
            false
        }
    }

    fun createSecureSSLContext(): SSLContext? {
        return try {
            val sslContext = SSLContext.getInstance("TLS")

            val trustManager = createTrustManager()

            sslContext.init(null, arrayOf(trustManager), secureRandom)
            logger.info("SSL context created successfully")
            sslContext
        } catch (e: Exception) {
            logger.error("Failed to create SSL context", e)
            null
        }
    }

    private fun createTrustManager(): X509TrustManager {
        return object : X509TrustManager {
            override fun checkClientTrusted(chain: Array<X509Certificate>, authType: String) {
            }

            override fun checkServerTrusted(chain: Array<X509Certificate>, authType: String) {
                if (chain.isEmpty()) {
                    throw java.security.cert.CertificateException("Certificate chain is empty")
                }

                try {
                    chain[0].checkValidity()
                } catch (e: Exception) {
                    logger.error("Server certificate validation failed", e)
                    throw java.security.cert.CertificateException("Server certificate validation failed", e)
                }
            }

            override fun getAcceptedIssuers(): Array<X509Certificate> {
                return arrayOf()
            }
        }
    }

    fun hashToken(token: String, salt: ByteArray): String {
        return try {
            val spec = javax.crypto.spec.PBEKeySpec(
                token.toCharArray(),
                salt,
                10000,
                256
            )
            val factory = javax.crypto.SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
            val hash = factory.generateSecret(spec).encoded
            Base64.encodeToString(hash, Base64.DEFAULT)
        } catch (e: Exception) {
            logger.error("Failed to hash token", e)
            ""
        }
    }

    fun generateSalt(): ByteArray {
        val salt = ByteArray(16)
        secureRandom.nextBytes(salt)
        return salt
    }

    fun sanitizeForLogging(input: String): String {
        return input
            .replace(Regex("[0-9]{10,}"), "***")
            .replace(Regex("\\b[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Z|a-z]{2,}\\b"), "***@***.***")
            .replace(Regex("\\b(?:[0-9]{1,3}\\.){3}[0-9]{1,3}\\b"), "***.***.***.***")
            .replace(Regex("[A-Za-z0-9+/]{20,}={0,2}"), "***")
    }

    data class EncryptedData(
        val data: ByteArray,
        val iv: ByteArray
    ) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as EncryptedData

            if (!data.contentEquals(other.data)) return false
            if (!iv.contentEquals(other.iv)) return false

            return true
        }

        override fun hashCode(): Int {
            var result = data.contentHashCode()
            result = 31 * result + iv.contentHashCode()
            return result
        }
    }
}
