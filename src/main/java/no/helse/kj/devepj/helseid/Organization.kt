package no.helse.kj.devepj.helseid

import com.fasterxml.jackson.annotation.JsonProperty

data class Organization(@JsonProperty("id") val id: String, @JsonProperty("name") val name: String)
