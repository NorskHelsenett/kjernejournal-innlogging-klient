package no.helse.kj.devepj.kjinnlogging

import arrow.core.Either
import arrow.core.flatMap
import arrow.core.left
import arrow.core.right
import com.fasterxml.jackson.databind.ObjectMapper
import com.nimbusds.oauth2.sdk.pkce.CodeChallenge
import com.nimbusds.oauth2.sdk.pkce.CodeChallengeMethod
import com.nimbusds.oauth2.sdk.pkce.CodeVerifier
import com.nimbusds.oauth2.sdk.token.AccessToken
import com.nimbusds.oauth2.sdk.token.DPoPAccessToken
import io.javalin.http.Context
import mu.KotlinLogging
import no.helse.kj.devepj.dto.Autorisasjon
import no.helse.kj.devepj.dto.getHprAutorisasjonFromContext
import no.helse.kj.devepj.helseid.HelseIdTokenBundle
import no.helse.kj.devepj.helseid.createDPoPProof
import no.helse.kj.devepj.kjinnlogging.dto.*
import no.helse.kj.devepj.pages.EpjError
import org.eclipse.jetty.http.HttpMethod
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration

private val log = KotlinLogging.logger {}

private const val HEADER_SOURCE_SYSTEM = "X-SOURCE-SYSTEM"

private const val HEADER_AUTHORIZATION = "Authorization"
private const val HEADER_CONTENT_TYPE = "Content-Type"

private const val HELSEINDIKATOR_FEILSTATUS = 0

private const val HEADER_DPOP = "DPoP"
private const val SOURCE_SYSTEM_VALUE = "Kjernejournal EPJ, v0.1.0"
private const val F_NUMMER_SYSTEM_KODE = "urn:oid:2.16.578.1.12.4.1.4.1"
private const val AUTORIATIV_KILDE_SKATTEETATEN = "https://www.skatteetaten.no"
private val mapper = ObjectMapper()
private val client: HttpClient = HttpClient.newBuilder()
  .version(HttpClient.Version.HTTP_1_1)
  .followRedirects(HttpClient.Redirect.NORMAL)
  .connectTimeout(Duration.ofSeconds(20))
  .build()

const val KJERNEJOURNAL_SESSION_REFERENCE_SESSION_KEY = "kjernejournal/session"
fun getKjernejournalSessionReferenceFromContext(ctx: Context): Either<EpjError, KjernejournalSessionReference> {
  val sessionReference =
    ctx.sessionAttribute<KjernejournalSessionReference>(KJERNEJOURNAL_SESSION_REFERENCE_SESSION_KEY)
      ?: return EpjError(
        "Feil i sesjon",
        500,
        "Mangler referense til kjernejournal sesjon på lokal sesjon",
        null
      ).left()

  return sessionReference.right()
}

fun createKjernejournalSessionWithContext(
  ctx: Context,
  tokens: HelseIdTokenBundle,
  patientId: String,
  accessBasis: KjernejournalInnloggingGrunnlag,
): Either<EpjError, KjernejournalSessionReference> {
  return KjernejournalConfiguration.fromContext(ctx).flatMap { config ->
    getHprAutorisasjonFromContext(ctx).flatMap { autorization ->
      createKjernejournalSession(
        config,
        patientId,
        accessBasis,
        autorization,
        tokens.accessToken
      )
    }
  }
}

data class KjernejournalSessionReference(val sessionId: String, val loginUri: URI, val logoutURI: URI)

