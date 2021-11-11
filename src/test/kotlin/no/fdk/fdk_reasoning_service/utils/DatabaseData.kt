package no.fdk.fdk_reasoning_service.utils

import no.fdk.fdk_reasoning_service.model.TurtleDBO
import no.fdk.fdk_reasoning_service.service.gzip
import org.bson.Document

private val responseReader = TestResponseReader()

val EVENT_UNION_DATA = TurtleDBO(
    id = "event-union-graph",
    turtle = gzip(responseReader.readFile("events.ttl"))
)

fun TurtleDBO.mapDBO(): Document =
    Document()
        .append("_id", id)
        .append("turtle", turtle)
