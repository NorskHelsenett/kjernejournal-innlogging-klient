package no.helse.kj.devepj.pages

import arrow.core.flatMap
import io.javalin.Javalin
import io.javalin.http.Context
import no.helse.kj.devepj.helseid.HelseIdClient
import no.helse.kj.devepj.helseid.getHelseIdConfigurationFromContext
import no.helse.kj.devepj.helseid.getHelseIdTokensFromContext
import no.helse.kj.devepj.helseid.hentTokensMedRefreshTokenGrant
import no.helse.kj.devepj.kjinnlogging.KJERNEJOURNAL_SESSION_REFERENCE_SESSION_KEY
import no.helse.kj.devepj.kjinnlogging.KjernejournalSessionReference
import no.helse.kj.devepj.kjinnlogging.endKjernejournalSessionWithContext
import no.helse.kj.devepj.kjinnlogging.getKjernejournalSatusWithContext
import no.helse.kj.devepj.kjinnlogging.getKjernejournalSessionReferenceFromContext
import no.helse.kj.devepj.kjinnlogging.refreshSession
import no.helse.kj.devepj.logging.logToEventStore
import no.helse.kj.devepj.dto.getHprAutorisasjonFromContext
import no.helse.kj.devepj.dto.getUserPidFromContext
import no.helse.kj.devepj.kjinnlogging.KjernejournalConfiguration

object AktivSesjon {
  fun register(app: Javalin) {
    app.get("/sesjon") { view(it) }
    app.beforeMatched("/sesjon") {
      if (it.sessionAttribute<KjernejournalSessionReference>(KJERNEJOURNAL_SESSION_REFERENCE_SESSION_KEY) == null) it.redirect(
        "/pasientvalg"
      )
    }
    app.get("/sesjon/loggut") { loggUtBruker(it) }
    app.get("/sesjon/byttpasient") { byttPasient(it) }
    app.post("/sesjon/holdsesjon") { holdSesjon(it) }
  }

  private fun holdSesjon(ctx: Context) {
    val attest = ctx.sessionAttribute<Any>("patient/attest")

    getHelseIdConfigurationFromContext(ctx).flatMap { helseIdConfig ->
      getHelseIdTokensFromContext(ctx).flatMap { tokens ->
        hentTokensMedRefreshTokenGrant(
          helseIdConfig,
          tokens.refreshToken,
          attest
        )
      }
    }.flatMap { tokens ->
      KjernejournalConfiguration.fromContext(ctx)
        .flatMap { config ->
          getKjernejournalSessionReferenceFromContext(ctx)
            .flatMap { sessionReference -> refreshSession(config, sessionReference, tokens.accessToken) }
        }
    }
      .onLeft { visError(ctx, it, "Hold sesjon") }
      .onRight { lifetime ->
        logToEventStore(ctx, "Holder Kjernejournal innloggingsesjon i live")
        ctx.json(
          mapOf(
            Pair("status", "ok"),
            Pair("levetid", lifetime)
          )
        )
      }
  }

  private fun view(ctx: Context) {
    val patientPid = ctx.sessionAttribute<String>("patient/pid")

    if (patientPid == null) {
      visError(ctx, EpjError("Mangler pasient", 400, "Mangler aktiv pasient"))
      return
    }

    getHelseIdTokensFromContext(ctx).flatMap { tokens ->
      getKjernejournalSatusWithContext(ctx, patientPid, tokens).flatMap { indikator ->
        getKjernejournalSessionReferenceFromContext(ctx)
          .onRight { sessionReference ->
            ctx.render(
              "/templates/aktivsesjon.ftl", mapOf(
                Pair("virksomhet", HelseIdClient.fromContext(ctx).getOrNull()?.name ?: "Ukjent"),
                Pair("bruker", getUserPidFromContext(ctx).getOrNull() ?: "Ukjent"),
                Pair("kjernejournalUrl", sessionReference.loginUri),
                Pair("miljo", KjernejournalConfiguration.fromContext(ctx).getOrNull()?.name ?: "Ukjent"),
                Pair("pasient", ctx.sessionAttribute<String>("patient/pid")),
                Pair("grunnlag", ctx.sessionAttribute<Any>("patient/grunnlag").toString()),
                Pair("indikator", indikator),
                Pair("autorisasjon", getHprAutorisasjonFromContext(ctx).getOrNull() ?: "Ukjent")
              )
            )
          }
      }
    }.onLeft { visError(ctx, it, "Aktiv sesjon") }
  }

  private fun byttPasient(ctx: Context) {
    ctx.sessionAttribute("patient/pid", null)
    ctx.redirect("/pasientvalg")
  }

  private fun loggUtBruker(ctx: Context) {
    getHelseIdTokensFromContext(ctx).flatMap { tokens ->
      endKjernejournalSessionWithContext(ctx, tokens)
        .onRight { ctx.redirect("/logout") }
    }
  }
}