fun createKjernejournalSession(
  configuration: KjernejournalConfiguration,
  pasientId: String,
  grunnlag: KjernejournalInnloggingGrunnlag,
  autorisasjon: Autorisasjon,
  token: DPoPAccessToken,
): Either<EpjError, KjernejournalSessionReference> {
  try {
    val createUri = URI.create("${configuration.innloggingApiUri}/api/session/create")
    val dpopProof = createDPoPProof(HttpMethod.POST, createUri, token)
    val codeVerifier = CodeVerifier()

    val response: HttpResponse<String> = client.send<String>(
      HttpRequest.newBuilder()
        .uri(createUri)
        .timeout(Duration.ofMinutes(2))
        .header(HEADER_SOURCE_SYSTEM, SOURCE_SYSTEM_VALUE)
        .header(HEADER_AUTHORIZATION, token.toAuthorizationHeader())
        .header(HEADER_CONTENT_TYPE, "application/json")
        .header(HEADER_DPOP, dpopProof)
        .POST(
          HttpRequest.BodyPublishers.ofString(
            mapper.writeValueAsString(
              CreateSessionRequest(
                CodeChallenge.compute(CodeChallengeMethod.S256, codeVerifier).getValue(),
                mapOf(
                  Pair(
                    "patient_identifier",
                    mapOf(
                      Pair("id", pasientId),
                      Pair("system", F_NUMMER_SYSTEM_KODE),
                      Pair("authority", AUTORIATIV_KILDE_SKATTEETATEN)
                    )
                  ),

                  Pair(
                    "practitioner_authorization",
                    mapOf(
                      Pair("code", autorisasjon.name),
                      Pair("system", "urn:oid:2.16.578.1.12.4.1.1.9060"),
                      Pair("assigner", "https://www.helsedirektoratet.no/")
                    )
                  ),
                  Pair(
                    "access_basis",
                    mapOf(
                      Pair("code", grunnlag.name),
                      Pair("system", "SYSTEM_GRUNNLAG"),
                      Pair("assigner", "https://nhn.no")
                    )
                  )
                )
              )
            )
          )
        )
        .build(),
      HttpResponse.BodyHandlers.ofString()
    )

    if (response.statusCode() >= 300) {
      return EpjError(
        "Feil under innlogging i kjernejournal",
        response.statusCode(),
        response.body(),
        cause = null
      ).left()
    }

    val createSessionResponse: CreateSessionResponse =
      mapper.readValue(response.body(), CreateSessionResponse::class.java)

    return KjernejournalSessionReference(
      createSessionResponse.sessionId,
      URI.create("${configuration.webUri}/hentpasient.html?code=" + createSessionResponse.code + "&ehr_code_verifier=" + codeVerifier.getValue()),
      URI.create("${configuration.webUri}/loggut")
    ).right()
  } catch (e: Exception) {
    return EpjError("Intern feil under innlogging i kjernejournal", 500, e.message, e).left()
  }
}

fun refreshKjernejournalSessionWithContext(
  ctx: Context,
  tokens: HelseIdTokenBundle,
): Either<EpjError, Long> {
  return KjernejournalConfiguration.fromContext(ctx).flatMap { config ->
    getKjernejournalSessionReferenceFromContext(ctx).flatMap { sessionRef ->
      refreshSession(config, sessionRef, tokens.accessToken)
    }
  }
}

fun refreshSession(
  configuration: KjernejournalConfiguration,
  kjernejournalSessionReference: KjernejournalSessionReference,
  token: DPoPAccessToken,
): Either<EpjError, Long> {
  try {
    val refreshUri = URI.create("${configuration.innloggingApiUri}/api/session/refresh")
    val dpopProof = createDPoPProof(HttpMethod.POST, refreshUri)
    val response: HttpResponse<String> = client.send(
      HttpRequest.newBuilder()
        .uri(refreshUri)
        .timeout(Duration.ofMinutes(2))
        .header(HEADER_SOURCE_SYSTEM, SOURCE_SYSTEM_VALUE)
        .header(HEADER_AUTHORIZATION, token.toAuthorizationHeader())
        .header(HEADER_DPOP, dpopProof)
        .POST(
          HttpRequest.BodyPublishers.ofString(
            mapper.writeValueAsString(RefreshSessionRequest(kjernejournalSessionReference.sessionId))
          )
        )
        .build(), HttpResponse.BodyHandlers.ofString()
    )

    if (response.statusCode() >= 300) {
      return EpjError("Feil under oppdatering av sesjon i kjernejournal", response.statusCode(), response.body()).left()
    }
    return token.lifetime.right()
  } catch (e: Exception) {
    return EpjError("Intern feil under oppdatering av sesjon i kjernejournal", 500, e.message, e).left()
  }
}


