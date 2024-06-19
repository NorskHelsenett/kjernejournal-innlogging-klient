package no.helse.kj.devepj.tillitsrammeverk

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty

/* Koder hentet fra
https://github.com/NorskHelsenett/Tillitsrammeverk/blob/main/specs/forretningsregler_for_bruk_av_attestering.md#32-forretningsregler-for-attributtet-purpose-of-use
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
enum class PurposeOfUse(@JsonProperty("display") val display: String) {
  TREAT("Treatment"),
  ETREAT("Emergency Treatment"),
  COC("Coordination of care"),
  BTG("Break the glass")
}
