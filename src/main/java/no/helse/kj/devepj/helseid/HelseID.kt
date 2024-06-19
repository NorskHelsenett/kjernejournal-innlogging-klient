package no.helse.kj.devepj.helseid

import arrow.core.Either
import arrow.core.flatMap
import arrow.core.left
import arrow.core.right
import com.nimbusds.jose.JOSEException
import com.nimbusds.jose.JWSAlgorithm
import com.nimbusds.jose.JWSHeader
import com.nimbusds.jose.crypto.RSASSASigner
import com.nimbusds.jose.jwk.JWK
import com.nimbusds.jose.jwk.KeyType
import com.nimbusds.jwt.JWT
import com.nimbusds.jwt.JWTClaimsSet
import com.nimbusds.jwt.SignedJWT
import com.nimbusds.oauth2.sdk.AuthorizationCode
import com.nimbusds.oauth2.sdk.AuthorizationCodeGrant
import com.nimbusds.oauth2.sdk.AuthorizationGrant
import com.nimbusds.oauth2.sdk.AuthorizationRequest
import com.nimbusds.oauth2.sdk.PushedAuthorizationRequest
import com.nimbusds.oauth2.sdk.PushedAuthorizationResponse
import com.nimbusds.oauth2.sdk.RefreshTokenGrant
import com.nimbusds.oauth2.sdk.ResponseType
import com.nimbusds.oauth2.sdk.Scope
import com.nimbusds.oauth2.sdk.TokenRequest
import com.nimbusds.oauth2.sdk.auth.JWTAuthenticationClaimsSet
import com.nimbusds.oauth2.sdk.auth.PrivateKeyJWT
import com.nimbusds.oauth2.sdk.http.HTTPResponse
import com.nimbusds.oauth2.sdk.id.Audience
import com.nimbusds.oauth2.sdk.id.ClientID
import com.nimbusds.oauth2.sdk.id.State
import com.nimbusds.oauth2.sdk.pkce.CodeChallengeMethod
import com.nimbusds.oauth2.sdk.pkce.CodeVerifier
import com.nimbusds.oauth2.sdk.token.DPoPAccessToken
import com.nimbusds.oauth2.sdk.token.RefreshToken
import com.nimbusds.openid.connect.sdk.AuthenticationRequest
import com.nimbusds.openid.connect.sdk.Nonce
import com.nimbusds.openid.connect.sdk.OIDCTokenResponse
import com.nimbusds.openid.connect.sdk.claims.ACR
import io.javalin.http.Context
import java.io.IOException
import java.net.MalformedURLException
import java.net.URI
import java.net.URL
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.security.PrivateKey
import java.time.Instant
import java.util.Arrays
import java.util.Date
import java.util.Objects
import java.util.UUID
import java.util.stream.Collectors
import java.util.stream.Stream
import mu.KotlinLogging
import no.helse.kj.devepj.pages.EpjError
import org.apache.commons.lang3.StringUtils
import org.eclipse.jetty.http.HttpMethod
import org.slf4j.MDC

private val log = KotlinLogging.logger {}

const val HELSEID_CONFIGURATION_SESSION_KEY = "helseid/configuration"
const val HELSEID_PAR_SESSION_KEY = "helseid/par"
const val HELSEID_TOKENS_SESSION_KEY = "helseid/tokens"

private const val CLAIM_PRACTITIONER_HPR = "helseid://claims/hpr/hpr_number"
private const val CLAIM_PRACTITIONER_GIVEN_NAME = "given_name"
private const val CLAIM_PRACTITIONER_MIDDLE_NAME = "middle_name"
private const val CLAIM_PRACTITIONER_FAMILY_NAME = "family_name"
private const val CLAIM_PRACTITIONER_ID = "helseid://claims/identity/pid"
private val TOKEN_EXPIRY_SECONDS: Long = 60L
private val IDP: List<String> = listOf("idp:testidp-oidc")

fun getHelseIdConfigurationFromContext(ctx: Context): Either<EpjError, HelseIdConfiguration> {
  val helseIdConfiguration = ctx.sessionAttribute<HelseIdConfiguration>(HELSEID_CONFIGURATION_SESSION_KEY)
    ?: return EpjError("HelseID operasjon feilet", 500, "Mangler kontekst på sesjon", null).left()

  return helseIdConfiguration.right()
}