fun endKjernejournalSessionWithContext(
  ctx: Context,
  tokens: HelseIdTokenBundle,
): Either<EpjError, Boolean> {
  return KjernejournalConfiguration.fromContext(ctx).flatMap { config ->
    getKjernejournalSessionReferenceFromContext(ctx).flatMap { sessionRef ->
      endSession(config, sessionRef, tokens.accessToken)
    }
  }
}

fun endSession(
  configuration: KjernejournalConfiguration,
  kjernejournalSessionReference: KjernejournalSessionReference,
  token: DPoPAccessToken,
): Either<EpjError, Boolean> {
  try {
    val endUri = URI.create("${configuration.innloggingApiUri}/api/session/end")
    val dpopProof = createDPoPProof(HttpMethod.POST, endUri)
    val response: HttpResponse<String> = client.send(
      HttpRequest.newBuilder()
        .uri(endUri)
        .timeout(Duration.ofMinutes(2))
        .header(HEADER_SOURCE_SYSTEM, SOURCE_SYSTEM_VALUE)
        .header(HEADER_AUTHORIZATION, token.toAuthorizationHeader())
        .header(HEADER_DPOP, dpopProof)
        .POST(
          HttpRequest.BodyPublishers.ofString(
            mapper.writeValueAsString(RefreshSessionRequest(kjernejournalSessionReference.sessionId))
          )
        )
        .build(), HttpResponse.BodyHandlers.ofString()
    )

    if (response.statusCode() >= 300) {
      return EpjError("Feil under avslutting av sesjon", response.statusCode(), response.body()).left()
    }
    return true.right()
  } catch (e: Exception) {
    return EpjError("Intern feil under avslutting av sesjon", 500, e.message, e).left()
  }
}

fun getKjernejournalSatusWithContext(
  ctx: Context,
  patientId: String,
  tokens: HelseIdTokenBundle,
): Either<EpjError, Int> {
  return KjernejournalConfiguration.fromContext(ctx).flatMap { config ->
    getKjernejournalStatus(
      config,
      patientId,
      tokens.accessToken
    )
  }
}

/**
 * Henter indikator basert på dokumentasjon
 * https://kjernejournal.atlassian.net/wiki/spaces/KJERNEJOURDOK1/pages/664634018/Integration+Guide+Kjernejournal+Portal
 *
 * @return
 */
fun getKjernejournalStatus(
  configuration: KjernejournalConfiguration,
  patientId: String,
  token: AccessToken,
): Either<EpjError, Int> {
  try {
    val response: HttpResponse<String> = client.send(
      HttpRequest.newBuilder()
        .uri(configuration.statusApiUri)
        .header(HEADER_AUTHORIZATION, "Bearer ${token.value}")
        .header(HEADER_CONTENT_TYPE, "application/json")
        .header("X-EPJ-System", SOURCE_SYSTEM_VALUE)
        .POST(
          HttpRequest.BodyPublishers.ofString(
            mapper.writeValueAsString(HelseIndikatorRequest(patientId))
          )
        )
        .build(), HttpResponse.BodyHandlers.ofString()
    )

    if (response.statusCode() >= 300) {
      log.error(
        "Feil under henting av helseindikator. Status: {}\n Respons: {}",
        response.statusCode(),
        response.body()
      )
      return HELSEINDIKATOR_FEILSTATUS.right() // 0 er feilkode
    }

    val helseindikatorResponse: HelseIndikatorResponse =
      mapper.readValue(response.body(), HelseIndikatorResponse::class.java)

    val status: Int = helseindikatorResponse.status
    return if (status > 4) {
      EpjError("Henting av helseindikator", 500, "Mottok for høy statuskode ${status}").left()
    } else {
      status.right()
    }
  } catch (e: Exception) {
    return EpjError("Intern feil ved henting av helseindikator", 500, e.message, e).left()
  }
}
