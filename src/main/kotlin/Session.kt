import arrow.core.Option
import arrow.core.toOption
import org.http4k.core.Request
import org.http4k.core.cookie.Cookie
import org.http4k.core.cookie.SameSite
import org.http4k.core.cookie.cookie
import types.auth.Session
import java.util.*

class SessionCookieFactory(
    val cookieName: String = "session",
    private val ttl: Long = 60 * 60 * 24 * 7 * 30,
) {
    fun create(
        session: Session,
        ttl: Long = this.ttl,
        httpOnly: Boolean = true
    ): Cookie =
        Cookie(
            name = cookieName,
            value = session.value,
            maxAge = ttl,
            secure = true,
            httpOnly = httpOnly,
            sameSite = SameSite.Lax
        )

    fun destroy() = Cookie(
        name = cookieName,
        value = "",
        maxAge = -1,
        secure = true,
        sameSite = SameSite.Lax
    )
}

fun Request.sessionCookie(name: String = "session"): Cookie? = this.cookie(name)

fun Cookie.asSession() = Session(this.value)

fun Request.asSession(name: String = "session"): Option<Session> = this
    .sessionCookie(name)
    .toOption()
    .map { it.asSession() }

fun UUID.asSession() = Session(this.toString())

fun generateSession() = UUID
    .randomUUID()
    .asSession()
