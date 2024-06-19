package no.helse.kj.devepj.dto

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import io.javalin.http.Context
import java.util.Arrays
import no.helse.kj.devepj.pages.EpjError

const val USER_PID_SESSION_KEY = "user/pid"
fun getUserPidFromContext(ctx: Context): Either<EpjError, String> {
  val userPid = ctx.sessionAttribute<String>(USER_PID_SESSION_KEY)
    ?: return EpjError("Feil på bruker", 500, "Mangler brukers fødselsnummer på sesjon", null).left()

  return userPid.right()
}

const val HPR_AUTORISASJON_SESSION_KEY = "user/autorisasjon"
fun getHprAutorisasjonFromContext(ctx: Context): Either<EpjError, Autorisasjon> {
  val autorisasjon = ctx.sessionAttribute<Autorisasjon>(HPR_AUTORISASJON_SESSION_KEY)
    ?: return EpjError("Feil på bruker", 500, "Mangler brukers autorisasjon på sesjon", null).left()

  return autorisasjon.right()
}
/*
Koder fra Volven 9060
https://volven.no/produkt.asp?id=509195&catID=3&subID=8
*/
enum class Autorisasjon(val beskrivelse: String) {
  AA("Ambulansearbeider"),
  AT("Apotektekniker"),
  AU("Audiograf"),
  BI("Bioingeniør"),
  ET("Ergoterapeut"),
  FA1("Provisorfarmasøyt"),
  FA2("Reseptarfarmasøyt"),
  FB("Fiskehelsebiolog"),
  FO("Fotterapeut"),
  FT("Fysioterapeut"),
  HE("Helsesekretær"),
  HF("Helsefagarbeider"),
  HP("Hjelpepleier"),
  JO("Jordmor"),
  KE("Klinisk ernæringsfysiolog"),
  KI("Kiropraktor"),
  LE("Lege"),
  NP("Naprapat"),
  OA("Omsorgsarbeider"),
  OI("Ortopediingeniør"),
  OP("Optiker"),
  OR("Ortoptist"),
  OS("Osteopat"),
  PE("Perfusjonist"),
  PM("Paramedisiner"),
  PS("Psykolog"),
  RA("Radiograf"),
  SP("Sykepleier"),
  TH("Tannhelsesekretær"),
  TL("Tannlege"),
  TP("Tannpleier"),
  TT("Tanntekniker"),
  VE("Veterinær"),
  VP("Vernepleier"),
  XX("Ukjent/uspesifisert"),
  MT("Manuellterapeut");


  companion object {
    fun fraKode(kode: String?, defaultValue: Autorisasjon): Autorisasjon {
      return Arrays.stream(values())
        .filter { autorisasjon: Autorisasjon -> autorisasjon.name.equals(kode, ignoreCase = true) }
        .findFirst()
        .orElse(defaultValue)
    }

    fun tilFrontend(): List<Map<String, String>> {
      return Arrays.stream(values()).map { autorisasjon: Autorisasjon ->
        java.util.Map.of(
          "value",
          autorisasjon.name,
          "label",
          autorisasjon.beskrivelse
        )
      }
        .toList()
    }
  }
}
