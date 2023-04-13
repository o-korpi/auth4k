import arrow.core.Either
import arrow.core.left
import arrow.core.right
import types.auth.PasswordHasher
import types.auth.Session
import types.user.LoginKey
import types.user.StoredUser
import types.user.UserCredentials
import types.user.UserId

sealed class AuthenticationException(message: String) : Exception(message) {
    object InvalidCredentials : AuthenticationException("Invalid login key or password")
    data class UserNotFound(val userLoginKey: LoginKey) : AuthenticationException("User $userLoginKey not found")
}

sealed class SessionException(message: String) : Exception(message) {
    data class SessionNotFound(val session: Session) : SessionException("Session $session not found")
    data class UserNotFound(val userLoginKey: LoginKey) : SessionException("User $userLoginKey not found")
}

sealed class RegistrationException(message: String) : Exception(message) {
    data class UserAlreadyExists(val userLoginKey: LoginKey) : RegistrationException("User $userLoginKey already exists")
}


class Authentication<User> (
    private val passwordHasher: PasswordHasher,
    private val getUserByLoginKey: (LoginKey) -> StoredUser?,
) {

    fun login(
        userCredentials: UserCredentials,
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
        userCredentials: UserCredentials,
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
        userCredentials: UserCredentials,
        addToDatabase: (User) -> Unit
    ): Either<RegistrationException, User> {
        this.getUserByLoginKey(userCredentials.loginKey) ?:
            return RegistrationException
                .UserAlreadyExists(userCredentials.loginKey)
                .left()

        return user.also(addToDatabase).right()
    }

    fun logout(userCredentials: UserCredentials, removeSession: (UserCredentials) -> Unit) = removeSession(userCredentials)
    fun <User : UserCredentials> logoutUser(user: User, removeSession: (UserCredentials) -> Unit) = removeSession(user)
    fun logout(userId: UserId, removeSession: (UserId) -> Unit) = removeSession(userId)

    fun delete(userCredentials: UserCredentials, deleteFromDatabase: (UserCredentials) -> Unit) = deleteFromDatabase(userCredentials)
    fun delete(userId: UserId, deleteFromDatabase: (UserId) -> Unit) = deleteFromDatabase(userId)
}
