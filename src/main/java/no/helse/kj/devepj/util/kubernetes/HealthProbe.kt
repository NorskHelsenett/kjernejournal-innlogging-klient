package no.helse.kj.devepj.util.kubernetes

import io.javalin.Javalin
import io.javalin.config.JavalinConfig
import io.javalin.http.Context
import io.javalin.util.JavalinException

val READY_PATH = "/readyz"
val ALIVE_PATH = "/alivez"

class HealthProbe(private val port: Int, livenessCriteria: () -> Boolean = { true }) {
  private var isAlive = false
  private var isReady = false
  val app = Javalin.create { javalinConfig: JavalinConfig -> javalinConfig.showJavalinBanner = false }
    .get(READY_PATH) { context: Context -> context.status(if (isReady && livenessCriteria()) 200 else 503) }
    .get(ALIVE_PATH) { context: Context -> context.status(if (isAlive && livenessCriteria()) 200 else 503) }

  fun probe(service: Runnable) {
    start()
    markAlive()
    service.run()
    markReady()
  }

  private fun start() {
    try {
      app.start(port)
    } catch (e: JavalinException) {
      // Har startet allerede, ofte i test
      return
    }
  }

  private fun markAlive() {
    isAlive = true
  }

  private fun markReady() {
    isReady = true
  }
}