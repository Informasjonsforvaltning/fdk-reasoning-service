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

    fun findHarvestedCatalog(catalogId: String): String? =
        readFileContent("$catalogPrefix-$catalogId")

    fun saveContent(content: String, filename: String) {
        informationModelGridFsTemplate.delete(Query(Criteria.where("filename").`is`(filename)))
        informationModelGridFsTemplate.store(gzip(content).byteInputStream(), filename)
    }

    fun findReasonedUnion() =
        readFileContent(filename = "$reasonedPrefix-$UNION_ID")

    fun findCatalog(fdkId: String) =
        readFileContent(filename = "$reasonedPrefix-$catalogPrefix-$fdkId")

    fun findCatalogs(): List<String> =
        informationModelGridFsTemplate
            .find(Query(Criteria.where("filename").regex("^$reasonedPrefix-$catalogPrefix-")))
            .toList()
            .map { informationModelGridFsTemplate.getResource(it) }
            .map { it.content.bufferedReader(Charsets.UTF_8) }
            .map { ungzip(it.readText()) }

    fun findInformationModel(fdkId: String) =
        readFileContent(filename = "$reasonedPrefix-$infoModelPrefix-$fdkId")

    fun saveReasonedUnion(content: String) =
        saveContent(content, filename = "$reasonedPrefix-$UNION_ID")

    fun saveCatalog(content: String, fdkId: String) =
        saveContent(content, filename = "$reasonedPrefix-$catalogPrefix-$fdkId")

    fun saveInformationModel(content: String, fdkId: String) =
        saveContent(content, filename = "$reasonedPrefix-$infoModelPrefix-$fdkId")

    private fun readFileContent(filename: String): String? =
        informationModelGridFsTemplate.findOne(Query(Criteria.where("filename").`is`(filename)))
            ?.let { informationModelGridFsTemplate.getResource(it) }
            ?.content
            ?.bufferedReader(Charsets.UTF_8)
            ?.use { ungzip(it.readText()) }
}
