package auth4k.types.auth

import at.favre.lib.crypto.bcrypt.BCrypt
import auth4k.types.user.HashedPassword
import auth4k.types.user.RawPassword

object DefaultBcryptHasher : PasswordHasher {
    override fun hash(rawPassword: RawPassword): HashedPassword =
        HashedPassword(BCrypt.withDefaults().hashToString(12, rawPassword.value.toCharArray()))

    override fun validate(rawPassword: RawPassword, password: HashedPassword): Boolean =
        BCrypt.verifyer().verify(rawPassword.value.toCharArray(), password.value.toCharArray()).verified
}