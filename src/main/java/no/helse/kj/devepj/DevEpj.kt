package no.helse.kj.devepj

import com.fasterxml.jackson.databind.ObjectMapper
import io.javalin.Javalin
import io.javalin.http.Context
import io.javalin.json.JavalinJackson
import io.javalin.rendering.template.JavalinFreemarker
import no.helse.kj.devepj.helseid.getHelseIdTokensFromContext
import no.helse.kj.devepj.logging.LogApi
import no.helse.kj.devepj.pages.AktivSesjon
import no.helse.kj.devepj.pages.Events
import no.helse.kj.devepj.pages.Login
import no.helse.kj.devepj.pages.Pasientvalg
import no.helse.kj.devepj.util.Config
import no.helse.kj.devepj.util.ConfigBuilder
import no.helse.kj.devepj.util.kubernetes.HealthProbe

fun main() {
  val config = ConfigBuilder("/application.properties").toConfig()
  HealthProbe(5000).probe { start(config) }
}

fun erBrukerLoggetInn(ctx: Context): Boolean {
  return getHelseIdTokensFromContext(ctx).isRight()
}

fun start(config: Config) {
  Javalin
    .create {
      it.fileRenderer(JavalinFreemarker())
      it.jsonMapper(JavalinJackson(ObjectMapper()))
      it.staticFiles.add { staticConfig ->
        staticConfig.hostedPath = "/assets"
        staticConfig.directory = "assets"
      }
    }
    .get("/") { ctx ->
      if (erBrukerLoggetInn(ctx))
        ctx.redirect("/pasientvalg")
      else
        ctx.redirect("/login")
    }
    .also { Feilhandterer.feilhandtering(it) }
    .also { Login(config).register(it) }
    .also { Pasientvalg.register(it) }
    .also { AktivSesjon.register(it) }
    .also { Events.register(it) }
    .also { if (config.weblogsActive) LogApi.init(it, "logs") }
    .also { it.start(8888) }
}