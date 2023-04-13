import org.http4k.core.*
import org.http4k.routing.*
import org.http4k.server.*
import org.http4k.filter.*
import types.user.UserCredentials


val database = mutableMapOf<Long, UserCredentials>()
val sessionDatabase = mutableMapOf<String, Long>()


val routing: HttpHandler = routes(
    "/ping" bind Method.GET to {
        Response(Status.OK).body("pong")
    }
)

fun main(args: Array<String>) {
    val app: HttpHandler = DebuggingFilters.PrintRequest().then(routing)
    val server = app.asServer(SunHttp(9000)).start()
    println("Server started on ${server.port()}")
}
