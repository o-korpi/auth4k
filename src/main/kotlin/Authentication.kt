import arrow.core.Either
import arrow.core.None
import arrow.core.left
import arrow.core.right
import types.auth.DefaultBcryptHasher
import types.auth.PasswordHasher
import types.auth.Session
import types.user.*

@Suppress("unused")
sealed class AuthenticationException(message: String) : Exception(message) {
    object InvalidCredentials : AuthenticationException("Invalid login key or password")
    data class UserNotFound(val userLoginKey: LoginKey) : AuthenticationException("User $userLoginKey not found")
}


@Suppress("unused")
data class SessionNotFound(val session: Session) : SessionException("Session $session not found")

@Suppress("unused")
sealed class SessionException(message: String) : Exception(message) {
    data class UserNotFound(val userLoginKey: LoginKey) : SessionException("User $userLoginKey not found")
}

sealed class RegistrationException(message: String) : Exception(message) {
    data class UserAlreadyExists(val userLoginKey: LoginKey) : RegistrationException("User $userLoginKey already exists")
}


@Suppress("unused")
class Authentication<KeyType : LoginKey> (
    private val passwordHasher: PasswordHasher = DefaultBcryptHasher,
    private val getUserByLoginKey: (KeyType) -> User<KeyType>?,
) {

    fun login(
        rawUserCredentials: RawUserCredentials<KeyType>,
        createSession: () -> Session
    ): Either<AuthenticationException, Session> {
        val user: User<KeyType> = this.getUserByLoginKey(rawUserCredentials.loginKey)
            ?: return AuthenticationException
                .UserNotFound(rawUserCredentials.loginKey)
                .left()

        if (!this.passwordHasher.validate(rawUserCredentials.password, user.credentials.password)) {
            return AuthenticationException
                .InvalidCredentials
                .left()
        }

        return createSession().right()
    }

    fun login(
        userCredentials: RawUserCredentials<KeyType>,
        createSession: (Session, UserId) -> Unit
    ): Either<AuthenticationException, Session> {
        val user: User<KeyType> = this.getUserByLoginKey(userCredentials.loginKey)
            ?: return AuthenticationException
                .UserNotFound(userCredentials.loginKey)
                .left()

        if (!this.passwordHasher.validate(userCredentials.password, user.credentials.password)) {
            return AuthenticationException
                .InvalidCredentials
                .left()
        }
        return generateSession()
            .also { session -> createSession(session, user.id) }
            .right()
    }

    /** Logs the user in using a session. Does **not** refresh the session on its own. */
    fun sessionLogin(
        session: Session,
        getUserBySession: (Session) -> Either<SessionException, User<KeyType>>,
    ): Either<SessionException, User<KeyType>> = getUserBySession(session)

    fun <UserType : User<KeyType>> register(
        user: UserShell<KeyType, NoIdUser<KeyType, User<KeyType>>>,
        userDetails: UserDetails,
        addToDatabase: (NoIdUser<KeyType, UserType>) -> Unit
    ): Either<RegistrationException, None> {
        if (this.getUserByLoginKey(user.credentials.loginKey) != null)
            return RegistrationException
                .UserAlreadyExists(user.credentials.loginKey)
                .left()

        return user
            .toNoIdUser(userDetails, user.credentials.hash(passwordHasher))
            .also(addToDatabase)
            .right()
            .map { _ -> None }
    }

    fun logout(userCredentials: RawUserCredentials<KeyType>, removeSession: (RawUserCredentials<KeyType>) -> Unit) = removeSession(userCredentials)
    fun <User : RawUserCredentials<KeyType>> logoutUser(user: User, removeSession: (RawUserCredentials<KeyType>) -> Unit) = removeSession(user)
    fun logout(userId: UserId, removeSession: (UserId) -> Unit) = removeSession(userId)

    fun delete(userCredentials: RawUserCredentials<KeyType>, deleteFromDatabase: (RawUserCredentials<KeyType>) -> Unit) = deleteFromDatabase(userCredentials)
    fun delete(userId: UserId, deleteFromDatabase: (UserId) -> Unit) = deleteFromDatabase(userId)
}
