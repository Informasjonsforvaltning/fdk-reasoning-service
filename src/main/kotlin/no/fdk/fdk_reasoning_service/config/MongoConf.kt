package no.fdk.fdk_reasoning_service.config

import com.mongodb.client.MongoClient
import com.mongodb.client.MongoClients
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.mongodb.core.MongoTemplate


@Configuration
open class MongoConf(
    private val connectionString: MongoConnectionString,
    private val databases: MongoDatabases
) {

    @Bean
    open fun mongoClient(): MongoClient {
        return MongoClients.create(connectionString.uri)
    }

    @Bean
    open fun eventMongoTemplate(): MongoTemplate {
        return MongoTemplate(mongoClient(), databases.events);
    }
}
