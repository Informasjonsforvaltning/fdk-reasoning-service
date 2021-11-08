package no.fdk.fdk_reasoning_service.repository

import no.fdk.fdk_reasoning_service.service.UNION_ID
import no.fdk.fdk_reasoning_service.service.gzip
import no.fdk.fdk_reasoning_service.service.ungzip
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query
import org.springframework.data.mongodb.gridfs.GridFsTemplate
import org.springframework.stereotype.Service

private const val reasonedPrefix = "reasoned"
private const val catalogPrefix = "catalog"
private const val infoModelPrefix = "infoModel"

@Service
class InformationModelRepository(private val informationModelGridFsTemplate: GridFsTemplate) {

    fun findHarvestedUnion(): String? =
        readFileContent("information-model-catalogs-union-graph")

    private fun saveReasonedModel(content: String, filename: String) {
        informationModelGridFsTemplate.delete(Query(Criteria.where("filename").`is`(filename)))
        informationModelGridFsTemplate.store(gzip(content).byteInputStream(), filename)
    }

    fun saveReasonedUnion(content: String) =
        saveReasonedModel(content, filename = "$reasonedPrefix-$UNION_ID")

    fun saveCatalog(content: String, fdkId: String) =
        saveReasonedModel(content, filename = "$reasonedPrefix-$catalogPrefix-$fdkId")

    fun saveInformationModel(content: String, fdkId: String) =
        saveReasonedModel(content, filename = "$reasonedPrefix-$infoModelPrefix-$fdkId")

    private fun readFileContent(filename: String): String? =
        informationModelGridFsTemplate.findOne(Query(Criteria.where("filename").`is`(filename)))
            ?.let { informationModelGridFsTemplate.getResource(it) }
            ?.content
            ?.bufferedReader(Charsets.UTF_8)
            ?.use { ungzip(it.readText()) }
}
