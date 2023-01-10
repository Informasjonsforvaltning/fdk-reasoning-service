package no.fdk.fdk_reasoning_service.utils

import com.mongodb.ConnectionString
import com.mongodb.MongoClientSettings
import com.mongodb.client.MongoClient
import com.mongodb.client.MongoClients
import no.fdk.fdk_reasoning_service.utils.ApiTestContext.Companion.mongoContainer
import org.bson.codecs.configuration.CodecRegistries
import org.bson.codecs.pojo.PojoCodecProvider
import org.springframework.http.*
import java.io.BufferedReader
import java.net.HttpURLConnection
import java.net.URL


fun apiGet(port: Int, endpoint: String, acceptHeader: String?): Map<String,Any> {

    return try {
        val connection = URL("http://localhost:$port$endpoint").openConnection() as HttpURLConnection
        if(acceptHeader != null) connection.setRequestProperty("Accept", acceptHeader)
        connection.connect()

        if(isOK(connection.responseCode)) {
            val responseBody = connection.inputStream.bufferedReader().use(BufferedReader::readText)
            mapOf(
                "body"   to responseBody,
                "header" to connection.headerFields,
                "status" to connection.responseCode)
        } else {
            mapOf(
                "status" to connection.responseCode,
                "header" to " ",
                "body"   to " "
            )
        }
    } catch (e: Exception) {
        mapOf(
            "status" to e.toString(),
            "header" to " ",
            "body"   to " "
        )
    }
}

private fun isOK(response: Int?): Boolean = HttpStatus.resolve(response ?: 0)?.is2xxSuccessful ?: false

fun populateDB() {
    val connectionString = ConnectionString("mongodb://${MONGO_USER}:${MONGO_PASSWORD}@localhost:${mongoContainer.getMappedPort(MONGO_PORT)}/?authSource=admin&authMechanism=SCRAM-SHA-1")
    val pojoCodecRegistry = CodecRegistries.fromRegistries(
        MongoClientSettings.getDefaultCodecRegistry(), CodecRegistries.fromProviders(
            PojoCodecProvider.builder().automatic(true).build()))

    val client: MongoClient = MongoClients.create(connectionString)

    val eventDatabase = client.getDatabase("eventHarvester").withCodecRegistry(pojoCodecRegistry)
    val eventCollection = eventDatabase.getCollection("fdkCatalogTurtle")
    eventCollection.insertOne(EVENT_CATALOG_DATA.mapDBO())

    val publicServicesDatabase = client.getDatabase("publicServiceHarvester").withCodecRegistry(pojoCodecRegistry)
    val publicServiceCatalogsCollection = publicServicesDatabase.getCollection("fdkCatalogTurtle")
    publicServiceCatalogsCollection.insertOne(PUBLIC_SERVICE_CATALOG_0_DATA.mapDBO())
    publicServiceCatalogsCollection.insertOne(PUBLIC_SERVICE_CATALOG_1_DATA.mapDBO())

    val datasetsDatabase = client.getDatabase("datasetHarvester").withCodecRegistry(pojoCodecRegistry)
    val datasetsCollection = datasetsDatabase.getCollection("turtle")
    datasetsCollection.insertOne(DATASET_CATALOG_DATA.mapDBO())

    val conceptsDatabase = client.getDatabase("conceptHarvester").withCodecRegistry(pojoCodecRegistry)
    val conceptsCollection = conceptsDatabase.getCollection("turtle")
    conceptsCollection.insertOne(CONCEPT_COLLECTION_DATA.mapDBO())

    val dataServicesDatabase = client.getDatabase("dataServiceHarvester").withCodecRegistry(pojoCodecRegistry)
    val dataServicesCollection = dataServicesDatabase.getCollection("turtle")
    dataServicesCollection.insertOne(DATA_SERVICE_CATALOG_DATA.mapDBO())

    client.close()
}
