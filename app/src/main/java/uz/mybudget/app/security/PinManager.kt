package uz.mybudget.app.security

import android.content.Context
import android.os.Build
import android.util.Base64
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

class PinManager(context: Context) {
    private val preferences = context.applicationContext.getSharedPreferences(
        "my_budget_security",
        Context.MODE_PRIVATE
    )

    fun hasPin(): Boolean = preferences.contains(KEY_HASH) && preferences.contains(KEY_SALT)

    fun setPin(pin: String) {
        require(isValidPin(pin)) { "PIN 4–6 ta raqamdan iborat bo‘lishi kerak" }
        val salt = ByteArray(SALT_SIZE).also { SecureRandom().nextBytes(it) }
        val algorithm = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            ALGORITHM_SHA256
        } else {
            ALGORITHM_SHA1
        }
        val hash = hash(pin, salt, algorithm)
        preferences.edit()
            .putString(KEY_SALT, Base64.encodeToString(salt, Base64.NO_WRAP))
            .putString(KEY_HASH, Base64.encodeToString(hash, Base64.NO_WRAP))
            .putString(KEY_ALGORITHM, algorithm)
            .apply()
    }

    fun verify(pin: String): Boolean {
        val saltValue = preferences.getString(KEY_SALT, null) ?: return false
        val expectedValue = preferences.getString(KEY_HASH, null) ?: return false
        val algorithm = preferences.getString(KEY_ALGORITHM, ALGORITHM_SHA1) ?: ALGORITHM_SHA1
        val salt = Base64.decode(saltValue, Base64.NO_WRAP)
        val expected = Base64.decode(expectedValue, Base64.NO_WRAP)
        val actual = hash(pin, salt, algorithm)
        return constantTimeEquals(expected, actual)
    }

    fun changePin(currentPin: String, newPin: String): Boolean {
        if (!verify(currentPin) || !isValidPin(newPin)) return false
        setPin(newPin)
        return true
    }

    companion object {
        fun isValidPin(pin: String): Boolean = pin.length in 4..6 && pin.all(Char::isDigit)

        private const val KEY_SALT = "pin_salt"
        private const val KEY_HASH = "pin_hash"
        private const val KEY_ALGORITHM = "pin_algorithm"
        private const val ITERATIONS = 120_000
        private const val KEY_LENGTH = 256
        private const val SALT_SIZE = 16

        private const val ALGORITHM_SHA256 = "PBKDF2WithHmacSHA256"
        private const val ALGORITHM_SHA1 = "PBKDF2WithHmacSHA1"

        private fun hash(pin: String, salt: ByteArray, algorithm: String): ByteArray {
            val spec = PBEKeySpec(pin.toCharArray(), salt, ITERATIONS, KEY_LENGTH)
            return try {
                SecretKeyFactory.getInstance(algorithm)
                    .generateSecret(spec)
                    .encoded
            } finally {
                spec.clearPassword()
            }
        }

        private fun constantTimeEquals(first: ByteArray, second: ByteArray): Boolean {
            if (first.size != second.size) return false
            var result = 0
            for (index in first.indices) {
                result = result or (first[index].toInt() xor second[index].toInt())
            }
            return result == 0
        }
    }
}
