import arrow.core.Either
import org.http4k.core.Filter
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status
import org.http4k.core.cookie.cookie
import types.auth.Session

// todo: add alternative invoke to redirect to login page
object Filters {
    object SessionAuth {
        operator fun <User> invoke(
            auth: Authentication<User>,
            cookieFactory: SessionCookieFactory = SessionCookieFactory(),
            getUserBySession: (Session) -> Either<SessionException, User>,
            onLoginResponse: ((Request) -> Response)? = null
        ) = Filter { next -> { req ->
            req.asSession(cookieFactory.cookieName).fold(
                { Response(Status.UNAUTHORIZED) },  // No session cookie
                { session ->
                    auth.sessionLogin(session, getUserBySession).fold(
                        { Response(Status.UNAUTHORIZED) }, // Session not found
                        {
                            val loginResponse = onLoginResponse ?: next

                            loginResponse(req).cookie(cookieFactory.create(session))
                        }
                    )
                }
            )
        } }
    }
}
