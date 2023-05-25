package auth4k.types.user

interface UserDetails

sealed class UserEntityException(msg: String) : Exception(msg) {
    object IdNotGivenException : UserEntityException("Attempted ID access despite user not yet having been given an ID")
    object DetailsNotGivenException : UserEntityException("Attempted details access despite user not yet having been given details")
    object UserPasswordNotHashedException : UserEntityException("Access to user password despite it not yet being hashed.")
}

abstract class UserEntity {
    abstract val userId: UserId?
    abstract val userDetails: UserDetails?
    abstract val userCredentials: HashedUserCredentials?
    fun getId(): UserId = userId ?: throw UserEntityException.IdNotGivenException

    @Suppress("unused")
    fun getDetails(): UserDetails = userDetails ?: throw UserEntityException.DetailsNotGivenException
    fun hasDetails(): Boolean = userDetails != null
    fun getCredentials(): HashedUserCredentials = userCredentials ?: throw UserEntityException.UserPasswordNotHashedException

}

interface UserBuilder<T : UserEntity> {
    fun addId(user: T, userId: UserId): T
    fun addCredentials(user: T, credentials: HashedUserCredentials): T
    fun addDetails(user: T, details: UserDetails): T
}

/*
interface CredentialsContainer {
    val credentials: UserCredentials
}

/** A shell of a user, containing only (raw) credentials */
interface UserShell<out NoIdType : <NoIdUser<User> : CredentialsContainer>> {
    override val credentials: RawUserCredentials
    fun toNoIdUser(details: UserDetails, credentials: HashedUserCredentials): NoIdType
}

interface UserDetailsContainer : CredentialsContainer {
    val details: UserDetails
    override val credentials: HashedUserCredentials
}

interface NoIdUser<out UserType : User> : UserDetailsContainer {
    override val credentials: HashedUserCredentials

    fun assignId(id: UserId): UserType
}

interface User : UserDetailsContainer {
    val id: UserId
}

interface UserDetails
*/