package no.helse.kj.devepj.kjinnlogging

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import com.fasterxml.jackson.annotation.JsonProperty
import io.javalin.http.Context
import mu.KotlinLogging
import no.helse.kj.devepj.pages.EpjError
import no.helse.kj.devepj.util.YAML_MAPPER
import no.helse.kj.devepj.util.readFiles
import java.net.URI
import java.nio.file.Paths

private val log = KotlinLogging.logger {}


const val KJERNEJOURNAL_CONFIGURATION_SESSION_KEY = "kjernejournal/configuration"
data class KjernejournalConfiguration(
  @JsonProperty("id") val id: String,
  @JsonProperty("name") val name: String,
  @JsonProperty("webUri") val webUri: URI,
  @JsonProperty("statusApiUri") val statusApiUri: URI,
  @JsonProperty("innloggingApiUri") val innloggingApiUri: URI,
) {
  companion object {
    private val DEFAULT_AKTIVE_MILJO = "LOCAL,ST1,ST3"
    var KJERNEJOURNAL_ENVIRONMENT_MAP: Map<String, KjernejournalConfiguration>
    init {
      try {
        val DEFAULT_KJ_CONFIG_DIR = System.getProperty("user.dir") + "/kjernejournal"
        val aktiveMiljo = (System.getenv("aktive_miljo") ?: DEFAULT_AKTIVE_MILJO).uppercase().split(",")

        val configDirectory = System.getenv("KJERNEJOURNAL_CONFIG_DIRECTORY") ?: DEFAULT_KJ_CONFIG_DIR
        log.info { "Henter Kjernejournal-testmiljøkonfig fra: $configDirectory" }
        val pairs = readFiles(Paths.get(configDirectory), ::readKjConfig)
          // Filtrerer ut inaktive Kjernejournal-miljøer
          .filter { aktiveMiljo.contains(it.id.uppercase()) }
          .map { Pair(it.id, it) }
          .toList()

        KJERNEJOURNAL_ENVIRONMENT_MAP = mapOf(*pairs.toTypedArray())
      } catch (e: Exception) {
        log.error(e) { "AUCH" }
        KJERNEJOURNAL_ENVIRONMENT_MAP = emptyMap()
      }
    }

    private fun readKjConfig(raw: String): KjernejournalConfiguration {
      return YAML_MAPPER.readValue(raw, KjernejournalConfiguration::class.java)
    }

    fun fromId(id: String?): KjernejournalConfiguration {
      return KJERNEJOURNAL_ENVIRONMENT_MAP[id] ?: KJERNEJOURNAL_ENVIRONMENT_MAP.entries.first().value
    }

    fun toFrontend(): List<Map<String, String>> {
      return KJERNEJOURNAL_ENVIRONMENT_MAP.entries.stream()
        .map { e ->
          mapOf(
            Pair("value", e.key),
            Pair("label", e.value.name)
          )
        }
        .toList()
    }

    fun fromContext(ctx: Context): Either<EpjError, KjernejournalConfiguration> {
      val environment = ctx.sessionAttribute<KjernejournalConfiguration>(KJERNEJOURNAL_CONFIGURATION_SESSION_KEY)
        ?: return EpjError("Feil i miljø", 500, "Mangler kjernejournalmiljø på sesjon", null).left()

      return environment.right()
    }
  }
}

