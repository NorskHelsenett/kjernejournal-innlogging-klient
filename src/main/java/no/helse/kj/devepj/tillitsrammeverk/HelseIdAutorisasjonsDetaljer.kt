package no.helse.kj.devepj.tillitsrammeverk

import com.fasterxml.jackson.annotation.JsonProperty

abstract class HelseIdAutorisasjonsDetaljer(@JsonProperty("type") val type: String)