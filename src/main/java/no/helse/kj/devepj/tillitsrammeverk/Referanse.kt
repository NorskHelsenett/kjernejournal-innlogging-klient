package no.helse.kj.devepj.tillitsrammeverk

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty

@JsonInclude(JsonInclude.Include.NON_NULL)
data class Referanse(@JsonProperty("id") val id: String, @JsonProperty("user_selected") val userSelected: Boolean)
