package no.helse.kj.devepj.util

import io.javalin.security.BasicAuthCredentials
import java.net.URI
import java.util.Optional.ofNullable
import java.util.Properties
import mu.KotlinLogging

private val log = KotlinLogging.logger {}

data class ConfigException(override val message: String, override val cause: Throwable? = null) :
  RuntimeException(message, cause)

class ConfigBuilder(path: String) : Properties() {
  init {
    load(object {}.javaClass.getResourceAsStream(path))
    System.getenv().entries.forEach {
      setProperty(it.key, it.value)
    }
  }

  private fun logProperty(key: String, defaultValue: Any?) {
    if (getProperty("config.debug.active", "true") != "true") return

    val originalValue = getProperty(key)

    // Maskerer presenterer default vedi hvis original er tom
    val value = originalValue ?: defaultValue.toString()

    // Maskerer hvis property er hemmelig
    val ingoreKeysWith = ofNullable(getProperty("config.debug.mask.list"))
      .map { it.split(",") }
      .map { it.toSet() }
      .orElseGet { setOf("password", "token", "secret", "credentials") }
    val result = if (ingoreKeysWith.any { key.contains(it) }) "*".repeat(value.length) else value

    // Logger property og indikerer om default verdi er brukt
    log.info { "Property $key=$result ${if (originalValue == null) "(default)" else ""}" }
  }

  fun <T> getTyped(key: String, defaultValue: T?, converter: (s: String) -> T): T {
    logProperty(key, defaultValue)

    if (!containsKey(key)) {
      if (defaultValue == null) {
        throw ConfigException("Property $key mangler.")
      }
      return defaultValue
    }
    try {
      return converter.invoke(getProperty(key))
    } catch (e: RuntimeException) {
      throw ConfigException("Property $key har sannsynligvis feil type", e)
    }
  }

  fun <T> getTypedList(
    key: String,
    defaultValue: List<String>? = null,
    delimiter: String = ",",
    converter: (s: String) -> T,
  ): List<T> {
    try {
      return getList(key, defaultValue, delimiter).map(converter)
    } catch (e: Exception) {
      throw ConfigException("Listeproperty $key har sannsynligvis en eller flere innslag av feil type", e)
    }
  }


  fun getBoolean(key: String, defaultValue: Boolean? = null): Boolean {
    return getTyped(key, defaultValue) { it.toBoolean() }
  }

  fun getInt(key: String, defaultValue: Int? = null): Int {
    return getTyped(key, defaultValue) { it.toInt() }
  }

  fun getLong(key: String, defaultValue: Long? = null): Long {
    return getTyped(key, defaultValue) { it.toLong() }
  }

  fun getString(key: String, defaultValue: String? = null): String {
    return getTyped(key, defaultValue) { it }
  }

  fun getURI(key: String, defaultValue: URI? = null): URI {
    return getTyped(key, defaultValue) { URI.create(it) }
  }

  fun getList(key: String, defaultValue: List<String>? = null, delimiter: String = ","): List<String> {
    return getTyped(key, defaultValue) { it.split(delimiter) }
  }


  fun getSet(key: String, defaultValue: Set<String>? = null, delimiter: String = ","): Set<String> {
    return getTyped(key, defaultValue) { it.split(delimiter).toSet() }
  }

  fun getBasicAuthList(
    key: String,
    defaultValue: List<BasicAuthCredentials>? = null,
    delimiter: String = ",",
  ): List<BasicAuthCredentials> {
    return getTyped(key, defaultValue) { raw ->
      raw.split(delimiter).map {
        val pair = it.split(":")

        BasicAuthCredentials(pair[0], pair[1])
      }
    }
  }

  fun toConfig(): Config {
    return Config(
      healthProbeActive = getBoolean("health.probe.active"),
      tlsActive = getBoolean("tls.active"),

      // HelseId
      helseIdStsUri = getURI("helseid.sts.uri"),
      helseIdRedirectUri = getURI("helseid.redirect.uri"),
      helseIdLogoutUri = getURI("helseid.logout.uri"),
      helseIdScopes = getSet("helseid.scopes"),
      helesIdResourceIndicators = getTypedList<URI>("helseid.resources", converter = { URI.create(it) }),

      // Web application API (mot Kjernejournal)
      webAppContextPath = getString("webapplication.contextpath"),
      webAppSourceSystem = getString("webapplication.sourcesystem"),


      // Utvikling (sett default verdier slik det skal stå i produksjon)
      weblogsActive = getBoolean("weblogs.expose", false),
      weblogsPath = getString("weblogs.path", "/logs"),

      aktiveMiljo = getSet("aktive_miljo"),

      legacyProps = this
    )
  }
}

data class Config(
  // Kubernetes
  val healthProbeActive: Boolean,
  val tlsActive: Boolean,

  // Utvikling/test
  val weblogsActive: Boolean,
  val weblogsPath: String,

  // HelseId
  val helseIdStsUri: URI,
  val helseIdRedirectUri: URI,
  val helseIdLogoutUri: URI,
  val helseIdScopes: Set<String>,
  val helesIdResourceIndicators: List<URI>,

  // Kjernejournal API
  val webAppContextPath: String,
  val webAppSourceSystem: String,

  val aktiveMiljo: Set<String>,

  val legacyProps: Properties,
)
