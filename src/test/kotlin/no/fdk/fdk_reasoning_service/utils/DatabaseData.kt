package no.fdk.fdk_reasoning_service.utils

import no.fdk.fdk_reasoning_service.model.ExternalRDFData
import no.fdk.fdk_reasoning_service.model.TurtleDBO
import no.fdk.fdk_reasoning_service.service.gzip
import org.bson.Document

private val responseReader = TestResponseReader()

val EVENT_0_DATA = TurtleDBO(
    id = EVENT_ID_0,
    turtle = gzip(responseReader.readFile("event_0.ttl"))
)

val EVENT_1_DATA = TurtleDBO(
    id = EVENT_ID_1,
    turtle = gzip(responseReader.readFile("event_1.ttl"))
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
    losData = responseReader.parseTurtleFile("los.rdf", "RDFXML"))

fun TurtleDBO.mapDBO(): Document =
    Document()
        .append("_id", id)
        .append("turtle", turtle)
