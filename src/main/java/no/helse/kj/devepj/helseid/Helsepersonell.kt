package no.helse.kj.devepj.helseid

import com.fasterxml.jackson.annotation.JsonProperty

data class Helsepersonell(@JsonProperty("navn") val navn: String, @JsonProperty("hprNr") val hprNr: String, @JsonProperty("pid") val pid: String)
