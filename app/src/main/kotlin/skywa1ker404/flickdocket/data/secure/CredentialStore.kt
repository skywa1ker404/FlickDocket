package skywa1ker404.flickdocket.data.secure

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.security.GeneralSecurityException
import java.security.KeyStore
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/** Provider credentials. Kept apart from the Room database and from UI settings, and excluded from backups. */
interface CredentialStore {
    val hasTmdbKey: Flow<Boolean>
    suspend fun tmdbKey(): String?
    suspend fun saveTmdbKey(key: String)
    suspend fun clearTmdbKey()
}

interface SecretCipher {
    fun encrypt(plain: String): String
    /** null when the blob is corrupt or its key is gone (e.g. restored onto another device). */
    fun decryptOrNull(blob: String): String?
}

/** AES-256-GCM with a non-exportable Android Keystore key. Output = Base64(iv[12] + ciphertext). */
class KeystoreSecretCipher(private val alias: String = "flickdocket_credentials_v1") : SecretCipher {
    private fun key(): SecretKey {
        val ks = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (ks.getKey(alias, null) as? SecretKey)?.let { return it }
        val gen = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
        gen.init(
            KeyGenParameterSpec.Builder(alias, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256).build(),
        )
        return gen.generateKey()
    }

    override fun encrypt(plain: String): String {
        val c = Cipher.getInstance(TRANSFORM).apply { init(Cipher.ENCRYPT_MODE, key()) }
        return Base64.getEncoder().encodeToString(c.iv + c.doFinal(plain.toByteArray(Charsets.UTF_8)))
    }

    override fun decryptOrNull(blob: String): String? = try {
        val raw = Base64.getDecoder().decode(blob)
        val c = Cipher.getInstance(TRANSFORM).apply { init(Cipher.DECRYPT_MODE, key(), GCMParameterSpec(128, raw, 0, IV_BYTES)) }
        String(c.doFinal(raw, IV_BYTES, raw.size - IV_BYTES), Charsets.UTF_8)
    } catch (e: GeneralSecurityException) { null } catch (e: IllegalArgumentException) { null }

    private companion object { const val TRANSFORM = "AES/GCM/NoPadding"; const val IV_BYTES = 12 }
}

// File name "credentials" => datastore/credentials.preferences_pb, which backup_rules.xml and data_extraction_rules.xml exclude.
private val Context.credentialDataStore: DataStore<Preferences> by preferencesDataStore(name = "credentials")

class EncryptedCredentialStore(context: Context, private val cipher: SecretCipher = KeystoreSecretCipher()) : CredentialStore {
    private val store = context.applicationContext.credentialDataStore
    private val tmdb = stringPreferencesKey("tmdb")

    override val hasTmdbKey: Flow<Boolean> = store.data.map { it[tmdb] != null }

    override suspend fun tmdbKey(): String? {
        val blob = store.data.first()[tmdb] ?: return null
        return cipher.decryptOrNull(blob) ?: run { clearTmdbKey(); null }
    }

    override suspend fun saveTmdbKey(key: String) { val enc = cipher.encrypt(key); store.edit { it[tmdb] = enc } }
    override suspend fun clearTmdbKey() { store.edit { it.remove(tmdb) } }
}
