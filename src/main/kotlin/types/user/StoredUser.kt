package types.user

interface StoredUser {
    val id: UserId
    val credentials: UserCredentials
}
