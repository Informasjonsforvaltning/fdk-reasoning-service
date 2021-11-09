package no.fdk.fdk_reasoning_service.config

import com.mongodb.client.MongoClient
import com.mongodb.client.MongoClients
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.core.SimpleMongoClientDatabaseFactory
import org.springframework.data.mongodb.core.convert.DbRefResolver
import org.springframework.data.mongodb.core.convert.DefaultDbRefResolver
import org.springframework.data.mongodb.core.convert.MappingMongoConverter
import org.springframework.data.mongodb.core.mapping.MongoMappingContext
import org.springframework.data.mongodb.gridfs.GridFsTemplate


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
    open fun datasetMongoTemplate(): MongoTemplate {
        return MongoTemplate(mongoClient(), databases.datasets)
    }

    @Bean
    open fun eventMongoTemplate(): MongoTemplate {
        return MongoTemplate(mongoClient(), databases.events)
    }

    @Bean
    open fun conceptMongoTemplate(): MongoTemplate {
        return MongoTemplate(mongoClient(), databases.concepts)
    }

    @Bean
    open fun publicServiceMongoTemplate(): MongoTemplate {
        return MongoTemplate(mongoClient(), databases.publicServices)
    }

    @Bean
    open fun informationModelGridFsTemplate(): GridFsTemplate {
        val databaseFactory = SimpleMongoClientDatabaseFactory(mongoClient(), databases.infoModels)
        val dbRefResolver: DbRefResolver = DefaultDbRefResolver(databaseFactory)
        val converter = MappingMongoConverter(dbRefResolver, MongoMappingContext())
        converter.setCodecRegistryProvider(databaseFactory)
        return GridFsTemplate(databaseFactory, converter)
    }
}
