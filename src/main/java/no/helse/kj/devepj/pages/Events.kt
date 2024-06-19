package no.helse.kj.devepj.pages

import io.javalin.Javalin
import io.javalin.http.Context
import io.javalin.http.sse.SseClient
import no.helse.kj.devepj.logging.getEventStorefromContext

object Events {
  fun register(app: Javalin) {
    app.get("/events") { view(it) }
    app.sse("/events/source") { handleClient(it) }
  }

  private fun view(context: Context) {

    context.render(
      "/templates/events.ftl", mapOf(
        Pair("virksomhet", context.sessionAttribute<Any>("klient").toString()),
      )
    )
  }

  private fun handleClient(client: SseClient) {
    client.keepAlive()
    getEventStorefromContext(client.ctx()).addClient(client)
  }
}
