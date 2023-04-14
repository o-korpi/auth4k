import arrow.core.Either
import arrow.core.left
import arrow.core.right
import types.auth.DefaultBcryptHasher
import types.auth.PasswordHasher
import types.auth.Session
import types.user.LoginKey
import types.user.StoredUser
import types.user.UserCredentials
import types.user.UserId

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
class Authentication<User, KeyType : LoginKey> (
    private val passwordHasher: PasswordHasher = DefaultBcryptHasher,
    private val getUserByLoginKey: (KeyType) -> StoredUser?,
) {

    fun login(
        userCredentials: UserCredentials<KeyType>,
        createSession: () -> Session
    ): Either<AuthenticationException, Session> {
        val user: StoredUser = this.getUserByLoginKey(userCredentials.loginKey)
            ?: return AuthenticationException
                .UserNotFound(userCredentials.loginKey)
                .left()

        if (!this.passwordHasher.validate(userCredentials.password.value, user.credentials.password.value)) {
            return AuthenticationException
                .InvalidCredentials
                .left()
        }

        return createSession().right()
    }

    fun login(
        userCredentials: UserCredentials<KeyType>,
        createSession: (Session, UserId) -> Unit
    ): Either<AuthenticationException, Session> {
        val user: StoredUser = this.getUserByLoginKey(userCredentials.loginKey)
            ?: return AuthenticationException
                .UserNotFound(userCredentials.loginKey)
                .left()

        if (!this.passwordHasher.validate(userCredentials.password.value, user.credentials.password.value)) {
            return AuthenticationException
                .InvalidCredentials
                .left()
        }
        return generateSession()
            .also { session -> createSession(session, user.id) }
            .right()
    }

    /** Logs the user in using a session. Does **not** refresh the session on its own. */
    fun <User> sessionLogin(
        session: Session,
        getUserBySession: (Session) -> Either<SessionException, User>,
    ): Either<SessionException, User> = getUserBySession(session)

    fun register(
        user: User,
        userCredentials: UserCredentials<KeyType>,
        addToDatabase: (User) -> Unit
    ): Either<RegistrationException, User> {
        this.getUserByLoginKey(userCredentials.loginKey) ?:
            return RegistrationException
                .UserAlreadyExists(userCredentials.loginKey)
                .left()

        return user.also(addToDatabase).right()
    }

    fun logout(userCredentials: UserCredentials<KeyType>, removeSession: (UserCredentials<KeyType>) -> Unit) = removeSession(userCredentials)
    fun <User : UserCredentials<KeyType>> logoutUser(user: User, removeSession: (UserCredentials<KeyType>) -> Unit) = removeSession(user)
    fun logout(userId: UserId, removeSession: (UserId) -> Unit) = removeSession(userId)

    fun delete(userCredentials: UserCredentials<KeyType>, deleteFromDatabase: (UserCredentials<KeyType>) -> Unit) = deleteFromDatabase(userCredentials)
    fun delete(userId: UserId, deleteFromDatabase: (UserId) -> Unit) = deleteFromDatabase(userId)
}
