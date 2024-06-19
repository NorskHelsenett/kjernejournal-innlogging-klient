package no.helse.kj.devepj.logging

import io.javalin.Javalin
import io.javalin.http.Context
import io.javalin.http.sse.SseClient
import io.javalin.security.RouteRole

object LogApi {
  fun init(app: Javalin, path: String, vararg roles: RouteRole) {
    app.sse("$path/source", { client: SseClient ->
      client.keepAlive()
      LogbackWebLogAppender.addClient(client)
    }, *roles)

    app.get(path, { ctx: Context ->
      ctx.contentType("text/html")
      LogbackWebLogAppender::class.java.getResourceAsStream("/web-log.html")?.let { ctx.result(it) }
    }, *roles)
  }
}
