package com.dluvian.voyage.data.account

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.dluvian.nostr_kt.generateMnemonic
import com.dluvian.voyage.core.PubkeyHex
import rust.nostr.protocol.Event
import rust.nostr.protocol.Keys
import rust.nostr.protocol.UnsignedEvent


typealias Mnemonic = String

private const val MNEMONIC = "mnemonic"
private const val FILENAME = "voyage_encrypted_mnemonic"
private const val MAIN_ACCOUNT_INDEX = 0u

class MnemonicSigner(context: Context) : IPubkeyProvider {
    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    // Initialize EncryptedSharedPreferences
    private val sharedPreferences = EncryptedSharedPreferences.create(
        context,
        FILENAME,
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    init {
        if (getMnemonic() == null) {
            sharedPreferences.edit()
                .putString(MNEMONIC, generateMnemonic())
                .apply()
        }
    }

    override fun tryGetPubkeyHex(): Result<PubkeyHex> {
        // TODO: Implement anonymous signing when rust-nostr new release published
        return runCatching { deriveMainAccount().publicKey().toHex() }
    }

    fun sign(unsignedEvent: UnsignedEvent): Result<Event> {
        val keys = deriveMainAccount()

        if (unsignedEvent.author().toHex() != keys.publicKey().toHex()) {
            val err = "Author of unsigned event ${unsignedEvent.author().toHex()} " +
                    "does not match mnemonic main account ${keys.publicKey().toHex()}"
            return Result.failure(IllegalArgumentException(err))
        }

        return runCatching { unsignedEvent.sign(keys) }
    }

    private fun deriveMainAccount(): Keys {
        return Keys.fromMnemonicWithAccount(
            mnemonic = getMnemonic() ?: throw IllegalStateException("No mnemonic saved"),
            passphrase = null,
            account = MAIN_ACCOUNT_INDEX
        )
    }

    private fun getMnemonic(): Mnemonic? = sharedPreferences.getString(MNEMONIC, null)
}
