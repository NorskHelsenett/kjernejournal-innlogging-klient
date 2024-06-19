package no.helse.kj.devepj.kjinnlogging

import java.util.Arrays

enum class KjernejournalInnloggingGrunnlag {
  SAMTYKKE,
  AKUTT,
  UNNTAK;

  companion object {
    fun fraKode(kode: String?): KjernejournalInnloggingGrunnlag {
      return Arrays.stream(values())
        .filter { env: KjernejournalInnloggingGrunnlag -> env.name.equals(kode, ignoreCase = true) }
        .findFirst()
        .orElse(SAMTYKKE)
    }
  }
}
