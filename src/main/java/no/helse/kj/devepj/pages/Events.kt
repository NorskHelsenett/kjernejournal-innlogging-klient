package no.helse.kj.devepj.pages

import io.javalin.Javalin
import io.javalin.http.Context
import io.javalin.http.sse.SseClient
import no.helse.kj.devepj.dto.getUserPidFromContext
import no.helse.kj.devepj.helseid.HelseIdClient
import no.helse.kj.devepj.kjinnlogging.KjernejournalConfiguration
import no.helse.kj.devepj.logging.getEventStorefromContext

object Events {
  fun register(app: Javalin) {
    app.get("/events") { view(it) }
    app.sse("/events/source") { handleClient(it) }
  }

  private fun view(context: Context) {

    context.render(
      "/templates/events.ftl", mapOf(
        Pair("bruker", getUserPidFromContext(context).getOrNull() ?: "Ukjent"),
        Pair("miljo", KjernejournalConfiguration.fromContext(context).getOrNull()?.name ?: "Ukjent"),
        Pair("virksomhet", HelseIdClient.fromContext(context).getOrNull()?.name ?: "Ukjent"),
      )
    )
  }

  private fun handleClient(client: SseClient) {
    client.keepAlive()
    getEventStorefromContext(client.ctx()).addClient(client)
  }
}
