package no.helse.kj.devepj.tillitsrammeverk

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty

/* Et utvalg koder
https://hl7norway.github.io/AuditEvent/currentbuild/CodeSystem-carerelation.html
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
enum class PurposeOfUseDetails(@JsonProperty("display") val display: String) {
  HENV_BEH("Henvendelse fra pasientens behandler"),
  BEHANDLER("Bruker har behandlingsansvar for pasienten"),
  HENVISNING("Henvisning til vurdering"),
  BLAALYS("Blålys"),

  ITSYSARB("IT-Systemarbeid")
}
