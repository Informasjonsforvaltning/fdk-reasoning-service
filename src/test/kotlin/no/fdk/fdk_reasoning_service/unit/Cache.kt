package no.fdk.fdk_reasoning_service.unit

import no.fdk.fdk_reasoning_service.cache.ReferenceDataCache
import no.fdk.fdk_reasoning_service.config.ApplicationURI
import no.fdk.fdk_reasoning_service.utils.ApiTestContext
import no.fdk.fdk_reasoning_service.utils.CONCEPT_REPORT
import no.fdk.fdk_reasoning_service.utils.RDF_DATA
import no.fdk.fdk_reasoning_service.utils.TEST_DATE
import no.fdk.fdk_reasoning_service.utils.TestResponseReader
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import kotlin.test.assertTrue

@Tag("unit")
class Cache : ApiTestContext() {
    private val uris: ApplicationURI = mock()
    private val cache = ReferenceDataCache(uris)
    private val responseReader = TestResponseReader()

    @Test
    fun testCacheOrganizations() {
        whenever(uris.orgInternal)
            .thenReturn("http://localhost:5000/organizations")
            .thenReturn("http://localhost:5000/404")

        val expected = responseReader.parseTurtleFile("orgs.ttl")

        cache.updateOrganizations()
        assertTrue(expected.isIsomorphicWith(cache.organizations()), "able to update model")
        cache.updateOrganizations()
        assertTrue(expected.isIsomorphicWith(cache.organizations()), "keeps old data when update fails")
    }

    @Test
    fun testCacheLOS() {
        whenever(uris.los)
            .thenReturn("http://localhost:5000/reference-data/los")
            .thenReturn("http://localhost:5000/404")

        val expected = responseReader.parseTurtleFile("los.ttl")

        cache.updateLOS()
        assertTrue(expected.isIsomorphicWith(cache.los()), "able to update model")
        cache.updateLOS()
        assertTrue(expected.isIsomorphicWith(cache.los()), "keeps old data when update fails")
    }

    @Test
    fun testCacheEUROVOC() {
        whenever(uris.eurovocs)
            .thenReturn("http://localhost:5000/reference-data/eu/eurovocs")
            .thenReturn("http://localhost:5000/404")

        val expected = responseReader.parseTurtleFile("eurovocs.ttl")

        cache.updateEUROVOC()
        assertTrue(expected.isIsomorphicWith(cache.eurovocs()), "able to update model")
        cache.updateEUROVOC()
        assertTrue(expected.isIsomorphicWith(cache.eurovocs()), "keeps old data when update fails")
    }

    @Test
    fun testCacheDataThemes() {
        whenever(uris.dataThemes)
            .thenReturn("http://localhost:5000/reference-data/eu/data-themes")
            .thenReturn("http://localhost:5000/404")

        val expected = responseReader.parseTurtleFile("data_themes.ttl")

        cache.updateDataThemes()
        assertTrue(expected.isIsomorphicWith(cache.dataThemes()), "able to update model")
        cache.updateDataThemes()
        assertTrue(expected.isIsomorphicWith(cache.dataThemes()), "keeps old data when update fails")
    }

    @Test
    fun testCacheConceptSubjects() {
        whenever(uris.conceptSubjects)
            .thenReturn("http://localhost:5000/reference-data/digdir/concept-subjects")
            .thenReturn("http://localhost:5000/404")

        val expected = responseReader.parseTurtleFile("concept_subjects.ttl")

        cache.updateConceptSubjects()
        assertTrue(expected.isIsomorphicWith(cache.conceptSubjects()), "able to update model")
        cache.updateConceptSubjects()
        assertTrue(expected.isIsomorphicWith(cache.conceptSubjects()), "keeps old data when update fails")
    }

    @Test
    fun testCacheConceptStatuses() {
        whenever(uris.conceptStatuses)
            .thenReturn("http://localhost:5000/reference-data/eu/concept-statuses")
            .thenReturn("http://localhost:5000/404")

        val expected = responseReader.parseTurtleFile("concept_statuses.ttl")

        cache.updateConceptStatuses()
        assertTrue(expected.isIsomorphicWith(cache.conceptStatuses()), "able to update model")
        cache.updateConceptStatuses()
        assertTrue(expected.isIsomorphicWith(cache.conceptStatuses()), "keeps old data when update fails")
    }

}
