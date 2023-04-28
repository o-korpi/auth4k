package types.user

import types.auth.PasswordHasher

sealed interface UserCredentials {
    val loginKey: String
    val password: Password
}

data class RawUserCredentials(
    override val loginKey: String,
    override val password: RawPassword
) : UserCredentials {
    fun hash(hasher: PasswordHasher): HashedUserCredentials =
        HashedUserCredentials(loginKey, hasher.hash(password))
}

data class HashedUserCredentials(
    override val loginKey: String,
    override val password: HashedPassword,

): UserCredentials
