package no.helse.kj.devepj.tillitsrammeverk

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
import no.helse.kj.devepj.helseid.Organization

@JsonInclude(JsonInclude.Include.NON_NULL)
data class Patient(
  @JsonProperty("identifier") val identifier: IdentitetAttributt?,
  @JsonProperty("point_of_care") val pointOfCare: IdentitetAttributt?,
  @JsonProperty("department") val department: IdentitetAttributt?,
) {
  class Builder {
    private var identifier: IdentitetAttributt? = null
    private var pointOfCare: IdentitetAttributt? = null
    private var department: IdentitetAttributt? = null

    fun withIdentifier(patientIdentifier: String?, name: String?): Builder {
      this.identifier = IdentitetAttributt(
        patientIdentifier!!,
        "urn:oid:2.16.578.1.12.4.1.4.1"
      )
      return this
    }

    fun withPointOfCare(organizationIdentifier: String?, name: String?): Builder {
      this.pointOfCare = IdentitetAttributt(
        organizationIdentifier!!,
        "urn:oid:2.16.578.1.12.4.1.4.101"
      )
      return this
    }

    fun withPointOfCare(organization: Organization): Builder {
      this.pointOfCare = IdentitetAttributt(
        organization.id,
        "urn:oid:2.16.578.1.12.4.1.4.101"
      )
      return this
    }

    fun withDepartment(organizationIdentifier: String?, name: String?): Builder {
      this.department = IdentitetAttributt(
        organizationIdentifier!!,
        "urn:oid:2.16.578.1.12.4.1.4.102"
      )
      return this
    }

    fun build(): Patient {
      return Patient(this.identifier, this.pointOfCare, this.department)
    }
  }
}
