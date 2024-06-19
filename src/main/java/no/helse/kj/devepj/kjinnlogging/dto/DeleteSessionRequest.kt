package no.helse.kj.devepj.kjinnlogging.dto

import com.fasterxml.jackson.annotation.JsonProperty

data class DeleteSessionRequest(@JsonProperty("sessionId") val sessionId: String)
