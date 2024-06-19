package no.helse.kj.devepj.dto

import java.util.Arrays
import no.helse.kj.devepj.kjinnlogging.KjernejournalInnloggingGrunnlag
import no.helse.kj.devepj.tillitsrammeverk.PurposeOfUse
import no.helse.kj.devepj.tillitsrammeverk.PurposeOfUseDetails

enum class EpjGrunnlag(
  val kjGrunnlag: KjernejournalInnloggingGrunnlag,
  val purposeOfUse: PurposeOfUse,
  val purposeOfUseDetails: PurposeOfUseDetails,
) {
  VANLIG_BEHANDLING(
    KjernejournalInnloggingGrunnlag.SAMTYKKE,
    PurposeOfUse.TREAT,
    PurposeOfUseDetails.BEHANDLER
  ),
  AKUTT_BEHANDLING(
    KjernejournalInnloggingGrunnlag.AKUTT,
    PurposeOfUse.ETREAT,
    PurposeOfUseDetails.BLAALYS
  ),
  SKAL_BARE_SJEKKE_NOE(
    KjernejournalInnloggingGrunnlag.UNNTAK,
    PurposeOfUse.COC,
    PurposeOfUseDetails.ITSYSARB
  );

  companion object {
    fun fraKode(kode: String?): EpjGrunnlag {
      val `val` = Arrays.stream(values())
        .filter { env: EpjGrunnlag -> env.name.equals(kode, ignoreCase = true) }
        .findFirst()
        .orElse(VANLIG_BEHANDLING)
      return `val`
    }
  }
}
