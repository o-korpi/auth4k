import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

import org.junit.jupiter.api.Assertions.*
import types.auth.DefaultBcryptHasher
import types.auth.Session
import types.user.*
import kotlin.random.Random

class AuthenticationTest {


    data class UserImpl(
        override val userId: UserId?,
        override val userDetails: UserDetails?,
        override val userCredentials: HashedUserCredentials?
    ) : UserEntity()

    class UserBuilderImpl : UserBuilder<UserImpl> {
        override fun addId(user: UserImpl, userId: UserId): UserImpl = UserImpl(userId, user.userDetails, user.userCredentials)

        override fun addCredentials(user: UserImpl, credentials: HashedUserCredentials): UserImpl = UserImpl(user.userId, user.userDetails, credentials)
        override fun addDetails(user: UserImpl, details: UserDetails): UserImpl = UserImpl(user.userId, details, user.userCredentials)
    }
    private val builder = UserBuilderImpl()

    private val userDb = mutableMapOf<UserId, UserImpl>()
    private fun addToDb(user: UserImpl): UserId =
        getFreeId().also { id ->
            println(user)
            userDb[id] = builder.addId(user, id)
        }

    private fun getFreeId(): UserId {
        val randomId = UserId(Random.nextLong(100 + userDb.size.toLong()))
        return if (userDb.containsKey(randomId))
            getFreeId()
        else
            randomId
    }

    val getUserByLoginKey = { loginKey: String -> userDb.filter {  entry: Map.Entry<UserId, UserImpl> ->
        entry.value.getCredentials().loginKey == loginKey
    }.values.firstOrNull() }
    private val auth = Authentication<UserImpl>(userBuilder = builder) {
        getUserByLoginKey(it)
    }

    @BeforeEach
    fun setUp() {
        userDb.clear()

    }

    @Test
    fun testLogin() {
        val userCredentials = RawUserCredentials("test@testmail.com", RawPassword("testpassword123"))
        val sessionDb = mutableMapOf<Session, UserId>()
        val createSession = { session: Session, userId: UserId ->
            sessionDb[session] = userId
        }

        val existingTestUser = UserImpl(null, object : UserDetails {}, userCredentials.hash(DefaultBcryptHasher))
        val id = addToDb(existingTestUser)

        val session = auth.login(userCredentials, createSession)
        assertTrue(session.isRight(), "Login should have been successful")
        assertEquals(id, sessionDb[session.getOrNull()!!])
        sessionDb.clear()

        val invalidLoginCredentials = RawUserCredentials("wrong@testmail.com", RawPassword("testpassword123"))
        val invalidLogin = auth.login(invalidLoginCredentials, createSession)
        assertTrue(invalidLogin.isLeft())
        assertInstanceOf(AuthenticationException.UserNotFound::class.java, invalidLogin.leftOrNull())
        sessionDb.clear()
        val wrongPasswordCredentials = RawUserCredentials("test@testmail.com", RawPassword("wrongpassword123"))
        val wrongPasswordLogin = auth.login(wrongPasswordCredentials, createSession)
        assertTrue(wrongPasswordLogin.isLeft())
        assertInstanceOf(AuthenticationException.InvalidCredentials::class.java, wrongPasswordLogin.leftOrNull())
    }

    @Test
    fun sessionLogin() {
    }

    @Test
    fun register() {
        val userCount = userDb.size
        val details = object : UserDetails {}
        val newUserCredentials = RawUserCredentials("test@testmail.com", RawPassword("testpassword123"))
        val newUser = UserImpl(null, details, null)

        val registration = auth.register(newUser, newUserCredentials, ::addToDb)
        assertFalse(registration.fold({ true }, { false }))
        val id: UserId = registration.getOrNull()!!

        assertEquals(newUserCredentials.loginKey, userDb[id]!!.getCredentials().loginKey)
        assertEquals(userCount + 1, userDb.size)
        val newUserPassword = userDb[id]?.getCredentials()?.password
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