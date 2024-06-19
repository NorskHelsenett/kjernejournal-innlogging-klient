package no.helse.kj.devepj.kjinnlogging.dto

import com.fasterxml.jackson.annotation.JsonProperty

data class CreateSessionRequest(
  @JsonProperty("ehr_code_challenge") val ehr_code_challenge: String,
  @JsonProperty("claims") val claims: Map<String, Any>,
)
