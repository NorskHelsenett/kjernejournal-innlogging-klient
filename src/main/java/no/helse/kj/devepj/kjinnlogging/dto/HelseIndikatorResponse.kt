package no.helse.kj.devepj.kjinnlogging.dto

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty

@JsonIgnoreProperties(ignoreUnknown = true)
data class HelseIndikatorResponse(@JsonProperty("status") val status: Int, @JsonProperty("returTekst") val returTekst: String)

