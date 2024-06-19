package no.helse.kj.devepj.tillitsrammeverk

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty

@JsonInclude(JsonInclude.Include.NON_NULL)
data class CareRelationship(
  @JsonProperty("healthcare_service") val healthcareService: KodeverkAttributt?,
  @JsonProperty("purpose_of_use") val purposeOfUse: KodeverkAttributt?,
  @JsonProperty("purpose_of_use_details") val purposeOfUseDetails: KodeverkAttributt?,
  @JsonProperty("decision_ref") val decisionRef: Referanse?,
) {
  class Builder {
    private var healthcareService: KodeverkAttributt? = null
    private var purposeOfUse: KodeverkAttributt? = null
    private var purposeOfUseDetails: KodeverkAttributt? = null
    private var decisionRef: Referanse? = null

    fun withHealthcareService(tjenestetype: FellesTjenestetype): Builder {
      this.healthcareService = KodeverkAttributt(
        tjenestetype.kode,
        "urn:oid:2.16.578.1.12.4.1.1.8666"
      )
      return this
    }

    fun withPurposeOfUse(purposeOfUse: PurposeOfUse): Builder {
      this.purposeOfUse = KodeverkAttributt(
        purposeOfUse.name,
        "urn:oid:2.16.840.1.113883.1.11.20448"
      )
      return this
    }

    fun withPurposeOfUseDetails(purposeOfUseDetails: PurposeOfUseDetails): Builder {
      this.purposeOfUseDetails = KodeverkAttributt(
        purposeOfUseDetails.name,
        "urn:AuditEventHL7Norway/CodeSystem/carerelation"
      )
      return this
    }

    fun withDecisionRef(id: String, userSelected: Boolean): Builder {
      this.decisionRef = Referanse(
        id,
        userSelected
      )
      return this
    }

    fun build(): CareRelationship {
      return CareRelationship(
        this.healthcareService,
        this.purposeOfUse,
        this.purposeOfUseDetails,
        this.decisionRef
      )
    }
  }
}