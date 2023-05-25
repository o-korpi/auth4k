import arrow.core.left
import arrow.core.right
import auth4k.*
import auth4k.types.auth.DefaultBcryptHasher
import auth4k.types.auth.Session
import auth4k.types.user.*
import com.natpryce.hamkrest.assertion.assertThat
import org.http4k.core.Method.GET
import org.http4k.core.Method.POST
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.OK
import org.http4k.core.Status.Companion.UNAUTHORIZED
import org.http4k.core.cookie.cookie
import org.http4k.core.cookie.cookies
import org.http4k.core.then
import org.http4k.hamkrest.hasStatus
import org.http4k.routing.bind
import org.http4k.routing.routes
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class AuthFiltersTest {
    data class User(
        override val userId: UserId?,
        override val userDetails: UserDetails?,
        override val userCredentials: HashedUserCredentials?
    ) : UserEntity()

    object UserBuilderImpl : UserBuilder<User> {
        override fun addId(user: User, userId: UserId): User =
            User(userId, user.userDetails, user.userCredentials)

        override fun addCredentials(user: User, credentials: HashedUserCredentials): User =
            User(user.userId, user.userDetails, credentials)

        override fun addDetails(user: User, details: UserDetails): User =
            User(user.userId, details, user.userCredentials)

    }

    val db: MutableMap<UserId, User> = mutableMapOf()
    val sdb: MutableMap<Session, User> = mutableMapOf()

    @Test
    fun testAccess() {
        val auth = Authentication(
            DefaultBcryptHasher,
            UserBuilderImpl
        ) { loginKey: String ->
            db.filter { it.value.getCredentials().loginKey == loginKey }.map { it.value }.firstOrNull()
        }
        val filter = AuthFilters.SessionAuth(
            auth = auth,
            exemptRoutes = setOf("/login"),
            getUserBySession = { session: Session -> sdb[session]?.right() ?: SessionException.UserNotFound(session).left() }
        )

        val app = filter.then(
            routes(
                "/ping" bind GET to {
                    Response(OK).body("pong")
                },
                "/login" bind GET to {
                    Response(OK).body("login")
                }
            )
        )
        val req = Request(GET, "/ping")
        val response = app(req)
        assertThat(response, hasStatus(UNAUTHORIZED))

        // should be able to access a page exempt from auth
        val successResponse = app(Request(GET, "/login"))
        assertThat(successResponse, hasStatus(OK))

    }

    @Test
    fun testRegisterAndLoginAndLogout() {
        val auth = Authentication(
            userBuilder = UserBuilderImpl
        ) { loginKey: String ->
            db.filter { it.value.getCredentials().loginKey == loginKey }.map { it.value }.firstOrNull()
        }
        val filter = AuthFilters.SessionAuth(
            auth = auth,
            exemptRoutes = setOf("/register"),
            getUserBySession = { session: Session -> sdb[session]?.right() ?: SessionException.UserNotFound(session).left() }
        )
        val app = filter.then(
            routes(
                "/ping" bind GET to {
                    Response(OK).body("pong")
                },
                "/register" bind POST to { req ->
                    // crude method to get values here, use a json library in a real scenario
                    val map = req.body.toString()
                        .removePrefix("{")
                        .removeSuffix("}")
                        .replace("'", "")
                        .split(',').associate {
                            val kv = it.split(':')
                                .map { it.trim() }

                            kv.first() to kv.last()
                        }

                    val id = auth.register(
                        User(
                            null,
                            (object : UserDetails {}),
                            null
                        ),
                        RawUserCredentials(
                            map["loginKey"]!!,
                            RawPassword(map["password"]!!)
                        )
                    ) {
                        val userId = UserId(1L)
                        db[userId] = UserBuilderImpl.addId(it, userId)
                        userId
                    }

                    val session = generateSession()
                    val loginUser = auth.login(RawUserCredentials("hello@world.com", RawPassword("hunter2"))) { _, userId ->
                        sdb[session] = db[userId]!!
                    }
                    assertTrue(loginUser.isRight())


                    val cookie = SessionCookieFactory().create(
                        session,
                        secure = false
                    )

                    Response(OK).body("registered $id").cookie(cookie)
                }
            )
        )
        val response = app(Request(POST, "/register").body("{ 'loginKey': 'hello@world.com', 'password': 'hunter2'}"))
        assertThat(response, hasStatus(OK))
        assertEquals(1, response.cookies().size)

        val accessAttempt = app(Request(GET, "/ping").cookie(response.cookies().first().also { println(it) }))
        assertThat(accessAttempt, hasStatus(OK))

        auth.logout(UserId(1)) { userId ->
            sdb.remove(sdb.filter { it.value.userId == userId }.keys.firstOrNull())
        }

        val accessAttempt2 = app(Request(GET, "/ping").cookie(response.cookies().first().also { println(it) }))
        assertThat(accessAttempt2, hasStatus(UNAUTHORIZED))
    }
}
