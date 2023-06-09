package auth4k

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import auth4k.types.auth.DefaultBcryptHasher
import auth4k.types.auth.PasswordHasher
import auth4k.types.auth.Session
import auth4k.types.user.*

@Suppress("unused")
sealed class AuthenticationException(message: String) : Exception(message) {
    object InvalidCredentials : AuthenticationException("Invalid login key or password")
    data class UserNotFound(val userLoginKey: String) : AuthenticationException("User $userLoginKey not found")
}


@Suppress("unused")
data class SessionNotFound(val session: Session) : SessionException("Session $session not found")

@Suppress("unused")
sealed class SessionException(message: String) : Exception(message) {
    data class UserNotFound(val userSession: Session) : SessionException("User with session $userSession not found")
}

sealed class RegistrationException(message: String) : Exception(message) {
    data class UserAlreadyExists(val userLoginKey: String) : RegistrationException("User $userLoginKey already exists")
    data class InvalidUser(val loginKey: String) : RegistrationException("User $loginKey has no user details")
}


@Suppress("unused")
class Authentication<Id : IdType, User : UserEntity<Id>> (
    private val passwordHasher: PasswordHasher = DefaultBcryptHasher,
    private val userBuilder: UserBuilder<Id, User>,
    private val getUserByLoginKey: (String) -> User?,
) {
    fun login(
        rawUserCredentials: RawUserCredentials,
        createSession: () -> Session
    ): Either<AuthenticationException, Session> {
        val user: User = this.getUserByLoginKey(rawUserCredentials.loginKey)
            ?: return AuthenticationException
                .UserNotFound(rawUserCredentials.loginKey)
                .left()

        if (!this.passwordHasher.validate(rawUserCredentials.password, user.getCredentials().password)) {
            return AuthenticationException
                .InvalidCredentials
                .left()
        }

        return createSession().right()
    }

    /**
     * Authenticates the user, and if successful, generates a session ID which the caller can use.
     *
     * Does not generate the session cookie.
     * */
    fun login(
        userCredentials: RawUserCredentials,
        createSession: (Session, Id) -> Unit
    ): Either<AuthenticationException, Session> {
        val user: User = this.getUserByLoginKey(userCredentials.loginKey)
            ?: return AuthenticationException
                .UserNotFound(userCredentials.loginKey)
                .left()

        if (!this.passwordHasher.validate(userCredentials.password, user.getCredentials().password)) {
            return AuthenticationException
                .InvalidCredentials
                .left()
        }

        return generateSession()
            .also { session -> createSession(session, user.getId()) }
            .right()
    }

    /** Logs the user in using a session.
     *
     * Does **not** refresh the session on its own. Such behaviour has to be implemented by the caller.
     * */
    fun sessionLogin(
        session: Session,
        getUserBySession: (Session) -> Either<SessionException, User>,
    ): Either<SessionException, User> = getUserBySession(session)

    /** Registers a user, given an empty user shell (containing at most their details), their credentials
     * and a method to add them to the database. Note that the function which adds them to the database must
     * also give them their ID, using the `UserBuilder`. */
    fun register(
        user: User,
        credentials: RawUserCredentials,
        addToDatabase: (User) -> Id
    ): Either<RegistrationException, Id> {
        if (this.getUserByLoginKey(credentials.loginKey) != null)
            return RegistrationException
                .UserAlreadyExists(credentials.loginKey)
                .left()

        if (!user.hasDetails())
            return RegistrationException
                .InvalidUser(credentials.loginKey)
                .left()

        return userBuilder
            .addCredentials(user, credentials.hash(passwordHasher))
            .let { newUser ->
                addToDatabase(newUser)
                    .also { id -> userBuilder.addId(newUser, id) }
            }
            .right()
    }

    fun logout(userLoginKey: String, removeSession: (String) -> Unit) = removeSession(userLoginKey)
    fun logout(userCredentials: HashedUserCredentials, removeSession: (HashedUserCredentials) -> Unit) = removeSession(userCredentials)
    fun logout(userId: LongUserId, removeSession: (LongUserId) -> Unit) = removeSession(userId)

    fun delete(userCredentials: HashedUserCredentials, deleteFromDatabase: (HashedUserCredentials) -> Unit) = deleteFromDatabase(userCredentials)
    fun delete(userId: LongUserId, deleteFromDatabase: (LongUserId) -> Unit) = deleteFromDatabase(userId)
}
