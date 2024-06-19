package no.helse.kj.devepj.helseid

import com.nimbusds.jose.JOSEException
import com.nimbusds.jose.JOSEObjectType
import com.nimbusds.jose.JWSAlgorithm
import com.nimbusds.jose.JWSHeader
import com.nimbusds.jose.crypto.RSASSASigner
import com.nimbusds.jose.jwk.KeyUse
import com.nimbusds.jose.jwk.RSAKey
import com.nimbusds.jose.util.Base64URL
import com.nimbusds.jwt.JWTClaimsSet
import com.nimbusds.jwt.SignedJWT
import com.nimbusds.oauth2.sdk.token.DPoPAccessToken
import com.nimbusds.openid.connect.sdk.Nonce
import java.net.URI
import java.nio.charset.StandardCharsets
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException
import java.security.interfaces.RSAPublicKey
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.Date
import java.util.UUID
import org.eclipse.jetty.http.HttpMethod

private const val HASHING_ALGORITHM_SHA_256 = "SHA-256"
private const val CLAIM_ACCESS_TOKEN_HASH = "ath"
private const val CLAIM_HTTP_METHOD = "htm"
private const val CLAIM_HTTP_URL = "htu"

private val generator = createGenerator()
private val digest = createMessageDigest()
private val JOSE_TYPE_DPOP_JWT = JOSEObjectType("dpop+jwt")
private val keyPair: KeyPair = generator.generateKeyPair()

/**
 * Velger å generere en unik JWK for å signere DPoP, her kan også samme JWK som ellers brukes mot HelseID benyttes
 */
private val DPOP_RSA_KEY_JWK: RSAKey = RSAKey.Builder(keyPair.public as RSAPublicKey)
  .privateKey(keyPair.private)
  .keyUse(KeyUse.SIGNATURE)
  .keyID(UUID.randomUUID().toString())
  .build()

fun createDPoPProof(
  method: HttpMethod,
  url: URI,
  token: DPoPAccessToken? = null,
  nonce: Nonce? = null,
): String {
  val issuedTime = Instant.now()
  val notBeforeTime = Instant.now()
  val expiry = Instant.now().plus(20, ChronoUnit.SECONDS)

  val jti = Base64URL.encode(UUID.randomUUID().toString()).toString()

  val builder = JWTClaimsSet.Builder()
    .jwtID(jti)
    .claim(CLAIM_HTTP_METHOD, method.toString())
    .claim(CLAIM_HTTP_URL, url.toString())
    .issueTime(Date.from(issuedTime))
    .notBeforeTime(Date.from(notBeforeTime))
    .expirationTime(Date.from(expiry))

  if (token != null) {
    builder.claim(CLAIM_ACCESS_TOKEN_HASH, hash(token.value))
  }

  if (nonce != null) {
    builder.claim("nonce", nonce.value)
  }

  val claims = builder.build()
  val signedJWT = SignedJWT(
    JWSHeader.Builder(JWSAlgorithm.RS256)
      .type(JOSE_TYPE_DPOP_JWT)
      .jwk(DPOP_RSA_KEY_JWK.toPublicJWK())
      .build(),
    claims
  )
  try {
    signedJWT.sign(RSASSASigner(DPOP_RSA_KEY_JWK))
  } catch (e: JOSEException) {
    throw RuntimeException(e)
  }
  return signedJWT.serialize()
}

private fun hash(originalString: String): String {
  return Base64URL.encode(digest.digest(originalString.toByteArray(StandardCharsets.US_ASCII))).toString()
}

private fun createMessageDigest(): MessageDigest {
  try {
    return MessageDigest.getInstance( /* algorithm = */HASHING_ALGORITHM_SHA_256)
  } catch (e: NoSuchAlgorithmException) {
    throw RuntimeException(e)
  }
}

private fun createGenerator(): KeyPairGenerator {
  try {
    val gen = KeyPairGenerator.getInstance("RSA")
    gen.initialize(2048)

    return gen
  } catch (e: NoSuchAlgorithmException) {
    throw RuntimeException(e)
  }
}