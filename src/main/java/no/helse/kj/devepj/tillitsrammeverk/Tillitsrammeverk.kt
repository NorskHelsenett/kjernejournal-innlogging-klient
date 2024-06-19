package no.helse.kj.devepj.tillitsrammeverk

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
import java.util.Collections

// https://github.com/NorskHelsenett/Tillitsrammeverk/blob/main/specs/informasjons_og_datamodell.md
@JsonInclude(JsonInclude.Include.NON_NULL)
class Tillitsrammeverk(
  @JsonProperty("practitioner") val practitioner: Practitioner?,
  @JsonProperty("care_relationship") val careRelationship: CareRelationship?,
  @JsonProperty("patients") val patients: List<Patient>?,
) : HelseIdAutorisasjonsDetaljer("nhn:tillitsrammeverk:parameters") {
  override fun toString(): String {
    return "Tillitsrammeverk{" +
            "practitioner=" + practitioner +
            ", careRelationship=" + careRelationship +
            ", patients=" + patients +
            '}'
  }

  class Builder {
    private var practitioner: Practitioner? = null
    private var careRelationship: CareRelationship? = null
    private val patients: MutableList<Patient> = ArrayList()

    fun withPractitioner(practitioner: Practitioner?): Builder {
      this.practitioner = practitioner
      return this
    }

    fun withCareRelationship(careRelationship: CareRelationship?): Builder {
      this.careRelationship = careRelationship
      return this
    }

    fun withPatient(patient: Patient): Builder {
      patients.add(patient)
      return this
    }

    fun build(): Tillitsrammeverk {
      return Tillitsrammeverk(practitioner, careRelationship, Collections.unmodifiableList(patients))
    }
  }
}
