package no.helse.kj.devepj.helseid

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.ObjectMapper
import com.nimbusds.jose.jwk.JWK
import io.javalin.http.Context
import mu.KotlinLogging
import no.helse.kj.devepj.pages.EpjError
import no.helse.kj.devepj.util.YAML_MAPPER
import no.helse.kj.devepj.util.readFiles
import java.nio.file.Paths

private val log = KotlinLogging.logger {}

const val HELSEID_CLIENT_SESSION_KEY = "helseid/client"
data class HelseIdClient(
  val id: String,
  val name: String,
  val parentOrg: Organization,
  val childOrg: Organization,
  val jwk: JWK,
) {
  companion object {
    private val DEFAULT_HELSEID_CLIENT_DIR = System.getProperty("user.dir") + "/clients"
    private val jsonMapper = ObjectMapper()

    val HELSEID_CLIENT_MAP: Map<String, HelseIdClient>

    init {
      val clientDirectory = System.getenv("HELSEID_CLIENT_DIRECTORY") ?: DEFAULT_HELSEID_CLIENT_DIR
      log.info { "Henter HelseID-klienter fra: $clientDirectory" }
      val pairs = readFiles(Paths.get(clientDirectory), ::readClient)
        .map { Pair(it.id, it) }
        .toList()
      this.HELSEID_CLIENT_MAP = mapOf(*pairs.toTypedArray())
    }

    private fun readClient(content: String): HelseIdClient {
      val rawClient = YAML_MAPPER.readValue(content, RawHelseIdClient::class.java)

      return HelseIdClient(
        rawClient.id,
        rawClient.name,
        rawClient.parent_org,
        rawClient.child_org,
        JWK.parse(jsonMapper.writeValueAsString(rawClient.jwk))
      )
    }

    data class RawHelseIdClient(
      @JsonProperty("id") val id: String,
      @JsonProperty("name") val name: String,
      @JsonProperty("parent_org") val parent_org: Organization,
      @JsonProperty("child_org") val child_org: Organization,
      @JsonProperty("jwk") val jwk: Any,
    )

    fun fromContext(ctx: Context): Either<EpjError, HelseIdClient> {
      val helseIdClient = ctx.sessionAttribute<HelseIdClient>(HELSEID_CLIENT_SESSION_KEY)
        ?: return EpjError("HelseID operasjon feilet", 500, "Mangler klient på sesjon", null).left()

      return helseIdClient.right()
    }

    fun fromId(id: String?): HelseIdClient {
      return HELSEID_CLIENT_MAP[id] ?: HELSEID_CLIENT_MAP.entries.first().value
    }

    fun toFrontend(): List<Map<String, String>> {
      return HELSEID_CLIENT_MAP.entries
        .map { klient ->
          mapOf(
            Pair("value", klient.key),
            Pair("label", klient.value.name)
          )
        }.toList()
    }
  }
}