fun getHelseIdTokensFromContext(ctx: Context): Either<EpjError, HelseIdTokenBundle> {
  val helseIdTokenBundle = ctx.sessionAttribute<HelseIdTokenBundle>(HELSEID_TOKENS_SESSION_KEY)
    ?: return EpjError("HelseID operasjon feilet", 500, "Mangler tokens på sesjon", null).left()

  return helseIdTokenBundle.right()
}

data class HelseIdConfiguration(
  val stsUrl: URI,
  val redirectUri: URI,
  val logoutUri: URI,

  val privateKeyJwk: JWK,
  val clientId: String,

  val scopes: Set<String> = emptySet(),
  val resourceIndicators: List<URI> = emptyList(),
)

data class HelseIdParResponse(val state: State, val codeVerifier: CodeVerifier, val redirectURI: URI)

fun pushedAuthenticationRequest(
  configuration: HelseIdConfiguration,
  brukerFnr: String?,
): Either<EpjError, HelseIdParResponse> {
  val state = State()
  val codeVerifier = CodeVerifier()
  try {
    val clientAuth = PrivateKeyJWT(
      JWTAuthenticationClaimsSet(ClientID(configuration.clientId), Audience(configuration.stsUrl)),
      JWSAlgorithm.RS256,
      toPrivateKey(configuration.privateKeyJwk),
      configuration.privateKeyJwk.keyID,
      null
    )

    val authzRequestBuilder = AuthenticationRequest.Builder(
      AuthenticationRequest(
        configuration.stsUrl,
        ResponseType(ResponseType.Value.CODE),
        Scope.parse(configuration.scopes),
        ClientID(configuration.clientId),
        configuration.redirectUri,
        state,
        Nonce()
      )
    )
      .resources(*configuration.resourceIndicators.toTypedArray())
      .codeChallenge(codeVerifier, CodeChallengeMethod.S256)

    // Logg inn automatisk hos HelseID sin TestIDP hvis brukerFnr er satt
    // Da slipper man å se HelseID sin innloggingsside i det hele tatt
    if (brukerFnr != null) {
      authzRequestBuilder.acrValues(IDP.stream().map { value: String? -> ACR(value) }
        .collect(Collectors.toList()))
      authzRequestBuilder.customParameter("test_security_level", "4")
      authzRequestBuilder.customParameter("test_pid", brukerFnr)
    }

    val parHttpResponse =
      PushedAuthorizationRequest(
        URI.create("${configuration.stsUrl}/connect/par"),
        clientAuth,
        authzRequestBuilder.build()
      )
        .toHTTPRequest()
        .send()

    val parResponse = PushedAuthorizationResponse.parse(parHttpResponse)
    if (!parResponse.indicatesSuccess()) {
      val errorObject = parResponse.toErrorResponse().errorObject
      return EpjError(
        "Feil ved pushed authorization request",
        errorObject.httpStatusCode,
        errorObject.description,
        cause = null
      ).left()
    }

    val successResponse = parResponse.toSuccessResponse()

    // Bygg opp en URI som inneholder requestUri fra PAR-responsen. Brukeren vil så bli redirectet til denne URLen
    // for å fullføre innloggingen hos HelseID
    val redirectURI = AuthorizationRequest.Builder(successResponse.requestURI, ClientID(configuration.clientId))
      .endpointURI(URI.create("${configuration.stsUrl}/connect/authorize"))
      .build()
      .toURI()

    return HelseIdParResponse(
      state,
      codeVerifier,
      redirectURI
    ).right()

  } catch (e: Exception) {
    return EpjError("Kune ikke gjøre pushed authentication request", 500, e.message, e).left()
  }
}

fun hentTokensMedAuthorizationCodeGrant(
  configuration: HelseIdConfiguration,
  parResponse: HelseIdParResponse,
  code: String?,
  state: String?,
): Either<EpjError, HelseIdTokenBundle> {
  if (!parResponse.state.value.equals(state, ignoreCase = true)) {
    return EpjError(
      "Fikk feil state fra HelseID",
      500,
      "Forventet " + parResponse.state.value + " men fikk " + state,
      cause = null
    ).left()
  }
  val grant =
    AuthorizationCodeGrant(AuthorizationCode(code), configuration.redirectUri, parResponse.codeVerifier)

  return hentHelseIdToken(configuration, grant).flatMap { processHelseIdResponse(it) }
}

