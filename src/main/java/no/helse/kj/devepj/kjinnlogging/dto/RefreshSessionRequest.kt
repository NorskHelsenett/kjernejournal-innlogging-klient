package no.helse.kj.devepj.kjinnlogging.dto

import com.fasterxml.jackson.annotation.JsonProperty

data class RefreshSessionRequest(@JsonProperty("sessionId") val sessionId: String)
