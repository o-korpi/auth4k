package types.auth

import at.favre.lib.crypto.bcrypt.BCrypt

object DefaultBcryptHasher : PasswordHasher {
    override fun hash(rawPassword: String): String =
        BCrypt.withDefaults().hashToString(12, rawPassword.toCharArray())

    override fun validate(rawPassword: String, password: String): Boolean =
        BCrypt.verifyer().verify(rawPassword.toCharArray(), password.toCharArray()).verified
}