// host/src/main/java/com/gamehub/host/secure/SecureStorage.kt
package com.gamehub.host.secure

import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import androidx.annotation.RequiresApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.security.KeyPairGenerator
import java.security.KeyStore
import java.security.Signature
import java.util.Base64
import androidx.core.content.edit


class SecureStorage(private val context: Context) {
    private val keyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
    private val KEY_ALIAS = "gamehub_device_key"
    private val prefs: SharedPreferences by lazy {
        context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
    }
    init {
        if (!keyStore.containsAlias(KEY_ALIAS)) {
            val keyPairGenerator = KeyPairGenerator.getInstance(KeyProperties.KEY_ALGORITHM_RSA, "AndroidKeyStore")
            val spec = KeyGenParameterSpec.Builder(
                KEY_ALIAS,
                KeyProperties.PURPOSE_SIGN or KeyProperties.PURPOSE_VERIFY
            )
                .setDigests(KeyProperties.DIGEST_SHA256)
                .setSignaturePaddings(KeyProperties.SIGNATURE_PADDING_RSA_PKCS1)
                .build()
            keyPairGenerator.initialize(spec)
            keyPairGenerator.generateKeyPair()
        }
    }
    fun signNonce(nonce: ByteArray): ByteArray {
        val privateKey = (keyStore.getKey(KEY_ALIAS, null) as java.security.PrivateKey)
        val signature = Signature.getInstance("SHA256withRSA")
        signature.initSign(privateKey)
        signature.update(nonce)
        return signature.sign()
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun getPublicKeyBase64(): String {
        val certificate = keyStore.getCertificate(KEY_ALIAS)
        val publicKey = certificate.publicKey
        return Base64.getEncoder().encodeToString(publicKey.encoded)
    }
    suspend fun save(key: String, data: String) = withContext(Dispatchers.IO) {
        prefs.edit() { putString(key, data) }
    }

    suspend fun load(key: String): String? = withContext(Dispatchers.IO) {
        prefs.getString(key, null)
    }

    suspend fun delete(key: String) = withContext(Dispatchers.IO) {
        prefs.edit() { remove(key) }
    }

    suspend fun exists(key: String): Boolean = withContext(Dispatchers.IO) {
        prefs.contains(key)
    }
}