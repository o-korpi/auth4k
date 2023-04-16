package types.user

import types.auth.PasswordHasher

sealed interface UserCredentials<KeyType : LoginKey> {
    val loginKey: KeyType
    val password: Password
}

data class RawUserCredentials<KeyType : LoginKey>(
    override val loginKey: KeyType,
    override val password: RawPassword
) : UserCredentials<KeyType> {
    fun hash(hasher: PasswordHasher): HashedUserCredentials<KeyType> =
        HashedUserCredentials(loginKey, hasher.hash(password))
}

data class HashedUserCredentials<KeyType : LoginKey> (
    override val loginKey: KeyType,
    override val password: HashedPassword,

): UserCredentials<KeyType>
