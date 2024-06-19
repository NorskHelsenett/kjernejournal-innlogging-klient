package no.helse.kj.devepj.pages

import arrow.core.Either
import arrow.core.flatMap
import arrow.core.flatten
import arrow.core.left
import arrow.core.right
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.PropertyNamingStrategies
import io.javalin.Javalin
import io.javalin.http.Context
import java.util.UUID
import no.helse.kj.devepj.dto.Autorisasjon
import no.helse.kj.devepj.dto.EpjGrunnlag
import no.helse.kj.devepj.dto.getHprAutorisasjonFromContext
import no.helse.kj.devepj.dto.getUserPidFromContext
import no.helse.kj.devepj.helseid.HELSEID_TOKENS_SESSION_KEY
import no.helse.kj.devepj.helseid.HelseIdTokenBundle
import no.helse.kj.devepj.helseid.HelseIdClient
import no.helse.kj.devepj.helseid.refreshTokenWithContext
import no.helse.kj.devepj.kjinnlogging.KJERNEJOURNAL_SESSION_REFERENCE_SESSION_KEY
import no.helse.kj.devepj.kjinnlogging.KjernejournalConfiguration
import no.helse.kj.devepj.kjinnlogging.KjernejournalInnloggingGrunnlag
import no.helse.kj.devepj.kjinnlogging.createKjernejournalSessionWithContext
import no.helse.kj.devepj.logging.logToEventStore
import no.helse.kj.devepj.tillitsrammeverk.CareRelationship
import no.helse.kj.devepj.tillitsrammeverk.FellesTjenestetype
import no.helse.kj.devepj.tillitsrammeverk.Patient
import no.helse.kj.devepj.tillitsrammeverk.Practitioner
import no.helse.kj.devepj.tillitsrammeverk.PurposeOfUse
import no.helse.kj.devepj.tillitsrammeverk.PurposeOfUseDetails
import no.helse.kj.devepj.tillitsrammeverk.Tillitsrammeverk

private val objectMapper = ObjectMapper().setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE)
private val writer = objectMapper.writerWithDefaultPrettyPrinter()

// TODO: Gjør det tydeligere i skjermbildet på hvilke prosesser som skjer og hva de kan tilsvare i en EPJ
// TODO: Gjøre hendelser til "toasts" i stedet for en logg?
object Pasientvalg {
  fun register(app: Javalin) {
    app.get("/pasientvalg") { view(it) }
    app.beforeMatched("/pasientvalg") {
      if (it.sessionAttribute<HelseIdTokenBundle>(HELSEID_TOKENS_SESSION_KEY) == null) it.redirect(
        "/"
      )
    }
    app.post("/pasientvalg/velg") { handterValgtPasient(it) }
    app.get("/pasientvalg/loggut") { loggUtBruker(it) }
  }

  private fun view(ctx: Context) {
    val attestPurposeOfUSe = EpjGrunnlag.fraKode("UNNTAK")

    HelseIdClient.fromContext(ctx).map { client ->
      getHprAutorisasjonFromContext(ctx).flatMap { authorization ->
        lagTillitsrammeverk(
          client,
          FellesTjenestetype.BEDRIFT, // Hardkoder, bør bli nedtrekk i frontend
          authorization,
          attestPurposeOfUSe.purposeOfUse,
          attestPurposeOfUSe.purposeOfUseDetails
        )
      }
    }.flatten()
      .flatMap { attest ->
        try {
          writer.writeValueAsString(attest).right()
        } catch (e: Exception) {
          EpjError("Kan ikke parse tillitsrammeverk JSON", 200, e.message, e).left()
        }
      }
      .onRight { internalView(ctx, it, false) }
      .onLeft { visError(ctx, it, "Pasientvalg") }
  }

  private fun internalView(ctx: Context, attestJson: String?, attestFeil: Boolean) {
    ctx.render(
      "/templates/pasientvalg.ftl", mapOf(
        Pair("bruker", getUserPidFromContext(ctx).getOrNull() ?: "Ukjent"),
        Pair("virksomhet", HelseIdClient.fromContext(ctx).getOrNull()?.name ?: "Ukjent"),
        Pair("autorisasjon", getHprAutorisasjonFromContext(ctx).getOrNull() ?: "Ukjent"),
        Pair("miljo", KjernejournalConfiguration.fromContext(ctx).getOrNull()?.name ?: "Ukjent"),
        Pair("attest", attestJson),
        Pair("attestFeil", attestFeil),
      )
    )
  }

  private fun handleAttest(rawAttest: String?): Either<EpjError, Any> {
    return try {
      objectMapper.readValue(rawAttest, Any::class.java).right()
    } catch (e: Exception) {
      return EpjError("Feil format på tillitsrammeverk", 500, e.message, e).left()
    }
  }

  private fun handterValgtPasient(ctx: Context) {
    val pasient = ctx.formParam("pasient").also { ctx.sessionAttribute("patient/pid", it) }
    val grunnlag = KjernejournalInnloggingGrunnlag.fraKode(ctx.formParam("access_basis"))
      .also { ctx.sessionAttribute("patient/grunnlag", it) }

    if (pasient.isNullOrBlank()) {
      visError(ctx, EpjError("Feil i pasientvalg", 500, "Mangler pasient-id"))
      return
    }

    handleAttest(ctx.formParam("attest"))
      .onLeft {
        logToEventStore(ctx, "Feil format på tillitsrammeverk")
        internalView(ctx, ctx.formParam("attest"), true)
        return
      }
      .onRight { ctx.sessionAttribute("patient/attest", it) }
      // TODO: Legg til organisasjon-informasjon som authorization details
      .flatMap { attest -> refreshTokenWithContext(ctx, attest) }
      .flatMap { tokens -> createKjernejournalSessionWithContext(ctx, tokens, pasient, grunnlag) }
      .onLeft { visError(ctx, it, "Pasientvalg") }
      .onRight {
        ctx.sessionAttribute(KJERNEJOURNAL_SESSION_REFERENCE_SESSION_KEY, it)
        ctx.redirect("/sesjon")
      }
  }
}

private fun loggUtBruker(ctx: Context) {
  ctx.redirect("/logout")
}

private fun lagTillitsrammeverk(
  klient: HelseIdClient,
  tjenestetype: FellesTjenestetype,
  autorization: Autorisasjon,
  purposeOfUse: PurposeOfUse,
  purposeOfUseDetails: PurposeOfUseDetails,
): Either<EpjError, Tillitsrammeverk> {
  try {
    return Tillitsrammeverk.Builder()
      .withPractitioner(
        Practitioner.Builder()
          .withLegalEntity(klient.parentOrg)
          .withPointOfCare(klient.childOrg)
          .withAuthorization(autorization)
          .build()
      )
      .withCareRelationship(
        CareRelationship.Builder()
          .withPurposeOfUse(purposeOfUse)
          .withPurposeOfUseDetails(purposeOfUseDetails)
          .withDecisionRef(UUID.randomUUID().toString(), true)
          .withHealthcareService(tjenestetype)
          .build()
      )
      .withPatient(
        Patient.Builder()
          .withPointOfCare(klient.childOrg)
          .build()
      )
      .build()
      .right()
  } catch (e: Exception) {
    return EpjError("Kan ikke bygge attest", 500, e.message, e).left()
  }
}
