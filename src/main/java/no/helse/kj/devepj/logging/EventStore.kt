package no.helse.kj.devepj.logging

import io.javalin.http.Context
import io.javalin.http.sse.SseClient
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Queue
import java.util.concurrent.ArrayBlockingQueue
import java.util.function.Consumer


const val EVENT_STORE_SESSION_KEY = "eventstore"
fun getEventStorefromContext(ctx: Context): EventStore {
  var eventStore = ctx.sessionAttribute<EventStore>(EVENT_STORE_SESSION_KEY)

  if (eventStore == null) {
    eventStore = EventStore()
    ctx.sessionAttribute(EVENT_STORE_SESSION_KEY, eventStore)
  }

  return eventStore
}

fun logToEventStore(context: Context, message: String) {
  getEventStorefromContext(context).logEvent(message)
}

class EventStore {
  private val buffer = CircularStringBuffer(100)
  private val clients: Queue<SseClient> = ArrayBlockingQueue(10)

  fun addClient(client: SseClient) {
    try {
      clients.add(client)

      client.sendEvent("connected", "Receiving logs...")
      buffer.forEach { message: String? -> client.sendEvent(SSE_EVENT, message!!) }
      client.onClose { clients.remove(client) }
    } catch (e: IllegalStateException) {
      client.sendEvent("error", "Too many clients connected")
    }
  }

  fun logEvent(message: String) {
    val event = ZonedDateTime.now(ZoneId.of("Europe/Oslo")).format(DATEFORMAT) + " " + message
    buffer.add(event)
    publish(event)
  }

  fun publish(message: String?) {
    clients.forEach(Consumer { client: SseClient -> client.sendEvent(SSE_EVENT, message!!) })
  }

  companion object {
    private const val SSE_EVENT = "event"
    private val DATEFORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss")
  }
}
