package no.helse.kj.devepj.tillitsrammeverk

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty

@JsonInclude(JsonInclude.Include.NON_NULL)
data class KodeverkAttributt(@JsonProperty("code") val code: String, @JsonProperty("system") val system: String)
