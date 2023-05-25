import arrow.core.left
import arrow.core.right
import auth4k.AuthFilters
import auth4k.Authentication
import auth4k.SessionException
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

import org.junit.jupiter.api.Assertions.*
import auth4k.types.auth.DefaultBcryptHasher
import auth4k.types.auth.Session
import auth4k.types.user.*
import com.natpryce.hamkrest.assertion.assertThat
import org.http4k.core.*
import org.http4k.core.Method.*
import org.http4k.core.Status.Companion.OK
import org.http4k.hamkrest.hasStatus
import org.http4k.routing.*
import java.util.*

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
            exemptRoutes = listOf("/login"),
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
        assertThat(response, hasStatus(Status.UNAUTHORIZED))

        // should be able to access a page exempt from auth
        val successResponse = app(Request(GET, "/login"))
        assertThat(successResponse, hasStatus(OK))

    }
}
