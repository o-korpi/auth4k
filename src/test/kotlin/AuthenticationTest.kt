import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

import org.junit.jupiter.api.Assertions.*
import types.auth.DefaultBcryptHasher
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