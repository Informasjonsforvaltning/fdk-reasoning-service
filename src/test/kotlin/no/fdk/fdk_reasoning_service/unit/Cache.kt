package no.fdk.fdk_reasoning_service.unit

import io.mockk.every
import io.mockk.mockk
import no.fdk.fdk_reasoning_service.cache.ReferenceDataCache
import no.fdk.fdk_reasoning_service.config.ApplicationURI
import no.fdk.fdk_reasoning_service.utils.ApiTestContext
import no.fdk.fdk_reasoning_service.utils.TestResponseReader
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import kotlin.test.assertTrue

@Tag("unit")
class Cache : ApiTestContext() {
    private val uris: ApplicationURI = mockk()
    private val cache = ReferenceDataCache(uris)
    private val responseReader = TestResponseReader()

    @Test
    fun testCacheOrganizations() {
        every { uris.orgInternal } returns
            "http://localhost:5050/organizations" andThen
            "http://localhost:5050/404"

        val expected = responseReader.parseTurtleFile("rdf-data/organization-catalog/orgs.ttl")

        cache.updateOrganizations()
        assertTrue(expected.isIsomorphicWith(cache.organizations()), "able to update model")
        cache.updateOrganizations()
        assertTrue(expected.isIsomorphicWith(cache.organizations()), "keeps old data when update fails")
    }

    @Test
    fun testCacheLOS() {
        every { uris.los } returns
            "http://localhost:5050/reference-data/los" andThen
            "http://localhost:5050/404"

        val expected = responseReader.parseTurtleFile("rdf-data/reference-data/los.ttl")

        cache.updateLOS()
        assertTrue(expected.isIsomorphicWith(cache.los()), "able to update model")
        cache.updateLOS()
        assertTrue(expected.isIsomorphicWith(cache.los()), "keeps old data when update fails")
    }

    @Test
    fun testCacheEUROVOC() {
        every { uris.eurovocs } returns
            "http://localhost:5050/reference-data/eu/eurovocs" andThen
            "http://localhost:5050/404"

        val expected = responseReader.parseTurtleFile("rdf-data/reference-data/eurovocs.ttl")

        cache.updateEUROVOC()
        assertTrue(expected.isIsomorphicWith(cache.eurovocs()), "able to update model")
        cache.updateEUROVOC()
        assertTrue(expected.isIsomorphicWith(cache.eurovocs()), "keeps old data when update fails")
    }

    @Test
    fun testCacheDataThemes() {
        every { uris.dataThemes } returns
            "http://localhost:5050/reference-data/eu/data-themes" andThen
            "http://localhost:5050/404"

        val expected = responseReader.parseTurtleFile("rdf-data/reference-data/data_themes.ttl")

        cache.updateDataThemes()
        assertTrue(expected.isIsomorphicWith(cache.dataThemes()), "able to update model")
        cache.updateDataThemes()
        assertTrue(expected.isIsomorphicWith(cache.dataThemes()), "keeps old data when update fails")
    }

    @Test
    fun testCacheMobilityThemes() {
        every { uris.mobilityThemes } returns
            "http://localhost:5050/reference-data/eu/mobility-themes" andThen
            "http://localhost:5050/404"

        val expected = responseReader.parseTurtleFile("rdf-data/reference-data/mobility_themes.ttl")

        cache.updateMobilityThemes()
        assertTrue(expected.isIsomorphicWith(cache.mobilityThemes()), "able to update model")
        cache.updateMobilityThemes()
        assertTrue(expected.isIsomorphicWith(cache.mobilityThemes()), "keeps old data when update fails")
    }

    @Test
    fun testCacheConceptSubjects() {
        every { uris.conceptSubjects } returns
            "http://localhost:5050/reference-data/digdir/concept-subjects" andThen
            "http://localhost:5050/404"

        val expected = responseReader.parseTurtleFile("rdf-data/reference-data/concept_subjects.ttl")

        cache.updateConceptSubjects()
        assertTrue(expected.isIsomorphicWith(cache.conceptSubjects()), "able to update model")
        cache.updateConceptSubjects()
        assertTrue(expected.isIsomorphicWith(cache.conceptSubjects()), "keeps old data when update fails")
    }

    @Test
    fun testCacheConceptStatuses() {
        every { uris.conceptStatuses } returns
            "http://localhost:5050/reference-data/eu/concept-statuses" andThen
            "http://localhost:5050/404"

        val expected = responseReader.parseTurtleFile("rdf-data/reference-data/concept_statuses.ttl")

        cache.updateConceptStatuses()
        assertTrue(expected.isIsomorphicWith(cache.conceptStatuses()), "able to update model")
        cache.updateConceptStatuses()
        assertTrue(expected.isIsomorphicWith(cache.conceptStatuses()), "keeps old data when update fails")
    }

    @Test
    fun testCacheDistributionStatuses() {
        every { uris.distributionStatuses } returns
            "http://localhost:5050/reference-data/eu/distribution-statuses" andThen
            "http://localhost:5050/404"

        val expected = responseReader.parseTurtleFile("rdf-data/reference-data/distribution_statuses.ttl")

        cache.updateDistributionStatuses()
        assertTrue(expected.isIsomorphicWith(cache.distributionStatuses()), "able to update model")
        cache.updateDistributionStatuses()
        assertTrue(expected.isIsomorphicWith(cache.distributionStatuses()), "keeps old data when update fails")
    }

    @Test
    fun testCacheMobilityDataStandards() {
        every { uris.mobilityDataStandards } returns
            "http://localhost:5050/reference-data/mobility/data-standards" andThen
            "http://localhost:5050/404"

        val expected = responseReader.parseTurtleFile("rdf-data/reference-data/mobility_data_standards.ttl")

        cache.updateMobilityDataStandards()
        assertTrue(expected.isIsomorphicWith(cache.mobilityDataStandards()), "able to update model")
        cache.updateMobilityDataStandards()
        assertTrue(expected.isIsomorphicWith(cache.mobilityDataStandards()), "keeps old data when update fails")
    }

    @Test
    fun testCacheMobilityConditions() {
        every { uris.mobilityConditions } returns
            "http://localhost:5050/reference-data/mobility/conditions-for-access-and-usage" andThen
            "http://localhost:5050/404"

        val expected = responseReader.parseTurtleFile("rdf-data/reference-data/mobility_conditions.ttl")

        cache.updateMobilityConditions()
        assertTrue(expected.isIsomorphicWith(cache.mobilityConditions()), "able to update model")
        cache.updateMobilityConditions()
        assertTrue(expected.isIsomorphicWith(cache.mobilityConditions()), "keeps old data when update fails")
    }

}
