package com.example.watt_vision_mobile.data

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyPermanentlyInvalidatedException
import android.security.keystore.KeyProperties
import com.example.watt_vision_mobile.BuildConfig
import java.io.IOException
import java.security.KeyStore
import java.util.Base64
import javax.crypto.AEADBadTagException
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

internal interface TokenStore {
    fun read(): String?
    fun save(token: String)
    fun clear()
}

internal class TokenManager(context: Context) : TokenStore {
    // Este arquivo já está excluído das três modalidades de backup.
    private val prefs = context.getSharedPreferences("watt_vision_prefs", Context.MODE_PRIVATE)

    override fun read(): String? {
        if (prefs.contains(LEGACY_TOKEN)) {
            checkWrite(prefs.edit().remove(LEGACY_TOKEN).commit())
        }
        val encrypted = prefs.getString(ENCRYPTED_TOKEN, null) ?: return null
        return try {
            val (iv, ciphertext) = encrypted.split('.', limit = 2).also {
                require(it.size == 2)
            }.map(Base64.getDecoder()::decode)
            require(iv.size == 12 && ciphertext.size > 16)
            val key = keyStore().getKey(KEY_ALIAS, null) as? SecretKey
            if (key == null) {
                clear()
                return null
            }
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(128, iv))
            cipher.updateAAD(environment())
            cipher.doFinal(ciphertext).toString(Charsets.UTF_8)
        } catch (_: KeyPermanentlyInvalidatedException) {
            discardInvalidKey()
            null
        } catch (_: AEADBadTagException) {
            clear()
            null
        } catch (_: IllegalArgumentException) {
            clear()
            null
        }
    }

    override fun save(token: String) {
        val store = keyStore()
        val key = store.getKey(KEY_ALIAS, null) as? SecretKey ?: generateKey()
        val cipher = try {
            encryptionCipher(key)
        } catch (_: KeyPermanentlyInvalidatedException) {
            discardInvalidKey()
            encryptionCipher(generateKey())
        }
        cipher.updateAAD(environment())
        val encoder = Base64.getEncoder()
        val iv = encoder.encodeToString(cipher.iv)
        val ciphertext = encoder.encodeToString(cipher.doFinal(token.toByteArray(Charsets.UTF_8)))
        checkWrite(prefs.edit().remove(LEGACY_TOKEN).putString(ENCRYPTED_TOKEN, "$iv.$ciphertext").commit())
    }

    override fun clear() {
        if (!prefs.edit().remove(LEGACY_TOKEN).remove(ENCRYPTED_TOKEN).commit()) {
            // Revoga a capacidade de decifrar um token que possa ter restado em disco.
            keyStore().deleteEntry(KEY_ALIAS)
            throw IOException("Falha ao limpar as preferências de autenticação.")
        }
    }

    private fun discardInvalidKey() {
        clear()
        keyStore().deleteEntry(KEY_ALIAS)
    }

    private fun encryptionCipher(key: SecretKey): Cipher = Cipher.getInstance(TRANSFORMATION).apply {
        init(Cipher.ENCRYPT_MODE, key)
    }

    private fun environment() = "${BuildConfig.BUILD_TYPE}:${BuildConfig.API_BASE_URL}".toByteArray(Charsets.UTF_8)

    private fun keyStore() = KeyStore.getInstance(ANDROID_KEY_STORE).apply { load(null) }

    private fun generateKey(): SecretKey =
        KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEY_STORE).run {
            init(KeyGenParameterSpec.Builder(
                KEY_ALIAS, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            ).setKeySize(256)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .build())
            generateKey()
        }

    private fun checkWrite(success: Boolean) {
        if (!success) throw IOException("Falha ao gravar as preferências de autenticação.")
    }

    private companion object {
        const val ANDROID_KEY_STORE = "AndroidKeyStore"
        const val KEY_ALIAS = "watt_vision_session"
        const val TRANSFORMATION = "AES/GCM/NoPadding"
        const val LEGACY_TOKEN = "jwt_token"
        const val ENCRYPTED_TOKEN = "jwt_token_encrypted"
    }
}
