package no.helse.kj.devepj

import io.javalin.Javalin
import io.javalin.http.Context
import io.javalin.http.ExceptionHandler
import no.helse.kj.devepj.pages.visFeilside
import org.slf4j.Logger
import org.slf4j.LoggerFactory

object Feilhandterer {
  private val log: Logger = LoggerFactory.getLogger(Feilhandterer::class.java)

  fun feilhandtering(app: Javalin) {
    app.exception(Exception::class.java, (ExceptionHandler { exception: Exception, ctx: Context ->
      try {
        log.error("Feil ved håndtering av request", exception)
        visFeilside(
          ctx,
          "KjernejournalEPJ",
          500,
          "Ukjent feil i dev-epj",
          exception.message!!,
          exception.stackTrace
        )
      } catch (e: Exception) {
        log.error("Feil ved under rendring av feil:", e)
        ctx.status(500)
      }
    }))
  }
}
