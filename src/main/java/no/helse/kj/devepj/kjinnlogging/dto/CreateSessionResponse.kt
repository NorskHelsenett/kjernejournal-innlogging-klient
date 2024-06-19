package no.helse.kj.devepj.kjinnlogging.dto

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty

@JsonIgnoreProperties(ignoreUnknown = true)
data class CreateSessionResponse(
  @JsonProperty("code") val code: String,
  @JsonProperty("sessionId") val sessionId: String,
)
