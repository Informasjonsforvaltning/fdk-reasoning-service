package no.fdk.fdk_reasoning_service.service

import no.fdk.fdk_reasoning_service.cache.ReferenceDataCache
import no.fdk.fdk_reasoning_service.config.ApplicationURI
import no.fdk.fdk_reasoning_service.model.CatalogType
import no.fdk.fdk_reasoning_service.rdf.BR
import no.fdk.fdk_reasoning_service.rdf.CV
import no.fdk.fdk_reasoning_service.rdf.PROV
import org.apache.jena.rdf.model.*
import org.apache.jena.sparql.vocabulary.FOAF
import org.apache.jena.vocabulary.DCTerms
import org.apache.jena.vocabulary.RDF
import org.apache.jena.vocabulary.ROV
import org.springframework.stereotype.Service


@Service
class OrganizationService(
    private val referenceDataCache: ReferenceDataCache,
    private val uris: ApplicationURI,
    private val orgAdapter: OrganizationCatalogAdapter
): Reasoner {

    override fun reason(inputModel: Model, catalogType: CatalogType): Model {
        val orgData = referenceDataCache.organizations()
        if (orgData.isEmpty) throw Exception("Missing org data")

        val organizationPredicates = when (catalogType) {
            CatalogType.PUBLICSERVICES -> listOf(CV.hasCompetentAuthority, CV.ownedBy, DCTerms.publisher)
            else -> listOf(DCTerms.publisher)
        }
        val organizationResources = if (catalogType == CatalogType.DATASETS) {
            inputModel.extractOrganizations(organizationPredicates)
                .plus(inputModel.extreactQualifiedAttributionAgents())
        } else inputModel.extractOrganizations(organizationPredicates)
        return orgData.createModelOfOrganizationsWithOrgData(
            organizationURIs = organizationResources
                .filter { it.dctIdentifierIsInadequateAsOrgId() }
                .filter { it.isURIResource }
                .mapNotNull { it.uri }
                .toSet(),
            orgBaseURI = uris.orgExternal
        ).addNameWhenMissing(organizationResources.toSet(), inputModel, orgData, uris.orgExternal)
            .addOrgPathWhenMissing(organizationResources.toSet(), inputModel, orgData, uris.orgExternal)
    }

    private fun Model.addNameWhenMissing(
        organizations: Set<Resource>,
        catalogData: Model,
        orgData: Model,
        orgBaseURI: String
    ): Model {
        val names = ModelFactory.createDefaultModel()

        organizations.asSequence()
            .filterNot { it.hasProperty(FOAF.name) }
            .map {
                Pair(
                    it,
                    catalogData.dctIdentifierIfOrgId(it)
                        ?.let { orgId -> orgData.getResource(orgURI(orgId, orgBaseURI)) })
            }
            .filter { it.second != null }
            .forEach { names.add(names.createResource(it.first.uri), FOAF.name, it.second?.getProperty(FOAF.name)?.`object`) }

        add(names)
        return this
    }

    private fun Model.addOrgPathWhenMissing(
        organizations: Set<Resource>,
        catalogData: Model,
        orgData: Model,
        orgBaseURI: String
    ): Model {
        val orgPaths = ModelFactory.createDefaultModel()

        val organizationsMissingOrgPath = organizations.asSequence()
            .filterNot { it.hasProperty(BR.orgPath) }

        organizationsMissingOrgPath
            .map {
                Pair(
                    it,
                    catalogData.dctIdentifierIfOrgId(it)
                        ?.let { orgId -> orgData.getResource(orgURI(orgId, orgBaseURI)) })
            }
            .filter { it.second != null }
            .forEach { orgPaths.add(orgPaths.createResource(it.first.uri), BR.orgPath, it.second?.getProperty(BR.orgPath)?.`object`) }

        organizationsMissingOrgPath
            .filterNot { orgPaths.containsTriple("<${it.uri}>", "?p", "?o") }
            .map { Triple(it, catalogData.dctIdentifierIfOrgId(it), it.foafName()) }
            .forEach {
                getOrgPath(it.second, it.third, orgBaseURI)?.let { orgPath ->
                    orgPaths.add(orgPaths.createResource(it.first.uri), BR.orgPath, orgPath)
                }
            }

        add(orgPaths)
        return this
    }

    private fun Model.dctIdentifierIfOrgId(organization: Resource): String? {
        val orgId: String? = getProperty(organization, DCTerms.identifier)?.string
        val regex = Regex("""^[0-9]{9}$""")
        val matching = regex.findAll(orgId ?: "").toList()

        return if (matching.size == 1) orgId
        else null
    }

    private fun Resource.foafName(): String? {
        val names = listProperties(FOAF.name)?.toList()
        val nb = names?.find { it.language == "nb" }
        val nn = names?.find { it.language == "nn" }
        val en = names?.find { it.language == "en" }
        return when {
            names == null -> null
            names.isEmpty() -> null
            names.size == 1 -> names.first().string
            nb != null -> nb.string
            nn != null -> nn.string
            en != null -> en.string
            else -> names.first().string
        }
    }

    private fun getOrgPath(orgId: String?, orgName: String?, orgBaseURI: String): String? =
        when {
            orgId != null -> orgAdapter.orgPathAdapter(orgId, orgBaseURI)
            orgName != null -> orgAdapter.orgPathAdapter(orgName, orgBaseURI)
            else -> null
        }

    private fun orgURI(orgId: String, orgBaseURI: String) = "$orgBaseURI/$orgId"


    private fun Model.createModelOfOrganizationsWithOrgData(organizationURIs: Set<String>, orgBaseURI: String): Model {
        val model = ModelFactory.createDefaultModel()
        model.setNsPrefixes(nsPrefixMap)

        organizationURIs.map { Pair(it, orgResourceForOrganization(it, orgBaseURI)) }
            .filter { it.second != null }
            .forEach {
                model.createResource(it.first).addPropertiesFromOrgResource(it.second)
            }

        return model
    }

    private fun Model.orgResourceForOrganization(organizationURI: String, orgBaseURI: String): Resource? =
        orgIdFromURI(organizationURI)
            ?.let { "$orgBaseURI/${orgIdFromURI(organizationURI)}" }
            ?.let { uri ->
                if (containsTriple("<$uri>", "?p", "?o")) getResource(uri)
                else orgAdapter.downloadOrgData(uri)
            }

    private fun Resource.addPropertiesFromOrgResource(orgResource: Resource?) {
        if (orgResource != null) {
            safeAddProperty(RDF.type, orgResource.getProperty(RDF.type)?.`object`)
            safeAddProperty(DCTerms.identifier, orgResource.getProperty(DCTerms.identifier)?.`object`)
            safeAddProperty(BR.orgPath, orgResource.getProperty(BR.orgPath)?.`object`)
            safeAddProperty(ROV.legalName, orgResource.getProperty(ROV.legalName)?.`object`)
            safeAddProperty(FOAF.name, orgResource.getProperty(FOAF.name)?.`object`)
            safeAddProperty(ROV.orgType, orgResource.getProperty(ROV.orgType)?.`object`)
        }
    }


    private fun Model.extreactQualifiedAttributionAgents(): List<Resource> =
        listResourcesWithProperty(PROV.qualifiedAttribution)
            .toList()
            .flatMap { it.listProperties(PROV.qualifiedAttribution).toList() }
            .asSequence()
            .filter { it.isResourceProperty() }
            .map { it.resource }
            .flatMap { it.listProperties(PROV.agent).toList() }
            .filter { it.isResourceProperty() }
            .map { it.resource }
            .toList()

    private fun Model.extractOrganizations(organizationsPredicates: List<Property>): List<Resource> =
        organizationsPredicates.flatMap { organizationPredicate ->
            listResourcesWithProperty(organizationPredicate)
                .toList()
                .flatMap { it.listProperties(organizationPredicate).toList() }
                .asSequence()
                .filter { it.isResourceProperty() }
                .map { it.resource }
                .toList()
        }


    private fun Resource.dctIdentifierIsInadequateAsOrgId(): Boolean =
        listProperties(DCTerms.identifier)
            .toList()
            .map { it.`object` }
            .mapNotNull { it.extractOrganizationId() }
            .isEmpty()

    private fun RDFNode.extractOrganizationId(): String? =
        when {
            isURIResource -> orgIdFromURI(asResource().uri)
            isLiteral -> orgIdFromURI(asLiteral().string)
            else -> null
        }

    private fun orgIdFromURI(uri: String): String? {
        val regex = Regex("""[0-9]{9}""")
        val allMatching = regex.findAll(uri).toList()

        return if (allMatching.size == 1) allMatching.first().value
        else null
    }

}

