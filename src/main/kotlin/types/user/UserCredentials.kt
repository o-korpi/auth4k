package types.user

interface UserCredentials<KeyType : LoginKey> {
    val loginKey: KeyType
    val password: Password
}
