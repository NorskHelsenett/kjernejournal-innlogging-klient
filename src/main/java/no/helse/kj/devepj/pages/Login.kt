package no.helse.kj.devepj.pages

import arrow.core.flatMap
import io.javalin.Javalin
import io.javalin.http.Context
import no.helse.kj.devepj.helseid.HELSEID_CLIENT_SESSION_KEY
import no.helse.kj.devepj.helseid.HELSEID_CONFIGURATION_SESSION_KEY
import no.helse.kj.devepj.helseid.HELSEID_PAR_SESSION_KEY
import no.helse.kj.devepj.helseid.HELSEID_TOKENS_SESSION_KEY
import no.helse.kj.devepj.helseid.HelseIdConfiguration
import no.helse.kj.devepj.helseid.HelseIdParResponse
import no.helse.kj.devepj.helseid.getHelseIdConfigurationFromContext
import no.helse.kj.devepj.helseid.hentInnloggetBruker
import no.helse.kj.devepj.helseid.hentTokensMedAuthorizationCodeGrant
import no.helse.kj.devepj.helseid.logoutHelseIdWithContext
import no.helse.kj.devepj.helseid.pushedAuthenticationRequest
import no.helse.kj.devepj.kjinnlogging.KJERNEJOURNAL_CONFIGURATION_SESSION_KEY
import no.helse.kj.devepj.logging.logToEventStore
import no.helse.kj.devepj.dto.Autorisasjon
import no.helse.kj.devepj.dto.HPR_AUTORISASJON_SESSION_KEY
import no.helse.kj.devepj.dto.USER_PID_SESSION_KEY
import no.helse.kj.devepj.helseid.HelseIdClient
import no.helse.kj.devepj.kjinnlogging.KjernejournalConfiguration
import no.helse.kj.devepj.util.Config
import no.helse.kj.devepj.util.endringerTilFrontend

private const val STATE = "state"
private const val CODE = "code"

class Login(val config: Config) {
  fun register(app: Javalin) {
    app.get("/login", ::view)
    app.post("/api/login", ::loggInn)
    app.get("/api/callback", ::handterCallbackFraHelseID)
    app.get("/logout", ::loggUt)
  }

  private fun view(context: Context) {
    val endringsfil = endringerTilFrontend()
    context.render(
      "/templates/login.ftl",
      mapOf(
        Pair("klienter", HelseIdClient.toFrontend()),
        Pair("autorisasjoner", Autorisasjon.tilFrontend()),
        Pair("endringer", endringsfil.endringer),
        Pair("endringsid", endringsfil.hash),
        Pair("miljo", KjernejournalConfiguration.toFrontend())
      )
    )
  }

  private fun loggInn(ctx: Context) {
    // Lagrer autorisasjon på sesjon, brukes i attest
    ctx.formParam("autorisasjon")
      .let { Autorisasjon.fraKode(it, defaultValue = Autorisasjon.LE) }
      .also { ctx.sessionAttribute(HPR_AUTORISASJON_SESSION_KEY, it) }

    // Lagrer kjernejournal konfigurasjon på sesjon, brukes ved oppretting av sesjon i kj
    ctx.formParam("miljo")
      .let { KjernejournalConfiguration.fromId(it) }
      .also { ctx.sessionAttribute(KJERNEJOURNAL_CONFIGURATION_SESSION_KEY, it) }

    val helseIdClient = ctx.formParam("klient")
      .let { HelseIdClient.fromId(it) }
      .also { ctx.sessionAttribute(HELSEID_CLIENT_SESSION_KEY, it) }

    val helseIdConfiguration = HelseIdConfiguration(
      stsUrl = config.helseIdStsUri,
      redirectUri = config.helseIdRedirectUri,
      logoutUri = config.helseIdLogoutUri,
      scopes = config.helseIdScopes,
      resourceIndicators = config.helesIdResourceIndicators,
      clientId = helseIdClient.id,
      privateKeyJwk = helseIdClient.jwk
    ).also { ctx.sessionAttribute(HELSEID_CONFIGURATION_SESSION_KEY, it) }

    logToEventStore(
      ctx,
      "Logger inn i HelseID med ${helseIdClient.childOrg.name}[${helseIdClient.childOrg.id}] (Overenhet: ${helseIdClient.parentOrg.name}[${helseIdClient.parentOrg.id}])"
    )

    ctx.formParam("bruker")
      .also { brukerFnr ->
        pushedAuthenticationRequest(
          helseIdConfiguration,
          brukerFnr
        )
          .onLeft { visError(ctx, it, "Innlogging") }
          .onRight {
            ctx.sessionAttribute(HELSEID_PAR_SESSION_KEY, it)
            ctx.redirect(it.redirectURI.toString())
          }
      }
  }

  private fun handterCallbackFraHelseID(ctx: Context) {
    val parResponse = ctx.sessionAttribute<HelseIdParResponse>(HELSEID_PAR_SESSION_KEY)
    if (parResponse == null) {
      visError(
        ctx,
        EpjError("Innlogging med HelseID feilet", 500, "Mangler refereanse til PAR på sesjon", null),
        "Innlogging"
      )
      return
    }

    getHelseIdConfigurationFromContext(ctx)
      .flatMap { hentTokensMedAuthorizationCodeGrant(it, parResponse, ctx.queryParam(CODE), ctx.queryParam(STATE)) }
      .onRight { ctx.sessionAttribute(HELSEID_TOKENS_SESSION_KEY, it) }
      .flatMap { hentInnloggetBruker(it.accessToken) }
      .onLeft { visError(ctx, it, "Innlogging") }
      .onRight { logToEventStore(ctx, "Logget inn i HelseID som bruker ${it.navn} (HRPNR: ${it.hprNr})") }
      .onRight {
        ctx.sessionAttribute(USER_PID_SESSION_KEY, it.pid)
        ctx.redirect("/pasientvalg")
      }
  }

  private fun loggUt(ctx: Context) {
    logoutHelseIdWithContext(ctx).onRight {
      logToEventStore(ctx, "Avslutter sesjon mot HelseID")
      ctx.req().session.invalidate()
      ctx.redirect(it.toString())
    }.onLeft { visError(ctx, it, "Utlogging fra HelseID") }
  }
}
