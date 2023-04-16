package types.user


interface CredentialsContainer<KeyType : LoginKey> {
    val credentials: UserCredentials<KeyType>
}

/** A shell of a user, containing only (raw) credentials */
interface UserShell<KeyType : LoginKey, out NoIdType : NoIdUser<KeyType, User<KeyType>>> : CredentialsContainer<KeyType> {
    override val credentials: RawUserCredentials<KeyType>
    fun toNoIdUser(details: UserDetails, credentials: HashedUserCredentials<KeyType>): NoIdType
}

interface UserDetailsContainer<KeyType : LoginKey> : CredentialsContainer<KeyType> {
    val details: UserDetails
    override val credentials: HashedUserCredentials<KeyType>
}

interface NoIdUser<KeyType : LoginKey, out UserType : User<KeyType>> : UserDetailsContainer<KeyType> {
    override val credentials: HashedUserCredentials<KeyType>

    fun assignId(id: UserId): UserType
}

interface User<KeyType : LoginKey> : UserDetailsContainer<KeyType> {
    val id: UserId
}

interface UserDetails
