# fdk-reasoning-service
A service between harvesting of graphs and indexing that extends RDF triples on the harvested data.
The service is used to prepare it for usage in the frontend applications (i.e. fdk-portal, fdk-fulltext-search, rdf-diff-store, fdk-sparql-service, datafabrikken)

## Develop and run locally
### Requirements
- [maven](https://github.com/apache/maven) (recommended)
- java 17
- docker
- docker-compose

## Run tests
```
mvn verify
```

## Run locally
### docker-compose
```
docker-compose up -d rabbitmq
docker-compose up -d mongodb
mvn spring-boot:run -Dspring-boot.run.arguments="--spring.profiles.active=develop"
```

In another terminal:
```
docker-compose up -d
```
Alternatively run only one of the harvesters, e.g.
```
docker-compose up -d {relevant-harvester}
```
When reasoning is completed send request (log message contains `"Successfully sent reasoning completed message for {CATALOG-TYPE}"`)
```
curl http://localhost:8080/{catalog-type}
```

### Example - Datasets

```
docker-compose up -d rabbitmq
docker-compose up -d mongodb
mvn spring-boot:run -Dspring-boot.run.arguments="--spring.profiles.active=develop"
```

In a seperate terminal
```
docker-compose up -d dataset-harvester
```

Wait for successful reasoning, then run
```
curl http://localhost:8080/datasets
```
