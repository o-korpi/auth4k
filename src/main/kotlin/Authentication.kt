import arrow.core.Either
import arrow.core.left
import arrow.core.right
import types.auth.DefaultBcryptHasher
import types.auth.PasswordHasher
import types.auth.Session
import types.user.*

@Suppress("unused")
sealed class AuthenticationException(message: String) : Exception(message) {
    object InvalidCredentials : AuthenticationException("Invalid login key or password")
    data class UserNotFound(val userLoginKey: String) : AuthenticationException("User $userLoginKey not found")
}


@Suppress("unused")
data class SessionNotFound(val session: Session) : SessionException("Session $session not found")

@Suppress("unused")
sealed class SessionException(message: String) : Exception(message) {
    data class UserNotFound(val userLoginKey: String) : SessionException("User $userLoginKey not found")
}

sealed class RegistrationException(message: String) : Exception(message) {
    data class UserAlreadyExists(val userLoginKey: String) : RegistrationException("User $userLoginKey already exists")
    data class InvalidUser(val user: UserEntity) : RegistrationException("User $user has no user details")
}


@Suppress("unused")
class Authentication<User : UserEntity> (
    private val passwordHasher: PasswordHasher = DefaultBcryptHasher,
    private val userBuilder: UserBuilder<User>,
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

    fun login(
        userCredentials: RawUserCredentials,
        createSession: (Session, UserId) -> Unit
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

    /** Logs the user in using a session. Does **not** refresh the session on its own. */
    fun sessionLogin(
        session: Session,
        getUserBySession: (Session) -> Either<SessionException, User>,
    ): Either<SessionException, User> = getUserBySession(session)

    fun register(
        user: User,
        credentials: RawUserCredentials,
        addToDatabase: (User) -> UserId
    ): Either<RegistrationException, UserId> {
        if (this.getUserByLoginKey(credentials.loginKey) != null)
            return RegistrationException
                .UserAlreadyExists(credentials.loginKey)
                .left()

        if (!user.hasDetails())
            return RegistrationException
                .InvalidUser(user)
                .left()

        println("User $user pre")
        return userBuilder
            .addCredentials(user, credentials.hash(passwordHasher))
            .let { newUser ->
                println("new user $newUser")
                addToDatabase(newUser)
                    .also { id -> userBuilder.addId(newUser, id) }
            }
            .right()
    }

    fun logout(userLoginKey: String, removeSession: (String) -> Unit) = removeSession(userLoginKey)
    fun logout(userCredentials: HashedUserCredentials, removeSession: (HashedUserCredentials) -> Unit) = removeSession(userCredentials)
    fun logout(userId: UserId, removeSession: (UserId) -> Unit) = removeSession(userId)

    fun delete(userCredentials: HashedUserCredentials, deleteFromDatabase: (HashedUserCredentials) -> Unit) = deleteFromDatabase(userCredentials)
    fun delete(userId: UserId, deleteFromDatabase: (UserId) -> Unit) = deleteFromDatabase(userId)
}
