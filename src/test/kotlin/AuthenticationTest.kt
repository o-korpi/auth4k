import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

import org.junit.jupiter.api.Assertions.*
import types.auth.DefaultBcryptHasher
import types.auth.PasswordHasher
import types.user.*
import kotlin.random.Random

class AuthenticationTest {

    data class UserShellImpl(override val credentials: RawUserCredentials<Email>) : UserShell<Email, NoIdUserImpl> {
        override fun toNoIdUser(details: UserDetails, credentials: HashedUserCredentials<Email>): NoIdUserImpl =
            NoIdUserImpl(details, credentials)

    }

    data class NoIdUserImpl(override val details: UserDetails, override val credentials: HashedUserCredentials<Email>) : NoIdUser<Email, UserImpl> {
        override fun assignId(id: UserId): UserImpl =
            UserImpl(id, details, credentials)
    }

    data class UserImpl(
        override val id: UserId,
        override val details: UserDetails,
        override val credentials: HashedUserCredentials<Email>
    ) : User<Email>

    private val userDb = mutableMapOf<UserId, UserImpl>()
    private fun addToDb(noIdUser: NoIdUserImpl): UserId =
        getFreeId().also { id ->
            userDb[id] = noIdUser.assignId(id)
        }

    private fun getFreeId(): UserId {
        val randomId = UserId(Random.nextLong(100 + userDb.size.toLong()))
        return if (userDb.containsKey(randomId))
            getFreeId()
        else
            randomId
    }

    val getUserByLoginKey = { loginKey: Email -> userDb.filter {  entry: Map.Entry<UserId, UserImpl> ->
        entry.value.credentials.loginKey.value == loginKey.value
    }.values.firstOrNull() }
    private val auth = Authentication<Email> {
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
        val userCount = userDb.size
        val newUserCredentials = RawUserCredentials(Email("test@testmail.com"), RawPassword("testpassword123"))
        val newUser = UserShellImpl(newUserCredentials)
        class Details : UserDetails

        val registration = auth.register(newUser, Details()) {
            println("here")
            addToDb(it)
        }
        assertFalse(registration.fold({ true }, { false }))
        val id = registration.getOrNull()!!
        assertEquals(newUser, userDb[newUser.id.value])
        assertEquals(userCount + 1, userDb.size)
        val newUserPassword = userDb[newUser.id.value]?.credentials?.password
        assertNotNull(newUserPassword)
        assertNotEquals(newUserCredentials.password, newUserPassword)
        assertTrue(DefaultBcryptHasher.validate(newUserCredentials.password, newUserPassword!!))
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