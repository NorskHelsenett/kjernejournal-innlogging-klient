package no.helse.kj.devepj.tillitsrammeverk

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
import no.helse.kj.devepj.dto.Autorisasjon
import no.helse.kj.devepj.helseid.Organization

@JsonInclude(JsonInclude.Include.NON_NULL)
data class Practitioner(
  @JsonProperty("legal_entity") val legalEntity: IdentitetAttributt?,
  @JsonProperty("point_of_care") val pointOfCare: IdentitetAttributt?,
  @JsonProperty("department") @JsonInclude(JsonInclude.Include.NON_NULL) val department: IdentitetAttributt?,
  @JsonProperty("auhtorization") val authorization: KodeverkAttributt?
) {
  class Builder {
    private var legalEntity: IdentitetAttributt? = null
    private var pointOfCare: IdentitetAttributt? = null
    private var department: IdentitetAttributt? = null
    private var authorization: KodeverkAttributt? = null

    fun withLegalEntity(organization: Organization): Builder {
      this.legalEntity = IdentitetAttributt(
        organization.id,
        "urn:oid:2.16.578.1.12.4.1.4.101"
      )
      return this
    }

    fun withLegalEntity(organizationIdentifier: String?, name: String?): Builder {
      this.legalEntity = IdentitetAttributt(
        organizationIdentifier!!,
        "urn:oid:2.16.578.1.12.4.1.4.101"
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

    fun withDepartment(departmentIdentifier: String?, name: String?): Builder {
      this.department = IdentitetAttributt(
        departmentIdentifier!!,
        "urn:oid:2.16.578.1.12.4.1.4.102"
      )
      return this
    }

    fun withAuthorization(role: Autorisasjon): Builder {
      this.authorization = KodeverkAttributt(
        role.name,
        "urn:oid:2.16.578.1.12.4.1.1.9060"
      )
      return this
    }

    fun build(): Practitioner {
      return Practitioner(legalEntity, pointOfCare, department, authorization)
    }
  }
}