package no.fdk.fdk_reasoning_service.utils

import no.fdk.fdk_reasoning_service.model.ExternalRDFData
import no.fdk.fdk_reasoning_service.model.TurtleDBO
import no.fdk.fdk_reasoning_service.service.gzip
import org.bson.Document

private val responseReader = TestResponseReader()

val EVENT_CATALOG_DATA = TurtleDBO(
    id = EVENT_CATALOG_ID,
    turtle = gzip(responseReader.readFile("events.ttl"))
)

val PUBLIC_SERVICE_CATALOG_0_DATA = TurtleDBO(
    id = PUBLIC_SERVICE_CATALOG_0_ID,
    turtle = gzip(responseReader.readFile("public_service_0.ttl"))
)

val PUBLIC_SERVICE_CATALOG_1_DATA = TurtleDBO(
    id = PUBLIC_SERVICE_CATALOG_1_ID,
    turtle = gzip(responseReader.readFile("public_service_1.ttl"))
)

val DATASET_CATALOG_DATA = TurtleDBO(
    id = "catalog-$DATASET_CATALOG_ID",
    turtle = gzip(responseReader.readFile("datasets.ttl"))
)

val CONCEPT_COLLECTION_DATA = TurtleDBO(
    id = "collection-$CONCEPT_COLLECTION_ID",
    turtle = gzip(responseReader.readFile("concepts.ttl"))
)

val DATA_SERVICE_CATALOG_DATA = TurtleDBO(
    id = "catalog-$DATA_SERVICE_CATALOG_ID",
    turtle = gzip(responseReader.readFile("dataservices.ttl"))
)
val RDF_DATA = ExternalRDFData(
    orgData = responseReader.parseTurtleFile("orgs.ttl"),
    losData = responseReader.parseTurtleFile("los.ttl", "TURTLE"),
    eurovocs = responseReader.parseTurtleFile("eurovocs.ttl", "TURTLE"),
    dataThemes = responseReader.parseTurtleFile("data_themes.ttl", "TURTLE"),
    conceptStatuses = responseReader.parseTurtleFile("concept_statuses.ttl", "TURTLE"),
    conceptSubjects = responseReader.parseTurtleFile("concept_subjects.ttl", "TURTLE"))

fun TurtleDBO.mapDBO(): Document =
    Document()
        .append("_id", id)
        .append("turtle", turtle)
