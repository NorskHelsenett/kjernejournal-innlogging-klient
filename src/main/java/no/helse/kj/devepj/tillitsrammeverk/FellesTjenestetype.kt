package no.helse.kj.devepj.tillitsrammeverk

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty

@JsonInclude(JsonInclude.Include.NON_NULL)
enum class FellesTjenestetype(@JsonProperty("kode") val kode: String, @JsonProperty("beskrivelse") val beskrivelse: String) {
  BEDRIFT("01", "Bedriftshelsetjeneste"),
  ALLMENN("05", "Allmennlege"),
  SAKSBEHANDLING("8", "Saksbehandling pasientopplysninger"),

  FORSKRIVNING("FR", "Forskrivning"),

  FASTLEGE_IKKE_FAST("KX17", "Fastlege, liste uten fast lege")
}
