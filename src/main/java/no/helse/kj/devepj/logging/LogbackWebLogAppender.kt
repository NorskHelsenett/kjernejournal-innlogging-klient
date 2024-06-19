package no.helse.kj.devepj.logging

import ch.qos.logback.classic.spi.LoggingEvent
import ch.qos.logback.classic.spi.StackTraceElementProxy
import ch.qos.logback.core.AppenderBase
import ch.qos.logback.core.encoder.Encoder
import io.javalin.http.sse.SseClient
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Arrays
import java.util.Queue
import java.util.concurrent.ArrayBlockingQueue
import java.util.function.Consumer
import java.util.stream.Collectors
import java.util.stream.Stream

class LogbackWebLogAppender<E : LoggingEvent?> : AppenderBase<E>() {
  var encoder: Encoder<E>? = null
  override fun append(event: E) {
    val formattedLog = format(event)
    buffer.add(formattedLog)
    publish(formattedLog)
  }

  override fun start() {
    super.start()
    if (this.encoder == null) {
      buffer.add(
        "LogbackWebLogAppender støtter nå encoder! \nTa en titt her https://helsegitlab.nhn.no/samhandlingsplattform/kjernejournal-portal/biblioteker/javalin-web-log\n\n"
      )
    }
  }

  private fun formatError(loggingEvent: E): String {
    val throwableProxy = loggingEvent?.throwableProxy ?: return ""

    val cause = """
        
        ${throwableProxy.className}: ${throwableProxy.message}
        
        """.trimIndent()
    val stackTrace = Arrays.stream(throwableProxy.stackTraceElementProxyArray)
      .map { obj: StackTraceElementProxy -> obj.stackTraceElement }
      .map { st: StackTraceElement -> "    at " + st.className + "." + st.methodName + "(" + st.fileName + ":" + st.lineNumber + ")" }
      .collect(Collectors.joining("\n"))

    return cause + stackTrace
  }

  private fun format(loggingEvent: E): String {
    if (this.encoder == null) {
      return formatLogEntry(loggingEvent)
    }

    return String(encoder!!.encode(loggingEvent))
  }

  private fun formatLogEntry(loggingEvent: E): String {
    val logEntry = loggingEvent?.formattedMessage + formatError(loggingEvent)

    return Stream.of(
      ZonedDateTime
        .now(ZoneId.of("Europe/Oslo"))
        .format(DATEFORMAT),
      logEntry.replace("\n", "\\n")
    )
      .collect(Collectors.joining(" "))
  }

  companion object {
    private val DATEFORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss")
    var buffer: CircularStringBuffer = CircularStringBuffer(100)
    var clients: Queue<SseClient> = ArrayBlockingQueue(100)

    fun addClient(client: SseClient) {
      if (!clients.add(client)) {
        client.sendEvent("error", "Too many clients connected")
      } else {
        client.sendEvent("connected", "Receiving logs...")
        buffer.forEach { log: String? -> client.sendEvent("log", log!!) }
        client.onClose { clients.remove(client) }
      }
    }

    fun publish(log: String?) {
      clients.forEach(Consumer { client: SseClient -> client.sendEvent("log", log!!) })
    }
  }
}