fun logoutHelseID(configuration: HelseIdConfiguration, tokenBundle: HelseIdTokenBundle): Either<EpjError, URL> {
  val url: String =
    "${configuration.stsUrl}/connect/endsession?id_token_hint=" + tokenBundle.idToken.serialize() + "&post_logout_redirect_uri=" + URLEncoder.encode(
      configuration.logoutUri.toString(),
      StandardCharsets.UTF_8
    )
  try {
    return URI.create(url).toURL().right()
  } catch (e: MalformedURLException) {
    return EpjError("Feil format på url $url", 500, e.message, e).left()
  }
}

fun logoutHelseIdWithContext(ctx: Context): Either<EpjError, URL> {
  return getHelseIdConfigurationFromContext(ctx).flatMap { helseIdConfig ->
    getHelseIdTokensFromContext(ctx).flatMap { tokens ->
      logoutHelseID(helseIdConfig, tokens)
    }
  }
}

fun hentTokensMedRefreshTokenGrant(
  configuration: HelseIdConfiguration,
  refreshToken: RefreshToken,
  authorizationDetails: Any?,
): Either<EpjError, HelseIdTokenBundle> {
  return hentHelseIdToken(configuration, RefreshTokenGrant(refreshToken), authorizationDetails)
    .flatMap { processHelseIdResponse(it) }
}

fun refreshTokenWithContext(ctx: Context, authorizationDetails: Any? = null): Either<EpjError, HelseIdTokenBundle> {
  val attest = authorizationDetails ?: ctx.sessionAttribute<Any>("patient/attest")

  return getHelseIdConfigurationFromContext(ctx).flatMap { helseIdConfig ->
    getHelseIdTokensFromContext(ctx).flatMap { tokens ->
      hentTokensMedRefreshTokenGrant(
        helseIdConfig,
        tokens.refreshToken,
        attest
      )
    }
  }
}

fun hentInnloggetBruker(accessToken: DPoPAccessToken): Either<EpjError, Helsepersonell> {
  try {
    val claims = SignedJWT.parse(accessToken.value).jwtClaimsSet

    val name = Stream.of(
      claims.getStringClaim(CLAIM_PRACTITIONER_GIVEN_NAME),
      claims.getStringClaim(CLAIM_PRACTITIONER_MIDDLE_NAME),
      claims.getStringClaim(CLAIM_PRACTITIONER_FAMILY_NAME)
    ).filter { obj: String? -> Objects.nonNull(obj) }
      .collect(Collectors.joining(" "))

    return Helsepersonell(
      if (!StringUtils.isBlank(name)) name else "NAVN_MANGLER_I_HELSEID_TOKEN",
      claims.getStringClaim(CLAIM_PRACTITIONER_HPR),
      claims.getStringClaim(CLAIM_PRACTITIONER_ID)
    ).right()
  } catch (e: Exception) {
    return EpjError("Feil under henting av innlogget bruker", 500, e.message, e).left()
  }
}


private fun toPrivateKey(jwk: JWK?): PrivateKey {
  if (jwk!!.keyType == KeyType.RSA) {
    val rsaKey = jwk.toRSAKey()
    return rsaKey.toPrivateKey()
  } else if (jwk.keyType == KeyType.EC) {
    val ecKey = jwk.toECKey()
    return ecKey.toPrivateKey()
  } else {
    throw RuntimeException("JWK type er ikke RSA eller EC")
  }
}

private fun hentHelseIdToken(
  configuration: HelseIdConfiguration,
  grant: AuthorizationGrant,
  authorizationDetails: Any? = null,
): Either<EpjError, HTTPResponse> {
  return _hentHelseIdToken(configuration, grant, createClaims(configuration, authorizationDetails), null)
    .flatMap { httpResponse ->
      if (httpResponse.indicatesSuccess()) {
        return httpResponse.right()
      }

      // I DPoP kan en request feile med BAD REQUEST for å så gjenta requesten med en en fornyet nonce som ble returnert
      // https://www.rfc-editor.org/rfc/rfc9449#name-authorization-server-provid
      if (httpResponse.statusCode == HTTPResponse.SC_BAD_REQUEST && httpResponse.body.contains("use_dpop_nonce")) {
        val nonce = httpResponse.dPoPNonce
        return _hentHelseIdToken(configuration, grant, createClaims(configuration, authorizationDetails), nonce)
      }

      return EpjError(
        "Feil under henting av token fra HelseID",
        httpResponse.statusCode,
        httpResponse.body,
        cause = null
      ).left()
    }
}

