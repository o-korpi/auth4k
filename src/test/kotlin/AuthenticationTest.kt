import arrow.core.left
import arrow.core.right
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

import org.junit.jupiter.api.Assertions.*
import auth4k.types.auth.DefaultBcryptHasher
import auth4k.types.auth.Session
import auth4k.types.user.*
import java.util.*
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
    private val auth = auth4k.Authentication<UserImpl>(userBuilder = builder) {
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
        assertInstanceOf(auth4k.AuthenticationException.UserNotFound::class.java, invalidLogin.leftOrNull())
        sessionDb.clear()
        val wrongPasswordCredentials = RawUserCredentials("test@testmail.com", RawPassword("wrongpassword123"))
        val wrongPasswordLogin = auth.login(wrongPasswordCredentials, createSession)
        assertTrue(wrongPasswordLogin.isLeft())
        assertInstanceOf(auth4k.AuthenticationException.InvalidCredentials::class.java, wrongPasswordLogin.leftOrNull())
    }

    @Test
    fun sessionLogin() {
        val userCredentials = RawUserCredentials("test@testmail.com", RawPassword("testpassword123"))
        val existingTestUser = UserImpl(null, object : UserDetails {}, userCredentials.hash(DefaultBcryptHasher))
        val id = addToDb(existingTestUser)

        val sessionDb = mutableMapOf<Session, UserId>()
        val createSession = { session: Session, userId: UserId ->
            sessionDb[session] = userId
        }

        val existingSession = Session(UUID.randomUUID().toString())
        createSession(existingSession, id)

        val login = auth.sessionLogin(existingSession) {
            userDb[sessionDb[it]]?.right() ?: auth4k.SessionException.UserNotFound(it).left()
        }

        assertTrue(login.isRight())
        assertEquals(id, login.getOrNull()?.userId)

        val invalidLogin = auth.sessionLogin(Session(UUID.randomUUID().toString())) {
            userDb[sessionDb[it]]?.right() ?: auth4k.SessionException.UserNotFound(it).left()
        }

        assertTrue(invalidLogin.isLeft())
        assertInstanceOf(auth4k.SessionException.UserNotFound::class.java, invalidLogin.leftOrNull())

        // test scenario where user no longer exists
        userDb.clear()
        val attempt = auth.sessionLogin(existingSession) {
            userDb[sessionDb[it]]?.right() ?: auth4k.SessionException.UserNotFound(it).left()
        }
        assertTrue(attempt.isLeft())
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
    fun testLogout() {
        var sessionDb = mutableMapOf<Session, UserId>()
        val createSession = { session: Session, userId: UserId ->
            sessionDb[session] = userId
        }
        fun randomSession() = Session(UUID.randomUUID().toString())

        createSession(randomSession(), UserId(42))
        assertEquals(1, sessionDb.size)
        auth.logout(UserId(42)) { id ->
            sessionDb = sessionDb.filter { it.value != id }.toMutableMap()
        }
        assertEquals(0, sessionDb.size)
    }

    @Test
    fun testDelete() {
        val userCredentials = RawUserCredentials("test@testmail.com", RawPassword("testpassword123"))
        val existingTestUser = UserImpl(null, object : UserDetails {}, userCredentials.hash(DefaultBcryptHasher))
        val id = addToDb(existingTestUser)

        assertEquals(1, userDb.size)
        assertTrue(userDb.containsKey(id))

        auth.delete(id) {
            userDb.remove(id)
        }

        assertFalse(userDb.containsKey(id))
        assertEquals(0, userDb.size)
    }
}