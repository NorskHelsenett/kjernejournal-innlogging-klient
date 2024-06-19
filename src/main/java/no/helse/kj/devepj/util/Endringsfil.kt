package no.helse.kj.devepj.util

import com.nimbusds.jose.util.Base64URL
import java.io.BufferedReader
import java.io.InputStreamReader
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException
import java.util.Objects
import no.helse.kj.devepj.pages.Login

data class Endring(val value: String)
data class Endringsfil(val endringer: List<Endring>, val hash: String)


fun endringerTilFrontend(): Endringsfil {
  val endringer = lesEndringer()

  return Endringsfil(
    lesEndringer().map { Endring(it) },
    hashString(endringer.joinToString("\n"))
  )
}


fun hashString(input: String): String {
  var digestor: MessageDigest? = null
  try {
    digestor = MessageDigest.getInstance("SHA-256")
  } catch (e: NoSuchAlgorithmException) {
    // Den finnes, lover
  }

  val digest = digestor!!.digest(input.toByteArray(StandardCharsets.UTF_8))
  return Base64URL.encode(digest).toString()
}

fun lesEndringer(): List<String> {
  try {
    Objects.requireNonNull(
      Login::class.java.classLoader
        .getResourceAsStream("assets/endringer")
    ).use { inputStream ->
      val reader =
        BufferedReader(InputStreamReader(inputStream, StandardCharsets.UTF_8))
      return reader.lines().toList()
    }
  } catch (e: Exception) {
    return emptyList()
  }
}