private fun createClaims(configuration: HelseIdConfiguration, authorizationDetails: Any?): JWTClaimsSet {
  val builder = JWTClaimsSet.Builder()
    .audience("${configuration.stsUrl}/connect/token")
    .jwtID(UUID.randomUUID().toString())
    .subject(configuration.clientId)
    .issuer(configuration.clientId)
    .notBeforeTime(Date())
    .expirationTime(Date.from(Instant.now().plusSeconds(TOKEN_EXPIRY_SECONDS)))

  if (authorizationDetails != null) {
    builder.claim("authorization_details", authorizationDetails)
  }

  return builder.build()
}

private fun _hentHelseIdToken(
  configuration: HelseIdConfiguration,
  grant: AuthorizationGrant,
  claims: JWTClaimsSet,
  dpopNonce: Nonce?,
): Either<EpjError, HTTPResponse> {
  val privateKeyJWT: PrivateKeyJWT
  try {
    val signedJWT = SignedJWT(
      JWSHeader.Builder(JWSAlgorithm.RS256).build(),
      claims
    )
    signedJWT.sign(RSASSASigner(configuration.privateKeyJwk.toRSAKey()))
    privateKeyJWT = PrivateKeyJWT(signedJWT)
  } catch (e: JOSEException) {
    return EpjError("Feil ved signering av token request", 500, e.message, e).left()
  }
  val tokenUri = URI.create("${configuration.stsUrl}/connect/token")
  val tokenRequest = TokenRequest(
    tokenUri,
    privateKeyJWT,
    grant,
    Scope.parse(configuration.scopes),
    Arrays.asList(*configuration.resourceIndicators.toTypedArray()),
    null
  )

  try {
    val proof: String = createDPoPProof(HttpMethod.POST, tokenUri, nonce = dpopNonce)
    val httpRequest = tokenRequest.toHTTPRequest()
    httpRequest.accept = "application/json"
    httpRequest.setHeader("DPoP", proof)
    httpRequest.setHeader("event.id", MDC.get("event.id"))

    return httpRequest.send().right()
  } catch (e: IOException) {
    return EpjError("Feil ved henting av HelseID-token", 500, e.message, e).left()
  }
}

data class HelseIdTokenBundle(val refreshToken: RefreshToken, val idToken: JWT, val accessToken: DPoPAccessToken)

private fun processHelseIdResponse(httpResponse: HTTPResponse): Either<EpjError, HelseIdTokenBundle> {
  if (!httpResponse.indicatesSuccess()) {
    log.error("Failed response: {}\n{}", httpResponse.statusCode, httpResponse.body)
    return EpjError(
      "Forespørsel til HelseID feilet",
      httpResponse.statusCode,
      httpResponse.body,
      cause = null
    ).left()
  }

  val tokenResponse: OIDCTokenResponse
  try {
    tokenResponse = OIDCTokenResponse.parse(httpResponse)
  } catch (e: com.nimbusds.oauth2.sdk.ParseException) {
    return EpjError("Feil ved innlesing av respons", 500, e.message, e).left()
  }

  if (!tokenResponse.indicatesSuccess()) {
    return EpjError(
      "Token-forespørsel feilet",
      httpResponse.statusCode,
      httpResponse.body,
      cause = null
    ).left()
  }

  val idTokenJwt = tokenResponse.oidcTokens.idToken
  val accessToken = tokenResponse.oidcTokens.dPoPAccessToken
  val refreshToken = tokenResponse.oidcTokens.refreshToken ?: return EpjError(
    "Prossesering av respons fra HelseID feilet",
    500,
    "Mangler refresh token, er offline_access med som scope?",
    null
  ).left()

  return HelseIdTokenBundle(
    refreshToken,
    idTokenJwt,
    accessToken
  ).right()
}