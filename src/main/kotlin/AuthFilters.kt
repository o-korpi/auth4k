package auth4k

import arrow.core.Either
import org.http4k.core.Filter
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status
import org.http4k.core.cookie.cookie
import auth4k.types.auth.Session
import auth4k.types.user.UserEntity

object AuthFilters {
    object SessionAuth {
        operator fun <User : UserEntity> invoke(
            auth: Authentication<User>,
            cookieFactory: SessionCookieFactory = SessionCookieFactory(),
            exemptRoutes: Set<String>,
            getUserBySession: (Session) -> Either<SessionException, User>,
            onLoginResponse: ((Request) -> Response)? = null
        ) = Filter { next -> { req ->

            if (exemptRoutes.contains(req.uri.path)) {
                next(req)
            } else {
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
            }
        } }

        operator fun <User : UserEntity> invoke(
            auth: Authentication<User>,
            redirectRoute: String,
            cookieFactory: SessionCookieFactory = SessionCookieFactory(),
            exemptRoutes: Set<String>,
            getUserBySession: (Session) -> Either<SessionException, User>,
            onLoginResponse: ((Request) -> Response)? = null
        ) = Filter { next -> { req ->
            if (exemptRoutes.contains(req.uri.path)) {
                next(req)
            } else {
                val redirectResponse = Response(Status.SEE_OTHER).header("location", redirectRoute)

                req.asSession(cookieFactory.cookieName).fold(
                    { redirectResponse },  // No session cookie
                    { session ->
                        auth.sessionLogin(session, getUserBySession).fold(
                            { redirectResponse }, // Session not found
                            {
                                val loginResponse = onLoginResponse ?: next

                                loginResponse(req).cookie(cookieFactory.create(session))
                            }
                        )
                    }
                )
            }
        } }
    }
}
