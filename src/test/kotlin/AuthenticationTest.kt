import arrow.core.firstOrNone
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

import org.junit.jupiter.api.Assertions.*
import types.user.*

class AuthenticationTest {
    data class UserCreds(override val loginKey: Email, override val password: Password) : UserCredentials<Email>
    data class User(override val id: UserId, override val credentials: UserCredentials<Email>) : StoredUser

    private val userDb = mutableMapOf<Long, User>()

    val getUserByLoginKey = { loginKey: Email -> userDb.filter {  entry: Map.Entry<Long, User> ->
        entry.value.credentials.loginKey.value == loginKey.value
    }.values.firstOrNull() }
    val auth = Authentication<User, Email> {
        getUserByLoginKey(it)
    }

    @BeforeEach
    fun setUp() {
        userDb.clear()

    }

    @Test
    fun login() {
    }

    @Test
    fun testLogin() {
    }

    @Test
    fun sessionLogin() {
    }

    @Test
    fun register() {

    }

    @Test
    fun logout() {
    }

    @Test
    fun logoutUser() {
    }

    @Test
    fun testLogout() {
    }

    @Test
    fun delete() {
    }

    @Test
    fun testDelete() {
    }
}