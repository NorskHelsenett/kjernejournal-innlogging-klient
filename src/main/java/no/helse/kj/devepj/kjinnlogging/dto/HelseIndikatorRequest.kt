package no.helse.kj.devepj.kjinnlogging.dto

import com.fasterxml.jackson.annotation.JsonProperty

data class HelseIndikatorRequest(@JsonProperty("fnr") val fnr: String)
